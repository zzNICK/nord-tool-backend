package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CronogramaSemanalRepositoryImpl implements CronogramaSemanalRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;
    private final String qBuscarPorId;
    private final String qListar;

    public CronogramaSemanalRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qInserir = queries.get("SPI.CRONOGRAMA_SEMANAL.INSERIR");
        this.qAlterar = queries.get("SPU.CRONOGRAMA_SEMANAL.ALTERAR");
        this.qDeletar = queries.get("SPD.CRONOGRAMA_SEMANAL.DELETAR");
        this.qBuscarPorId = queries.get("SPS.CRONOGRAMA_SEMANAL.BUSTAR_POR_ID");
        this.qListar = queries.get("SPS.CRONOGRAMA_SEMANAL.LISTAR");
    }

    @Override
    public CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        return executor.executar("Salvar cronograma semanal", () -> {
            Long id = JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(cronogramaSemanal),
                    "id_cronograma_semanal");
            return CronogramaSemanalDto.converterToDto(buscar(id));
        });
    }

    @Override
    public CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        return executor.executar("Alterar cronograma semanal", () -> {
            jdbc.update(qAlterar, new BeanPropertySqlParameterSource(cronogramaSemanal));
            return CronogramaSemanalDto.converterToDto(buscar(cronogramaSemanal.getId()));
        });
    }

    @Override
    public void deletarCronogramaSemanal(Long id) {
        executor.executar("Excluir cronograma semanal", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }

    @Override
    public CronogramaSemanal buscarPorIdCronogramaSemanal(Long id) {
        return executor.executar("Buscar cronograma semanal", () -> buscar(id));
    }

    @Override
    public List<CronogramaSemanal> listarCronogramaSemanal() {
        return executor.executar("Listar cronogramas semanais", () ->
                jdbc.query(qListar, BeanPropertyRowMapper.newInstance(CronogramaSemanal.class)));
    }

    private CronogramaSemanal buscar(Long id) {
        return jdbc.queryForObject(qBuscarPorId, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(CronogramaSemanal.class));
    }
}
