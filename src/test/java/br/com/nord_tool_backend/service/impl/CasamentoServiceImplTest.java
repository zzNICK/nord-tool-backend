package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.domain.CasamentoMarco;
import br.com.nord_tool_backend.dto.CasamentoConfiguracaoDto;
import br.com.nord_tool_backend.dto.CasamentoDashboardDto;
import br.com.nord_tool_backend.dto.CasamentoTotaisDto;
import br.com.nord_tool_backend.form.CasamentoConfiguracaoForm;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class CasamentoServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private CasamentoRepository repository;
    private CasamentoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CasamentoRepository.class);
        service = new CasamentoServiceImpl(repository, autorizacao);
    }

    private CasamentoMarco marco(long id, String titulo, String prazo) {
        CasamentoMarco m = new CasamentoMarco();
        m.setId(id);
        m.setNmTitulo(titulo);
        m.setDtPrazo(prazo == null ? null : LocalDate.parse(prazo));
        m.setInConcluido(false);
        return m;
    }

    private CasamentoTotaisDto totais(int marcos, int concluidos) {
        CasamentoTotaisDto t = new CasamentoTotaisDto();
        t.setQtFornecedores(5);
        t.setQtFornecedoresContratados(2);
        t.setVlContratado(new BigDecimal("16000.50"));
        t.setQtConvidados(80);
        t.setQtConvidadosConfirmados(30);
        t.setQtPessoasConfirmadas(52);
        t.setQtMarcos(marcos);
        t.setQtMarcosConcluidos(concluidos);
        return t;
    }

    @Test
    void configuracaoVemDasChavesOuNulaQuandoAusente() {
        Map<String, String> config = new HashMap<>();
        config.put("casal", "Nicolas & Thaina");
        config.put("dataCasamento", "2027-10-12");
        when(repository.listarConfiguracao()).thenReturn(config);

        CasamentoConfiguracaoDto dto = service.buscarConfiguracao();

        assertEquals("Nicolas & Thaina", dto.getCasal());
        assertEquals("2027-10-12", dto.getDataCasamento());

        when(repository.listarConfiguracao()).thenReturn(new HashMap<>());
        assertNull(service.buscarConfiguracao().getCasal());
        assertNull(service.buscarConfiguracao().getDataCasamento());
    }

    @Test
    void salvaCasalEDataAparandoOTexto() {
        when(repository.listarConfiguracao()).thenReturn(new HashMap<>());
        CasamentoConfiguracaoForm form = new CasamentoConfiguracaoForm();
        form.setCasal("  Ana & Beto ");
        form.setDataCasamento("2027-10-12");

        service.salvarConfiguracao(form);

        verify(repository).salvarConfiguracao("casal", "Ana & Beto");
        verify(repository).salvarConfiguracao("dataCasamento", "2027-10-12");
    }

    @Test
    void recusaDataInexistente() {
        CasamentoConfiguracaoForm form = new CasamentoConfiguracaoForm();
        form.setCasal("Ana & Beto");
        form.setDataCasamento("2027-02-31");

        assertThrows(NordException.class, () -> service.salvarConfiguracao(form));
        verify(repository, never()).salvarConfiguracao(anyString(), anyString());
    }

    @Test
    void dashboardMontaTotaisPercentualEProximosMarcos() {
        when(repository.buscarTotais()).thenReturn(totais(10, 3));
        when(repository.listarConfiguracao()).thenReturn(new HashMap<>());
        when(repository.listarProximosMarcos(5)).thenReturn(List.of(marco(1, "A", "2026-10-12"), marco(2, "B", null)));

        CasamentoDashboardDto d = service.dashboard();

        assertEquals(5, d.getQtFornecedores());
        assertEquals(2, d.getQtFornecedoresContratados());
        assertEquals(new BigDecimal("16000.50"), d.getVlContratado());
        assertEquals(80, d.getQtConvidados());
        assertEquals(30, d.getQtConvidadosConfirmados());
        assertEquals(52, d.getQtPessoasConfirmadas());
        assertEquals(10, d.getQtMarcos());
        assertEquals(3, d.getQtMarcosConcluidos());
        assertEquals(30, d.getPcMarcosConcluidos());
        assertEquals(2, d.getProximosMarcos().size());
        assertEquals("A", d.getProximosMarcos().get(0).getNmTitulo());
    }

    @Test
    void percentualArredondaESemMarcosEhZero() {
        when(repository.listarConfiguracao()).thenReturn(new HashMap<>());
        when(repository.listarProximosMarcos(5)).thenReturn(List.of());

        when(repository.buscarTotais()).thenReturn(totais(3, 2));
        assertEquals(67, service.dashboard().getPcMarcosConcluidos());

        when(repository.buscarTotais()).thenReturn(totais(0, 0));
        assertEquals(0, service.dashboard().getPcMarcosConcluidos());
    }

    @Test
    void totaisNulosViramZero() {
        when(repository.buscarTotais()).thenReturn(new CasamentoTotaisDto());
        when(repository.listarConfiguracao()).thenReturn(new HashMap<>());
        when(repository.listarProximosMarcos(5)).thenReturn(List.of());

        CasamentoDashboardDto d = service.dashboard();

        assertEquals(0, d.getQtConvidados());
        assertEquals(BigDecimal.ZERO, d.getVlContratado());
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.buscarConfiguracao());
        verifyNoInteractions(repository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.salvarConfiguracao(null));
        verifyNoInteractions(repository);
    }
}
