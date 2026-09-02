package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.EmpresaDto;
import java.util.List;

public interface EmpresaService {
    List<EmpresaDto> listarEmpresas();
}
