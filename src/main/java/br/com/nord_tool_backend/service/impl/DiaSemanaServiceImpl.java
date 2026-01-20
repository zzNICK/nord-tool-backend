package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.DiaSemana;
import br.com.nord_tool_backend.dto.DiaSemanaDto;
import br.com.nord_tool_backend.repository.DiaSemanaRepository;
import br.com.nord_tool_backend.service.DiaSemanaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiaSemanaServiceImpl implements DiaSemanaService {

    @Autowired
    private DiaSemanaRepository diaSemanaRepository;

    public List<DiaSemanaDto> listarDiaSemana(){
        List<DiaSemana> lsDiaSemana = diaSemanaRepository.listarDiaSemana();
        return lsDiaSemana.stream().map(DiaSemanaDto::converterToDomain).collect(Collectors.toList());
    }
}
