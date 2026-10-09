package br.com.nord_tool_backend.service.investimento;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provedor brapi.dev (gratuito). O token vem de NORD_COTACAO_TOKEN e nunca vai para o log nem para o cliente.
 * Um ticker por chamada: funciona em qualquer plano.
 */
@Component @Slf4j
public class BrapiCotacaoProvider implements CotacaoProvider {

    private final RestTemplate http;
    private final String url;
    private final String token;

    @Autowired
    public BrapiCotacaoProvider(RestTemplateBuilder builder,
                                @Value("${nord-tool.cotacao.url:https://brapi.dev/api}") String url,
                                @Value("${nord-tool.cotacao.token:}") String token,
                                @Value("${nord-tool.cotacao.timeout-seconds:5}") long timeoutSegundos) {
        this(builder.setConnectTimeout(Duration.ofSeconds(timeoutSegundos))
                .setReadTimeout(Duration.ofSeconds(timeoutSegundos)).build(), url, token);
    }

    /** Construtor com o cliente HTTP já montado (testes do CotacaoServiceImpl usam um servidor simulado). */
    public BrapiCotacaoProvider(RestTemplate http, String url, String token) {
        this.http = http;
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.token = token == null ? "" : token.trim();
    }

    @Override
    public Optional<Cotacao> cotar(String ticker) {
        JsonNode resultado = consultar(ticker, false);
        if (resultado == null) return Optional.empty();
        JsonNode preco = resultado.path("regularMarketPrice");
        if (!preco.isNumber() || preco.decimalValue().signum() <= 0) return Optional.empty();
        Instant momento = Instant.now();
        String hora = resultado.path("regularMarketTime").asText("");
        if (!hora.isEmpty()) {
            try {
                momento = Instant.parse(hora);
            } catch (Exception ignorado) {
                // hora fora do padrão: vale o momento da consulta
            }
        }
        return Optional.of(new Cotacao(preco.decimalValue(), momento));
    }

    @Override
    public List<ProventoCotado> proventos(String ticker) {
        JsonNode resultado = consultar(ticker, true);
        if (resultado == null) return Collections.emptyList();
        List<ProventoCotado> proventos = new ArrayList<>();
        for (JsonNode d : resultado.path("dividendsData").path("cashDividends")) {
            LocalDate pagamento = data(d.path("paymentDate"));
            LocalDate com = data(d.path("lastDatePrior"));
            if (com == null) com = data(d.path("approvedOn"));
            BigDecimal valor = d.path("rate").isNumber() ? d.path("rate").decimalValue() : null;
            if (pagamento == null || com == null || valor == null || valor.signum() <= 0 || pagamento.isBefore(com)) continue;
            proventos.add(new ProventoCotado(com, pagamento, valor));
        }
        return proventos;
    }

    private JsonNode consultar(String ticker, boolean comProventos) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (!token.isEmpty()) headers.setBearerAuth(token);
            String alvo = url + "/quote/" + ticker + (comProventos ? "?dividends=true" : "");
            JsonNode corpo = http.exchange(alvo, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
            JsonNode resultados = corpo == null ? null : corpo.path("results");
            return resultados != null && resultados.isArray() && resultados.size() > 0 ? resultados.get(0) : null;
        } catch (Exception ex) {
            log.warn("Cotação de {} indisponível: {}", ticker, ex.getClass().getSimpleName());
            return null;
        }
    }

    private static LocalDate data(JsonNode no) {
        String texto = no.asText("");
        if (texto.length() < 10) return null;
        try {
            return LocalDate.parse(texto.substring(0, 10));
        } catch (Exception ex) {
            return null;
        }
    }
}
