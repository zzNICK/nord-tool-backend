package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.repository.EmpresaRepository;
import br.com.nord_tool_backend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class EmpresaServiceImpl implements EmpresaService {
    private final EmpresaRepository empresaRepository;

    public List<EmpresaDto> listarEmpresas() {
        return empresaRepository.listarEmpresas().stream()
                .map(EmpresaDto::converterToDomain).collect(Collectors.toList());
    }
}
