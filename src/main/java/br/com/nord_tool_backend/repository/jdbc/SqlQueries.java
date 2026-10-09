package br.com.nord_tool_backend.repository.jdbc;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Catálogo das queries SQL de {@code classpath:query/*.properties}, lidas em UTF-8 num mapa próprio
 * (fora do {@code Environment}). Chave repetida em dois arquivos ou chave ausente derrubam o startup,
 * porque os repositories resolvem as suas queries no construtor.
 */
@Component
public class SqlQueries {

    static final String LOCAL = "classpath*:query/*.properties";

    private final Map<String, String> queries;

    public SqlQueries() {
        this.queries = Collections.unmodifiableMap(carregar());
    }

    /** Devolve a query da chave; lança {@link IllegalStateException} se ela não existir. */
    public String get(String chave) {
        String sql = queries.get(chave);
        if (sql == null) {
            throw new IllegalStateException("Query SQL não encontrada: " + chave);
        }
        return sql;
    }

    private static Map<String, String> carregar() {
        Map<String, String> mapa = new HashMap<>();
        Map<String, String> origem = new HashMap<>();
        try {
            for (Resource arquivo : new PathMatchingResourcePatternResolver().getResources(LOCAL)) {
                Properties propriedades = new Properties();
                try (Reader leitor = new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8)) {
                    propriedades.load(leitor);
                }
                for (String chave : propriedades.stringPropertyNames()) {
                    String anterior = origem.putIfAbsent(chave, arquivo.getFilename());
                    if (anterior != null) {
                        throw new IllegalStateException("Query SQL " + chave + " definida em " + anterior
                                + " e em " + arquivo.getFilename());
                    }
                    mapa.put(chave, propriedades.getProperty(chave));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível ler as queries SQL", ex);
        }
        return mapa;
    }
}
