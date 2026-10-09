package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.domain.enums.RegraDataEnum;
import br.com.nord_tool_backend.domain.enums.SituacaoLancamentoEnum;
import br.com.nord_tool_backend.domain.enums.TipoFluxoEnum;
import br.com.nord_tool_backend.domain.enums.TipoProjecaoEnum;
import br.com.nord_tool_backend.dto.FinanceiroCategoriaDto;
import br.com.nord_tool_backend.dto.FinanceiroLancamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroListaDto;
import br.com.nord_tool_backend.dto.FinanceiroPessoaDto;
import br.com.nord_tool_backend.dto.FinanceiroResumoDto;
import br.com.nord_tool_backend.form.FinanceiroCategoriaForm;
import br.com.nord_tool_backend.form.FinanceiroLancamentoForm;
import br.com.nord_tool_backend.form.FinanceiroPessoaForm;
import br.com.nord_tool_backend.form.FinanceiroRealizadoForm;
import br.com.nord_tool_backend.repository.FinanceiroProjecaoRepository;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import br.com.nord_tool_backend.service.FinanceiroService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinanceiroServiceImpl implements FinanceiroService {

    static final String MSG_CONFLITO = "O lançamento mudou. Sincronize e tente novamente.";
    static final int MAX_PARCELAS = 120;

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final FinanceiroRepository repository;
    private final FinanceiroProjecaoRepository projecaoRepository;
    private final Clock clock;
    private final AutorizacaoService autorizacao;

    public FinanceiroServiceImpl(FinanceiroRepository repository, FinanceiroProjecaoRepository projecaoRepository, Clock clock, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.projecaoRepository = projecaoRepository;
        this.clock = clock;
        this.autorizacao = autorizacao;
    }

    // ---------- listagem e resumo ----------

    @Override
    @Transactional(readOnly = true)
    public FinanceiroListaDto listar(FinanceiroFiltro filtro) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        validarFiltro(filtro);
        List<FinanceiroLancamentoDto> lancamentos = repository.listarLancamentos(filtro).stream()
                .map(FinanceiroLancamentoDto::de).collect(Collectors.toList());
        return new FinanceiroListaDto(lancamentos, resumo(filtro), listarPessoas(), listarCategorias());
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceiroResumoDto resumir(FinanceiroFiltro filtro) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        validarFiltro(filtro);
        return resumo(filtro);
    }

    private FinanceiroResumoDto resumo(FinanceiroFiltro filtro) {
        FinanceiroRepository.Totais t = repository.resumir(filtro);
        BigDecimal entradas = zero(t.entradas);
        BigDecimal saidas = zero(t.saidas);
        return new FinanceiroResumoDto(entradas, saidas, entradas.subtract(saidas), zero(t.entradasRealizadas),
                zero(t.saidasRealizadas), t.qtLancamentos, t.qtRealizados);
    }

    // ---------- lançamentos ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FinanceiroLancamentoDto> criar(FinanceiroLancamentoForm form, Long idUsuario) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        String requisicao = uuidObrigatorio(form.getCdRequisicao());
        int parcelas = form.getQtParcelas() == null ? 1 : form.getQtParcelas();
        if (parcelas < 1 || parcelas > MAX_PARCELAS) throw invalido("O número de parcelas deve ser de 1 a " + MAX_PARCELAS);

        // Reenvio: devolve o que já foi criado, sem validar de novo nem duplicar.
        if (repository.buscarLancamentoPorRequisicao(requisicao).isPresent()) {
            return jaCriados(requisicao, parcelas);
        }

        FinanceiroCategoria categoria = validarCategoria(form.getIdCategoria(), null);
        validarPessoa(form.getIdPessoa(), null);
        LocalDate competenciaBase = competencia(form);
        for (int i = 0; i < parcelas; i++) exigirMesAberto(competenciaBase.plusMonths(i));

        List<FinanceiroLancamentoDto> criados = new ArrayList<>();
        for (int i = 0; i < parcelas; i++) {
            FinanceiroLancamento l = converter(form);
            l.setCdRequisicao(requisicaoDaParcela(requisicao, i));
            l.setDtCompetencia(competenciaBase.plusMonths(i));
            l.setDtLancamento(form.getDtLancamento().plusMonths(i));
            l.setIdUsuarioCriacao(idUsuario);
            if (parcelas > 1) {
                l.setNrParcela(i + 1);
                l.setQtParcela(parcelas);
            }
            Long id = repository.inserirLancamento(l)
                    // Corrida: outra requisição com o mesmo UUID acabou de gravar.
                    .orElseGet(() -> repository.buscarLancamentoPorRequisicao(l.getCdRequisicao()).orElseThrow(() ->
                            new EntradaInvalidaException("Não foi possível salvar o lançamento")));
            registrarLeitura(categoria, id, l.getVlLancamento(), idUsuario);
            criados.add(FinanceiroLancamentoDto.de(lancamento(id)));
        }
        return criados;
    }

    private List<FinanceiroLancamentoDto> jaCriados(String requisicao, int parcelas) {
        List<FinanceiroLancamentoDto> lista = new ArrayList<>();
        for (int i = 0; i < parcelas; i++) {
            repository.buscarLancamentoPorRequisicao(requisicaoDaParcela(requisicao, i))
                    .ifPresent(id -> lista.add(FinanceiroLancamentoDto.de(lancamento(id))));
        }
        return lista;
    }

    /** A 1ª parcela usa o UUID do cliente; as demais, UUIDs derivados dele (estáveis, para o reenvio não duplicar). */
    static String requisicaoDaParcela(String requisicao, int indice) {
        if (indice == 0) return requisicao;
        return UUID.nameUUIDFromBytes((requisicao + "#" + indice).getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroLancamentoDto alterar(Long id, FinanceiroLancamentoForm form, Long idUsuario) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        FinanceiroLancamento atual = lancamento(id);
        FinanceiroCategoria categoria = validarCategoria(form.getIdCategoria(), atual.getIdCategoria());
        validarPessoa(form.getIdPessoa(), atual.getIdPessoa());
        FinanceiroLancamento novo = converter(form);
        novo.setId(id);
        novo.setDtCompetencia(competencia(form));
        exigirMesAberto(atual.getDtCompetencia());
        exigirMesAberto(novo.getDtCompetencia());
        if (repository.alterarLancamento(novo, versao) == 0) throw conflito();
        registrarLeitura(categoria, id, novo.getVlLancamento(), idUsuario);
        return FinanceiroLancamentoDto.de(lancamento(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroLancamentoDto marcarRealizado(Long id, FinanceiroRealizadoForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        exigirMesAberto(lancamento(id).getDtCompetencia());
        if (repository.marcarRealizado(id, Boolean.TRUE.equals(form.getInRealizado()), versao) == 0) throw conflito();
        return FinanceiroLancamentoDto.de(lancamento(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id, Integer nrVersao) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        int versao = versaoObrigatoria(nrVersao);
        exigirMesAberto(lancamento(id).getDtCompetencia());
        if (repository.deletarLancamento(id, versao) == 0) throw conflito();
    }

    // ---------- pessoas ----------

    @Override
    @Transactional(readOnly = true)
    public List<FinanceiroPessoaDto> listarPessoas() {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        return repository.listarPessoas().stream().map(FinanceiroPessoaDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroPessoaDto criarPessoa(FinanceiroPessoaForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroPessoa p = new FinanceiroPessoa();
        p.setNmPessoa(nomePessoa(form.getNmPessoa(), null));
        p.setInCompartilhado(Boolean.TRUE.equals(form.getInCompartilhado()));
        p.setIdUsuario(form.getIdUsuario());
        p.setNrOrdem(form.getNrOrdem() == null ? 0 : form.getNrOrdem());
        p.setInAtivo(form.getInAtivo() == null || form.getInAtivo());
        return FinanceiroPessoaDto.de(pessoa(repository.inserirPessoa(p)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroPessoaDto atualizarPessoa(Long id, FinanceiroPessoaForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroPessoa atual = pessoa(id);
        if (form.getNmPessoa() != null) atual.setNmPessoa(nomePessoa(form.getNmPessoa(), id));
        if (form.getInCompartilhado() != null) atual.setInCompartilhado(form.getInCompartilhado());
        if (form.getIdUsuario() != null) atual.setIdUsuario(form.getIdUsuario());
        if (form.getNrOrdem() != null) atual.setNrOrdem(form.getNrOrdem());
        if (form.getInAtivo() != null) atual.setInAtivo(form.getInAtivo());
        repository.alterarPessoa(atual);
        return FinanceiroPessoaDto.de(pessoa(id));
    }

    // ---------- categorias ----------

    @Override
    @Transactional(readOnly = true)
    public List<FinanceiroCategoriaDto> listarCategorias() {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        return repository.listarCategorias().stream().map(FinanceiroCategoriaDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroCategoriaDto criarCategoria(FinanceiroCategoriaForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroCategoria c = new FinanceiroCategoria();
        c.setCdTipo(tipo(form.getCdTipo()).name());
        c.setCdProjecao(projecao(form.getCdProjecao()).name());
        c.setNmCategoria(nomeCategoria(form.getNmCategoria(), c.getCdTipo(), null));
        c.setInFixa(Boolean.TRUE.equals(form.getInFixa()));
        c.setCdRegraData(form.getCdRegraData() == null || form.getCdRegraData().trim().isEmpty() ? null : form.getCdRegraData());
        c.setNrDia(form.getNrDia());
        c.setNrOrdem(form.getNrOrdem() == null ? 0 : form.getNrOrdem());
        c.setInAtivo(form.getInAtivo() == null || form.getInAtivo());
        validarRegras(c);
        return FinanceiroCategoriaDto.de(categoria(repository.inserirCategoria(c)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroCategoriaDto atualizarCategoria(Long id, FinanceiroCategoriaForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroCategoria atual = categoria(id);
        if (form.getCdTipo() != null && !tipo(form.getCdTipo()).name().equals(atual.getCdTipo())) {
            if (repository.contarLancamentosDaCategoria(id) > 0) {
                throw invalido("Não é possível mudar o tipo de uma categoria que já tem lançamentos");
            }
            atual.setCdTipo(tipo(form.getCdTipo()).name());
        }
        if (form.getCdProjecao() != null) atual.setCdProjecao(projecao(form.getCdProjecao()).name());
        if (form.getNmCategoria() != null) atual.setNmCategoria(nomeCategoria(form.getNmCategoria(), atual.getCdTipo(), id));
        if (form.getInFixa() != null) atual.setInFixa(form.getInFixa());
        if (form.getCdRegraData() != null) {
            atual.setCdRegraData(form.getCdRegraData().trim().isEmpty() ? null : form.getCdRegraData());
            atual.setNrDia(form.getNrDia());
        } else if (form.getNrDia() != null) {
            atual.setNrDia(form.getNrDia());
        }
        if (form.getNrOrdem() != null) atual.setNrOrdem(form.getNrOrdem());
        if (form.getInAtivo() != null) atual.setInAtivo(form.getInAtivo());
        validarRegras(atual);
        repository.alterarCategoria(atual);
        return FinanceiroCategoriaDto.de(categoria(id));
    }

    // ---------- auxiliares ----------

    private FinanceiroLancamento lancamento(Long id) {
        return repository.buscarLancamento(id)
                .orElseThrow(() -> new NaoEncontradoException("Lançamento não encontrado"));
    }

    private FinanceiroPessoa pessoa(Long id) {
        return repository.buscarPessoa(id)
                .orElseThrow(() -> new NaoEncontradoException("Pessoa não encontrada"));
    }

    private FinanceiroCategoria categoria(Long id) {
        return repository.buscarCategoria(id)
                .orElseThrow(() -> new NaoEncontradoException("Categoria não encontrada"));
    }

    private static NordException conflito() {
        return new ConflitoException(MSG_CONFLITO);
    }

    private static NordException invalido(String mensagem) {
        return new EntradaInvalidaException(mensagem);
    }

    private static BigDecimal zero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static int versaoObrigatoria(Integer nrVersao) {
        if (nrVersao == null) throw invalido("Informe a versão do lançamento (nrVersao)");
        return nrVersao;
    }

    private static void validarFiltro(FinanceiroFiltro filtro) {
        if (filtro == null) return;
        try {
            filtro.competenciaData();
        } catch (DateTimeParseException ex) {
            throw invalido("Competência inválida. Use o formato yyyy-MM (ex.: 2026-10).");
        }
        if (filtro.getTipo() != null && !filtro.getTipo().trim().isEmpty() && !TipoFluxoEnum.de(filtro.getTipo()).isPresent()) {
            throw invalido("Tipo inválido. Use ENTRADA ou SAIDA.");
        }
        if (filtro.getSituacao() != null && !filtro.getSituacao().trim().isEmpty()
                && !SituacaoLancamentoEnum.de(filtro.getSituacao()).isPresent()) {
            throw invalido("Situação inválida. Use TODOS, REALIZADO ou PREVISTO.");
        }
        if (filtro.getDe() != null && filtro.getAte() != null && filtro.getDe().isAfter(filtro.getAte())) {
            throw invalido("A data inicial não pode ser posterior à data final");
        }
    }

    /** A categoria deve existir, estar ativa (manter a já associada é permitido) e aceitar lançamento digitado. */
    private FinanceiroCategoria validarCategoria(Long idCategoria, Long idAtual) {
        FinanceiroCategoria c = repository.buscarCategoria(idCategoria)
                .orElseThrow(() -> invalido("Categoria não encontrada"));
        boolean mesma = idAtual != null && idAtual.equals(idCategoria);
        if (!mesma && Boolean.FALSE.equals(c.getInAtivo())) throw invalido("A categoria está inativa");
        if (TipoProjecaoEnum.SALDO_ANTERIOR.name().equals(c.getCdProjecao())) {
            throw invalido("O saldo anterior é calculado pelo sistema e não aceita lançamentos");
        }
        return c;
    }

    /** Lançamentos de um mês fechado ficam travados; é preciso reabrir o mês para mexer neles. */
    private void exigirMesAberto(LocalDate competencia) {
        boolean fechado = projecaoRepository.buscarMes(competencia.withDayOfMonth(1))
                .map(m -> Boolean.TRUE.equals(m.getInFechado())).orElse(false);
        if (fechado) {
            throw invalido("O mês de " + FinanceiroProjecaoServiceImpl.rotulo(YearMonth.from(competencia)) + " está fechado. Reabra o mês para alterar lançamentos.");
        }
    }

    /** Cada atualização do valor de uma fatura vira uma leitura do dia: é a evolução que mede o ritmo de gasto. */
    private void registrarLeitura(FinanceiroCategoria categoria, Long idLancamento, BigDecimal valor, Long idUsuario) {
        if (!TipoProjecaoEnum.RITMO_FATURA.name().equals(categoria.getCdProjecao())) return;
        projecaoRepository.registrarLeitura(idLancamento, LocalDate.now(clock.withZone(FUSO)), valor, idUsuario);
    }

    /** A pessoa deve existir e estar ativa (manter a já associada ao lançamento é permitido). */
    private void validarPessoa(Long idPessoa, Long idAtual) {
        FinanceiroPessoa p = repository.buscarPessoa(idPessoa).orElseThrow(() -> invalido("Pessoa não encontrada"));
        boolean mesma = idAtual != null && idAtual.equals(idPessoa);
        if (!mesma && Boolean.FALSE.equals(p.getInAtivo())) throw invalido("A pessoa está inativa");
    }

    private String nomePessoa(String nome, Long idIgnorar) {
        String limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty()) throw invalido("Informe o nome da pessoa");
        boolean duplicado = repository.listarPessoas().stream()
                .anyMatch(p -> p.getNmPessoa().equalsIgnoreCase(limpo) && !p.getId().equals(idIgnorar));
        if (duplicado) throw invalido("Já existe uma pessoa com este nome");
        return limpo;
    }

    private String nomeCategoria(String nome, String cdTipo, Long idIgnorar) {
        String limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty()) throw invalido("Informe o nome da categoria");
        boolean duplicada = repository.listarCategorias().stream()
                .anyMatch(c -> c.getNmCategoria().equalsIgnoreCase(limpo) && c.getCdTipo().equals(cdTipo)
                        && !c.getId().equals(idIgnorar));
        if (duplicada) throw invalido("Já existe uma categoria com este nome neste tipo");
        return limpo;
    }

    private static TipoFluxoEnum tipo(String valor) {
        return TipoFluxoEnum.de(valor).orElseThrow(() -> invalido("Tipo inválido. Use ENTRADA ou SAIDA."));
    }

    private static TipoProjecaoEnum projecao(String valor) {
        return TipoProjecaoEnum.de(valor).orElseThrow(() -> invalido(
                "Projeção inválida. Use FIXA_MEDIA, SALDO_ANTERIOR, FIXA_VALOR, RITMO_FATURA, VARIAVEL_MEDIA ou MANUAL."));
    }

    /** Combinações permitidas entre tipo, projeção e regra de data. */
    private static void validarRegras(FinanceiroCategoria c) {
        if (TipoProjecaoEnum.SALDO_ANTERIOR.name().equals(c.getCdProjecao()) && !TipoFluxoEnum.ENTRADA.name().equals(c.getCdTipo())) {
            throw invalido("O saldo anterior só pode ser uma entrada");
        }
        if (TipoProjecaoEnum.RITMO_FATURA.name().equals(c.getCdProjecao()) && !TipoFluxoEnum.SAIDA.name().equals(c.getCdTipo())) {
            throw invalido("A projeção por ritmo de fatura só vale para saídas");
        }
        if (c.getCdRegraData() == null) {
            if (c.getNrDia() != null) throw invalido("Informe a regra de data junto com o dia");
            return;
        }
        RegraDataEnum regra = RegraDataEnum.de(c.getCdRegraData())
                .orElseThrow(() -> invalido("Regra de data inválida. Use DIA_MES ou DIA_UTIL."));
        c.setCdRegraData(regra.name());
        if (c.getNrDia() == null) throw invalido("Informe o dia da regra de data");
        int maximo = regra == RegraDataEnum.DIA_MES ? 31 : 23;
        if (c.getNrDia() < 1 || c.getNrDia() > maximo) {
            throw invalido("O dia deve ser de 1 a " + maximo + " para a regra " + regra.name());
        }
    }

    private static String uuidObrigatorio(String valor) {
        if (valor == null || valor.trim().isEmpty()) throw invalido("Informe o identificador da requisição (cdRequisicao)");
        try {
            return UUID.fromString(valor.trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw invalido("Identificador da requisição inválido (esperado um UUID)");
        }
    }

    /** Mês do lançamento: o informado (sempre dia 1) ou o mês da data do lançamento. */
    private static LocalDate competencia(FinanceiroLancamentoForm form) {
        LocalDate base = form.getDtCompetencia() != null ? form.getDtCompetencia() : form.getDtLancamento();
        return base.withDayOfMonth(1);
    }

    static FinanceiroLancamento converter(FinanceiroLancamentoForm form) {
        FinanceiroLancamento l = new FinanceiroLancamento();
        l.setDtLancamento(form.getDtLancamento());
        l.setIdCategoria(form.getIdCategoria());
        l.setIdPessoa(form.getIdPessoa());
        String descricao = form.getDsLancamento() == null ? "" : form.getDsLancamento().trim();
        l.setDsLancamento(descricao.isEmpty() ? null : descricao);
        l.setVlLancamento(form.getVlLancamento());
        l.setInRealizado(Boolean.TRUE.equals(form.getInRealizado()));
        return l;
    }
}
