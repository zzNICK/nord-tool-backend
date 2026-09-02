package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.repository.PermissaoRepository;
import br.com.nord_tool_backend.service.PermissaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PermissaoServiceImpl implements PermissaoService {
    private final PermissaoRepository permissaoRepository;

    public List<PermissaoDto> listarPermissoes() {
        return permissaoRepository.listarPermissoes().stream()
                .map(PermissaoDto::converterToDomain).collect(Collectors.toList());
    }
}
