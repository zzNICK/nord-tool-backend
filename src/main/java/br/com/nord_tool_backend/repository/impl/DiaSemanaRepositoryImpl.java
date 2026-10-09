package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.DiaSemana;
import br.com.nord_tool_backend.repository.DiaSemanaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DiaSemanaRepositoryImpl implements DiaSemanaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;

    public DiaSemanaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.LISTAR_TODOS_DIA_SEMANA");
    }

    @Override
    public List<DiaSemana> listarDiaSemana() {
        return executor.executar("Listar dias da semana", () ->
                jdbc.query(qListar, BeanPropertyRowMapper.newInstance(DiaSemana.class)));
    }
}
