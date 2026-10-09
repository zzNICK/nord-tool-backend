package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.ArquivoArmazenado;
import br.com.nord_tool_backend.repository.ArquivoArmazenadoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ArquivoArmazenadoRepositoryImpl implements ArquivoArmazenadoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qInserir;
    private final String qBuscarComConteudo;
    private final String qBuscarMetadados;
    private final String qDeletar;

    public ArquivoArmazenadoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qInserir = queries.get("SPI.ARQUIVO_ARMAZENADO.INSERIR");
        this.qBuscarComConteudo = queries.get("SPS.ARQUIVO_ARMAZENADO.BUSCAR_COM_CONTEUDO");
        this.qBuscarMetadados = queries.get("SPS.ARQUIVO_ARMAZENADO.BUSCAR_METADADOS");
        this.qDeletar = queries.get("SPD.ARQUIVO_ARMAZENADO.DELETAR");
    }

    @Override
    public ArquivoArmazenado inserir(ArquivoArmazenado arquivo) {
        return executor.executar("Salvar arquivo", () -> {
            arquivo.setId(JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(arquivo), "id_arquivo"));
            return arquivo;
        });
    }

    @Override
    public Optional<ArquivoArmazenado> buscarComConteudo(Long id) {
        return buscar(qBuscarComConteudo, id);
    }

    @Override
    public Optional<ArquivoArmazenado> buscarMetadados(Long id) {
        return buscar(qBuscarMetadados, id);
    }

    @Override
    public int deletar(Long id) {
        return executor.executar("Apagar arquivo", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }

    private Optional<ArquivoArmazenado> buscar(String sql, Long id) {
        return executor.executar("Buscar arquivo", () ->
                jdbc.query(sql, new MapSqlParameterSource("id", id), BeanPropertyRowMapper.newInstance(ArquivoArmazenado.class))
                        .stream().findFirst());
    }
}
