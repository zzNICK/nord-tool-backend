package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.CasamentoAnexo;
import br.com.nord_tool_backend.domain.CasamentoConvidado;
import br.com.nord_tool_backend.domain.CasamentoFornecedor;
import br.com.nord_tool_backend.domain.CasamentoMarco;
import br.com.nord_tool_backend.dto.CasamentoTotaisDto;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CasamentoRepositoryImpl implements CasamentoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;

    private final String qConfigListar;
    private final String qConfigSalvar;

    private final String qFornecedorListar;
    private final String qFornecedorBuscar;
    private final String qFornecedorInserir;
    private final String qFornecedorAlterar;
    private final String qFornecedorDeletar;

    private final String qAnexoListar;
    private final String qAnexoBuscar;
    private final String qAnexoInserir;
    private final String qAnexoDeletar;

    private final String qConvidadoListar;
    private final String qConvidadoBuscar;
    private final String qConvidadoInserir;
    private final String qConvidadoAlterar;
    private final String qConvidadoDeletar;

    private final String qMarcoListar;
    private final String qMarcoBuscar;
    private final String qMarcoProximos;
    private final String qMarcoContar;
    private final String qMarcoInserir;
    private final String qMarcoAlterar;
    private final String qMarcoConcluir;
    private final String qMarcoDeletar;

    private final String qTotais;

    public CasamentoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qConfigListar = queries.get("SPS.CASAMENTO.CONFIGURACAO.LISTAR");
        this.qConfigSalvar = queries.get("SPI.CASAMENTO.CONFIGURACAO.SALVAR");
        this.qFornecedorListar = queries.get("SPS.CASAMENTO.FORNECEDOR.LISTAR");
        this.qFornecedorBuscar = queries.get("SPS.CASAMENTO.FORNECEDOR.BUSCAR");
        this.qFornecedorInserir = queries.get("SPI.CASAMENTO.FORNECEDOR.INSERIR");
        this.qFornecedorAlterar = queries.get("SPU.CASAMENTO.FORNECEDOR.ALTERAR");
        this.qFornecedorDeletar = queries.get("SPD.CASAMENTO.FORNECEDOR.DELETAR");
        this.qAnexoListar = queries.get("SPS.CASAMENTO.ANEXO.LISTAR");
        this.qAnexoBuscar = queries.get("SPS.CASAMENTO.ANEXO.BUSCAR");
        this.qAnexoInserir = queries.get("SPI.CASAMENTO.ANEXO.INSERIR");
        this.qAnexoDeletar = queries.get("SPD.CASAMENTO.ANEXO.DELETAR");
        this.qConvidadoListar = queries.get("SPS.CASAMENTO.CONVIDADO.LISTAR");
        this.qConvidadoBuscar = queries.get("SPS.CASAMENTO.CONVIDADO.BUSCAR");
        this.qConvidadoInserir = queries.get("SPI.CASAMENTO.CONVIDADO.INSERIR");
        this.qConvidadoAlterar = queries.get("SPU.CASAMENTO.CONVIDADO.ALTERAR");
        this.qConvidadoDeletar = queries.get("SPD.CASAMENTO.CONVIDADO.DELETAR");
        this.qMarcoListar = queries.get("SPS.CASAMENTO.MARCO.LISTAR");
        this.qMarcoBuscar = queries.get("SPS.CASAMENTO.MARCO.BUSCAR");
        this.qMarcoProximos = queries.get("SPS.CASAMENTO.MARCO.PROXIMOS");
        this.qMarcoContar = queries.get("SPS.CASAMENTO.MARCO.CONTAR");
        this.qMarcoInserir = queries.get("SPI.CASAMENTO.MARCO.INSERIR");
        this.qMarcoAlterar = queries.get("SPU.CASAMENTO.MARCO.ALTERAR");
        this.qMarcoConcluir = queries.get("SPU.CASAMENTO.MARCO.CONCLUIR");
        this.qMarcoDeletar = queries.get("SPD.CASAMENTO.MARCO.DELETAR");
        this.qTotais = queries.get("SPS.CASAMENTO.DASHBOARD.TOTAIS");
    }

    // ---------- configuração ----------

    @Override
    public Map<String, String> listarConfiguracao() {
        return executor.executar("Erro ao ler a configuração do casamento", () -> {
            Map<String, String> mapa = new HashMap<>();
            jdbc.query(qConfigListar, rs -> {
                mapa.put(rs.getString("cdChave"), rs.getString("vlValor"));
            });
            return mapa;
        });
    }

    @Override
    public void salvarConfiguracao(String chave, String valor) {
        executor.executar("Erro ao salvar a configuração do casamento", () ->
                jdbc.update(qConfigSalvar, new MapSqlParameterSource().addValue("cdChave", chave).addValue("vlValor", valor)));
    }

    // ---------- fornecedores ----------

    @Override
    public List<CasamentoFornecedor> listarFornecedores() {
        return executor.executar("Erro ao listar fornecedores", () ->
                jdbc.query(qFornecedorListar, BeanPropertyRowMapper.newInstance(CasamentoFornecedor.class)));
    }

    @Override
    public Optional<CasamentoFornecedor> buscarFornecedor(Long id) {
        return executor.executar("Erro ao buscar fornecedor", () ->
                jdbc.query(qFornecedorBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CasamentoFornecedor.class)).stream().findFirst());
    }

    @Override
    public Long inserirFornecedor(CasamentoFornecedor f) {
        return executor.executar("Erro ao salvar fornecedor", () -> inserir(qFornecedorInserir, new BeanPropertySqlParameterSource(f), "id_fornecedor"));
    }

    @Override
    public void alterarFornecedor(CasamentoFornecedor f) {
        executor.executar("Erro ao alterar fornecedor", () -> jdbc.update(qFornecedorAlterar, new BeanPropertySqlParameterSource(f)));
    }

    @Override
    public void deletarFornecedor(Long id) {
        executor.executar("Erro ao excluir fornecedor", () -> jdbc.update(qFornecedorDeletar, new MapSqlParameterSource("id", id)));
    }

    // ---------- anexos ----------

    @Override
    public List<CasamentoAnexo> listarAnexos(Long idFornecedor) {
        return executor.executar("Erro ao listar anexos", () ->
                jdbc.query(qAnexoListar, new MapSqlParameterSource("idFornecedor", idFornecedor),
                        BeanPropertyRowMapper.newInstance(CasamentoAnexo.class)));
    }

    @Override
    public Optional<CasamentoAnexo> buscarAnexo(Long idAnexo) {
        return executor.executar("Erro ao buscar anexo", () ->
                jdbc.query(qAnexoBuscar, new MapSqlParameterSource("id", idAnexo),
                        BeanPropertyRowMapper.newInstance(CasamentoAnexo.class)).stream().findFirst());
    }

    @Override
    public Long inserirAnexo(Long idFornecedor, Long idArquivo, String descricao) {
        return executor.executar("Erro ao salvar anexo", () -> inserir(qAnexoInserir, new MapSqlParameterSource()
                .addValue("idFornecedor", idFornecedor).addValue("idArquivo", idArquivo).addValue("txDescricao", descricao), "id_anexo"));
    }

    @Override
    public void deletarAnexo(Long idAnexo) {
        executor.executar("Erro ao excluir anexo", () -> jdbc.update(qAnexoDeletar, new MapSqlParameterSource("id", idAnexo)));
    }

    // ---------- convidados ----------

    @Override
    public List<CasamentoConvidado> listarConvidados() {
        return executor.executar("Erro ao listar convidados", () ->
                jdbc.query(qConvidadoListar, BeanPropertyRowMapper.newInstance(CasamentoConvidado.class)));
    }

    @Override
    public Optional<CasamentoConvidado> buscarConvidado(Long id) {
        return executor.executar("Erro ao buscar convidado", () ->
                jdbc.query(qConvidadoBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CasamentoConvidado.class)).stream().findFirst());
    }

    @Override
    public Long inserirConvidado(CasamentoConvidado c) {
        return executor.executar("Erro ao salvar convidado", () -> inserir(qConvidadoInserir, new BeanPropertySqlParameterSource(c), "id_convidado"));
    }

    @Override
    public void alterarConvidado(CasamentoConvidado c) {
        executor.executar("Erro ao alterar convidado", () -> jdbc.update(qConvidadoAlterar, new BeanPropertySqlParameterSource(c)));
    }

    @Override
    public void deletarConvidado(Long id) {
        executor.executar("Erro ao excluir convidado", () -> jdbc.update(qConvidadoDeletar, new MapSqlParameterSource("id", id)));
    }

    // ---------- marcos ----------

    @Override
    public List<CasamentoMarco> listarMarcos() {
        return executor.executar("Erro ao listar marcos", () ->
                jdbc.query(qMarcoListar, BeanPropertyRowMapper.newInstance(CasamentoMarco.class)));
    }

    @Override
    public List<CasamentoMarco> listarProximosMarcos(int limite) {
        return executor.executar("Erro ao listar os próximos marcos", () ->
                jdbc.query(qMarcoProximos, new MapSqlParameterSource("limite", limite),
                        BeanPropertyRowMapper.newInstance(CasamentoMarco.class)));
    }

    @Override
    public Optional<CasamentoMarco> buscarMarco(Long id) {
        return executor.executar("Erro ao buscar marco", () ->
                jdbc.query(qMarcoBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CasamentoMarco.class)).stream().findFirst());
    }

    @Override
    public int contarMarcos() {
        return executor.executar("Erro ao contar marcos", () -> {
            Integer total = jdbc.queryForObject(qMarcoContar, new MapSqlParameterSource(), Integer.class);
            return total == null ? 0 : total;
        });
    }

    @Override
    public Long inserirMarco(CasamentoMarco m) {
        return executor.executar("Erro ao salvar marco", () -> inserir(qMarcoInserir, new MapSqlParameterSource()
                .addValue("nmTitulo", m.getNmTitulo())
                .addValue("dtPrazo", m.getDtPrazo() == null ? null : Date.valueOf(m.getDtPrazo()))
                .addValue("inConcluido", Boolean.TRUE.equals(m.getInConcluido()))
                .addValue("txObservacao", m.getTxObservacao()), "id_marco"));
    }

    @Override
    public void alterarMarco(CasamentoMarco m) {
        executor.executar("Erro ao alterar marco", () -> jdbc.update(qMarcoAlterar, new MapSqlParameterSource()
                .addValue("id", m.getId())
                .addValue("nmTitulo", m.getNmTitulo())
                .addValue("dtPrazo", m.getDtPrazo() == null ? null : Date.valueOf(m.getDtPrazo()))
                .addValue("txObservacao", m.getTxObservacao())));
    }

    @Override
    public void concluirMarco(Long id, boolean concluido) {
        executor.executar("Erro ao atualizar o marco", () -> jdbc.update(qMarcoConcluir,
                new MapSqlParameterSource().addValue("id", id).addValue("inConcluido", concluido)));
    }

    @Override
    public void deletarMarco(Long id) {
        executor.executar("Erro ao excluir marco", () -> jdbc.update(qMarcoDeletar, new MapSqlParameterSource("id", id)));
    }

    // ---------- dashboard ----------

    @Override
    public CasamentoTotaisDto buscarTotais() {
        return executor.executar("Erro ao calcular os totais do casamento", () ->
                jdbc.queryForObject(qTotais, new MapSqlParameterSource(), BeanPropertyRowMapper.newInstance(CasamentoTotaisDto.class)));
    }

    // ---------- auxiliares ----------

    private Long inserir(String sql, org.springframework.jdbc.core.namedparam.SqlParameterSource params, String colunaId) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(sql, params, keys, new String[]{colunaId});
        return keys.getKey().longValue();
    }
}
