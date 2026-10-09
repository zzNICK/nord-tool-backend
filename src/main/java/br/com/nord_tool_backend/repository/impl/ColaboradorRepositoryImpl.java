package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Colaborador;
import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.repository.ColaboradorRepository;
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
public class ColaboradorRepositoryImpl implements ColaboradorRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;
    private final String qBuscarPorId;
    private final String qListar;

    public ColaboradorRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qInserir = queries.get("SPI.COLABORADOR.INSERIR");
        this.qAlterar = queries.get("SPU.COLABORADOR.ALTERAR");
        this.qDeletar = queries.get("SPD.COLABORADOR.DELETAR");
        this.qBuscarPorId = queries.get("SPS.COLABORADOR.BUSCAR_POR_ID");
        this.qListar = queries.get("SPS.COLABORADOR.LISTAR");
    }

    @Override
    public ColaboradorDto salvarColaborador(Colaborador entidade) {
        return executor.executar("Salvar colaborador", () -> {
            Long id = JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(entidade), "id_user");
            return ColaboradorDto.converterToDto(buscar(id));
        });
    }

    @Override
    public ColaboradorDto alterarColaborador(Colaborador entidade) {
        return executor.executar("Alterar colaborador", () -> {
            jdbc.update(qAlterar, new BeanPropertySqlParameterSource(entidade));
            return ColaboradorDto.converterToDto(buscar(entidade.getId()));
        });
    }

    @Override
    public void deletarColaborador(Long id) {
        executor.executar("Excluir colaborador", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }

    @Override
    public Colaborador buscarPorIdColaborador(Long id) {
        return executor.executar("Buscar colaborador", () -> buscar(id));
    }

    @Override
    public List<Colaborador> listarColaboradores() {
        return executor.executar("Listar colaboradores", () -> jdbc.query(qListar, BeanPropertyRowMapper.newInstance(Colaborador.class)));
    }

    private Colaborador buscar(Long id) {
        return jdbc.queryForObject(qBuscarPorId, new MapSqlParameterSource("id", id), BeanPropertyRowMapper.newInstance(Colaborador.class));
    }
}
