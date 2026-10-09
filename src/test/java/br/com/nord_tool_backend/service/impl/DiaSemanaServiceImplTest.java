package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.DiaSemana;
import br.com.nord_tool_backend.dto.DiaSemanaDto;
import br.com.nord_tool_backend.repository.DiaSemanaRepository;
import br.com.nord_tool_backend.service.DiaSemanaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DiaSemanaServiceImplTest {

    @Mock

    private AutorizacaoService autorizacao;


    @InjectMocks
    private DiaSemanaServiceImpl diaSemanaService;

    @Mock
    private DiaSemanaRepository diaSemanaRepository;

    List<DiaSemana> lsDiaSemana = new ArrayList<>();
    List<DiaSemanaDto> lsDiaSemanaDto = new ArrayList<>();

    @BeforeEach
    public void setup() {
        lsDiaSemana.add(DiaSemana.builder()
                .id(1L)
                .nmDiaSemana("Segunda-Feira")
                .build());

        lsDiaSemanaDto.add(DiaSemanaDto.builder()
                .idDiaSemana(1L)
                .nmDiaSemana("Segunda-Feira")
                .build());
    }

    @Test
    void deveListarDiaSemana() {
        when(diaSemanaRepository.listarDiaSemana()).thenReturn((lsDiaSemana));
        List<DiaSemanaDto> diaSemanaDto = diaSemanaService.listarDiaSemana();
        assertEquals(lsDiaSemanaDto, diaSemanaDto);
        verify(diaSemanaRepository).listarDiaSemana();
    }

    // ---------- autorização ----------

    @Test
    void semAutenticacaoNaoConsultaODado() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("Autenticação necessária"));
        assertThrows(NaoAutenticadoException.class, () -> diaSemanaService.listarDiaSemana());
        verifyNoInteractions(diaSemanaRepository);
    }
}
