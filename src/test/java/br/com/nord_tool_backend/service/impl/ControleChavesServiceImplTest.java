package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.ObraControleChaves;
import br.com.nord_tool_backend.domain.RequisicaoChaveConsulta;
import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.DashboardControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.dto.RetiradaControleChavesDto;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;
import br.com.nord_tool_backend.repository.ControleChavesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ControleChavesServiceImplTest {

    private static final LocalDateTime DT_RETIRADA = LocalDateTime.of(2026, 9, 29, 8, 30);
    private static final LocalDateTime DT_RECEBIMENTO = LocalDateTime.of(2026, 9, 29, 17, 45);

    @Mock


    private AutorizacaoService autorizacao;



    @InjectMocks
    private ControleChavesServiceImpl controleChavesService;

    @Mock
    private ControleChavesRepository controleChavesRepository;

    @Test
    void deveListarObrasExtraindoNomeEEliminandoDuplicadas() {
        when(controleChavesRepository.listarObras()).thenReturn(List.of(
                ObraControleChaves.builder().nmApartamentoVistoria("Obra Alfa -101").build(),
                ObraControleChaves.builder().nmApartamentoVistoria("Obra Alfa_102").build(),
                ObraControleChaves.builder().nmApartamentoVistoria("Obra Beta 203").build(),
                ObraControleChaves.builder().nmApartamentoVistoria("   ").build(),
                ObraControleChaves.builder().nmApartamentoVistoria(null).build()));

        List<ObraControleChavesDto> obras = controleChavesService.listarObras();
        assertEquals(2, obras.size());
        assertEquals("Obra Alfa", obras.get(0).getNmObra());
        assertEquals("Obra Alfa", obras.get(0).getIdObra());
        assertEquals("Obra Beta", obras.get(1).getNmObra());
        verify(controleChavesRepository).listarObras();
    }

    @Test
    void deveListarApartamentosAplicandoBuscaSemDiferenciarMaiusculasEPaginacao() {
        when(controleChavesRepository.listarApartamentos()).thenReturn(List.of(
                apartamento(1L, "Alfa 101"), apartamento(2L, "ALFA 102"),
                apartamento(3L, "Beta 201"), apartamento(4L, "Alfa 103")));

        List<ApartamentoControleChavesDto> apartamentos = controleChavesService.listarApartamentos("aLfA", 1, 1);
        assertEquals(1, apartamentos.size());
        assertEquals(2L, apartamentos.get(0).getIdApartamentoVistoria());
        verify(controleChavesRepository).listarApartamentos();
    }

    @Test
    void deveUsarValoresPadraoDePaginacaoQuandoLimiteInvalidoEPaginaNegativa() {
        when(controleChavesRepository.listarApartamentos()).thenReturn(List.of(apartamento(1L, "A 101"), apartamento(2L, "A 102")));
        List<ApartamentoControleChavesDto> apartamentos = controleChavesService.listarApartamentos(null, 0, -1);
        assertEquals(2, apartamentos.size());
    }

    @Test
    void deveRetornarDashboardComContagensERecentesFiltradasPorObra() {
        when(controleChavesRepository.listarHistorico()).thenReturn(List.of(
                consulta(1L, "RET-1", "Obra Alfa - 101", "Maria", "ABERTO", DT_RETIRADA, null, null),
                consulta(2L, "RET-2", "Obra Beta 201", "João", "RECEBIDO", DT_RETIRADA,
                        DT_RECEBIMENTO, 9L),
                consulta(3L, "RET-3", "Obra Alfa_102", "Ana", "ABERTO", DT_RETIRADA, null, null)));
        when(controleChavesRepository.contarChavesEmCampo()).thenReturn(2L);
        when(controleChavesRepository.contarChavesNoQuadro()).thenReturn(5L);
        when(controleChavesRepository.contarChavesEntregues()).thenReturn(3L);

        DashboardControleChavesDto dashboard = controleChavesService.buscarDashboard(1, "obra alfa");

        assertEquals(2L, dashboard.getQtChavesEmCampo());
        assertEquals(5L, dashboard.getQtChavesNoQuadro());
        assertEquals(3L, dashboard.getQtChavesEntregues());
        assertEquals(1, dashboard.getRetiradasRecentes().size());
        assertEquals(1L, dashboard.getRetiradasRecentes().get(0).getIdRequisicao());
        verify(controleChavesRepository).contarChavesEmCampo();
        verify(controleChavesRepository).contarChavesNoQuadro();
        verify(controleChavesRepository).contarChavesEntregues();
    }

    @Test
    void deveAplicarBuscaStatusObraEPaginacaoAoHistorico() {
        when(controleChavesRepository.listarHistorico()).thenReturn(List.of(
                consulta(1L, "RET-100", "Obra Alfa - 101", "Maria", "ABERTO", DT_RETIRADA, null, null),
                consulta(2L, "RET-200", "Obra Alfa 102", "João", "RECEBIDO", DT_RETIRADA, DT_RECEBIMENTO, 9L),
                consulta(3L, "RET-300", "Obra Beta 201", "Maria", "ABERTO", DT_RETIRADA, null, null)));

        List<RetiradaControleChavesDto> historico = controleChavesService.listarHistorico("mArIa", "aberto", "Obra Alfa", 10, 0);
        assertEquals(1, historico.size());
        assertEquals(1L, historico.get(0).getIdRequisicao());
    }

    @Test
    void deveBuscarHistoricoPorCodigoDeRetirada() {
        when(controleChavesRepository.listarHistorico()).thenReturn(List.of(
                consulta(1L, "RET-ABC", "Obra Alfa 101", "Maria", "ABERTO", DT_RETIRADA, null, null),
                consulta(2L, "RET-XYZ", "Obra Alfa 102", "João", "ABERTO", DT_RETIRADA, null, null)));

        List<RetiradaControleChavesDto> historico =
                controleChavesService.listarHistorico("abc", null, null, 20, 0);

        assertEquals(1, historico.size());
        assertEquals("RET-ABC", historico.get(0).getCdCodigoRetirada());
    }

    @Test
    void deveRejeitarStatusDeFiltroInvalido() {
        assertThrows(NordException.class,
                () -> controleChavesService.listarHistorico(null, "CANCELADO", null, 20, 0));

        verifyNoInteractions(controleChavesRepository);
    }

    @Test
    void deveCriarRetiradaComStatusAbertoEDadosDoForm() {
        when(controleChavesRepository.criarRetirada(any())).thenReturn(15L);
        when(controleChavesRepository.buscarPorId(15L)).thenReturn(
                consulta(15L, "RET-15", "Obra Alfa 101", "Maria", "ABERTO", DT_RETIRADA, null, null));
        NovaRetiradaControleChavesForm novaRetiradaControleChavesForm =
                NovaRetiradaControleChavesForm.builder()
                        .nmTipoItem("APARTAMENTO")
                        .idApartamentoVistoria(101L)
                        .idUserRetirada(7L)
                        .idUserLiberacao(8L)
                        .build();

        RetiradaControleChavesDto retirada = controleChavesService.criarRetirada(novaRetiradaControleChavesForm);

        assertEquals(15L, retirada.getIdRequisicao());
        assertEquals("ABERTO", retirada.getNmStatusRetiradaControle());
        verify(controleChavesRepository).criarRetirada(org.mockito.ArgumentMatchers.argThat(requisicao ->
                requisicao.getIdApartamentoVistoria().equals(101L)
                        && requisicao.getIdUserRetirada().equals(7L)
                        && requisicao.getIdUserLiberacao().equals(8L)
                        && "ABERTO".equals(requisicao.getNmStatusRequisicao())
                        && requisicao.getCdRetirada().startsWith("RET-")
                        && requisicao.getDtRetirada() != null));
    }

    @Test
    void deveCriarRetiradaDeFerramenta() {
        when(controleChavesRepository.criarRetirada(any())).thenReturn(16L);
        when(controleChavesRepository.buscarPorId(16L)).thenReturn(
                RequisicaoChaveConsulta.builder()
                        .idRequisicao(16L)
                        .cdRetirada("RET-16")
                        .dtRetirada(DT_RETIRADA)
                        .idFerramenta(5L)
                        .nmFerramenta("Furadeira")
                        .cdTipoItemRequisicao("FERRAMENTA")
                        .idUserRetirada(7L)
                        .nmPessoaRetirante("Maria")
                        .idUserLiberacao(8L)
                        .nmPessoaLiberador("João")
                        .nmStatusRequisicao("ABERTO")
                        .build());
        NovaRetiradaControleChavesForm novaRetiradaControleChavesForm =
                NovaRetiradaControleChavesForm.builder()
                        .nmTipoItem("FERRAMENTA")
                        .idFerramenta(5L)
                        .idUserRetirada(7L)
                        .idUserLiberacao(8L)
                        .build();

        RetiradaControleChavesDto retirada = controleChavesService.criarRetirada(novaRetiradaControleChavesForm);

        assertEquals(16L, retirada.getIdRequisicao());
        assertNull(retirada.getApartamentoControleChavesDto());
        assertNotNull(retirada.getFerramentaControleChavesDto());
        assertEquals("Furadeira", retirada.getFerramentaControleChavesDto().getNmFerramenta());
        verify(controleChavesRepository).criarRetirada(org.mockito.ArgumentMatchers.argThat(requisicao ->
                requisicao.getIdFerramenta().equals(5L)
                        && requisicao.getIdApartamentoVistoria() == null
                        && "FERRAMENTA".equals(requisicao.getCdTipoItemRequisicao())));
    }

    @Test
    void deveRejeitarRetiradaComTipoApartamentoEIdFerramentaPreenchido() {
        NovaRetiradaControleChavesForm novaRetiradaControleChavesForm =
                NovaRetiradaControleChavesForm.builder()
                        .nmTipoItem("APARTAMENTO")
                        .idApartamentoVistoria(101L)
                        .idFerramenta(5L)
                        .idUserRetirada(7L)
                        .idUserLiberacao(8L)
                        .build();

        assertThrows(NordException.class,
                () -> controleChavesService.criarRetirada(novaRetiradaControleChavesForm));

        verifyNoInteractions(controleChavesRepository);
    }

    @Test
    void deveRejeitarCriacaoComFormNulo() {
        assertThrows(NordException.class, () -> controleChavesService.criarRetirada(null));

        verifyNoInteractions(controleChavesRepository);
    }

    @Test
    void deveRejeitarCriacaoComIdsObrigatoriosInvalidos() {
        NovaRetiradaControleChavesForm novaRetiradaControleChavesForm =
                NovaRetiradaControleChavesForm.builder()
                        .idApartamentoVistoria(0L)
                        .idUserRetirada(7L)
                        .idUserLiberacao(8L)
                        .build();

        assertThrows(NordException.class,
                () -> controleChavesService.criarRetirada(novaRetiradaControleChavesForm));

        verifyNoInteractions(controleChavesRepository);
    }

    @Test
    void deveReceberRetiradaAbertaEDevolverDtoCompleto() {
        when(controleChavesRepository.buscarPorId(20L)).thenReturn(
                consulta(20L, "RET-20", "Obra Alfa 101", "Maria", "ABERTO", DT_RETIRADA, null, null),
                consulta(20L, "RET-20", "Obra Alfa 101", "Maria", "RECEBIDO", DT_RETIRADA,
                        DT_RECEBIMENTO, 9L));
        RecebimentoControleChavesForm recebimentoControleChavesForm =
                RecebimentoControleChavesForm.builder().idUserRecebimento(9L).build();

        RetiradaControleChavesDto retirada = controleChavesService.receberRetirada(20L, recebimentoControleChavesForm);

        assertEquals("RECEBIDO", retirada.getNmStatusRetiradaControle());
        assertNotNull(retirada.getRecebedorControleChavesDto());
        assertEquals(9L, retirada.getRecebedorControleChavesDto().getIdUserRecebimento());
        verify(controleChavesRepository).receberRetirada(eq(20L), eq(9L), any(LocalDateTime.class), eq("RECEBIDO"));
    }

    @Test
    void deveRejeitarRecebimentoDeRequisicaoJaRecebida() {
        when(controleChavesRepository.buscarPorId(20L)).thenReturn(
                consulta(20L, "RET-20", "Obra Alfa 101", "Maria", "RECEBIDO", DT_RETIRADA,
                        DT_RECEBIMENTO, 9L));

        assertThrows(NordException.class, () -> controleChavesService.receberRetirada(20L,
                RecebimentoControleChavesForm.builder().idUserRecebimento(9L).build()));

        verify(controleChavesRepository, never()).receberRetirada(anyLong(), anyLong(), any(), any());
    }

    @Test
    void deveRejeitarRecebimentoComIdOuFormInvalido() {
        assertThrows(NordException.class, () -> controleChavesService.receberRetirada(0L,
                RecebimentoControleChavesForm.builder().idUserRecebimento(9L).build()));
        assertThrows(NordException.class, () -> controleChavesService.receberRetirada(20L, null));
        assertThrows(NordException.class, () -> controleChavesService.receberRetirada(20L,
                RecebimentoControleChavesForm.builder().idUserRecebimento(null).build()));

        verifyNoInteractions(controleChavesRepository);
    }

    @Test
    void deveRejeitarDadosDeRecebimentoInconsistentesNaLeitura() {
        when(controleChavesRepository.buscarPorId(20L)).thenReturn(
                consulta(20L, "RET-20", "Obra Alfa 101", "Maria", "ABERTO", DT_RETIRADA,
                        DT_RECEBIMENTO, 9L));

        assertThrows(NordException.class, () -> controleChavesService.receberRetirada(20L,
                RecebimentoControleChavesForm.builder().idUserRecebimento(9L).build()));

        verify(controleChavesRepository, never()).receberRetirada(anyLong(), anyLong(), any(), any());
    }

    @Test
    void deveRejeitarStatusRecebidoSemDadosDeRecebimentoNoHistorico() {
        when(controleChavesRepository.listarHistorico()).thenReturn(List.of(
                consulta(20L, "RET-20", "Obra Alfa 101", "Maria", "RECEBIDO", DT_RETIRADA, null, null)));

        assertThrows(NordException.class,
                () -> controleChavesService.listarHistorico(null, null, null, 20, 0));
    }

    @Test
    void deveReportarConflitoQuandoOutraPessoaRecebeuDuranteAtualizacao() {
        when(controleChavesRepository.buscarPorId(20L)).thenReturn(
                consulta(20L, "RET-20", "Obra Alfa 101", "Maria", "ABERTO", DT_RETIRADA, null, null),
                consulta(20L, "RET-20", "Obra Alfa 101", "Maria", "RECEBIDO", DT_RETIRADA,
                        DT_RECEBIMENTO, 10L));

        assertThrows(NordException.class, () -> controleChavesService.receberRetirada(20L,
                RecebimentoControleChavesForm.builder().idUserRecebimento(9L).build()));

        verify(controleChavesRepository).receberRetirada(eq(20L), eq(9L), any(LocalDateTime.class), eq("RECEBIDO"));
    }

    @Test
    void deveRetornarNullAoConverterConsultaNulaParaDto() {
        assertNull(RetiradaControleChavesDto.converterToDto(null));
    }

    private ApartamentoVistoria apartamento(Long idApartamentoVistoria, String nmApartamentoVistoria) {
        return ApartamentoVistoria.builder()
                .id(idApartamentoVistoria)
                .nmApartamentoVistoria(nmApartamentoVistoria)
                .build();
    }

    private RequisicaoChaveConsulta consulta(Long idRequisicao, String cdRetirada,
                                              String nmApartamentoVistoria, String nmPessoaRetirante,
                                              String nmStatusRequisicao, LocalDateTime dtRetirada,
                                              LocalDateTime dtRecebimento, Long idUserRecebimento) {
        return RequisicaoChaveConsulta.builder()
                .idRequisicao(idRequisicao)
                .cdRetirada(cdRetirada)
                .dtRetirada(dtRetirada)
                .dtRecebimento(dtRecebimento)
                .idApartamentoVistoria(101L)
                .nmApartamentoVistoria(nmApartamentoVistoria)
                .idUserRetirada(7L)
                .nmPessoaRetirante(nmPessoaRetirante)
                .nmPermissaoRetirante("Retirante")
                .idUserLiberacao(8L)
                .nmPessoaLiberador("João")
                .nmPermissaoLiberador("Liberador")
                .idUserRecebimento(idUserRecebimento)
                .nmPessoaRecebedor(idUserRecebimento == null ? null : "Recebedor")
                .nmPermissaoRecebedor(idUserRecebimento == null ? null : "Recebedor")
                .nmStatusRequisicao(nmStatusRequisicao)
                .build();
    }

    // ---------- cotas e limites ----------

    @Test
    void paginaNuncaPassaDoLimiteMesmoQuandoOClientePedeMais() {
        when(controleChavesRepository.listarApartamentos()).thenReturn(LongStream.rangeClosed(1, 150)
                .mapToObj(i -> apartamento(i, "A " + i)).collect(Collectors.toList()));
        assertEquals(ControleChavesServiceImpl.MAX_POR_PAGINA, controleChavesService.listarApartamentos(null, 500, 0).size());
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> controleChavesService.listarObras());
        verifyNoInteractions(controleChavesRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> controleChavesService.criarRetirada(null));
        verifyNoInteractions(controleChavesRepository);
    }
}
