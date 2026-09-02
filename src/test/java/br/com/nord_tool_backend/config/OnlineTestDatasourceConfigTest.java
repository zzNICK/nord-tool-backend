package br.com.nord_tool_backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineTestDatasourceConfigTest {

    @Test
    void deveConfigurarSearchPathEmTodaConexaoDoPoolOnlineTest() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-online-test.yaml"));

        Properties properties = yaml.getObject();

        assertEquals(
                "SET search_path TO apartamento, public",
                properties.getProperty("spring.datasource.hikari.connection-init-sql")
        );
        assertEquals(
                "-c search_path=apartamento,public",
                properties.getProperty("spring.datasource.hikari.data-source-properties.options")
        );
        assertEquals(
                "apartamento,public",
                properties.getProperty("spring.datasource.hikari.data-source-properties.currentSchema")
        );
        assertTrue(properties.getProperty("spring.datasource.url").startsWith("${SPRING_DATASOURCE_URL:"));
        assertEquals("${SPRING_DATASOURCE_USERNAME:${PGUSER}}",
                properties.getProperty("spring.datasource.username"));
        assertEquals("${SPRING_DATASOURCE_PASSWORD:${PGPASSWORD}}",
                properties.getProperty("spring.datasource.password"));
    }

    @Test
    void deveAplicarSearchPathNoDataSourceEfetivamenteCriadoPeloSpring() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/teste",
                        "spring.datasource.username=usuario_teste",
                        "spring.datasource.password=senha_teste",
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
                        "spring.datasource.hikari.connection-init-sql=SET search_path TO apartamento, public",
                        "spring.datasource.hikari.data-source-properties.currentSchema=apartamento,public",
                        "spring.datasource.hikari.data-source-properties.options=-c search_path=apartamento,public"
                )
                .run(context -> {
                    DataSource dataSource = context.getBean(DataSource.class);
                    HikariDataSource hikari = (HikariDataSource) dataSource;
                    assertEquals(
                            "SET search_path TO apartamento, public",
                            hikari.getConnectionInitSql()
                    );
                    assertEquals(
                            "-c search_path=apartamento,public",
                            hikari.getDataSourceProperties().getProperty("options")
                    );
                    assertEquals(
                            "apartamento,public",
                            hikari.getDataSourceProperties().getProperty("currentSchema")
                    );
                });
    }
}
