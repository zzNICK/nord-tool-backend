package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.ApartamentoVistoriaHistoricoConsultaDto;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaHistoricoDto;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaHistoricoRepository;
import br.com.nord_tool_backend.service.ApartamentoVistoriaHistoricoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApartamentoVistoriaHistoricoServiceImpl implements ApartamentoVistoriaHistoricoService {
    private final Logger log = LogManager.getLogger(ApartamentoVistoriaHistoricoServiceImpl.class);

    private final ApartamentoVistoriaHistoricoRepository apartamentoVistoriaHistoricoRepository;

    @Override
    public List<ApartamentoVistoriaHistoricoDto> buscarHistoricoApartamentoVistoria(Long idApartamentoVistoria) {
        log.info("Iniciando método para buscar historico de alteracoes do apartamento");
        List<ApartamentoVistoriaHistoricoConsultaDto> lsApartamentoVistoriaHistoricoConsultaDto = apartamentoVistoriaHistoricoRepository.buscarHistorico(idApartamentoVistoria);
        return lsApartamentoVistoriaHistoricoConsultaDto.stream()
                .collect(Collectors.groupingBy(
                        ApartamentoVistoriaHistoricoConsultaDto::getNrVersao,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .map(ApartamentoVistoriaHistoricoDto::converter)
                .collect(Collectors.toList());
    }
}
