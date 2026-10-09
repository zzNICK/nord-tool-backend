package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.CaixinhaComprovante;
import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.domain.CaixinhaLancamento;
import br.com.nord_tool_backend.domain.CaixinhaResponsavel;
import br.com.nord_tool_backend.dto.CaixinhaComprovanteDto;
import br.com.nord_tool_backend.dto.CaixinhaLancamentoDto;
import br.com.nord_tool_backend.dto.CaixinhaListaDto;
import br.com.nord_tool_backend.dto.CaixinhaResponsavelDto;
import br.com.nord_tool_backend.dto.CaixinhaResumoDto;
import br.com.nord_tool_backend.form.CaixinhaLancamentoForm;
import br.com.nord_tool_backend.form.CaixinhaMarcacaoForm;
import br.com.nord_tool_backend.form.CaixinhaResponsavelForm;
import br.com.nord_tool_backend.repository.CaixinhaRepository;
import br.com.nord_tool_backend.service.CaixinhaService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaixinhaServiceImpl implements CaixinhaService {

    static final String MSG_CONFLITO = "O lançamento mudou. Sincronize e tente novamente.";
    static final List<String> SITUACOES = Arrays.asList("TODOS", "A_PAGAR", "PAGO", "NAO_LANCADO");

    static final int MAX_COMPROVANTES_POR_LANCAMENTO = 10;

    private final CaixinhaRepository repository;
    private final ArmazenamentoService armazenamento;
    private final int maxComprovanteBytes;
    private final AutorizacaoService autorizacao;

    public CaixinhaServiceImpl(CaixinhaRepository repository, ArmazenamentoService armazenamento,
                               @Value("${nord-tool.caixinha.max-comprovante-bytes:5242880}") int maxComprovanteBytes, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.armazenamento = armazenamento;
        this.maxComprovanteBytes = maxComprovanteBytes;
        this.autorizacao = autorizacao;
    }

    // ---------- listagem e resumo ----------

    @Override
    @Transactional(readOnly = true)
    public CaixinhaListaDto listar(CaixinhaFiltro filtro) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.LEITURA);
        validarFiltro(filtro);
        List<CaixinhaLancamentoDto> lancamentos = repository.listarLancamentos(filtro).stream()
                .map(CaixinhaLancamentoDto::de).collect(Collectors.toList());
        return new CaixinhaListaDto(lancamentos, listarResponsaveis());
    }

    @Override
    @Transactional(readOnly = true)
    public CaixinhaResumoDto resumir(CaixinhaFiltro filtro) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.LEITURA);
        validarFiltro(filtro);
        CaixinhaRepository.Totais t = repository.resumir(filtro);
        BigDecimal total = t.total == null ? BigDecimal.ZERO : t.total;
        BigDecimal pago = t.pago == null ? BigDecimal.ZERO : t.pago;
        return new CaixinhaResumoDto(total, pago, total.subtract(pago), t.qtLancamentos, t.qtPagos, t.qtLancamentos - t.qtPagos);
    }

    // ---------- lançamentos ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaixinhaLancamentoDto criar(CaixinhaLancamentoForm form) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        String requisicao = uuidObrigatorio(form.getCdRequisicao(), "Informe o identificador da requisição (cdRequisicao)");
        // Reenvio: devolve o lançamento original sem validar de novo nem duplicar.
        java.util.Optional<Long> existente = repository.buscarLancamentoPorRequisicao(requisicao);
        if (existente.isPresent()) return CaixinhaLancamentoDto.de(lancamento(existente.get()));

        validarResponsavel(form.getIdResponsavel(), null);
        CaixinhaLancamento l = converter(form);
        l.setCdRequisicao(requisicao);
        Long id = repository.inserirLancamento(l)
                // Corrida: outra requisição com o mesmo UUID acabou de gravar.
                .orElseGet(() -> repository.buscarLancamentoPorRequisicao(requisicao).orElseThrow(() ->
                        new EntradaInvalidaException("Não foi possível salvar o lançamento")));
        return CaixinhaLancamentoDto.de(lancamento(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaixinhaLancamentoDto alterar(Long id, CaixinhaLancamentoForm form) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        CaixinhaLancamento atual = lancamento(id);
        validarResponsavel(form.getIdResponsavel(), atual.getIdResponsavel());
        CaixinhaLancamento novo = converter(form);
        novo.setId(id);
        if (repository.alterarLancamento(novo, versao) == 0) throw conflito();
        return CaixinhaLancamentoDto.de(lancamento(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaixinhaLancamentoDto marcar(Long id, CaixinhaMarcacaoForm form) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        CaixinhaLancamento atual = lancamento(id);
        boolean lancado = form.getLancado() != null ? form.getLancado() : Boolean.TRUE.equals(atual.getInLancado());
        boolean pago = form.getPago() != null ? form.getPago() : Boolean.TRUE.equals(atual.getInPago());
        if (repository.marcarLancamento(id, lancado, pago, versao) == 0) throw conflito();
        return CaixinhaLancamentoDto.de(lancamento(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id, Integer nrVersao) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        int versao = versaoObrigatoria(nrVersao);
        lancamento(id);
        List<Long> arquivos = repository.listarComprovantes(id).stream()
                .map(CaixinhaComprovante::getIdArquivo).collect(Collectors.toList());
        if (repository.deletarLancamento(id, versao) == 0) throw conflito();
        // As linhas dos comprovantes saem em cascata; os arquivos armazenados são apagados explicitamente.
        arquivos.forEach(armazenamento::apagar);
    }

    // ---------- responsáveis ----------

    @Override
    @Transactional(readOnly = true)
    public List<CaixinhaResponsavelDto> listarResponsaveis() {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.LEITURA);
        return repository.listarResponsaveis().stream().map(CaixinhaResponsavelDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaixinhaResponsavelDto criarResponsavel(CaixinhaResponsavelForm form) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        String nome = nomeResponsavel(form.getNmResponsavel(), null);
        CaixinhaResponsavel r = new CaixinhaResponsavel();
        r.setNmResponsavel(nome);
        r.setInAtivo(form.getInAtivo() == null || form.getInAtivo());
        Long id = repository.inserirResponsavel(r);
        return CaixinhaResponsavelDto.de(responsavel(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaixinhaResponsavelDto atualizarResponsavel(Long id, CaixinhaResponsavelForm form) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        CaixinhaResponsavel atual = responsavel(id);
        if (form.getNmResponsavel() != null) atual.setNmResponsavel(nomeResponsavel(form.getNmResponsavel(), id));
        if (form.getInAtivo() != null) atual.setInAtivo(form.getInAtivo());
        repository.alterarResponsavel(atual);
        return CaixinhaResponsavelDto.de(responsavel(id));
    }

    // ---------- comprovantes ----------

    @Override
    @Transactional(readOnly = true)
    public List<CaixinhaComprovanteDto> listarComprovantes(Long idLancamento) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.LEITURA);
        lancamento(idLancamento);
        return repository.listarComprovantes(idLancamento).stream().map(CaixinhaComprovanteDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaixinhaComprovanteDto anexarComprovante(Long idLancamento, String nomeArquivo, byte[] bytes, String cdRequisicao) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        lancamento(idLancamento);
        String requisicao = uuidOpcional(cdRequisicao);
        if (requisicao != null) {
            // Reenvio do mesmo arquivo: devolve o comprovante original, sem gravar outro PDF.
            java.util.Optional<Long> existente = repository.buscarComprovantePorRequisicao(requisicao);
            if (existente.isPresent()) return CaixinhaComprovanteDto.de(comprovante(existente.get()));
        }
        if (repository.listarComprovantes(idLancamento).size() >= MAX_COMPROVANTES_POR_LANCAMENTO) {
            throw new EntradaInvalidaException("O lançamento já tem o máximo de " + MAX_COMPROVANTES_POR_LANCAMENTO + " comprovantes");
        }
        String contentType = ArquivoValidador.validarComprovantePdf(nomeArquivo, bytes, maxComprovanteBytes);
        Long idArquivo = armazenamento.salvar(nomeArquivo, contentType, bytes);
        Long id = repository.inserirComprovante(idLancamento, idArquivo, requisicao);
        return CaixinhaComprovanteDto.de(comprovante(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarComprovante(Long idComprovante) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.LEITURA);
        CaixinhaComprovante c = comprovante(idComprovante);
        long versao = c.getDhCriacao() == null ? 0L : c.getDhCriacao().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new ArquivoDownload(armazenamento.abrir(c.getIdArquivo()), versao);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirComprovante(Long idComprovante) {
        autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA);
        CaixinhaComprovante c = comprovante(idComprovante);
        repository.deletarComprovante(idComprovante);
        armazenamento.apagar(c.getIdArquivo());
    }

    // ---------- auxiliares ----------

    private CaixinhaLancamento lancamento(Long id) {
        return repository.buscarLancamento(id)
                .orElseThrow(() -> new NaoEncontradoException("Lançamento não encontrado"));
    }

    private CaixinhaResponsavel responsavel(Long id) {
        return repository.buscarResponsavel(id)
                .orElseThrow(() -> new NaoEncontradoException("Responsável não encontrado"));
    }

    private CaixinhaComprovante comprovante(Long id) {
        return repository.buscarComprovante(id)
                .orElseThrow(() -> new NaoEncontradoException("Comprovante não encontrado"));
    }

    private static NordException conflito() {
        return new ConflitoException(MSG_CONFLITO);
    }

    private static NordException invalido(String mensagem) {
        return new EntradaInvalidaException(mensagem);
    }

    private static int versaoObrigatoria(Integer nrVersao) {
        if (nrVersao == null) throw invalido("Informe a versão do lançamento (nrVersao)");
        return nrVersao;
    }

    private static void validarFiltro(CaixinhaFiltro filtro) {
        if (filtro == null) return;
        if (filtro.getSituacao() != null && !SITUACOES.contains(filtro.getSituacao())) {
            throw invalido("Situação inválida. Use TODOS, A_PAGAR, PAGO ou NAO_LANCADO.");
        }
        if (filtro.getDe() != null && filtro.getAte() != null && filtro.getDe().isAfter(filtro.getAte())) {
            throw invalido("A data inicial não pode ser posterior à data final");
        }
    }

    /** O responsável deve existir e estar ativo (manter o já associado ao lançamento é permitido). */
    private void validarResponsavel(Long idResponsavel, Long idAtual) {
        CaixinhaResponsavel r = repository.buscarResponsavel(idResponsavel)
                .orElseThrow(() -> invalido("Responsável não encontrado"));
        boolean mesmo = idAtual != null && idAtual.equals(idResponsavel);
        if (!mesmo && Boolean.FALSE.equals(r.getInAtivo())) throw invalido("O responsável está inativo");
    }

    private String nomeResponsavel(String nome, Long idIgnorar) {
        String limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty()) throw invalido("Informe o nome do responsável");
        boolean duplicado = repository.listarResponsaveis().stream()
                .anyMatch(r -> r.getNmResponsavel().equalsIgnoreCase(limpo) && !r.getId().equals(idIgnorar));
        if (duplicado) throw invalido("Já existe um responsável com este nome");
        return limpo;
    }

    private static String uuidObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.trim().isEmpty()) throw invalido(mensagem);
        return uuidOpcional(valor);
    }

    private static String uuidOpcional(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        try {
            return UUID.fromString(valor.trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw invalido("Identificador da requisição inválido (esperado um UUID)");
        }
    }

    static CaixinhaLancamento converter(CaixinhaLancamentoForm form) {
        CaixinhaLancamento l = new CaixinhaLancamento();
        l.setDtLancamento(form.getDtLancamento());
        l.setIdResponsavel(form.getIdResponsavel());
        l.setTxInsumo(form.getTxInsumo().trim());
        l.setVlValor(form.getVlValor());
        l.setInLancado(Boolean.TRUE.equals(form.getInLancado()));
        l.setInPago(Boolean.TRUE.equals(form.getInPago()));
        return l;
    }
}
