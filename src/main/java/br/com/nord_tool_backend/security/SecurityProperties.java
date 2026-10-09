package br.com.nord_tool_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuração de segurança ({@code nord-tool.security.*}). A autenticação é sempre exigida; fora do
 * perfil {@code local} o startup falha sem chave JWT externa (ver {@link JwtServiceImpl} e {@link SecurityConfig}).
 *
 * <p>Chaves: {@code NORD_JWT_KEYS} no formato {@code kid1:base64;kid2:base64} e {@code NORD_JWT_ACTIVE_KID}
 * com a chave que assina. As demais continuam valendo para validar (rotação). {@code NORD_JWT_SECRET} é aceito
 * como chave única de compatibilidade.</p>
 */
@Component
public class SecurityProperties {

    private final String jwtKeys;
    private final String activeKid;
    private final String jwtSecret;
    private final long accessTokenMinutes;
    private final long sessionMaxHours;
    private final boolean allowEphemeralSecret;

    public SecurityProperties(
            @Value("${nord-tool.security.jwt-keys:}") String jwtKeys,
            @Value("${nord-tool.security.active-kid:}") String activeKid,
            @Value("${nord-tool.security.jwt-secret:}") String jwtSecret,
            @Value("${nord-tool.security.access-token-minutes:15}") long accessTokenMinutes,
            @Value("${nord-tool.security.session-max-hours:12}") long sessionMaxHours,
            @Value("${nord-tool.security.allow-ephemeral-secret:false}") boolean allowEphemeralSecret) {
        this.jwtKeys = jwtKeys;
        this.activeKid = activeKid;
        this.jwtSecret = jwtSecret;
        this.accessTokenMinutes = accessTokenMinutes;
        this.sessionMaxHours = sessionMaxHours;
        this.allowEphemeralSecret = allowEphemeralSecret;
    }

    public String getJwtKeys() { return jwtKeys; }
    public String getActiveKid() { return activeKid; }
    public String getJwtSecret() { return jwtSecret; }
    public long getAccessTokenMinutes() { return accessTokenMinutes; }
    public long getSessionMaxHours() { return sessionMaxHours; }
    public boolean isAllowEphemeralSecret() { return allowEphemeralSecret; }
}
