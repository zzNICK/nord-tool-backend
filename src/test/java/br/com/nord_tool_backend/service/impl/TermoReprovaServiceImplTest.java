package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.domain.TermoFoto;
import br.com.nord_tool_backend.domain.TermoReprova;
import br.com.nord_tool_backend.dto.TermoFotoDto;
import br.com.nord_tool_backend.dto.TermoReprovaResumoGeralDto;
import br.com.nord_tool_backend.service.CacheService;
import br.com.nord_tool_backend.dto.TermoReprovaDto;
import br.com.nord_tool_backend.form.OrdemFotoForm;
import br.com.nord_tool_backend.form.SituacaoTermoForm;
import br.com.nord_tool_backend.repository.TermoFotoRepository;
import br.com.nord_tool_backend.repository.TermoReprovaRepository;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import br.com.nord_tool_backend.storage.*;
import org.junit.jupiter.api.Nested;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TermoReprovaServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};

    private TermoReprovaRepository termoRepository;
    private TermoFotoRepository fotoRepository;
    private ArmazenamentoService armazenamento;
    private CacheService cacheService;
    private TermoReprovaServiceImpl service;

    @BeforeEach
    void setUp() {
        termoRepository = mock(TermoReprovaRepository.class);
        fotoRepository = mock(TermoFotoRepository.class);
        armazenamento = mock(ArmazenamentoService.class);
        cacheService = mock(CacheService.class);
        service = new TermoReprovaServiceImpl(termoRepository, fotoRepository, armazenamento, cacheService, autorizacao);
    }

    private TermoReprova termo(long id, int paginas, String situacao) {
        TermoReprova t = new TermoReprova();
        t.setId(id);
        t.setIdApartamentoVistoria(10L);
        t.setNrTermo(1);
        t.setIdArquivo(100L + id);
        t.setNmArquivo("termo.pdf");
        t.setNrPaginas(paginas);
        t.setNmSituacao(situacao);
        t.setDhAlteracao(LocalDateTime.now());
        t.setDhCriacao(LocalDateTime.now());
        return t;
    }

    private TermoFoto foto(long id, long idTermo, int pagina, long img, long mini) {
        TermoFoto f = new TermoFoto();
        f.setId(id);
        f.setIdTermoReprova(idTermo);
        f.setNrPagina(pagina);
        f.setNrOrdem(0);
        f.setIdArquivoImagem(img);
        f.setIdArquivoMiniatura(mini);
        f.setDhAlteracao(LocalDateTime.now());
        return f;
    }

    private void termoExiste(TermoReprova t) {
        when(termoRepository.buscarPorId(t.getId())).thenReturn(Optional.of(t));
    }

    // ---------- criar ----------

    @Test
    void numeraOTermoComMaisUmEGravaOArquivo() {
        when(termoRepository.apartamentoExiste(10L)).thenReturn(true);
        when(termoRepository.proximoNumero(10L)).thenReturn(3);
        when(armazenamento.salvar(eq("a.pdf"), eq("application/pdf"), any())).thenReturn(55L);
        when(termoRepository.inserir(any())).thenAnswer(inv -> {
            TermoReprova t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });
        TermoReprova gravado = termo(7L, 4, "PENDENTE");
        gravado.setNrTermo(3);
        termoExiste(gravado);

        TermoReprovaDto dto = service.criar(10L, "a.pdf", PDF, 4);

        ArgumentCaptor<TermoReprova> captor = ArgumentCaptor.forClass(TermoReprova.class);
        verify(termoRepository).inserir(captor.capture());
        assertEquals(3, captor.getValue().getNrTermo());
        assertEquals(55L, captor.getValue().getIdArquivo());
        assertEquals(4, captor.getValue().getNrPaginas());
        assertEquals(3, dto.getNrTermo());
    }

    @Test
    void naoCriaTermoParaApartamentoInexistente() {
        when(termoRepository.apartamentoExiste(10L)).thenReturn(false);

        NordException ex = assertThrows(NordException.class, () -> service.criar(10L, "a.pdf", PDF, 4));

        assertEquals(404, ex.getStatus().getStatus().value());
        verifyNoInteractions(armazenamento);
    }

    @Test
    void validaPaginasEPdfAntesDeGravar() {
        when(termoRepository.apartamentoExiste(10L)).thenReturn(true);

        assertThrows(NordException.class, () -> service.criar(10L, "a.pdf", PDF, 0));
        assertThrows(NordException.class, () -> service.criar(10L, "a.pdf", PDF, 81));
        assertThrows(NordException.class, () -> service.criar(10L, "a.pdf", JPEG, 3));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
        verify(termoRepository, never()).inserir(any());
    }

    // ---------- situação ----------

    @Test
    void concluirSemFotoEhRecusado() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.contarPorTermo(1L)).thenReturn(0);
        SituacaoTermoForm form = new SituacaoTermoForm();
        form.setSituacao("CONCLUIDO");

        NordException ex = assertThrows(NordException.class, () -> service.atualizarSituacao(1L, form));

        assertEquals("Anexe ao menos uma foto antes de concluir o termo.", ex.getMessage());
        verify(termoRepository, never()).atualizarSituacao(anyLong(), anyString(), any());
    }

    @Test
    void concluirComFotoGravaSituacaoENormalizaObservacao() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.contarPorTermo(1L)).thenReturn(2);
        SituacaoTermoForm form = new SituacaoTermoForm();
        form.setSituacao("concluido");
        form.setObservacao("   ");

        service.atualizarSituacao(1L, form);

        verify(termoRepository).atualizarSituacao(1L, "CONCLUIDO", null);
    }

    @Test
    void situacaoInvalidaEhRecusada() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        SituacaoTermoForm form = new SituacaoTermoForm();
        form.setSituacao("FINALIZADO");

        assertThrows(NordException.class, () -> service.atualizarSituacao(1L, form));
    }

    @Test
    void emAndamentoNaoExigeFoto() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        SituacaoTermoForm form = new SituacaoTermoForm();
        form.setSituacao("EM_ANDAMENTO");
        form.setObservacao(" obs ");

        service.atualizarSituacao(1L, form);

        verify(termoRepository).atualizarSituacao(1L, "EM_ANDAMENTO", "obs");
    }

    // ---------- exclusão em cascata + arquivos ----------

    @Test
    void excluirTermoApagaLinhasEDepoisTodosOsArquivos() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.listarPorTermo(1L)).thenReturn(List.of(foto(5, 1, 1, 501, 502), foto(6, 1, 2, 601, 602)));

        service.deletar(1L);

        InOrder ordem = inOrder(termoRepository, armazenamento);
        ordem.verify(termoRepository).deletar(1L);
        for (long idArquivo : new long[]{101, 501, 502, 601, 602}) {
            ordem.verify(armazenamento).apagar(idArquivo);
        }
    }

    @Test
    void excluirApartamentoApagaTodosOsTermosEArquivos() {
        when(termoRepository.listarIdsPorApartamento(10L)).thenReturn(List.of(1L, 2L));
        termoExiste(termo(1L, 3, "PENDENTE"));
        termoExiste(termo(2L, 3, "PENDENTE"));
        when(fotoRepository.listarPorTermo(anyLong())).thenReturn(new ArrayList<>());

        service.apagarPorApartamento(10L);

        verify(termoRepository).deletar(1L);
        verify(termoRepository).deletar(2L);
        verify(armazenamento).apagar(101L);
        verify(armazenamento).apagar(102L);
    }

    @Test
    void excluirTermoInexistenteRetorna404() {
        when(termoRepository.buscarPorId(9L)).thenReturn(Optional.empty());

        NordException ex = assertThrows(NordException.class, () -> service.deletar(9L));

        assertEquals(404, ex.getStatus().getStatus().value());
    }

    // ---------- trocar PDF ----------

    @Test
    void trocarPdfComMenosPaginasRemoveFotosQueSobramEAvisa() {
        TermoReprova atual = termo(1L, 5, "EM_ANDAMENTO");
        termoExiste(atual);
        when(armazenamento.salvar(anyString(), anyString(), any())).thenReturn(900L);
        when(fotoRepository.listarPorTermo(1L)).thenReturn(List.of(
                foto(5, 1, 2, 501, 502), foto(6, 1, 4, 601, 602), foto(7, 1, 5, 701, 702)));

        TermoReprovaDto dto = service.trocarArquivo(1L, "novo.pdf", PDF, 3);

        verify(fotoRepository, never()).deletar(5L);
        verify(fotoRepository).deletar(6L);
        verify(fotoRepository).deletar(7L);
        verify(termoRepository).atualizarArquivo(1L, 900L, 3);
        verify(armazenamento).apagar(101L);
        for (long idArquivo : new long[]{601, 602, 701, 702}) verify(armazenamento).apagar(idArquivo);
        verify(armazenamento, never()).apagar(501L);
        assertEquals(1, dto.getAvisos().size());
        assertTrue(dto.getAvisos().get(0).contains("2 fotos foram removidas"));
    }

    @Test
    void trocarPdfSemPerderPaginasNaoGeraAviso() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(armazenamento.salvar(anyString(), anyString(), any())).thenReturn(900L);
        when(fotoRepository.listarPorTermo(1L)).thenReturn(List.of(foto(5, 1, 3, 501, 502)));

        TermoReprovaDto dto = service.trocarArquivo(1L, "novo.pdf", PDF, 6);

        assertTrue(dto.getAvisos().isEmpty());
        verify(fotoRepository, never()).deletar(anyLong());
    }

    // ---------- fotos ----------

    @Test
    void adicionaFotoNaPaginaComProximaOrdemEGravaDoisArquivos() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.proximaOrdem(1L, 2)).thenReturn(4);
        when(armazenamento.salvar(eq("f.jpg"), eq("image/jpeg"), any())).thenReturn(11L);
        when(armazenamento.salvar(eq("miniatura-f.jpg"), eq("image/png"), any())).thenReturn(12L);
        when(fotoRepository.inserir(any())).thenAnswer(inv -> {
            TermoFoto f = inv.getArgument(0);
            f.setId(8L);
            return f;
        });
        when(fotoRepository.buscarPorId(8L)).thenReturn(Optional.of(foto(8, 1, 2, 11, 12)));

        TermoFotoDto dto = service.adicionarFoto(1L, "f.jpg", JPEG, PNG, 2, "  Parede  ");

        ArgumentCaptor<TermoFoto> captor = ArgumentCaptor.forClass(TermoFoto.class);
        verify(fotoRepository).inserir(captor.capture());
        assertEquals(2, captor.getValue().getNrPagina());
        assertEquals(4, captor.getValue().getNrOrdem());
        assertEquals("Parede", captor.getValue().getTxLegenda());
        assertEquals(11L, captor.getValue().getIdArquivoImagem());
        assertEquals(12L, captor.getValue().getIdArquivoMiniatura());
        assertEquals(8L, dto.getIdTermoFoto());
    }

    @Test
    void recusaFotoEmPaginaForaDoTermo() {
        termoExiste(termo(1L, 3, "PENDENTE"));

        assertThrows(NordException.class, () -> service.adicionarFoto(1L, "f.jpg", JPEG, JPEG, 4, null));
        assertThrows(NordException.class, () -> service.adicionarFoto(1L, "f.jpg", JPEG, JPEG, 0, null));
        verify(fotoRepository, never()).inserir(any());
    }

    @Test
    void recusaLegendaAcimaDe240ECorpoNaoImagem() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        String longa = new String(new char[241]).replace('\0', 'x');

        assertThrows(NordException.class, () -> service.adicionarFoto(1L, "f.jpg", JPEG, JPEG, 1, longa));
        assertThrows(NordException.class, () -> service.adicionarFoto(1L, "f.jpg", PDF, JPEG, 1, null));
    }

    @Test
    void editarExigeImagemEMiniaturaJuntas() {
        when(fotoRepository.buscarPorId(5L)).thenReturn(Optional.of(foto(5, 1, 1, 501, 502)));

        assertThrows(NordException.class, () -> service.editarFoto(5L, "n.jpg", JPEG, null, null, null));
        verify(fotoRepository, never()).atualizar(any());
    }

    @Test
    void trocarImagemDaFotoApagaOsArquivosAntigos() {
        when(fotoRepository.buscarPorId(5L)).thenReturn(Optional.of(foto(5, 1, 1, 501, 502)));
        when(armazenamento.salvar(eq("n.jpg"), anyString(), any())).thenReturn(801L);
        when(armazenamento.salvar(eq("miniatura-n.jpg"), anyString(), any())).thenReturn(802L);

        service.editarFoto(5L, "n.jpg", JPEG, JPEG, "nova", null);

        ArgumentCaptor<TermoFoto> captor = ArgumentCaptor.forClass(TermoFoto.class);
        verify(fotoRepository).atualizar(captor.capture());
        assertEquals(801L, captor.getValue().getIdArquivoImagem());
        assertEquals(802L, captor.getValue().getIdArquivoMiniatura());
        assertEquals("nova", captor.getValue().getTxLegenda());
        verify(armazenamento).apagar(501L);
        verify(armazenamento).apagar(502L);
    }

    @Test
    void mudarDePaginaRecalculaAOrdemEValidaAPagina() {
        when(fotoRepository.buscarPorId(5L)).thenReturn(Optional.of(foto(5, 1, 1, 501, 502)));
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.proximaOrdem(1L, 3)).thenReturn(2);

        service.editarFoto(5L, null, null, null, null, 3);

        ArgumentCaptor<TermoFoto> captor = ArgumentCaptor.forClass(TermoFoto.class);
        verify(fotoRepository).atualizar(captor.capture());
        assertEquals(3, captor.getValue().getNrPagina());
        assertEquals(2, captor.getValue().getNrOrdem());
        verify(armazenamento, never()).apagar(anyLong());
        assertThrows(NordException.class, () -> service.editarFoto(5L, null, null, null, null, 9));
    }

    @Test
    void excluirFotoApagaLinhaEArquivos() {
        when(fotoRepository.buscarPorId(5L)).thenReturn(Optional.of(foto(5, 1, 1, 501, 502)));

        service.excluirFoto(5L);

        InOrder ordem = inOrder(fotoRepository, armazenamento);
        ordem.verify(fotoRepository).deletar(5L);
        ordem.verify(armazenamento).apagar(501L);
        ordem.verify(armazenamento).apagar(502L);
    }

    @Test
    void ordenarSomenteFotosDoProprioTermo() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.listarPorTermo(1L)).thenReturn(List.of(foto(5, 1, 1, 501, 502), foto(6, 1, 1, 601, 602)));
        OrdemFotoForm a = new OrdemFotoForm();
        a.setIdTermoFoto(6L);
        a.setNrOrdem(0);
        OrdemFotoForm b = new OrdemFotoForm();
        b.setIdTermoFoto(5L);
        b.setNrOrdem(1);

        service.ordenarFotos(1L, List.of(a, b));

        verify(fotoRepository).atualizarOrdem(1L, 6L, 0);
        verify(fotoRepository).atualizarOrdem(1L, 5L, 1);

        OrdemFotoForm intrusa = new OrdemFotoForm();
        intrusa.setIdTermoFoto(99L);
        intrusa.setNrOrdem(0);
        assertThrows(NordException.class, () -> service.ordenarFotos(1L, List.of(intrusa)));
        verify(fotoRepository, never()).atualizarOrdem(eq(1L), eq(99L), anyInt());
    }

    @Test
    void ordenarRecusaItemIncompleto() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.listarPorTermo(1L)).thenReturn(List.of());

        assertThrows(NordException.class, () -> service.ordenarFotos(1L, List.of(new OrdemFotoForm())));
    }

    // ---------- resumo geral (dashboard) e cache ----------

    private TermoReprovaResumoGeralDto resumo(int total, int com, int sem, int concl, int andamento, int pend) {
        return new TermoReprovaResumoGeralDto(total, com, sem, concl, andamento, pend, null);
    }

    @Test
    void resumoCalculaOPercentualConcluidoComUmaCasaDecimal() {
        when(termoRepository.resumoGeral()).thenReturn(resumo(3, 2, 1, 1, 0, 1));

        TermoReprovaResumoGeralDto r = service.resumoGeral();

        assertEquals(33.3, r.getPercentualConcluido());
        assertEquals(3, r.getTotalApartamentosComReprova());
        assertEquals(1, r.getSemTermo());
    }

    @Test
    void resumoSemApartamentosTemPercentualZero() {
        when(termoRepository.resumoGeral()).thenReturn(resumo(0, 0, 0, 0, 0, 0));

        assertEquals(0.0, service.resumoGeral().getPercentualConcluido());
    }

    @Test
    void resumoTodosConcluidosEh100() {
        when(termoRepository.resumoGeral()).thenReturn(resumo(4, 4, 0, 4, 0, 0));

        assertEquals(100.0, service.resumoGeral().getPercentualConcluido());
    }

    @Test
    void resumoArredondaParaCima() {
        when(termoRepository.resumoGeral()).thenReturn(resumo(3, 3, 0, 2, 1, 0));

        assertEquals(66.7, service.resumoGeral().getPercentualConcluido());
    }

    @Test
    void alteracoesInvalidamOCacheDaListagemDeApartamentos() {
        // situação
        termoExiste(termo(1L, 3, "PENDENTE"));
        SituacaoTermoForm form = new SituacaoTermoForm();
        form.setSituacao("EM_ANDAMENTO");
        service.atualizarSituacao(1L, form);
        verify(cacheService, times(1)).limparTodos();

        // excluir termo
        when(fotoRepository.listarPorTermo(1L)).thenReturn(new ArrayList<>());
        service.deletar(1L);
        verify(cacheService, times(2)).limparTodos();

        // excluir foto
        when(fotoRepository.buscarPorId(5L)).thenReturn(Optional.of(foto(5, 1, 1, 501, 502)));
        service.excluirFoto(5L);
        verify(cacheService, times(3)).limparTodos();
    }

    @Test
    void criarETrocarPdfInvalidamOCache() {
        when(termoRepository.apartamentoExiste(10L)).thenReturn(true);
        when(termoRepository.proximoNumero(10L)).thenReturn(1);
        when(armazenamento.salvar(anyString(), anyString(), any())).thenReturn(55L);
        when(termoRepository.inserir(any())).thenAnswer(inv -> {
            TermoReprova tr = inv.getArgument(0);
            tr.setId(7L);
            return tr;
        });
        termoExiste(termo(7L, 2, "PENDENTE"));

        service.criar(10L, "a.pdf", PDF, 2);
        verify(cacheService, times(1)).limparTodos();

        when(fotoRepository.listarPorTermo(7L)).thenReturn(new ArrayList<>());
        service.trocarArquivo(7L, "b.pdf", PDF, 3);
        verify(cacheService, times(2)).limparTodos();
    }

    @Test
    void consultasNaoInvalidamOCache() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.listarPorTermo(1L)).thenReturn(new ArrayList<>());

        service.buscar(1L);

        verifyNoInteractions(cacheService);
    }

    @Nested
    class ArquivoValidadorTest {

        private byte[] comPrefixo(int tamanho, int... prefixo) {
            byte[] b = new byte[tamanho];
            for (int i = 0; i < prefixo.length; i++) b[i] = (byte) prefixo[i];
            return b;
        }

        private final int[] JPEG = {0xFF, 0xD8, 0xFF, 0xE0};
        private final int[] PNG = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        private final int[] PDF = {'%', 'P', 'D', 'F', '-'};

        @Test
        void aceitaPdfValido() {
            assertEquals("application/pdf", ArquivoValidador.validarPdf("termo.pdf", comPrefixo(100, PDF)));
        }

        @Test
        void rejeitaPdfSemAssinaturaOuAcimaDe15Mb() {
            NordException a = assertThrows(NordException.class,
                    () -> ArquivoValidador.validarPdf("x.pdf", comPrefixo(100, JPEG)));
            assertTrue(a.getMessage().contains("PDF"));
            assertThrows(NordException.class,
                    () -> ArquivoValidador.validarPdf("x.pdf", comPrefixo(ArquivoValidador.MAX_PDF_BYTES + 1, PDF)));
            assertDoesNotThrow(() -> ArquivoValidador.validarPdf("x.pdf", comPrefixo(ArquivoValidador.MAX_PDF_BYTES, PDF)));
        }

        @Test
        void detectaJpegEPngPeloConteudoENaoPelaExtensao() {
            assertEquals("image/jpeg", ArquivoValidador.validarImagem("a.png", comPrefixo(50, JPEG)));
            assertEquals("image/png", ArquivoValidador.validarImagem("a.jpg", comPrefixo(50, PNG)));
        }

        @Test
        void rejeitaImagemInvalidaOuAcimaDe5Mb() {
            assertThrows(NordException.class, () -> ArquivoValidador.validarImagem("a.gif", comPrefixo(50, 'G', 'I', 'F')));
            assertThrows(NordException.class, () -> ArquivoValidador.validarImagem("a.jpg", comPrefixo(ArquivoValidador.MAX_IMAGEM_BYTES + 1, JPEG)));
            assertDoesNotThrow(() -> ArquivoValidador.validarImagem("a.jpg", comPrefixo(ArquivoValidador.MAX_IMAGEM_BYTES, JPEG)));
        }

        @Test
        void rejeitaArquivoVazioOuNulo() {
            assertThrows(NordException.class, () -> ArquivoValidador.validarPdf("x.pdf", new byte[0]));
            assertThrows(NordException.class, () -> ArquivoValidador.validarImagem("x.jpg", null));
            assertThrows(NordException.class, () -> ArquivoValidador.validarPdf("x.pdf", new byte[3]));
        }

        @Test
        void validaNome() {
            char[] longo = new char[181];
            Arrays.fill(longo, 'a');
            assertThrows(NordException.class, () -> ArquivoValidador.validarNome(new String(longo)));
            assertDoesNotThrow(() -> ArquivoValidador.validarNome(new String(longo, 0, 180)));
            assertThrows(NordException.class, () -> ArquivoValidador.validarNome("a\nb.pdf"));
            assertThrows(NordException.class, () -> ArquivoValidador.validarNome("a\u0000.pdf"));
            assertThrows(NordException.class, () -> ArquivoValidador.validarNome("  "));
            assertThrows(NordException.class, () -> ArquivoValidador.validarNome(null));
            assertDoesNotThrow(() -> ArquivoValidador.validarNome("Relatório de reprova (1º).pdf"));
        }

        @Test
        void contratoAceitaPdfEImagemAte15Mb() {
            assertEquals("application/pdf", ArquivoValidador.validarContrato("c.pdf", comPrefixo(100, PDF)));
            assertEquals("image/jpeg", ArquivoValidador.validarContrato("c.jpg", comPrefixo(100, JPEG)));
            assertEquals("image/png", ArquivoValidador.validarContrato("c.png", comPrefixo(100, PNG)));
            assertDoesNotThrow(() -> ArquivoValidador.validarContrato("c.jpg", comPrefixo(ArquivoValidador.MAX_CONTRATO_BYTES, JPEG)));
        }

        @Test
        void contratoRecusaOutrosTiposVazioEAcimaDe15Mb() {
            assertThrows(NordException.class, () -> ArquivoValidador.validarContrato("c.txt", comPrefixo(100, 'G', 'I', 'F')));
            assertThrows(NordException.class, () -> ArquivoValidador.validarContrato("c.pdf", new byte[0]));
            assertThrows(NordException.class, () -> ArquivoValidador.validarContrato("c.pdf", comPrefixo(ArquivoValidador.MAX_CONTRATO_BYTES + 1, PDF)));
            assertThrows(NordException.class, () -> ArquivoValidador.validarContrato("a\nb.pdf", comPrefixo(100, PDF)));
        }

        @Test
        void comprovanteCaixinhaExigePdfCompletoDentroDoLimite() {
            byte[] ok = "%PDF-1.4\nconteudo\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            assertEquals(ArquivoValidador.PDF, ArquivoValidador.validarComprovantePdf("n.pdf", ok, 1024));
            byte[] semEof = "%PDF-1.4\ncortado".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThrows(NordException.class, () -> ArquivoValidador.validarComprovantePdf("n.pdf", semEof, 1024));
            assertThrows(NordException.class, () -> ArquivoValidador.validarComprovantePdf("n.pdf", ok, 10));
            assertThrows(NordException.class, () -> ArquivoValidador.validarComprovantePdf("n.pdf", new byte[0], 1024));
            assertThrows(NordException.class, () -> ArquivoValidador.validarComprovantePdf("n.pdf", "GIF89a%%EOF".getBytes(), 1024));
            // %%EOF só vale nos últimos 1024 bytes
            byte[] longe = new byte[3000];
            System.arraycopy(ok, 0, longe, 0, ok.length);
            assertThrows(NordException.class, () -> ArquivoValidador.validarComprovantePdf("n.pdf", longe, 5000));
        }
    }

    // ---------- cotas e limites ----------

    @Test
    void fotoAcimaDaCotaDoTermoEhRecusadaSemGravarArquivo() {
        termoExiste(termo(1L, 3, "PENDENTE"));
        when(fotoRepository.contarPorTermo(1L)).thenReturn(TermoReprovaServiceImpl.MAX_FOTOS_POR_TERMO);
        assertThrows(EntradaInvalidaException.class, () ->
                service.adicionarFoto(1L, "f.jpg", new byte[]{1}, new byte[]{1}, 1, null));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.buscar(1L));
        verifyNoInteractions(termoRepository, fotoRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.TERMO_REPROVA, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.deletar(1L));
        verifyNoInteractions(termoRepository, fotoRepository);
    }
}
