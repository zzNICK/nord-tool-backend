package br.com.nord_tool_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Lê o principal colocado no SecurityContext pelo {@link JwtAuthenticationFilter}. */
@Component
public class SecurityContextUsuarioAtualProvider implements UsuarioAtualProvider {

    @Override
    public Optional<UsuarioAutenticado> atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado)) {
            return Optional.empty();
        }
        return Optional.of((UsuarioAutenticado) auth.getPrincipal());
    }
}
