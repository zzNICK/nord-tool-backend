package br.com.nord_tool_backend.service.impl;

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

@ExtendWith(MockitoExtension.class)
class DiaSemanaServiceImplTest {

    @InjectMocks
    private DiaSemanaService diaSemanaService = new DiaSemanaServiceImpl();

    @Mock
    private DiaSemanaRepository diaSemanaRepository;

    List<DiaSemana> lsDiaSemana = new ArrayList<>();
    List<DiaSemanaDto> lsDiaSemanaDto = new ArrayList<>();

    @BeforeEach
    public void setup() {
        lsDiaSemana.add(DiaSemana.builder()
                .idDiaSemana(1)
                .nmDiaSemana("Segunda-Feira")
                .build());

        lsDiaSemanaDto.add(DiaSemanaDto.builder()
                .idDiaSemana(1)
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
}

