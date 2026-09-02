package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Empresa;
import java.util.List;

public interface EmpresaRepository {
    List<Empresa> listarEmpresas();
}
