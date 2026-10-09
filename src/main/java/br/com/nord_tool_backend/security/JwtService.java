package br.com.nord_tool_backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Emissão e validação dos tokens de acesso (JWT HS256 com kid, iss/aud, auth_time e versão de sessão). */
public interface JwtService {

    /** Duração do token de acesso. */
    Duration getDuracaoToken();

    /** Tempo máximo de uma sessão desde o login, somando as renovações. */
    Duration getDuracaoMaximaSessao();

    TokenEmitido emitir(UsuarioAutenticado usuario, Instant agora);

    /** Devolve o principal se a assinatura, o emissor, o público, o tipo e a validade conferirem. */
    Optional<UsuarioAutenticado> validar(String token);
}
