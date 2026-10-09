package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Cargo;
import br.com.nord_tool_backend.repository.CargoRepository;
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
public class CargoRepositoryImpl implements CargoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;

    public CargoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.CARGO.LISTAR");
        this.qInserir = queries.get("SPI.CARGO.INSERIR");
        this.qAlterar = queries.get("SPU.CARGO.ALTERAR");
        this.qDeletar = queries.get("SPD.CARGO.DELETAR");
    }

    @Override
    public List<Cargo> listarCargos() {
        return executor.executar("Listar cargos", () -> jdbc.query(qListar, BeanPropertyRowMapper.newInstance(Cargo.class)));
    }

    @Override
    public Cargo salvarCargo(Cargo entidade) {
        return executor.executar("Salvar cargo", () -> {
            entidade.setId(JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(entidade), "id_cargo"));
            return entidade;
        });
    }

    @Override
    public Cargo alterarCargo(Cargo entidade) {
        return executor.executar("Alterar cargo", () -> {
            jdbc.update(qAlterar, new BeanPropertySqlParameterSource(entidade));
            return entidade;
        });
    }

    @Override
    public void deletarCargo(Long id) {
        executor.executar("Excluir cargo", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }
}
