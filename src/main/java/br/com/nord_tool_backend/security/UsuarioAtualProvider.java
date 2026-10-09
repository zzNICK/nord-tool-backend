package br.com.nord_tool_backend.security;

import java.util.Optional;

/** Usuário da requisição atual. Os serviços dependem desta interface (simulável nos testes). */
public interface UsuarioAtualProvider {

    Optional<UsuarioAutenticado> atual();
}
