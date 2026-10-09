package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.DiaSemana;
import br.com.nord_tool_backend.dto.DiaSemanaDto;
import br.com.nord_tool_backend.repository.DiaSemanaRepository;
import br.com.nord_tool_backend.service.DiaSemanaService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaSemanaServiceImpl implements DiaSemanaService {
    private final Logger log = LogManager.getLogger(DiaSemanaServiceImpl.class);

    private final DiaSemanaRepository diaSemanaRepository;

    public List<DiaSemanaDto> listarDiaSemana(){
        log.info("Iniciando método para listar Dias da Semana");
        List<DiaSemana> lsDiaSemana = diaSemanaRepository.listarDiaSemana();
        List<DiaSemanaDto> lsDiaSemanaDto = lsDiaSemana.stream().map(DiaSemanaDto::converterToDto).collect(Collectors.toList());
        log.info("Finalizando método que consulta a lista dos Dias da Semana");
        return lsDiaSemanaDto;
    }
}
