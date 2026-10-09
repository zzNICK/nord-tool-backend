package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.TermoFoto;
import br.com.nord_tool_backend.repository.TermoFotoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TermoFotoRepositoryImpl implements TermoFotoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;
    private final String qBuscar;
    private final String qProximaOrdem;
    private final String qContar;
    private final String qInserir;
    private final String qAtualizar;
    private final String qAtualizarOrdem;
    private final String qDeletar;

    public TermoFotoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.TERMO_FOTO.LISTAR_POR_TERMO");
        this.qBuscar = queries.get("SPS.TERMO_FOTO.BUSCAR_POR_ID");
        this.qProximaOrdem = queries.get("SPS.TERMO_FOTO.PROXIMA_ORDEM");
        this.qContar = queries.get("SPS.TERMO_FOTO.CONTAR_POR_TERMO");
        this.qInserir = queries.get("SPI.TERMO_FOTO.INSERIR");
        this.qAtualizar = queries.get("SPU.TERMO_FOTO.ATUALIZAR");
        this.qAtualizarOrdem = queries.get("SPU.TERMO_FOTO.ATUALIZAR_ORDEM");
        this.qDeletar = queries.get("SPD.TERMO_FOTO.DELETAR");
    }

    @Override
    public List<TermoFoto> listarPorTermo(Long idTermo) {
        return executor.executar("Listar fotos do termo", () ->
                jdbc.query(qListar, new MapSqlParameterSource("idTermo", idTermo), BeanPropertyRowMapper.newInstance(TermoFoto.class)));
    }

    @Override
    public Optional<TermoFoto> buscarPorId(Long id) {
        return executor.executar("Buscar foto do termo", () ->
                jdbc.query(qBuscar, new MapSqlParameterSource("id", id), BeanPropertyRowMapper.newInstance(TermoFoto.class))
                        .stream().findFirst());
    }

    @Override
    public int proximaOrdem(Long idTermo, int nrPagina) {
        return executor.executar("Calcular ordem da foto do termo", () -> {
            Integer ordem = jdbc.queryForObject(qProximaOrdem,
                    new MapSqlParameterSource().addValue("idTermo", idTermo).addValue("nrPagina", nrPagina), Integer.class);
            return ordem == null ? 0 : ordem;
        });
    }

    @Override
    public int contarPorTermo(Long idTermo) {
        return executor.executar("Contar fotos do termo", () -> {
            Integer total = jdbc.queryForObject(qContar, new MapSqlParameterSource("idTermo", idTermo), Integer.class);
            return total == null ? 0 : total;
        });
    }

    @Override
    public TermoFoto inserir(TermoFoto foto) {
        return executor.executar("Salvar foto do termo", () -> {
            foto.setId(JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(foto), "id_termo_foto"));
            return foto;
        });
    }

    @Override
    public void atualizar(TermoFoto foto) {
        executor.executar("Alterar foto do termo", () -> jdbc.update(qAtualizar, new BeanPropertySqlParameterSource(foto)));
    }

    @Override
    public void atualizarOrdem(Long idTermo, Long idFoto, int nrOrdem) {
        executor.executar("Reordenar fotos do termo", () -> jdbc.update(qAtualizarOrdem, new MapSqlParameterSource()
                .addValue("idTermo", idTermo).addValue("id", idFoto).addValue("nrOrdem", nrOrdem)));
    }

    @Override
    public void deletar(Long id) {
        executor.executar("Excluir foto do termo", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }
}
