package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.StatusVistoria;
import br.com.nord_tool_backend.repository.StatusVistoriaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StatusVistoriaRepositoryImpl implements StatusVistoriaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;

    public StatusVistoriaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.LISTAR_TODOS_STATUS_VISTORIA");
    }

    @Override
    public List<StatusVistoria> listarStatusVistoria() {
        return executor.executar("Listar status de vistoria", () ->
                jdbc.query(qListar, BeanPropertyRowMapper.newInstance(StatusVistoria.class)));
    }
}
