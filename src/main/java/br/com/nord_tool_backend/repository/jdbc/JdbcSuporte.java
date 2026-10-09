package br.com.nord_tool_backend.repository.jdbc;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/** Operações JDBC repetidas pelos repositories (sem estado; usadas dentro de {@link JdbcExecutor#executar}). */
public final class JdbcSuporte {

    private JdbcSuporte() {
    }

    /** Executa o INSERT e devolve o id gerado na coluna informada. */
    public static Long inserir(NamedParameterJdbcTemplate jdbc, String sql, SqlParameterSource parametros, String colunaId) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(sql, parametros, chaves, new String[]{colunaId});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("INSERT não devolveu a chave " + colunaId);
        }
        return id.longValue();
    }
}
