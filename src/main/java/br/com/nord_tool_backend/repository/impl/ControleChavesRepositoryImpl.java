package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.domain.ObraControleChaves;
import br.com.nord_tool_backend.domain.RequisicaoChave;
import br.com.nord_tool_backend.domain.RequisicaoChaveConsulta;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.repository.ControleChavesRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ControleChavesRepositoryImpl implements ControleChavesRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListarObras;
    private final String qListarApartamentos;
    private final String qListarFerramentas;
    private final String qContarEmCampo;
    private final String qContarNoQuadro;
    private final String qContarEntregues;
    private final String qListarHistorico;
    private final String qBuscarPorId;
    private final String qInserir;
    private final String qReceber;

    public ControleChavesRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListarObras = queries.get("SPS.CONTROLE_CHAVES.LISTAR_OBRAS");
        this.qListarApartamentos = queries.get("SPS.CONTROLE_CHAVES.LISTAR_APARTAMENTOS");
        this.qListarFerramentas = queries.get("SPS.CONTROLE_CHAVES.LISTAR_FERRAMENTAS");
        this.qContarEmCampo = queries.get("SPS.CONTROLE_CHAVES.COUNT_EM_CAMPO");
        this.qContarNoQuadro = queries.get("SPS.CONTROLE_CHAVES.COUNT_NO_QUADRO");
        this.qContarEntregues = queries.get("SPS.CONTROLE_CHAVES.COUNT_ENTREGUES");
        this.qListarHistorico = queries.get("SPS.CONTROLE_CHAVES.LISTAR_HISTORICO");
        this.qBuscarPorId = queries.get("SPS.CONTROLE_CHAVES.BUSCAR_POR_ID");
        this.qInserir = queries.get("SPI.CONTROLE_CHAVES.INSERIR");
        this.qReceber = queries.get("SPU.CONTROLE_CHAVES.RECEBER");
    }

    @Override
    public List<ObraControleChaves> listarObras() {
        return executor.executar("Listar obras", () ->
                jdbc.query(qListarObras, BeanPropertyRowMapper.newInstance(ObraControleChaves.class)));
    }

    @Override
    public List<ApartamentoVistoria> listarApartamentos() {
        return executor.executar("Listar apartamentos do controle de chaves", () ->
                jdbc.query(qListarApartamentos, BeanPropertyRowMapper.newInstance(ApartamentoVistoria.class)));
    }

    @Override
    public List<Ferramenta> listarFerramentas() {
        return executor.executar("Listar ferramentas do controle de chaves", () ->
                jdbc.query(qListarFerramentas, BeanPropertyRowMapper.newInstance(Ferramenta.class)));
    }

    @Override
    public Long contarChavesEmCampo() {
        return contar("Contar chaves em campo", qContarEmCampo);
    }

    @Override
    public Long contarChavesNoQuadro() {
        return contar("Contar chaves no quadro", qContarNoQuadro);
    }

    @Override
    public Long contarChavesEntregues() {
        return contar("Contar chaves entregues", qContarEntregues);
    }

    @Override
    public List<RequisicaoChaveConsulta> listarHistorico() {
        return executor.executar("Listar histórico do controle de chaves", () ->
                jdbc.query(qListarHistorico, BeanPropertyRowMapper.newInstance(RequisicaoChaveConsulta.class)));
    }

    @Override
    public RequisicaoChaveConsulta buscarPorId(Long idRequisicao) {
        return executor.executar("Buscar requisição de chave", () ->
                jdbc.queryForObject(qBuscarPorId, new MapSqlParameterSource("idRequisicao", idRequisicao),
                        BeanPropertyRowMapper.newInstance(RequisicaoChaveConsulta.class)));
    }

    @Override
    public Long criarRetirada(RequisicaoChave requisicaoChave) {
        return executor.executar("Criar retirada de chave", () ->
                JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(requisicaoChave), "id_requisicao"));
    }

    @Override
    public void receberRetirada(Long idRequisicao, Long idUserRecebimento, LocalDateTime dtRecebimento, String nmStatusRequisicao) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idRequisicao", idRequisicao)
                .addValue("idUserRecebimento", idUserRecebimento)
                .addValue("dtRecebimento", dtRecebimento)
                .addValue("nmStatusRequisicao", nmStatusRequisicao);
        executor.executar("Receber retirada de chave", () -> {
            if (jdbc.update(qReceber, params) == 0) {
                throw new EntradaInvalidaException("A retirada não existe ou não está aberta para recebimento");
            }
        });
    }

    private Long contar(String operacao, String sql) {
        return executor.executar(operacao, () -> jdbc.queryForObject(sql, EmptySqlParameterSource.INSTANCE, Long.class));
    }
}
