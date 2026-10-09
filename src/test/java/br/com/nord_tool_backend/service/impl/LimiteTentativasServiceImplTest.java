package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.LimiteRequisicoesException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimiteTentativasServiceImplTest {

    private final LimiteTentativasServiceImpl service = new LimiteTentativasServiceImpl();

    @Test
    void permiteAteOLimiteERecusaAPartirDaSeguinte() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> service.registrar("ip:1.2.3.4", 3));
        }
        assertThrows(LimiteRequisicoesException.class, () -> service.registrar("ip:1.2.3.4", 3));
        assertThrows(LimiteRequisicoesException.class, () -> service.registrar("ip:1.2.3.4", 3));
    }

    @Test
    void chavesDiferentesTemContadoresIndependentes() {
        for (int i = 0; i < 3; i++) service.registrar("email:a@b.com", 3);
        assertDoesNotThrow(() -> service.registrar("email:c@d.com", 3));
        assertDoesNotThrow(() -> service.registrar("ip:1.2.3.4", 3));
    }
}
