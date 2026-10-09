package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.ApartamentoVistoriaHistorico;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaHistoricoConsultaDto;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaHistoricoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ApartamentoVistoriaHistoricoRepositoryImpl implements ApartamentoVistoriaHistoricoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qInserir;
    private final String qBuscar;
    private final String qVersoes;

    public ApartamentoVistoriaHistoricoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor,
                                                      SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qInserir = queries.get("SPI.APARTAMENTO_VISTORIA_HISTORICO");
        this.qBuscar = queries.get("SPS.APARTAMENTO_VISTORIA_HISTORICO");
        this.qVersoes = queries.get("SPS.APARTAMENTO_VISTORIA_HISTORICO_NR_VERSAO");
    }

    @Override
    public void salvarTodosHistoricos(List<ApartamentoVistoriaHistorico> historicos) {
        executor.executar("Salvar histórico do apartamento", () ->
                jdbc.batchUpdate(qInserir, SqlParameterSourceUtils.createBatch(historicos.toArray())));
    }

    @Override
    public List<ApartamentoVistoriaHistoricoConsultaDto> buscarHistorico(Long idApartamentoVistoria) {
        return executor.executar("Listar histórico do apartamento", () ->
                jdbc.query(qBuscar, new MapSqlParameterSource("idApartamentoVistoria", idApartamentoVistoria),
                        BeanPropertyRowMapper.newInstance(ApartamentoVistoriaHistoricoConsultaDto.class)));
    }

    @Override
    public List<Integer> buscarNrVersaoHistorico(Long idApartamentoVistoria) {
        return executor.executar("Listar versões do histórico do apartamento", () ->
                jdbc.query(qVersoes, new MapSqlParameterSource("idApartamentoVistoria", idApartamentoVistoria),
                        (rs, linha) -> rs.getInt("nrVersao")));
    }
}
