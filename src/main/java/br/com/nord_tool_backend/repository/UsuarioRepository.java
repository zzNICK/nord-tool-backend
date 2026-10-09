package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorEmail(String email);
    Optional<Usuario> buscarPorId(Long id);
    /** Só id, inAtivo e nrVersaoSessao: leitura barata usada na validação de cada requisição (com cache). */
    Optional<Usuario> buscarEstadoSessao(Long id);
    long contar();
    List<PerfilPermissao> listarPermissoes(Long idPerfil);
    /** Exceções de permissão do usuário (tabela usuario_permissao). */
    List<PerfilPermissao> listarPermissoesUsuario(Long idUsuario);
    Optional<Long> buscarIdPerfil(String cdPerfil);
    Usuario inserir(Usuario usuario);
    /** Incrementa o contador de falhas de forma atômica e devolve o novo valor. */
    int incrementarFalhas(Long id);
    void bloquearAte(Long id, LocalDateTime bloqueadoAte);
    void registrarLoginSucesso(Long id);
    /** Troca o hash e incrementa a versão da sessão (tokens anteriores deixam de valer). */
    void alterarSenha(Long id, String hash);
}
