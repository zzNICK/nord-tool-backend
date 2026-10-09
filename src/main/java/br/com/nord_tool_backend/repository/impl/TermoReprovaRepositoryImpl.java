package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.TermoReprova;
import br.com.nord_tool_backend.dto.TermoReprovaResumoGeralDto;
import br.com.nord_tool_backend.repository.TermoReprovaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TermoReprovaRepositoryImpl implements TermoReprovaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;
    private final String qBuscar;
    private final String qProximoNumero;
    private final String qApartamentoExiste;
    private final String qIds;
    private final String qInserir;
    private final String qAtualizarArquivo;
    private final String qAtualizarSituacao;
    private final String qDeletar;
    private final String qResumoGeral;

    public TermoReprovaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.TERMO_REPROVA.LISTAR_POR_APARTAMENTO");
        this.qBuscar = queries.get("SPS.TERMO_REPROVA.BUSCAR_POR_ID");
        this.qProximoNumero = queries.get("SPS.TERMO_REPROVA.PROXIMO_NUMERO");
        this.qApartamentoExiste = queries.get("SPS.TERMO_REPROVA.APARTAMENTO_EXISTE");
        this.qIds = queries.get("SPS.TERMO_REPROVA.IDS_POR_APARTAMENTO");
        this.qInserir = queries.get("SPI.TERMO_REPROVA.INSERIR");
        this.qAtualizarArquivo = queries.get("SPU.TERMO_REPROVA.ATUALIZAR_ARQUIVO");
        this.qAtualizarSituacao = queries.get("SPU.TERMO_REPROVA.ATUALIZAR_SITUACAO");
        this.qDeletar = queries.get("SPD.TERMO_REPROVA.DELETAR");
        this.qResumoGeral = queries.get("SPS.TERMO_REPROVA.RESUMO_GERAL");
    }

    @Override
    public boolean apartamentoExiste(Long idApartamento) {
        return executor.executar("Consultar apartamento", () -> {
            Integer total = jdbc.queryForObject(qApartamentoExiste, new MapSqlParameterSource("id", idApartamento), Integer.class);
            return total != null && total > 0;
        });
    }

    @Override
    public List<TermoReprova> listarPorApartamento(Long idApartamento) {
        return executor.executar("Listar termos de reprova", () ->
                jdbc.query(qListar, new MapSqlParameterSource("idApartamento", idApartamento),
                        BeanPropertyRowMapper.newInstance(TermoReprova.class)));
    }

    @Override
    public Optional<TermoReprova> buscarPorId(Long id) {
        return executor.executar("Buscar termo de reprova", () ->
                jdbc.query(qBuscar, new MapSqlParameterSource("id", id), BeanPropertyRowMapper.newInstance(TermoReprova.class))
                        .stream().findFirst());
    }

    @Override
    public int proximoNumero(Long idApartamento) {
        return executor.executar("Numerar termo de reprova", () -> {
            Integer proximo = jdbc.queryForObject(qProximoNumero, new MapSqlParameterSource("idApartamento", idApartamento),
                    Integer.class);
            return proximo == null ? 1 : proximo;
        });
    }

    @Override
    public List<Long> listarIdsPorApartamento(Long idApartamento) {
        return executor.executar("Listar termos de reprova do apartamento", () ->
                jdbc.queryForList(qIds, new MapSqlParameterSource("idApartamento", idApartamento), Long.class));
    }

    @Override
    public TermoReprova inserir(TermoReprova termo) {
        return executor.executar("Salvar termo de reprova", () -> {
            termo.setId(JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(termo), "id_termo_reprova"));
            return termo;
        });
    }

    @Override
    public void atualizarArquivo(Long id, Long idArquivo, int nrPaginas) {
        executor.executar("Trocar o PDF do termo", () -> jdbc.update(qAtualizarArquivo, new MapSqlParameterSource()
                .addValue("id", id).addValue("idArquivo", idArquivo).addValue("nrPaginas", nrPaginas)));
    }

    @Override
    public void atualizarSituacao(Long id, String situacao, String observacao) {
        executor.executar("Atualizar a situação do termo", () -> jdbc.update(qAtualizarSituacao, new MapSqlParameterSource()
                .addValue("id", id).addValue("situacao", situacao).addValue("observacao", observacao)));
    }

    @Override
    public void deletar(Long id) {
        executor.executar("Excluir termo de reprova", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }

    @Override
    public TermoReprovaResumoGeralDto resumoGeral() {
        return executor.executar("Calcular o resumo dos termos de reprova", () ->
                jdbc.queryForObject(qResumoGeral, EmptySqlParameterSource.INSTANCE,
                        BeanPropertyRowMapper.newInstance(TermoReprovaResumoGeralDto.class)));
    }
}
