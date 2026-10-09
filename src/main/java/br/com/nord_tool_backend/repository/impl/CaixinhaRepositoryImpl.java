package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.CaixinhaComprovante;
import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.domain.CaixinhaLancamento;
import br.com.nord_tool_backend.domain.CaixinhaResponsavel;
import br.com.nord_tool_backend.repository.CaixinhaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class CaixinhaRepositoryImpl implements CaixinhaRepository {

    private static final String ORDEM_LANCAMENTOS = " ORDER BY l.dt_lancamento DESC, l.id_lancamento DESC";

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;

    private final String qRespListar;
    private final String qRespBuscar;
    private final String qRespInserir;
    private final String qRespAlterar;

    private final String qLancListar;
    private final String qLancBuscar;
    private final String qLancRequisicao;
    private final String qLancInserir;
    private final String qLancAlterar;
    private final String qLancMarcar;
    private final String qLancDeletar;
    private final String qLancResumo;

    private final String qCompListar;
    private final String qCompBuscar;
    private final String qCompRequisicao;
    private final String qCompInserir;
    private final String qCompDeletar;

    public CaixinhaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qRespListar = queries.get("SPS.CAIXINHA.RESPONSAVEL.LISTAR");
        this.qRespBuscar = queries.get("SPS.CAIXINHA.RESPONSAVEL.BUSCAR");
        this.qRespInserir = queries.get("SPI.CAIXINHA.RESPONSAVEL.INSERIR");
        this.qRespAlterar = queries.get("SPU.CAIXINHA.RESPONSAVEL.ALTERAR");
        this.qLancListar = queries.get("SPS.CAIXINHA.LANCAMENTO.LISTAR");
        this.qLancBuscar = queries.get("SPS.CAIXINHA.LANCAMENTO.BUSCAR");
        this.qLancRequisicao = queries.get("SPS.CAIXINHA.LANCAMENTO.BUSCAR_REQUISICAO");
        this.qLancInserir = queries.get("SPI.CAIXINHA.LANCAMENTO.INSERIR");
        this.qLancAlterar = queries.get("SPU.CAIXINHA.LANCAMENTO.ALTERAR");
        this.qLancMarcar = queries.get("SPU.CAIXINHA.LANCAMENTO.MARCAR");
        this.qLancDeletar = queries.get("SPD.CAIXINHA.LANCAMENTO.DELETAR");
        this.qLancResumo = queries.get("SPS.CAIXINHA.LANCAMENTO.RESUMO");
        this.qCompListar = queries.get("SPS.CAIXINHA.COMPROVANTE.LISTAR");
        this.qCompBuscar = queries.get("SPS.CAIXINHA.COMPROVANTE.BUSCAR");
        this.qCompRequisicao = queries.get("SPS.CAIXINHA.COMPROVANTE.BUSCAR_REQUISICAO");
        this.qCompInserir = queries.get("SPI.CAIXINHA.COMPROVANTE.INSERIR");
        this.qCompDeletar = queries.get("SPD.CAIXINHA.COMPROVANTE.DELETAR");
    }

    // ---------- responsáveis ----------

    @Override
    public List<CaixinhaResponsavel> listarResponsaveis() {
        return executor.executar("Erro ao listar responsáveis", () ->
                jdbc.query(qRespListar, BeanPropertyRowMapper.newInstance(CaixinhaResponsavel.class)));
    }

    @Override
    public Optional<CaixinhaResponsavel> buscarResponsavel(Long id) {
        return executor.executar("Erro ao buscar responsável", () ->
                jdbc.query(qRespBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CaixinhaResponsavel.class)).stream().findFirst());
    }

    @Override
    public Long inserirResponsavel(CaixinhaResponsavel r) {
        return executor.executar("Erro ao salvar responsável", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(qRespInserir, new BeanPropertySqlParameterSource(r), keys, new String[]{"id_responsavel"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public void alterarResponsavel(CaixinhaResponsavel r) {
        executor.executar("Erro ao alterar responsável", () -> jdbc.update(qRespAlterar, new BeanPropertySqlParameterSource(r)));
    }

    // ---------- lançamentos ----------

    @Override
    public List<CaixinhaLancamento> listarLancamentos(CaixinhaFiltro filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = qLancListar + condicoes(filtro, params) + ORDEM_LANCAMENTOS;
        return executor.executar("Erro ao listar lançamentos", () ->
                jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(CaixinhaLancamento.class)));
    }

    @Override
    public Optional<CaixinhaLancamento> buscarLancamento(Long id) {
        return executor.executar("Erro ao buscar lançamento", () ->
                jdbc.query(qLancBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CaixinhaLancamento.class)).stream().findFirst());
    }

    @Override
    public Optional<Long> buscarLancamentoPorRequisicao(String cdRequisicao) {
        return executor.executar("Erro ao buscar lançamento", () ->
                jdbc.queryForList(qLancRequisicao, new MapSqlParameterSource("cdRequisicao", cdRequisicao), Long.class)
                        .stream().findFirst());
    }

    @Override
    public Optional<Long> inserirLancamento(CaixinhaLancamento l) {
        return executor.executar("Erro ao salvar lançamento", () ->
                jdbc.queryForList(qLancInserir, new MapSqlParameterSource()
                        .addValue("cdRequisicao", l.getCdRequisicao())
                        .addValue("dtLancamento", Date.valueOf(l.getDtLancamento()))
                        .addValue("idResponsavel", l.getIdResponsavel())
                        .addValue("txInsumo", l.getTxInsumo())
                        .addValue("vlValor", l.getVlValor())
                        .addValue("inLancado", Boolean.TRUE.equals(l.getInLancado()))
                        .addValue("inPago", Boolean.TRUE.equals(l.getInPago())), Long.class)
                        .stream().findFirst());
    }

    @Override
    public int alterarLancamento(CaixinhaLancamento l, int nrVersao) {
        return executor.executar("Erro ao alterar lançamento", () ->
                jdbc.update(qLancAlterar, new MapSqlParameterSource()
                        .addValue("id", l.getId())
                        .addValue("dtLancamento", Date.valueOf(l.getDtLancamento()))
                        .addValue("idResponsavel", l.getIdResponsavel())
                        .addValue("txInsumo", l.getTxInsumo())
                        .addValue("vlValor", l.getVlValor())
                        .addValue("inLancado", Boolean.TRUE.equals(l.getInLancado()))
                        .addValue("inPago", Boolean.TRUE.equals(l.getInPago()))
                        .addValue("nrVersao", nrVersao)));
    }

    @Override
    public int marcarLancamento(Long id, boolean lancado, boolean pago, int nrVersao) {
        return executor.executar("Erro ao atualizar lançamento", () ->
                jdbc.update(qLancMarcar, new MapSqlParameterSource().addValue("id", id)
                        .addValue("inLancado", lancado).addValue("inPago", pago).addValue("nrVersao", nrVersao)));
    }

    @Override
    public int deletarLancamento(Long id, int nrVersao) {
        return executor.executar("Erro ao excluir lançamento", () ->
                jdbc.update(qLancDeletar, new MapSqlParameterSource().addValue("id", id).addValue("nrVersao", nrVersao)));
    }

    @Override
    public Totais resumir(CaixinhaFiltro filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = qLancResumo + condicoes(filtro, params);
        return executor.executar("Erro ao calcular o resumo da Caixinha", () ->
                jdbc.queryForObject(sql, params, (rs, i) -> new Totais(
                        rs.getBigDecimal("total"), rs.getBigDecimal("pago"), rs.getInt("qtLancamentos"), rs.getInt("qtPagos"))));
    }

    /** Condições AND dos filtros; valores sempre por parâmetro nomeado (sem concatenar texto do cliente). */
    private static String condicoes(CaixinhaFiltro filtro, MapSqlParameterSource params) {
        StringBuilder sb = new StringBuilder();
        if (filtro == null) return "";
        if (filtro.getResponsavel() != null && !filtro.getResponsavel().trim().isEmpty()) {
            sb.append(" AND r.nm_responsavel = :responsavel");
            params.addValue("responsavel", filtro.getResponsavel().trim());
        }
        String situacao = filtro.getSituacao() == null ? "TODOS" : filtro.getSituacao();
        if ("A_PAGAR".equals(situacao)) sb.append(" AND l.in_pago = FALSE");
        else if ("PAGO".equals(situacao)) sb.append(" AND l.in_pago = TRUE");
        else if ("NAO_LANCADO".equals(situacao)) sb.append(" AND l.in_lancado = FALSE");
        if (filtro.getDe() != null) {
            sb.append(" AND l.dt_lancamento >= :de");
            params.addValue("de", Date.valueOf(filtro.getDe()));
        }
        if (filtro.getAte() != null) {
            sb.append(" AND l.dt_lancamento <= :ate");
            params.addValue("ate", Date.valueOf(filtro.getAte()));
        }
        return sb.toString();
    }

    // ---------- comprovantes ----------

    @Override
    public List<CaixinhaComprovante> listarComprovantes(Long idLancamento) {
        return executor.executar("Erro ao listar comprovantes", () ->
                jdbc.query(qCompListar, new MapSqlParameterSource("idLancamento", idLancamento),
                        BeanPropertyRowMapper.newInstance(CaixinhaComprovante.class)));
    }

    @Override
    public Optional<CaixinhaComprovante> buscarComprovante(Long id) {
        return executor.executar("Erro ao buscar comprovante", () ->
                jdbc.query(qCompBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CaixinhaComprovante.class)).stream().findFirst());
    }

    @Override
    public Optional<Long> buscarComprovantePorRequisicao(String cdRequisicao) {
        return executor.executar("Erro ao buscar comprovante", () ->
                jdbc.queryForList(qCompRequisicao, new MapSqlParameterSource("cdRequisicao", cdRequisicao), Long.class)
                        .stream().findFirst());
    }

    @Override
    public Long inserirComprovante(Long idLancamento, Long idArquivo, String cdRequisicao) {
        return executor.executar("Erro ao salvar comprovante", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            SqlParameterSource params = new MapSqlParameterSource().addValue("idLancamento", idLancamento)
                    .addValue("idArquivo", idArquivo).addValue("cdRequisicao", cdRequisicao);
            jdbc.update(qCompInserir, params, keys, new String[]{"id_comprovante"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public void deletarComprovante(Long id) {
        executor.executar("Erro ao excluir comprovante", () -> jdbc.update(qCompDeletar, new MapSqlParameterSource("id", id)));
    }
}
