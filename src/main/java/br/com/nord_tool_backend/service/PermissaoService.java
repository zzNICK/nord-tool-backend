package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.PermissaoDto;
import java.util.List;

public interface PermissaoService {
    List<PermissaoDto> listarPermissoes();
}
