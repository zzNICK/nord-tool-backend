package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.FinanceiroConfiguracao;
import br.com.nord_tool_backend.domain.FinanceiroFaturaAberta;
import br.com.nord_tool_backend.domain.FinanceiroFaturaLeitura;
import br.com.nord_tool_backend.domain.FinanceiroMes;
import br.com.nord_tool_backend.domain.FinanceiroRecorrencia;
import br.com.nord_tool_backend.domain.FinanceiroSomaMes;
import br.com.nord_tool_backend.repository.FinanceiroProjecaoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class FinanceiroProjecaoRepositoryImpl implements FinanceiroProjecaoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;

    private final String qConfBuscar;
    private final String qConfGarantir;
    private final String qConfAlterar;

    private final String qMesBuscar;
    private final String qMesListar;
    private final String qMesGarantir;
    private final String qMesSaldoInicial;
    private final String qMesFechar;
    private final String qMesReabrir;
    private final String qMesFechadoApos;
    private final String qMesPrimeira;
    private final String qMesPrevistos;

    private final String qSoma;

    private final String qFaturaDoMes;
    private final String qFaturaLeitura;
    private final String qFaturaLeituras;

    private final String qRecListar;
    private final String qRecInserir;
    private final String qRecAlterar;

    public FinanceiroProjecaoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qConfBuscar = queries.get("SPS.FINANCEIRO.CONFIGURACAO.BUSCAR");
        this.qConfGarantir = queries.get("SPI.FINANCEIRO.CONFIGURACAO.GARANTIR");
        this.qConfAlterar = queries.get("SPU.FINANCEIRO.CONFIGURACAO.ALTERAR");
        this.qMesBuscar = queries.get("SPS.FINANCEIRO.MES.BUSCAR");
        this.qMesListar = queries.get("SPS.FINANCEIRO.MES.LISTAR");
        this.qMesGarantir = queries.get("SPI.FINANCEIRO.MES.GARANTIR");
        this.qMesSaldoInicial = queries.get("SPU.FINANCEIRO.MES.SALDO_INICIAL");
        this.qMesFechar = queries.get("SPU.FINANCEIRO.MES.FECHAR");
        this.qMesReabrir = queries.get("SPU.FINANCEIRO.MES.REABRIR");
        this.qMesFechadoApos = queries.get("SPS.FINANCEIRO.MES.EXISTE_FECHADO_APOS");
        this.qMesPrimeira = queries.get("SPS.FINANCEIRO.MES.PRIMEIRA_COMPETENCIA");
        this.qMesPrevistos = queries.get("SPS.FINANCEIRO.MES.CONTAR_PREVISTOS");
        this.qSoma = queries.get("SPS.FINANCEIRO.SOMA.POR_CATEGORIA");
        this.qFaturaDoMes = queries.get("SPS.FINANCEIRO.FATURA.DO_MES");
        this.qFaturaLeitura = queries.get("SPI.FINANCEIRO.FATURA.LEITURA");
        this.qFaturaLeituras = queries.get("SPS.FINANCEIRO.FATURA.LEITURAS");
        this.qRecListar = queries.get("SPS.FINANCEIRO.RECORRENCIA.LISTAR");
        this.qRecInserir = queries.get("SPI.FINANCEIRO.RECORRENCIA.INSERIR");
        this.qRecAlterar = queries.get("SPU.FINANCEIRO.RECORRENCIA.ALTERAR");
    }

    // ---------- configuração ----------

    @Override
    public Optional<FinanceiroConfiguracao> buscarConfiguracao() {
        return executor.executar("Erro ao buscar a configuração", () ->
                jdbc.query(qConfBuscar, BeanPropertyRowMapper.newInstance(FinanceiroConfiguracao.class)).stream().findFirst());
    }

    @Override
    public void garantirConfiguracao() {
        executor.executar("Erro ao criar a configuração", () -> jdbc.update(qConfGarantir, new MapSqlParameterSource()));
    }

    @Override
    public void alterarConfiguracao(FinanceiroConfiguracao c) {
        executor.executar("Erro ao alterar a configuração", () -> jdbc.update(qConfAlterar, new MapSqlParameterSource()
                .addValue("vlMetaSaldo", c.getVlMetaSaldo())
                .addValue("nrMesesMedia", c.getNrMesesMedia())
                .addValue("nrDiaConferencia", c.getNrDiaConferencia())
                .addValue("nrDiaFechamentoFatura", c.getNrDiaFechamentoFatura())));
    }

    // ---------- meses ----------

    @Override
    public Optional<FinanceiroMes> buscarMes(LocalDate competencia) {
        return executor.executar("Erro ao buscar o mês", () ->
                jdbc.query(qMesBuscar, new MapSqlParameterSource("competencia", Date.valueOf(competencia)),
                        BeanPropertyRowMapper.newInstance(FinanceiroMes.class)).stream().findFirst());
    }

    @Override
    public List<FinanceiroMes> listarMeses(LocalDate de, LocalDate ate) {
        return executor.executar("Erro ao listar os meses", () ->
                jdbc.query(qMesListar, new MapSqlParameterSource().addValue("de", Date.valueOf(de)).addValue("ate", Date.valueOf(ate)),
                        BeanPropertyRowMapper.newInstance(FinanceiroMes.class)));
    }

    @Override
    public void garantirMes(LocalDate competencia) {
        executor.executar("Erro ao criar o mês", () -> jdbc.update(qMesGarantir, new MapSqlParameterSource("competencia", Date.valueOf(competencia))));
    }

    @Override
    public int definirSaldoInicial(LocalDate competencia, BigDecimal vlSaldoInicial) {
        return executor.executar("Erro ao salvar o saldo inicial", () -> jdbc.update(qMesSaldoInicial, new MapSqlParameterSource()
                .addValue("competencia", Date.valueOf(competencia)).addValue("vlSaldoInicial", vlSaldoInicial)));
    }

    @Override
    public int fecharMes(LocalDate competencia, BigDecimal vlSaldoFinal, Long idUsuario) {
        return executor.executar("Erro ao fechar o mês", () -> jdbc.update(qMesFechar, new MapSqlParameterSource()
                .addValue("competencia", Date.valueOf(competencia)).addValue("vlSaldoFinal", vlSaldoFinal)
                .addValue("idUsuario", idUsuario)));
    }

    @Override
    public int reabrirMes(LocalDate competencia) {
        return executor.executar("Erro ao reabrir o mês", () ->
                jdbc.update(qMesReabrir, new MapSqlParameterSource("competencia", Date.valueOf(competencia))));
    }

    @Override
    public boolean existeMesFechadoApos(LocalDate competencia) {
        return executor.executar("Erro ao consultar os meses fechados", () -> {
            Integer total = jdbc.queryForObject(qMesFechadoApos, new MapSqlParameterSource("competencia", Date.valueOf(competencia)), Integer.class);
            return total != null && total > 0;
        });
    }

    @Override
    public Optional<LocalDate> primeiraCompetencia() {
        return executor.executar("Erro ao consultar o primeiro mês", () -> {
            Date data = jdbc.queryForObject(qMesPrimeira, new MapSqlParameterSource(), Date.class);
            return Optional.ofNullable(data).map(Date::toLocalDate);
        });
    }

    @Override
    public int contarPrevistos(LocalDate competencia) {
        return executor.executar("Erro ao contar lançamentos previstos", () -> {
            Integer total = jdbc.queryForObject(qMesPrevistos, new MapSqlParameterSource("competencia", Date.valueOf(competencia)), Integer.class);
            return total == null ? 0 : total;
        });
    }

    // ---------- somas ----------

    @Override
    public List<FinanceiroSomaMes> somarPorCategoria(LocalDate de, LocalDate ate, Long idPessoa) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("de", Date.valueOf(de)).addValue("ate", Date.valueOf(ate));
        String sql = qSoma + (idPessoa == null ? "" : " AND l.id_pessoa = :idPessoa") + " GROUP BY l.dt_competencia, l.id_categoria";
        if (idPessoa != null) params.addValue("idPessoa", idPessoa);
        return executor.executar("Erro ao somar os lançamentos", () -> jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(FinanceiroSomaMes.class)));
    }

    // ---------- faturas ----------

    @Override
    public List<FinanceiroFaturaAberta> faturasDoMes(LocalDate competencia, Long idPessoa) {
        MapSqlParameterSource params = new MapSqlParameterSource("competencia", Date.valueOf(competencia));
        String sql = qFaturaDoMes + (idPessoa == null ? "" : " AND l.id_pessoa = :idPessoa");
        if (idPessoa != null) params.addValue("idPessoa", idPessoa);
        return executor.executar("Erro ao buscar as faturas do mês", () -> jdbc.query(sql, params, BeanPropertyRowMapper.newInstance(FinanceiroFaturaAberta.class)));
    }

    @Override
    public void registrarLeitura(Long idLancamento, LocalDate dtLeitura, BigDecimal vlLeitura, Long idUsuario) {
        executor.executar("Erro ao registrar a leitura da fatura", () -> jdbc.update(qFaturaLeitura, new MapSqlParameterSource()
                .addValue("idLancamento", idLancamento).addValue("dtLeitura", Date.valueOf(dtLeitura))
                .addValue("vlLeitura", vlLeitura).addValue("idUsuario", idUsuario)));
    }

    @Override
    public List<FinanceiroFaturaLeitura> listarLeituras(Long idLancamento) {
        return executor.executar("Erro ao listar as leituras da fatura", () ->
                jdbc.query(qFaturaLeituras, new MapSqlParameterSource("idLancamento", idLancamento),
                        BeanPropertyRowMapper.newInstance(FinanceiroFaturaLeitura.class)));
    }

    // ---------- recorrências ----------

    @Override
    public List<FinanceiroRecorrencia> listarRecorrencias() {
        return executor.executar("Erro ao listar as recorrências", () -> jdbc.query(
                qRecListar + " ORDER BY c.cd_tipo, LOWER(c.nm_categoria), r.id_recorrencia",
                BeanPropertyRowMapper.newInstance(FinanceiroRecorrencia.class)));
    }

    @Override
    public Optional<FinanceiroRecorrencia> buscarRecorrencia(Long id) {
        return executor.executar("Erro ao buscar a recorrência", () -> jdbc.query(qRecListar + " WHERE r.id_recorrencia = :id",
                new MapSqlParameterSource("id", id), BeanPropertyRowMapper.newInstance(FinanceiroRecorrencia.class)).stream().findFirst());
    }

    @Override
    public Long inserirRecorrencia(FinanceiroRecorrencia r) {
        return executor.executar("Erro ao salvar a recorrência", () -> {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(qRecInserir, parametros(r), keys, new String[]{"id_recorrencia"});
            return keys.getKey().longValue();
        });
    }

    @Override
    public int alterarRecorrencia(FinanceiroRecorrencia r, int nrVersao) {
        return executor.executar("Erro ao alterar a recorrência", () ->
                jdbc.update(qRecAlterar, parametros(r).addValue("id", r.getId()).addValue("nrVersao", nrVersao)));
    }

    private static MapSqlParameterSource parametros(FinanceiroRecorrencia r) {
        return new MapSqlParameterSource()
                .addValue("idCategoria", r.getIdCategoria()).addValue("idPessoa", r.getIdPessoa())
                .addValue("dsRecorrencia", r.getDsRecorrencia()).addValue("vlRecorrencia", r.getVlRecorrencia())
                .addValue("nrDia", r.getNrDia()).addValue("dtInicio", Date.valueOf(r.getDtInicio()))
                .addValue("dtFim", r.getDtFim() == null ? null : Date.valueOf(r.getDtFim()))
                .addValue("inAtivo", !Boolean.FALSE.equals(r.getInAtivo()));
    }
}
