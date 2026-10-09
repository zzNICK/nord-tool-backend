package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.FinanceiroAtivo;
import br.com.nord_tool_backend.domain.FinanceiroOperacao;
import br.com.nord_tool_backend.domain.FinanceiroProvento;
import br.com.nord_tool_backend.repository.FinanceiroInvestimentoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class FinanceiroInvestimentoRepositoryImpl implements FinanceiroInvestimentoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;

    private final String qAtivoListar;
    private final String qAtivoBuscar;
    private final String qAtivoTicker;
    private final String qAtivoInserir;
    private final String qAtivoAlterar;
    private final String qAtivoCotacao;

    private final String qOpListar;
    private final String qOpBuscar;
    private final String qOpRequisicao;
    private final String qOpInserir;
    private final String qOpDeletar;

    private final String qProvListar;
    private final String qProvBuscar;
    private final String qProvManual;
    private final String qProvImportado;
    private final String qProvDeletar;

    public FinanceiroInvestimentoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qAtivoListar = queries.get("SPS.FININV.ATIVO.LISTAR");
        this.qAtivoBuscar = queries.get("SPS.FININV.ATIVO.BUSCAR");
        this.qAtivoTicker = queries.get("SPS.FININV.ATIVO.BUSCAR_TICKER");
        this.qAtivoInserir = queries.get("SPI.FININV.ATIVO.INSERIR");
        this.qAtivoAlterar = queries.get("SPU.FININV.ATIVO.ALTERAR");
        this.qAtivoCotacao = queries.get("SPU.FININV.ATIVO.COTACAO");
        this.qOpListar = queries.get("SPS.FININV.OPERACAO.LISTAR");
        this.qOpBuscar = queries.get("SPS.FININV.OPERACAO.BUSCAR");
        this.qOpRequisicao = queries.get("SPS.FININV.OPERACAO.BUSCAR_REQUISICAO");
        this.qOpInserir = queries.get("SPI.FININV.OPERACAO.INSERIR");
        this.qOpDeletar = queries.get("SPD.FININV.OPERACAO.DELETAR");
        this.qProvListar = queries.get("SPS.FININV.PROVENTO.LISTAR");
        this.qProvBuscar = queries.get("SPS.FININV.PROVENTO.BUSCAR");
        this.qProvManual = queries.get("SPI.FININV.PROVENTO.MANUAL");
        this.qProvImportado = queries.get("SPI.FININV.PROVENTO.IMPORTADO");
        this.qProvDeletar = queries.get("SPD.FININV.PROVENTO.DELETAR");
    }

    @Override
    public List<FinanceiroAtivo> listarAtivos(Long idPessoa) {
        return executor.executar("Erro ao listar os fundos", () -> jdbc.query(qAtivoListar,
                new MapSqlParameterSource().addValue("idPessoa", idPessoa, java.sql.Types.BIGINT),
                BeanPropertyRowMapper.newInstance(FinanceiroAtivo.class)));
    }

    @Override
    public Optional<FinanceiroAtivo> buscarAtivo(Long id) {
        return executor.executar("Erro ao buscar o fundo", () -> jdbc.query(qAtivoBuscar, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(FinanceiroAtivo.class)).stream().findFirst());
    }

    @Override
    public Optional<FinanceiroAtivo> buscarAtivoPorTicker(String cdTicker, Long idPessoa) {
        return executor.executar("Erro ao buscar o fundo", () -> jdbc.query(qAtivoTicker,
                new MapSqlParameterSource().addValue("cdTicker", cdTicker).addValue("idPessoa", idPessoa),
                BeanPropertyRowMapper.newInstance(FinanceiroAtivo.class)).stream().findFirst());
    }

    @Override
    public Long inserirAtivo(FinanceiroAtivo a) {
        return executor.executar("Erro ao salvar o fundo", () -> jdbc.queryForObject(qAtivoInserir, new MapSqlParameterSource()
                .addValue("cdTicker", a.getCdTicker()).addValue("nmAtivo", a.getNmAtivo())
                .addValue("idPessoa", a.getIdPessoa()), Long.class));
    }

    @Override
    public int alterarAtivo(FinanceiroAtivo a, int nrVersao) {
        return executor.executar("Erro ao alterar o fundo", () -> jdbc.update(qAtivoAlterar, new MapSqlParameterSource()
                .addValue("id", a.getId()).addValue("nmAtivo", a.getNmAtivo())
                .addValue("inAtivo", !Boolean.FALSE.equals(a.getInAtivo())).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void atualizarCotacao(Long idAtivo, BigDecimal vlCotacao, LocalDateTime dhCotacao) {
        executor.executar("Erro ao guardar a cotação", () -> jdbc.update(qAtivoCotacao, new MapSqlParameterSource()
                .addValue("id", idAtivo).addValue("vlCotacao", vlCotacao).addValue("dhCotacao", Timestamp.valueOf(dhCotacao))));
    }

    @Override
    public List<FinanceiroOperacao> listarOperacoes(Collection<Long> idsAtivo) {
        if (idsAtivo.isEmpty()) return Collections.emptyList();
        return executor.executar("Erro ao listar as operações", () -> jdbc.query(qOpListar, new MapSqlParameterSource("idsAtivo", idsAtivo),
                BeanPropertyRowMapper.newInstance(FinanceiroOperacao.class)));
    }

    @Override
    public Optional<FinanceiroOperacao> buscarOperacao(Long id) {
        return executor.executar("Erro ao buscar a operação", () -> jdbc.query(qOpBuscar, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(FinanceiroOperacao.class)).stream().findFirst());
    }

    @Override
    public Optional<Long> buscarAtivoDaRequisicao(String cdRequisicao) {
        return executor.executar("Erro ao buscar a operação", () -> jdbc.queryForList(qOpRequisicao,
                new MapSqlParameterSource("cdRequisicao", cdRequisicao), Long.class).stream().findFirst());
    }

    @Override
    public Optional<Long> inserirOperacao(FinanceiroOperacao o) {
        return executor.executar("Erro ao salvar a operação", () -> jdbc.queryForList(qOpInserir, new MapSqlParameterSource()
                .addValue("cdRequisicao", o.getCdRequisicao()).addValue("idAtivo", o.getIdAtivo())
                .addValue("dtOperacao", Date.valueOf(o.getDtOperacao())).addValue("cdTipo", o.getCdTipo())
                .addValue("qtCotas", o.getQtCotas()).addValue("vlPreco", o.getVlPreco()), Long.class).stream().findFirst());
    }

    @Override
    public void excluirOperacao(Long id) {
        executor.executar("Erro ao excluir a operação", () -> jdbc.update(qOpDeletar, new MapSqlParameterSource("id", id)));
    }

    @Override
    public List<FinanceiroProvento> listarProventos(Collection<Long> idsAtivo) {
        if (idsAtivo.isEmpty()) return Collections.emptyList();
        return executor.executar("Erro ao listar os proventos", () -> jdbc.query(qProvListar, new MapSqlParameterSource("idsAtivo", idsAtivo),
                BeanPropertyRowMapper.newInstance(FinanceiroProvento.class)));
    }

    @Override
    public Optional<FinanceiroProvento> buscarProvento(Long id) {
        return executor.executar("Erro ao buscar o provento", () -> jdbc.query(qProvBuscar, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(FinanceiroProvento.class)).stream().findFirst());
    }

    @Override
    public void gravarProventoManual(FinanceiroProvento p) {
        executor.executar("Erro ao salvar o provento", () -> jdbc.update(qProvManual, parametros(p)));
    }

    @Override
    public boolean gravarProventoImportado(FinanceiroProvento p) {
        return executor.executar("Erro ao importar o provento", () -> jdbc.update(qProvImportado, parametros(p)) > 0);
    }

    @Override
    public void excluirProvento(Long id) {
        executor.executar("Erro ao excluir o provento", () -> jdbc.update(qProvDeletar, new MapSqlParameterSource("id", id)));
    }

    private static MapSqlParameterSource parametros(FinanceiroProvento p) {
        return new MapSqlParameterSource().addValue("idAtivo", p.getIdAtivo()).addValue("dtCom", Date.valueOf(p.getDtCom()))
                .addValue("dtPagamento", Date.valueOf(p.getDtPagamento())).addValue("vlPorCota", p.getVlPorCota());
    }
}
