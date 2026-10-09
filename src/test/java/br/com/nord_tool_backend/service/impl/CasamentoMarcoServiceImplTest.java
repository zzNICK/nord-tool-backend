package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.domain.CasamentoMarco;
import br.com.nord_tool_backend.dto.CasamentoMarcoDto;
import br.com.nord_tool_backend.form.CasamentoMarcoForm;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class CasamentoMarcoServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private CasamentoRepository repository;
    private CasamentoMarcoServiceImpl marcos;

    @BeforeEach
    void setUp() {
        repository = mock(CasamentoRepository.class);
        marcos = new CasamentoMarcoServiceImpl(repository, autorizacao);
    }

    private CasamentoMarco marco(long id, boolean concluido) {
        CasamentoMarco m = new CasamentoMarco();
        m.setId(id);
        m.setNmTitulo("Marco " + id);
        m.setInConcluido(concluido);
        return m;
    }

    @Test
    void marcosPadraoSaoOsDezDoLugiaComPrazosFixos() {
        when(repository.contarMarcos()).thenReturn(0);
        when(repository.listarMarcos()).thenReturn(List.of());

        marcos.criarPadrao();

        ArgumentCaptor<CasamentoMarco> captor = ArgumentCaptor.forClass(CasamentoMarco.class);
        verify(repository, times(10)).inserirMarco(captor.capture());
        List<CasamentoMarco> criados = captor.getAllValues();
        assertEquals("Definir orçamento total e reserva", criados.get(0).getNmTitulo());
        assertEquals(LocalDate.parse("2026-10-12"), criados.get(0).getDtPrazo());
        assertEquals("Confirmar logística e cronograma final", criados.get(9).getNmTitulo());
        assertEquals(LocalDate.parse("2027-09-12"), criados.get(9).getDtPrazo());
        assertTrue(criados.stream().noneMatch(m -> Boolean.TRUE.equals(m.getInConcluido())));
    }

    @Test
    void marcosPadraoSoComATabelaVazia() {
        when(repository.contarMarcos()).thenReturn(3);

        assertThrows(NordException.class, () -> marcos.criarPadrao());
        verify(repository, never()).inserirMarco(any());
    }

    @Test
    void criaMarcoNaoConcluidoEAlteraSemMexerNaConclusao() {
        when(repository.inserirMarco(any())).thenReturn(4L);
        when(repository.buscarMarco(4L)).thenReturn(Optional.of(marco(4, false)));
        CasamentoMarcoForm form = new CasamentoMarcoForm();
        form.setNmTitulo("  Contratar DJ ");
        form.setDtPrazo(LocalDate.parse("2027-05-01"));
        form.setTxObservacao("  ");

        CasamentoMarcoDto dto = marcos.criar(form);

        ArgumentCaptor<CasamentoMarco> captor = ArgumentCaptor.forClass(CasamentoMarco.class);
        verify(repository).inserirMarco(captor.capture());
        assertEquals("Contratar DJ", captor.getValue().getNmTitulo());
        assertEquals(false, captor.getValue().getInConcluido());
        assertNull(captor.getValue().getTxObservacao());
        assertFalse(dto.isInConcluido());

        marcos.alterar(4L, form);
        verify(repository).alterarMarco(any());
        verify(repository, never()).concluirMarco(anyLong(), anyBoolean());
    }

    @Test
    void concluirEDesmarcarGravamAConclusao() {
        when(repository.buscarMarco(4L)).thenReturn(Optional.of(marco(4, true)));

        assertTrue(marcos.concluir(4L, true).isInConcluido());
        verify(repository).concluirMarco(4L, true);

        marcos.concluir(4L, false);
        verify(repository).concluirMarco(4L, false);
    }

    @Test
    void marcoInexistenteRetorna404() {
        when(repository.buscarMarco(9L)).thenReturn(Optional.empty());

        assertEquals(404, assertThrows(NordException.class, () -> marcos.buscar(9L)).getStatus().getStatus().value());
        assertThrows(NordException.class, () -> marcos.deletar(9L));
        assertThrows(NordException.class, () -> marcos.concluir(9L, true));
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> marcos.listar());
        verifyNoInteractions(repository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> marcos.deletar(1L));
        verifyNoInteractions(repository);
    }
}
