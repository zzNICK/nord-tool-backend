package br.com.nord_tool_backend.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Política de senha comum à troca de senha e ao bootstrap: de 10 caracteres a 72 bytes em UTF-8
 * (o BCrypt ignora o que passa de 72 bytes, então senhas maiores seriam truncadas em silêncio).
 */
final class PoliticaSenha {

    static final int MIN_CARACTERES = 10;
    static final int MAX_BYTES = 72;

    private PoliticaSenha() {
    }

    /** Motivo da recusa, ou vazio se a senha atende à política. */
    static Optional<String> validar(String senha) {
        if (senha == null || senha.length() < MIN_CARACTERES) {
            return Optional.of("a senha deve ter ao menos " + MIN_CARACTERES + " caracteres");
        }
        if (excedeLimiteBcrypt(senha)) {
            return Optional.of("a senha deve ter no máximo " + MAX_BYTES + " bytes (acentos contam 2)");
        }
        return Optional.empty();
    }

    static boolean excedeLimiteBcrypt(String senha) {
        return senha != null && senha.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}
