package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.CaixinhaComprovante;
import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.domain.CaixinhaLancamento;
import br.com.nord_tool_backend.domain.CaixinhaResponsavel;
import br.com.nord_tool_backend.dto.CaixinhaComprovanteDto;
import br.com.nord_tool_backend.dto.CaixinhaLancamentoDto;
import br.com.nord_tool_backend.dto.CaixinhaResumoDto;
import br.com.nord_tool_backend.form.CaixinhaLancamentoForm;
import br.com.nord_tool_backend.form.CaixinhaMarcacaoForm;
import br.com.nord_tool_backend.form.CaixinhaResponsavelForm;
import br.com.nord_tool_backend.repository.CaixinhaRepository;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import br.com.nord_tool_backend.dto.*;
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

class CaixinhaServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private static final String UUID1 = "11111111-1111-1111-1111-111111111111";
    private static final String UUID2 = "22222222-2222-2222-2222-222222222222";
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF\n".getBytes(StandardCharsets.ISO_8859_1);

    private CaixinhaRepository repository;
    private ArmazenamentoService armazenamento;
    private CaixinhaServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CaixinhaRepository.class);
        armazenamento = mock(ArmazenamentoService.class);
        service = new CaixinhaServiceImpl(repository, armazenamento, 5 * 1024 * 1024, autorizacao);
    }

    private CaixinhaResponsavel responsavel(long id, boolean ativo) {
        CaixinhaResponsavel r = new CaixinhaResponsavel();
        r.setId(id);
        r.setNmResponsavel("Resp " + id);
        r.setInAtivo(ativo);
        return r;
    }

    private CaixinhaLancamento lancamento(long id, int versao) {
        CaixinhaLancamento l = new CaixinhaLancamento();
        l.setId(id);
        l.setDtLancamento(LocalDate.of(2026, 10, 1));
        l.setIdResponsavel(1L);
        l.setNmResponsavel("Resp 1");
        l.setTxInsumo("Cimento");
        l.setVlValor(new BigDecimal("10.50"));
        l.setInLancado(false);
        l.setInPago(false);
        l.setNrVersao(versao);
        l.setQtComprovantes(0);
        return l;
    }

    private CaixinhaLancamentoForm form(String requisicao) {
        CaixinhaLancamentoForm f = new CaixinhaLancamentoForm();
        f.setCdRequisicao(requisicao);
        f.setDtLancamento(LocalDate.of(2026, 10, 1));
        f.setIdResponsavel(1L);
        f.setTxInsumo("  Cimento ");
        f.setVlValor(new BigDecimal("10.50"));
        return f;
    }

    private void esperaErro(NordHttpEnum esperado, Runnable acao) {
        NordException ex = assertThrows(NordException.class, acao::run);
        assertEquals(esperado, ex.getStatus());
    }

    // ---------- criação idempotente ----------

    @Test
    void criaLancamentoNovoComInsumoSemEspacos() {
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.empty());
        when(repository.buscarResponsavel(1L)).thenReturn(Optional.of(responsavel(1, true)));
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(5L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));

        CaixinhaLancamentoDto dto = service.criar(form(UUID1));

        assertEquals(5L, dto.getIdLancamento());
        ArgumentCaptor<CaixinhaLancamento> captor = ArgumentCaptor.forClass(CaixinhaLancamento.class);
        verify(repository).inserirLancamento(captor.capture());
        assertEquals("Cimento", captor.getValue().getTxInsumo());
        assertEquals(UUID1, captor.getValue().getCdRequisicao());
    }

    @Test
    void reenvioComMesmoCdRequisicaoDevolveOJaCriadoSemInserir() {
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.of(5L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));

        assertEquals(5L, service.criar(form(UUID1)).getIdLancamento());

        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void corridaNaCriacaoDevolveOLancamentoVencedor() {
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.empty(), Optional.of(7L));
        when(repository.buscarResponsavel(1L)).thenReturn(Optional.of(responsavel(1, true)));
        when(repository.inserirLancamento(any())).thenReturn(Optional.empty());
        when(repository.buscarLancamento(7L)).thenReturn(Optional.of(lancamento(7, 1)));

        assertEquals(7L, service.criar(form(UUID1)).getIdLancamento());
    }

    @Test
    void criacaoExigeUuidValido() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(null)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form("nao-e-uuid")));
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void responsavelInexistenteOuInativoEhRecusadoNaCriacao() {
        when(repository.buscarLancamentoPorRequisicao(anyString())).thenReturn(Optional.empty());
        when(repository.buscarResponsavel(1L)).thenReturn(Optional.empty());
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(UUID1)));

        when(repository.buscarResponsavel(1L)).thenReturn(Optional.of(responsavel(1, false)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(UUID1)));
        verify(repository, never()).inserirLancamento(any());
    }

    // ---------- concorrência (409) ----------

    @Test
    void edicaoComVersaoDivergenteResponde409() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 3)));
        when(repository.buscarResponsavel(1L)).thenReturn(Optional.of(responsavel(1, true)));
        when(repository.alterarLancamento(any(), eq(2))).thenReturn(0);
        CaixinhaLancamentoForm f = form(null);
        f.setNrVersao(2);

        NordException ex = assertThrows(NordException.class, () -> service.alterar(5L, f));

        assertEquals(NordHttpEnum.HTTP_409, ex.getStatus());
        assertEquals("O lançamento mudou. Sincronize e tente novamente.", ex.getMessage());
    }

    @Test
    void edicaoComVersaoCorretaAltera() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 3)));
        when(repository.buscarResponsavel(1L)).thenReturn(Optional.of(responsavel(1, true)));
        when(repository.alterarLancamento(any(), eq(3))).thenReturn(1);
        CaixinhaLancamentoForm f = form(null);
        f.setNrVersao(3);

        assertEquals(5L, service.alterar(5L, f).getIdLancamento());
    }

    @Test
    void edicaoSemVersaoOuLancamentoInexistente() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.alterar(5L, form(null)));

        when(repository.buscarLancamento(9L)).thenReturn(Optional.empty());
        CaixinhaLancamentoForm f = form(null);
        f.setNrVersao(1);
        esperaErro(NordHttpEnum.HTTP_404, () -> service.alterar(9L, f));
    }

    @Test
    void manterResponsavelJaInativoNaEdicaoEhPermitido() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.buscarResponsavel(1L)).thenReturn(Optional.of(responsavel(1, false)));
        when(repository.alterarLancamento(any(), eq(1))).thenReturn(1);
        CaixinhaLancamentoForm f = form(null);
        f.setNrVersao(1);

        assertDoesNotThrow(() -> service.alterar(5L, f));
    }

    @Test
    void marcacaoMantemOQueNaoFoiEnviadoEExigeVersao() {
        CaixinhaLancamento atual = lancamento(5, 4);
        atual.setInLancado(true);
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(atual));
        when(repository.marcarLancamento(5L, true, true, 4)).thenReturn(1);
        CaixinhaMarcacaoForm f = new CaixinhaMarcacaoForm();
        f.setPago(true);
        f.setNrVersao(4);

        service.marcar(5L, f);

        verify(repository).marcarLancamento(5L, true, true, 4);

        when(repository.marcarLancamento(anyLong(), anyBoolean(), anyBoolean(), eq(1))).thenReturn(0);
        f.setNrVersao(1);
        esperaErro(NordHttpEnum.HTTP_409, () -> service.marcar(5L, f));
    }

    // ---------- exclusão ----------

    private CaixinhaComprovante comprovante(long id, long idArquivo) {
        CaixinhaComprovante c = new CaixinhaComprovante();
        c.setId(id);
        c.setIdLancamento(5L);
        c.setIdArquivo(idArquivo);
        c.setNmArquivo("nota.pdf");
        c.setNmContentType("application/pdf");
        c.setNrTamanhoBytes(100L);
        c.setDhCriacao(LocalDateTime.of(2026, 10, 1, 10, 0));
        return c;
    }

    @Test
    void exclusaoApagaOsArquivosDosComprovantes() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 2)));
        when(repository.listarComprovantes(5L)).thenReturn(Arrays.asList(comprovante(1, 100), comprovante(2, 101)));
        when(repository.deletarLancamento(5L, 2)).thenReturn(1);

        service.excluir(5L, 2);

        verify(armazenamento).apagar(100L);
        verify(armazenamento).apagar(101L);
    }

    @Test
    void exclusaoComVersaoDivergenteResponde409ENaoApagaArquivos() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 2)));
        when(repository.listarComprovantes(5L)).thenReturn(Collections.singletonList(comprovante(1, 100)));
        when(repository.deletarLancamento(5L, 1)).thenReturn(0);

        esperaErro(NordHttpEnum.HTTP_409, () -> service.excluir(5L, 1));

        verify(armazenamento, never()).apagar(anyLong());
    }

    // ---------- resumo e filtros ----------

    @Test
    void resumoCalculaAPagarEPendentes() {
        when(repository.resumir(any())).thenReturn(new CaixinhaRepository.Totais(new BigDecimal("300.00"), new BigDecimal("120.50"), 5, 2));

        CaixinhaResumoDto r = service.resumir(new CaixinhaFiltro(null, "TODOS", null, null));

        assertEquals(new BigDecimal("179.50"), r.getAPagar());
        assertEquals(5, r.getQtLancamentos());
        assertEquals(2, r.getQtPagos());
        assertEquals(3, r.getQtPendentes());
    }

    @Test
    void resumoVazioZeraTudo() {
        when(repository.resumir(any())).thenReturn(new CaixinhaRepository.Totais(null, null, 0, 0));

        CaixinhaResumoDto r = service.resumir(null);

        assertEquals(BigDecimal.ZERO, r.getTotal());
        assertEquals(BigDecimal.ZERO, r.getAPagar());
    }

    @Test
    void filtroInvalidoEhRecusado() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.listar(new CaixinhaFiltro(null, "QUALQUER", null, null)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.resumir(new CaixinhaFiltro(null, null, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1))));
    }

    @Test
    void listaTrazLancamentosEResponsaveis() {
        when(repository.listarLancamentos(any())).thenReturn(Collections.singletonList(lancamento(5, 1)));
        when(repository.listarResponsaveis()).thenReturn(Collections.singletonList(responsavel(1, true)));

        assertEquals(1, service.listar(null).getLancamentos().size());
        assertEquals(1, service.listar(null).getResponsaveis().size());
    }

    // ---------- responsáveis ----------

    @Test
    void responsavelDuplicadoSemDiferenciarCaixaEhRecusado() {
        when(repository.listarResponsaveis()).thenReturn(Collections.singletonList(responsavel(1, true)));
        CaixinhaResponsavelForm f = new CaixinhaResponsavelForm();
        f.setNmResponsavel("  resp 1 ");

        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarResponsavel(f));
        verify(repository, never()).inserirResponsavel(any());
    }

    @Test
    void desativaResponsavelSemMudarONome() {
        CaixinhaResponsavel r = responsavel(1, true);
        when(repository.buscarResponsavel(1L)).thenReturn(Optional.of(r));
        CaixinhaResponsavelForm f = new CaixinhaResponsavelForm();
        f.setInAtivo(false);

        service.atualizarResponsavel(1L, f);

        verify(repository).alterarResponsavel(argThat(x -> !x.getInAtivo() && "Resp 1".equals(x.getNmResponsavel())));
    }

    // ---------- comprovantes ----------

    @Test
    void anexaPdfValidoEGravaOArquivo() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.buscarComprovantePorRequisicao(UUID2)).thenReturn(Optional.empty());
        when(armazenamento.salvar(eq("nota.pdf"), eq("application/pdf"), any())).thenReturn(100L);
        when(repository.inserirComprovante(5L, 100L, UUID2)).thenReturn(9L);
        when(repository.buscarComprovante(9L)).thenReturn(Optional.of(comprovante(9, 100)));

        CaixinhaComprovanteDto dto = service.anexarComprovante(5L, "nota.pdf", PDF, UUID2);

        assertEquals(9L, dto.getIdComprovante());
    }

    @Test
    void reenvioDoMesmoComprovanteNaoGravaOutroArquivo() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.buscarComprovantePorRequisicao(UUID2)).thenReturn(Optional.of(9L));
        when(repository.buscarComprovante(9L)).thenReturn(Optional.of(comprovante(9, 100)));

        assertEquals(9L, service.anexarComprovante(5L, "nota.pdf", PDF, UUID2).getIdComprovante());

        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
        verify(repository, never()).inserirComprovante(anyLong(), anyLong(), any());
    }

    @Test
    void recusaArquivoQueNaoEhPdfCompletoOuPassaDoLimite() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};
        byte[] semEof = "%PDF-1.4\nconteudo cortado".getBytes(StandardCharsets.ISO_8859_1);
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        esperaErro(NordHttpEnum.HTTP_400, () -> service.anexarComprovante(5L, "foto.png", png, null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.anexarComprovante(5L, "nota.pdf", semEof, null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.anexarComprovante(5L, "nota.pdf", grande, null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.anexarComprovante(5L, "nota.pdf", PDF, "xx"));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
    }

    @Test
    void excluiComprovanteEApagaOArquivo() {
        when(repository.buscarComprovante(9L)).thenReturn(Optional.of(comprovante(9, 100)));

        service.excluirComprovante(9L);

        verify(repository).deletarComprovante(9L);
        verify(armazenamento).apagar(100L);
    }

    @Test
    void comprovanteInexistenteResponde404() {
        when(repository.buscarComprovante(anyLong())).thenReturn(Optional.empty());
        esperaErro(NordHttpEnum.HTTP_404, () -> service.excluirComprovante(1L));
        esperaErro(NordHttpEnum.HTTP_404, () -> service.baixarComprovante(1L));
    }

    @Nested
    class CaixinhaResumoJsonTest {

        @Test
        void resumoSerializaOsNomesQueOFrontendLe() throws Exception {
            String json = new ObjectMapper().writeValueAsString(
                    new CaixinhaResumoDto(BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"), 2, 1, 1));
            for (String campo : new String[]{"total", "pago", "aPagar", "qtLancamentos", "qtPagos", "qtPendentes"}) {
                assertTrue(json.contains("\"" + campo + "\":"), "falta o campo " + campo + " em " + json);
            }
            assertFalse(json.contains("\"apagar\""), json);
        }

        @Test
        void lancamentoSerializaOsNomesQueOFrontendLe() throws Exception {
            String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(
                    new CaixinhaLancamentoDto(1L, java.time.LocalDate.of(2026, 10, 1), 1L, "Ana", "x", BigDecimal.TEN, true, false, 1, 0));
            for (String campo : new String[]{"idLancamento", "dtLancamento", "idResponsavel", "nmResponsavel", "txInsumo", "vlValor",
                    "inLancado", "inPago", "nrVersao", "qtComprovantes"}) {
                assertTrue(json.contains("\"" + campo + "\":"), "falta o campo " + campo + " em " + json);
            }
            assertTrue(json.contains("\"dtLancamento\":\"01/10/2026\""), json);
        }
    }

    // ---------- cotas e limites ----------

    @Test
    void comprovanteAcimaDaCotaDoLancamentoEhRecusadoSemGravarArquivo() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.listarComprovantes(5L)).thenReturn(Collections.nCopies(
                CaixinhaServiceImpl.MAX_COMPROVANTES_POR_LANCAMENTO, new CaixinhaComprovante()));
        assertThrows(EntradaInvalidaException.class, () -> service.anexarComprovante(5L, "c.pdf", PDF, null));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CAIXINHA, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.listar(null));
        verifyNoInteractions(repository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CAIXINHA, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.excluir(1L, 1));
        verifyNoInteractions(repository);
    }
}
