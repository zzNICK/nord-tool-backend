package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Empresa;
import br.com.nord_tool_backend.repository.EmpresaRepository;
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
public class EmpresaRepositoryImpl implements EmpresaRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;

    public EmpresaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.EMPRESA.LISTAR");
        this.qInserir = queries.get("SPI.EMPRESA.INSERIR");
        this.qAlterar = queries.get("SPU.EMPRESA.ALTERAR");
        this.qDeletar = queries.get("SPD.EMPRESA.DELETAR");
    }

    @Override
    public List<Empresa> listarEmpresas() {
        return executor.executar("Listar empresas", () -> jdbc.query(qListar, BeanPropertyRowMapper.newInstance(Empresa.class)));
    }

    @Override
    public Empresa salvarEmpresa(Empresa entidade) {
        return executor.executar("Salvar empresa", () -> {
            entidade.setId(JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(entidade), "id_empresa"));
            return entidade;
        });
    }

    @Override
    public Empresa alterarEmpresa(Empresa entidade) {
        return executor.executar("Alterar empresa", () -> {
            jdbc.update(qAlterar, new BeanPropertySqlParameterSource(entidade));
            return entidade;
        });
    }

    @Override
    public void deletarEmpresa(Long id) {
        executor.executar("Excluir empresa", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }
}
