package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Permissao;
import br.com.nord_tool_backend.repository.PermissaoRepository;
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
public class PermissaoRepositoryImpl implements PermissaoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qListar;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;

    public PermissaoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.qListar = queries.get("SPS.PERMISSAO.LISTAR");
        this.qInserir = queries.get("SPI.PERMISSAO.INSERIR");
        this.qAlterar = queries.get("SPU.PERMISSAO.ALTERAR");
        this.qDeletar = queries.get("SPD.PERMISSAO.DELETAR");
    }

    @Override
    public List<Permissao> listarPermissoes() {
        return executor.executar("Listar permissões", () -> jdbc.query(qListar, BeanPropertyRowMapper.newInstance(Permissao.class)));
    }

    @Override
    public Permissao salvarPermissao(Permissao entidade) {
        return executor.executar("Salvar permissão", () -> {
            entidade.setId(JdbcSuporte.inserir(jdbc, qInserir, new BeanPropertySqlParameterSource(entidade), "id_permissao"));
            return entidade;
        });
    }

    @Override
    public Permissao alterarPermissao(Permissao entidade) {
        return executor.executar("Alterar permissão", () -> {
            jdbc.update(qAlterar, new BeanPropertySqlParameterSource(entidade));
            return entidade;
        });
    }

    @Override
    public void deletarPermissao(Long id) {
        executor.executar("Excluir permissão", () -> jdbc.update(qDeletar, new MapSqlParameterSource("id", id)));
    }
}
