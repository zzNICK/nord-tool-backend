package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.dto.FerramentaDto;
import br.com.nord_tool_backend.repository.FerramentaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FerramentaRepositoryImpl implements FerramentaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;
    private final String qBuscarPorId;
    private final String qListar;

    public FerramentaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qInserir = queries.get("SPI.FERRAMENTA.INSERIR");
        this.qAlterar = queries.get("SPU.FERRAMENTA.ALTERAR");
        this.qDeletar = queries.get("SPD.FERRAMENTA.DELETAR");
        this.qBuscarPorId = queries.get("SPS.FERRAMENTA.BUSCAR_POR_ID");
        this.qListar = queries.get("SPS.FERRAMENTA.LISTAR");
    }

    @Override
    public FerramentaDto salvarFerramenta(Ferramenta entidade) {
        return executor.executar("Salvar ferramenta", () -> {
            Long id = JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(entidade), "id_ferramenta");
            return FerramentaDto.converterToDto(buscar(id));
        });
    }

    @Override
    public FerramentaDto alterarFerramenta(Ferramenta entidade) {
        return executor.executar("Alterar ferramenta", () -> {
            jdbc.update(qAlterar, new BeanPropertySqlParameterSource(entidade));
            return FerramentaDto.converterToDto(buscar(entidade.getId()));
        });
    }

    @Override
    public void deletarFerramenta(Long id) {
        executor.executar("Excluir ferramenta", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }

    @Override
    public Ferramenta buscarPorIdFerramenta(Long id) {
        return executor.executar("Buscar ferramenta", () -> buscar(id));
    }

    @Override
    public List<Ferramenta> listarFerramentas() {
        return executor.executar("Listar ferramentas", () -> jdbc.query(qListar, BeanPropertyRowMapper.newInstance(Ferramenta.class)));
    }

    private Ferramenta buscar(Long id) {
        return jdbc.queryForObject(qBuscarPorId, new MapSqlParameterSource("id", id), BeanPropertyRowMapper.newInstance(Ferramenta.class));
    }
}
