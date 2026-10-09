package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.LimiteRequisicoesException;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LimiteTentativasServiceImpl implements LimiteTentativasService {

    public static final Duration JANELA = Duration.ofMinutes(1);
    static final String MSG_LIMITE = "Muitas tentativas. Aguarde um minuto e tente novamente.";

    /** Janela fixa contada a partir da primeira tentativa da chave. */
    private final Cache<String, AtomicInteger> contadores = Caffeine.newBuilder()
            .expireAfterWrite(JANELA)
            .maximumSize(100_000)
            .build();

    @Override
    public void registrar(String chave, int limitePorJanela) {
        AtomicInteger contador = contadores.get(chave, k -> new AtomicInteger());
        if (contador != null && contador.incrementAndGet() > limitePorJanela) {
            throw new LimiteRequisicoesException(MSG_LIMITE);
        }
    }
}
