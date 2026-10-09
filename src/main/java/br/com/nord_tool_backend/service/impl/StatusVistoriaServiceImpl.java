package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.StatusVistoria;
import br.com.nord_tool_backend.dto.StatusVistoriaDto;
import br.com.nord_tool_backend.repository.StatusVistoriaRepository;
import br.com.nord_tool_backend.service.StatusVistoriaService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatusVistoriaServiceImpl implements StatusVistoriaService {

    private final Logger log = LogManager.getLogger(StatusVistoriaServiceImpl.class);

    private final StatusVistoriaRepository statusVistoriaRepository;

    public List<StatusVistoriaDto> listarStatusVistoria(){
        log.info("Iniciando método para listar Status Vistoria");
        List<StatusVistoria> lsStatusVistoria = statusVistoriaRepository.listarStatusVistoria();
        List<StatusVistoriaDto> lsStatusVistoriaDto = lsStatusVistoria.stream().map(StatusVistoriaDto::converterToDto).collect(Collectors.toList());
        log.info("Finalizando método que consulta a lista de Status Vistoria");
        return lsStatusVistoriaDto;
    }
}
