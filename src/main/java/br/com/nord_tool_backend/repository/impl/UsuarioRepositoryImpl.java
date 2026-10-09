package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Nunca registra parâmetros (senha/hash): o JdbcExecutor só loga a operação e o SQLState. */
@Repository
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qBuscarPorEmail;
    private final String qBuscarPorId;
    private final String qEstadoSessao;
    private final String qContar;
    private final String qPermissoes;
    private final String qPermissoesUsuario;
    private final String qIdPerfil;
    private final String qInserir;
    private final String qIncrementarFalha;
    private final String qBloquear;
    private final String qRegistrarLogin;
    private final String qAlterarSenha;

    public UsuarioRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qBuscarPorEmail = queries.get("SPS.USUARIO.BUSCAR_POR_EMAIL");
        this.qBuscarPorId = queries.get("SPS.USUARIO.BUSCAR_POR_ID");
        this.qEstadoSessao = queries.get("SPS.USUARIO.ESTADO_SESSAO");
        this.qContar = queries.get("SPS.USUARIO.CONTAR");
        this.qPermissoes = queries.get("SPS.USUARIO.PERMISSOES");
        this.qPermissoesUsuario = queries.get("SPS.USUARIO.PERMISSOES_USUARIO");
        this.qIdPerfil = queries.get("SPS.USUARIO.ID_PERFIL_POR_CODIGO");
        this.qInserir = queries.get("SPI.USUARIO.INSERIR");
        this.qIncrementarFalha = queries.get("SPU.USUARIO.INCREMENTAR_FALHA");
        this.qBloquear = queries.get("SPU.USUARIO.BLOQUEAR");
        this.qRegistrarLogin = queries.get("SPU.USUARIO.REGISTRAR_LOGIN");
        this.qAlterarSenha = queries.get("SPU.USUARIO.ALTERAR_SENHA");
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return buscarUm("Buscar usuário", qBuscarPorEmail, new MapSqlParameterSource("email", email));
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return buscarUm("Buscar usuário", qBuscarPorId, new MapSqlParameterSource("id", id));
    }

    @Override
    public Optional<Usuario> buscarEstadoSessao(Long id) {
        return buscarUm("Consultar sessão do usuário", qEstadoSessao, new MapSqlParameterSource("id", id));
    }

    @Override
    public long contar() {
        return executor.executar("Contar usuários", () -> {
            Long total = jdbc.queryForObject(qContar, EmptySqlParameterSource.INSTANCE, Long.class);
            return total == null ? 0L : total;
        });
    }

    @Override
    public List<PerfilPermissao> listarPermissoes(Long idPerfil) {
        return executor.executar("Listar permissões do perfil", () ->
                jdbc.query(qPermissoes, new MapSqlParameterSource("idPerfil", idPerfil),
                        BeanPropertyRowMapper.newInstance(PerfilPermissao.class)));
    }

    @Override
    public List<PerfilPermissao> listarPermissoesUsuario(Long idUsuario) {
        return executor.executar("Listar permissões do usuário", () ->
                jdbc.query(qPermissoesUsuario, new MapSqlParameterSource("idUsuario", idUsuario),
                        BeanPropertyRowMapper.newInstance(PerfilPermissao.class)));
    }

    @Override
    public Optional<Long> buscarIdPerfil(String cdPerfil) {
        return executor.executar("Buscar perfil", () ->
                jdbc.queryForList(qIdPerfil, new MapSqlParameterSource("cdPerfil", cdPerfil), Long.class).stream().findFirst());
    }

    @Override
    public Usuario inserir(Usuario usuario) {
        return executor.executar("Salvar usuário", () -> {
            usuario.setId(JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(usuario), "id_usuario"));
            return usuario;
        });
    }

    @Override
    public int incrementarFalhas(Long id) {
        return executor.executar("Registrar falha de login", () -> {
            Integer falhas = jdbc.queryForObject(qIncrementarFalha, new MapSqlParameterSource("id", id), Integer.class);
            return falhas == null ? 0 : falhas;
        });
    }

    @Override
    public void bloquearAte(Long id, LocalDateTime bloqueadoAte) {
        executor.executar("Bloquear login", () -> jdbc.update(qBloquear, new MapSqlParameterSource()
                .addValue("id", id).addValue("bloqueadoAte", Timestamp.valueOf(bloqueadoAte))));
    }

    @Override
    public void registrarLoginSucesso(Long id) {
        executor.executar("Registrar login", () -> jdbc.update(qRegistrarLogin, new MapSqlParameterSource("id", id)));
    }

    @Override
    public void alterarSenha(Long id, String hash) {
        executor.executar("Alterar senha", () ->
                jdbc.update(qAlterarSenha, new MapSqlParameterSource().addValue("id", id).addValue("hash", hash)));
    }

    private Optional<Usuario> buscarUm(String operacao, String sql, MapSqlParameterSource params) {
        return executor.executar(operacao, () ->
                jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(Usuario.class)).stream().findFirst());
    }
}
