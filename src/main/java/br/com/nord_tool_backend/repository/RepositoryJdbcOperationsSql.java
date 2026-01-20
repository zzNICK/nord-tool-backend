package br.com.nord_tool_backend.repository;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

import java.util.List;

public abstract class RepositoryJdbcOperationsSql<T> {

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    protected Integer inserir(String sql, MapSqlParameterSource params) {
        return namedParameterJdbcTemplate.update(sql, params);
    }

    protected Integer atualizar(String sql, MapSqlParameterSource params) {
        return namedParameterJdbcTemplate.update(sql, params);
    }

    protected Integer deletar(String sql, MapSqlParameterSource params) {
        return namedParameterJdbcTemplate.update(sql, params);
    }

    protected List<T> salvarTodos(List<T> ls, String query) {

        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(ls.toArray());
        namedParameterJdbcTemplate.batchUpdate(query, batch);
        return ls;
    }

    protected <T> List<T> buscarTodos(String sql,
                                   BeanPropertyRowMapper<T> mapper) {
        return namedParameterJdbcTemplate.query(sql, mapper);
    }

    protected <D> List<D> buscarPadraoInteger(List<Integer> ls, String query, String key, BeanPropertyRowMapper<D> mapper) {

        MapSqlParameterSource params = new MapSqlParameterSource(key, ls);
        return namedParameterJdbcTemplate.query(query, params, mapper);
    }

    protected <T> T buscarUm(String sql,
                          MapSqlParameterSource params,
                          BeanPropertyRowMapper<T> mapper) {
        return namedParameterJdbcTemplate.queryForObject(sql, params, mapper);
    }
}
