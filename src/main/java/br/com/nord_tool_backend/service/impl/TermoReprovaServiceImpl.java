package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.TermoFoto;
import br.com.nord_tool_backend.domain.TermoReprova;
import br.com.nord_tool_backend.domain.enums.SituacaoTermoEnum;
import br.com.nord_tool_backend.dto.TermoFotoDto;
import br.com.nord_tool_backend.dto.TermoReprovaDto;
import br.com.nord_tool_backend.dto.TermoReprovaResumoDto;
import br.com.nord_tool_backend.dto.TermoReprovaResumoGeralDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.OrdemFotoForm;
import br.com.nord_tool_backend.form.SituacaoTermoForm;
import br.com.nord_tool_backend.repository.TermoFotoRepository;
import br.com.nord_tool_backend.repository.TermoReprovaRepository;
import br.com.nord_tool_backend.service.CacheService;
import br.com.nord_tool_backend.service.TermoReprovaService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermoReprovaServiceImpl implements TermoReprovaService {

    static final int MAX_PAGINAS = 80;
    /** Cota de fotos por termo: até 4 por página do limite de páginas. */
    static final int MAX_FOTOS_POR_TERMO = 4 * MAX_PAGINAS;
    static final int MAX_LEGENDA = 240;
    static final String MSG_CONCLUIR_SEM_FOTO = "Anexe ao menos uma foto antes de concluir o termo.";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final TermoReprovaRepository termoRepository;
    private final TermoFotoRepository fotoRepository;
    private final ArmazenamentoService armazenamento;
    // A listagem de apartamentos traz os dados do último termo: qualquer alteração invalida o cache.
    private final CacheService cacheService;

    private final AutorizacaoService autorizacao;

    // ---------- termos ----------

    @Override
    @Transactional(readOnly = true)
    public TermoReprovaResumoGeralDto resumoGeral() {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.LEITURA);
        TermoReprovaResumoGeralDto resumo = termoRepository.resumoGeral();
        int total = valor(resumo.getTotalApartamentosComReprova());
        double percentual = total == 0 ? 0.0
                : Math.round(valor(resumo.getConcluidos()) * 1000.0 / total) / 10.0;
        resumo.setPercentualConcluido(percentual);
        return resumo;
    }

    private static int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermoReprovaResumoDto> listarPorApartamento(Long idApartamento) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.LEITURA);
        return termoRepository.listarPorApartamento(idApartamento).stream()
                .map(this::toResumo).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TermoReprovaDto buscar(Long idTermo) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.LEITURA);
        return montar(termo(idTermo), new ArrayList<>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TermoReprovaDto criar(Long idApartamento, String nomeArquivo, byte[] pdf, int nrPaginas) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        if (!termoRepository.apartamentoExiste(idApartamento)) {
            throw new NaoEncontradoException("Apartamento não encontrado");
        }
        validarNrPaginas(nrPaginas);
        String contentType = ArquivoValidador.validarPdf(nomeArquivo, pdf);

        TermoReprova novo = new TermoReprova();
        novo.setIdApartamentoVistoria(idApartamento);
        novo.setNrTermo(termoRepository.proximoNumero(idApartamento));
        novo.setIdArquivo(armazenamento.salvar(nomeArquivo, contentType, pdf));
        novo.setNrPaginas(nrPaginas);
        Long id = termoRepository.inserir(novo).getId();
        cacheService.limparTodos();
        return montar(termo(id), new ArrayList<>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TermoReprovaDto trocarArquivo(Long idTermo, String nomeArquivo, byte[] pdf, int nrPaginas) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        TermoReprova atual = termo(idTermo);
        validarNrPaginas(nrPaginas);
        String contentType = ArquivoValidador.validarPdf(nomeArquivo, pdf);

        Long novoArquivo = armazenamento.salvar(nomeArquivo, contentType, pdf);
        List<Long> arquivosParaApagar = new ArrayList<>();
        arquivosParaApagar.add(atual.getIdArquivo());

        int removidas = 0;
        for (TermoFoto foto : fotoRepository.listarPorTermo(idTermo)) {
            if (foto.getNrPagina() > nrPaginas) {
                fotoRepository.deletar(foto.getId());
                arquivosParaApagar.add(foto.getIdArquivoImagem());
                arquivosParaApagar.add(foto.getIdArquivoMiniatura());
                removidas++;
            }
        }
        termoRepository.atualizarArquivo(idTermo, novoArquivo, nrPaginas);
        arquivosParaApagar.forEach(armazenamento::apagar);
        cacheService.limparTodos();

        List<String> avisos = new ArrayList<>();
        if (removidas > 0) {
            avisos.add(removidas + (removidas == 1 ? " foto foi removida" : " fotos foram removidas")
                    + " porque estavam em páginas que não existem no novo PDF.");
        }
        return montar(termo(idTermo), avisos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TermoReprovaDto atualizarSituacao(Long idTermo, SituacaoTermoForm form) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        termo(idTermo);
        SituacaoTermoEnum situacao = SituacaoTermoEnum.de(form.getSituacao())
                .orElseThrow(() -> new EntradaInvalidaException("Situação inválida. Use PENDENTE, EM_ANDAMENTO ou CONCLUIDO."));
        if (situacao == SituacaoTermoEnum.CONCLUIDO && fotoRepository.contarPorTermo(idTermo) < 1) {
            throw new EntradaInvalidaException(MSG_CONCLUIR_SEM_FOTO);
        }
        String observacao = form.getObservacao() == null || form.getObservacao().trim().isEmpty()
                ? null : form.getObservacao().trim();
        termoRepository.atualizarSituacao(idTermo, situacao.name(), observacao);
        cacheService.limparTodos();
        return montar(termo(idTermo), new ArrayList<>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletar(Long idTermo) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        TermoReprova termo = termo(idTermo);
        List<TermoFoto> fotos = fotoRepository.listarPorTermo(idTermo);
        // Primeiro as linhas (o cascade remove as fotos), depois os arquivos que elas referenciam.
        termoRepository.deletar(idTermo);
        cacheService.limparTodos();
        armazenamento.apagar(termo.getIdArquivo());
        for (TermoFoto foto : fotos) {
            armazenamento.apagar(foto.getIdArquivoImagem());
            armazenamento.apagar(foto.getIdArquivoMiniatura());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apagarPorApartamento(Long idApartamento) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        for (Long idTermo : termoRepository.listarIdsPorApartamento(idApartamento)) {
            deletar(idTermo);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload abrirPdf(Long idTermo) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.LEITURA);
        TermoReprova termo = termo(idTermo);
        return new ArquivoDownload(armazenamento.abrir(termo.getIdArquivo()), versao(termo.getDhAlteracao()));
    }

    // ---------- fotos ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TermoFotoDto adicionarFoto(Long idTermo, String nomeImagem, byte[] imagem, byte[] miniatura, int nrPagina, String legenda) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        TermoReprova termo = termo(idTermo);
        validarPagina(termo, nrPagina);
        if (fotoRepository.contarPorTermo(idTermo) >= MAX_FOTOS_POR_TERMO) {
            throw new EntradaInvalidaException("O termo já tem o máximo de " + MAX_FOTOS_POR_TERMO + " fotos");
        }
        String tipoImagem = ArquivoValidador.validarImagem(nomeImagem, imagem);
        String tipoMiniatura = ArquivoValidador.validarImagem(nomeImagem, miniatura);

        TermoFoto foto = new TermoFoto();
        foto.setIdTermoReprova(idTermo);
        foto.setNrPagina(nrPagina);
        foto.setNrOrdem(fotoRepository.proximaOrdem(idTermo, nrPagina));
        foto.setTxLegenda(legenda(legenda));
        foto.setIdArquivoImagem(armazenamento.salvar(nomeImagem, tipoImagem, imagem));
        foto.setIdArquivoMiniatura(armazenamento.salvar("miniatura-" + nomeImagem, tipoMiniatura, miniatura));
        Long id = fotoRepository.inserir(foto).getId();
        cacheService.limparTodos();
        return toDto(foto(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TermoFotoDto editarFoto(Long idFoto, String nomeImagem, byte[] imagem, byte[] miniatura, String legenda, Integer nrPagina) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        TermoFoto foto = foto(idFoto);
        if ((imagem == null) != (miniatura == null)) {
            throw new EntradaInvalidaException("Envie a imagem e a miniatura juntas.");
        }
        List<Long> arquivosParaApagar = new ArrayList<>();

        if (nrPagina != null && !nrPagina.equals(foto.getNrPagina())) {
            validarPagina(termo(foto.getIdTermoReprova()), nrPagina);
            foto.setNrPagina(nrPagina);
            foto.setNrOrdem(fotoRepository.proximaOrdem(foto.getIdTermoReprova(), nrPagina));
        }
        if (legenda != null) {
            foto.setTxLegenda(legenda(legenda));
        }
        if (imagem != null) {
            String tipoImagem = ArquivoValidador.validarImagem(nomeImagem, imagem);
            String tipoMiniatura = ArquivoValidador.validarImagem(nomeImagem, miniatura);
            arquivosParaApagar.add(foto.getIdArquivoImagem());
            arquivosParaApagar.add(foto.getIdArquivoMiniatura());
            foto.setIdArquivoImagem(armazenamento.salvar(nomeImagem, tipoImagem, imagem));
            foto.setIdArquivoMiniatura(armazenamento.salvar("miniatura-" + nomeImagem, tipoMiniatura, miniatura));
        }
        fotoRepository.atualizar(foto);
        cacheService.limparTodos();
        arquivosParaApagar.forEach(armazenamento::apagar);
        return toDto(foto(idFoto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirFoto(Long idFoto) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        TermoFoto foto = foto(idFoto);
        fotoRepository.deletar(idFoto);
        cacheService.limparTodos();
        armazenamento.apagar(foto.getIdArquivoImagem());
        armazenamento.apagar(foto.getIdArquivoMiniatura());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TermoFotoDto> ordenarFotos(Long idTermo, List<OrdemFotoForm> ordem) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA);
        termo(idTermo);
        Set<Long> idsDoTermo = fotoRepository.listarPorTermo(idTermo).stream()
                .map(TermoFoto::getId).collect(Collectors.toCollection(HashSet::new));
        for (OrdemFotoForm item : ordem) {
            if (item == null || item.getIdTermoFoto() == null || item.getNrOrdem() == null || item.getNrOrdem() < 0) {
                throw new EntradaInvalidaException("Informe a foto e uma ordem válida para cada item.");
            }
            if (!idsDoTermo.contains(item.getIdTermoFoto())) {
                throw new EntradaInvalidaException("A foto " + item.getIdTermoFoto() + " não pertence a este termo.");
            }
        }
        for (OrdemFotoForm item : ordem) {
            fotoRepository.atualizarOrdem(idTermo, item.getIdTermoFoto(), item.getNrOrdem());
        }
        return fotoRepository.listarPorTermo(idTermo).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload abrirImagem(Long idFoto) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.LEITURA);
        TermoFoto foto = foto(idFoto);
        return new ArquivoDownload(armazenamento.abrir(foto.getIdArquivoImagem()), versao(foto.getDhAlteracao()));
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload abrirMiniatura(Long idFoto) {
        autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.LEITURA);
        TermoFoto foto = foto(idFoto);
        return new ArquivoDownload(armazenamento.abrir(foto.getIdArquivoMiniatura()), versao(foto.getDhAlteracao()));
    }

    // ---------- auxiliares ----------

    private TermoReprova termo(Long id) {
        return termoRepository.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Termo de reprova não encontrado"));
    }

    private TermoFoto foto(Long id) {
        return fotoRepository.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Foto não encontrada"));
    }

    private void validarNrPaginas(int nrPaginas) {
        if (nrPaginas < 1 || nrPaginas > MAX_PAGINAS) {
            throw new EntradaInvalidaException("O número de páginas deve estar entre 1 e " + MAX_PAGINAS + ".");
        }
    }

    private void validarPagina(TermoReprova termo, int nrPagina) {
        if (nrPagina < 1 || nrPagina > termo.getNrPaginas()) {
            throw new EntradaInvalidaException("Página inválida: o termo tem " + termo.getNrPaginas() + " página(s).");
        }
    }

    private String legenda(String legenda) {
        if (legenda == null || legenda.trim().isEmpty()) return null;
        String limpa = legenda.trim();
        if (limpa.length() > MAX_LEGENDA) {
            throw new EntradaInvalidaException("A legenda deve ter no máximo " + MAX_LEGENDA + " caracteres.");
        }
        return limpa;
    }

    private static long versao(LocalDateTime dh) {
        return dh == null ? 0L : dh.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static String formatar(LocalDateTime dh) {
        return dh == null ? null : dh.format(FORMATO);
    }

    private TermoReprovaDto montar(TermoReprova termo, List<String> avisos) {
        List<TermoFotoDto> fotos = fotoRepository.listarPorTermo(termo.getId()).stream()
                .map(this::toDto).collect(Collectors.toList());
        return new TermoReprovaDto(termo.getId(), termo.getIdApartamentoVistoria(), termo.getNrTermo(),
                termo.getNmArquivo(), termo.getNrPaginas(), termo.getNmSituacao(), termo.getTxObservacao(),
                formatar(termo.getDhCriacao()), formatar(termo.getDhAlteracao()), versao(termo.getDhAlteracao()),
                fotos, avisos);
    }

    private TermoReprovaResumoDto toResumo(TermoReprova t) {
        return new TermoReprovaResumoDto(t.getId(), t.getNrTermo(), t.getNmArquivo(), t.getNmSituacao(),
                t.getNrPaginas(), t.getQtFotos() == null ? 0 : t.getQtFotos(),
                formatar(t.getDhAlteracao()), versao(t.getDhAlteracao()));
    }

    private TermoFotoDto toDto(TermoFoto f) {
        return new TermoFotoDto(f.getId(), f.getIdTermoReprova(), f.getNrPagina(), f.getNrOrdem(),
                f.getTxLegenda(), formatar(f.getDhAlteracao()), versao(f.getDhAlteracao()));
    }

}
