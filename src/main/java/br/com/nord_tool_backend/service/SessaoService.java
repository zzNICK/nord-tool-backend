package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.security.UsuarioAutenticado;

/**
 * Valida, a cada requisição, se a sessão do token ainda vale: usuário ativo, versão de sessão igual à do banco
 * e dentro do limite absoluto desde o login. O estado do banco fica em cache curto (revogação em até 60 s).
 */
public interface SessaoService {

    boolean valida(UsuarioAutenticado usuario);

    /** Descarta o estado em cache do usuário (troca de senha, desativação, mudança de permissões). */
    void invalidar(Long idUsuario);
}
