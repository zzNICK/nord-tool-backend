package br.com.nord_tool_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Diagnostico seguro do mesmo JdbcTemplate usado pelos repositories. */
@Slf4j
@Component
@Profile("online-test")
@RequiredArgsConstructor
public class OnlineTestDatasourceDiagnostics implements ApplicationRunner {

    private static final Map<String, String> RELACOES = criarRelacoes();

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        String searchPath = jdbcTemplate.queryForObject("SHOW search_path", String.class);
        String schemas = jdbcTemplate.queryForObject(
                "SELECT array_to_string(current_schemas(false), ',')", String.class);

        log.info("online-test datasource: search_path={}, schemas_ativos={}", searchPath, schemas);
        RELACOES.forEach((nomeLogico, nomeQualificado) -> {
            String regclassQualificada = jdbcTemplate.queryForObject(
                    "SELECT to_regclass(?)::text", String.class, nomeQualificado);
            String regclassPeloSearchPath = jdbcTemplate.queryForObject(
                    "SELECT to_regclass(?)::text", String.class, nomeLogico);
            log.info("online-test datasource: relacao {} existente={}, resolvida_pelo_search_path={}",
                    nomeQualificado, regclassQualificada, regclassPeloSearchPath);
        });
        jdbcTemplate.queryForList(
                "SELECT table_schema, table_name, column_name FROM information_schema.columns "
                        + "WHERE table_name IN ('apartamento_vistoria','obra','obras') "
                        + "ORDER BY table_schema, table_name, ordinal_position")
                .forEach(coluna -> log.info("online-test schema: {}.{}.{}",
                        coluna.get("table_schema"), coluna.get("table_name"), coluna.get("column_name")));
        validarConexoesDoPool();
    }

    private static Map<String, String> criarRelacoes() {
        Map<String, String> relacoes = new LinkedHashMap<>();
        relacoes.put("users", "apartamento.users");
        relacoes.put("empresas", "apartamento.empresas");
        relacoes.put("cargos", "apartamento.cargos");
        relacoes.put("permissoes", "apartamento.permissoes");
        relacoes.put("requisicoes_de_chaves", "apartamento.requisicoes_de_chaves");
        relacoes.put("apartamento_vistoria", "apartamento.apartamento_vistoria");
        return relacoes;
    }

    private void validarConexoesDoPool() {
        List<Connection> conexoes = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                Connection conexao = dataSource.getConnection();
                conexoes.add(conexao);
                try (Statement statement = conexao.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT current_schemas(false)::text, to_regclass('users')::text")) {
                    result.next();
                    String schemas = result.getString(1);
                    String users = result.getString(2);
                    if (users == null || !schemas.contains("apartamento")) {
                        throw new IllegalStateException(
                                "Conexao online-test sem search_path esperado: schemas=" + schemas
                                        + ", users=" + users);
                    }
                }
            }
            log.info("online-test datasource: search_path validado em {} conexoes simultaneas",
                    conexoes.size());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Falha ao validar search_path nas conexoes do datasource online-test", ex);
        } finally {
            conexoes.forEach(conexao -> {
                try {
                    conexao.close();
                } catch (Exception ex) {
                    log.warn("Falha ao devolver conexao de diagnostico ao pool", ex);
                }
            });
        }
    }
}
