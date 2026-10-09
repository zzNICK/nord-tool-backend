package br.com.nord_tool_backend.security;

import java.time.Instant;

/** Token de acesso emitido e o instante em que expira. */
public final class TokenEmitido {

    private final String token;
    private final Instant expiraEm;

    public TokenEmitido(String token, Instant expiraEm) {
        this.token = token;
        this.expiraEm = expiraEm;
    }

    public String getToken() { return token; }
    public Instant getExpiraEm() { return expiraEm; }
}
