package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.form.CronogramaSemanalForm;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.service.CronogramaSemanalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class CronogramaSemanalServiceImplTest {

    @Mock

    private AutorizacaoService autorizacao;


    @InjectMocks
    private CronogramaSemanalServiceImpl cronogramaSemanalService;

    @Mock
    private CronogramaSemanalRepository cronogramaSemanalRepository;

    CronogramaSemanal cronogramaSemanal = new CronogramaSemanal();
    CronogramaSemanalDto cronogramaSemanalDto = new CronogramaSemanalDto();
    List<CronogramaSemanalDto> lsCronogramaSemanalDto = new ArrayList<>();
    List<CronogramaSemanal> lsCronogramaSemanal = new ArrayList<>();
    CronogramaSemanalForm cronogramaSemanalForm = new CronogramaSemanalForm();

    @BeforeEach
    public void setup(){
        cronogramaSemanal = CronogramaSemanal.builder()
                .id(1L)
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCronogramaCategoria("nmCategoria")
                .nmStatusCronograma("Pendente")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build();

        cronogramaSemanalDto = CronogramaSemanalDto.builder()
                .id(cronogramaSemanal.getId())
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build();

        lsCronogramaSemanal.add(CronogramaSemanal.builder()
                .id(1L)
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCronogramaCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build());

        lsCronogramaSemanalDto.add(CronogramaSemanalDto.builder()
                .id(1L)
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build());

        cronogramaSemanalForm = CronogramaSemanalForm.builder()
                .idCronogramaSemanal(1L)
                .idDiaSemana(1)
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build();
    }

    @Test
    void deveSalvarCronogramaSemanal() {
        when(cronogramaSemanalRepository.salvarCronogramaSemanal(any(CronogramaSemanal.class))).thenReturn(cronogramaSemanalDto);
        CronogramaSemanalDto result = cronogramaSemanalService.salvarCronogramaSemanal(cronogramaSemanalForm);
        assertEquals(cronogramaSemanalDto, result);
        verify(cronogramaSemanalRepository).salvarCronogramaSemanal(any(CronogramaSemanal.class));
    }

    @Test
    void deveAlterarCronogramaSemanal() {
        when(cronogramaSemanalRepository.alterarCronogramaSemanal(any(CronogramaSemanal.class))).thenReturn(cronogramaSemanalDto);
        CronogramaSemanalDto result = cronogramaSemanalService.alterarCronogramaSemanal(cronogramaSemanalForm);
        assertEquals(cronogramaSemanalDto, result);
        verify(cronogramaSemanalRepository).alterarCronogramaSemanal(any(CronogramaSemanal.class));
    }

    @Test
    void deveDeletarCronogramaSemanal(){
        doNothing().when(cronogramaSemanalRepository).deletarCronogramaSemanal(anyLong());
        cronogramaSemanalService.deletarCronogramaSemanal(1L);
        verify(cronogramaSemanalRepository, times(1)).deletarCronogramaSemanal(anyLong());
    }

    @Test
    void deveRetornarUmCronogramaSemanal(){
        when(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(anyLong())).thenReturn(cronogramaSemanal);
        cronogramaSemanalService.buscarPorIdCronogramaSemanal(1L);
        verify(cronogramaSemanalRepository, times(1)).buscarPorIdCronogramaSemanal(anyLong());
    }

    @Test
    void deveRetornarUmaListaCronogramaSemanal(){
        when(cronogramaSemanalRepository.listarCronogramaSemanal()).thenReturn(lsCronogramaSemanal);
        cronogramaSemanalService.listarCronogramaSemanal();
        verify(cronogramaSemanalRepository, times(1)).listarCronogramaSemanal();
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.listarCronogramaSemanal());
        verifyNoInteractions(cronogramaSemanalRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.deletarCronogramaSemanal(1L));
        verifyNoInteractions(cronogramaSemanalRepository);
    }
}
