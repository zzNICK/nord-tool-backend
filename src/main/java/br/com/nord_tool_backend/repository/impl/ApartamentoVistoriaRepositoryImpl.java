package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.InfoGeralApartamentoVistoria;
import br.com.nord_tool_backend.domain.OrdenacaoApartamentoVistoria;
import br.com.nord_tool_backend.domain.enums.ApartamentoVistoriaFiltroEnum;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaDto;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaFiltroDto;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static br.com.nord_tool_backend.utils.StringUtils.parseDataPermitida;

@Repository
public class ApartamentoVistoriaRepositoryImpl implements ApartamentoVistoriaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;
    private final String qBuscar;
    private final String qListar;
    private final String qPaginacao;
    private final String qInfoGeral;
    private final Map<ApartamentoVistoriaFiltroEnum, String> qFiltros = new EnumMap<>(ApartamentoVistoriaFiltroEnum.class);

    public ApartamentoVistoriaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qInserir = queries.get("SPI.APARTAMENTO_VISTORIA");
        this.qAlterar = queries.get("SPU.APARTAMENTO_VISTORIA");
        this.qDeletar = queries.get("SPD.APARTAMENTO_VISTORIA.WHERE.ID");
        this.qBuscar = queries.get("SPS.BUSCAR.APARTAMENTO_VISTORIA");
        this.qListar = queries.get("SPS.LISTAR.APARTAMENTO_VISTORIA");
        this.qPaginacao = queries.get("SPS.APARTAMENTO_VISTORIA_PAGINACAO");
        this.qInfoGeral = queries.get("SPS.LISTAR.INFO_GERAL_APARTAMENTO_VISTORIA");
        for (ApartamentoVistoriaFiltroEnum filtro : ApartamentoVistoriaFiltroEnum.values()) {
            qFiltros.put(filtro, queries.get(filtro.getQueryProperty()));
        }
    }

    @Override
    public ApartamentoVistoriaDto salvarApartamentoVistoria(ApartamentoVistoria apartamentoVistoria) {
        return executor.executar("Salvar apartamento", () -> {
            apartamentoVistoria.setId(JdbcSuporte.inserir(jdbc, qInserir,
                    new BeanPropertySqlParameterSource(apartamentoVistoria), "id_apartamento_vistoria"));
            return ApartamentoVistoriaDto.converterToDto(apartamentoVistoria);
        });
    }

    @Override
    public ApartamentoVistoriaDto alterarApartamentoVistoria(ApartamentoVistoria apartamentoVistoria) {
        return executor.executar("Alterar apartamento", () -> {
            jdbc.update(qAlterar, new BeanPropertySqlParameterSource(apartamentoVistoria));
            return ApartamentoVistoriaDto.converterToDto(apartamentoVistoria);
        });
    }

    @Override
    public void deletarApartamentoVistoria(Long id) {
        executor.executar("Excluir apartamento", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }

    @Override
    public ApartamentoVistoria buscarApartamentoVistoria(Long id) {
        return executor.executar("Buscar apartamento", () ->
                jdbc.queryForObject(qBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(ApartamentoVistoria.class)));
    }

    @Override
    public List<ApartamentoVistoria> listarApartamentoVistoria() {
        return executor.executar("Listar apartamentos", () ->
                jdbc.query(qListar, BeanPropertyRowMapper.newInstance(ApartamentoVistoria.class)));
    }

    @Override
    public void salvarEmLote(List<ApartamentoVistoria> apartamentos) {
        executor.executar("Salvar apartamentos da planilha", () ->
                jdbc.batchUpdate(qInserir, SqlParameterSourceUtils.createBatch(apartamentos.toArray())));
    }

    @Override
    public List<ApartamentoVistoriaDto> listarApartamentoVistoriaFiltrado(ApartamentoVistoriaFiltroDto filtro, String filtraTodos,
                                                                          int nrPagina, int nrQuantidadePorPagina,
                                                                          OrdenacaoApartamentoVistoria ordenacao) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (filtraTodos != null && !filtraTodos.isEmpty()) {
            params.addValue("filtraTodos", "%" + filtraTodos + "%");
        }
        params.addValue("nmApartamentoVistoria", filtro.getNmApartamentoVistoria(), Types.VARCHAR);
        params.addValue("nmDiaSemana", filtro.getNmDiaSemana(), Types.VARCHAR);
        params.addValue("dtApartamentoVigente", parseDataPermitida(filtro.getDtApartamentoVigente()), Types.DATE);
        params.addValue("nmHorarioVistoria", filtro.getNmHorarioVistoria(), Types.VARCHAR);
        params.addValue("nmStatusVistoria", filtro.getNmStatusVistoria(), Types.VARCHAR);
        params.addValue("txObservacaoRevistoria", filtro.getTxObservacaoRevistoria(), Types.VARCHAR);
        params.addValue("dtRevistoriaVigente", parseDataPermitida(filtro.getDtRevistoriaVigente()), Types.DATE);
        params.addValue("nrPagina", nrPagina);
        params.addValue("nrQuantidadePorPagina", nrQuantidadePorPagina);

        // Só fragmentos constantes: a query do catálogo, o ORDER BY montado a partir dos enums e a paginação parametrizada.
        String sql = qFiltros.get(ApartamentoVistoriaFiltroEnum.para(filtraTodos)) + ordenacao.sql() + qPaginacao;
        return executor.executar("Listar apartamentos filtrados", () ->
                jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(ApartamentoVistoriaDto.class)));
    }

    @Override
    public List<InfoGeralApartamentoVistoria> listarInfoGeralApartamentoVistoria(String dtiApartamentoVistoriaFiltro,
                                                                                 String dtfApartamentoVistoriaFiltro) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dtiApartamentoVistoriaFiltro", parseDataPermitida(dtiApartamentoVistoriaFiltro), Types.DATE)
                .addValue("dtfApartamentoVistoriaFiltro", parseDataPermitida(dtfApartamentoVistoriaFiltro), Types.DATE);
        return executor.executar("Listar informações gerais dos apartamentos", () ->
                jdbc.query(qInfoGeral, params, BeanPropertyRowMapper.newInstance(InfoGeralApartamentoVistoria.class)));
    }
}
