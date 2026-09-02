package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Permissao;
import java.util.List;

public interface PermissaoRepository {
    List<Permissao> listarPermissoes();
}
