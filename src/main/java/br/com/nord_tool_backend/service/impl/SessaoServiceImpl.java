package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.SessaoService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class SessaoServiceImpl implements SessaoService {

    static final Duration VALIDADE_CACHE = Duration.ofSeconds(60);

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final Clock clock;
    private final Cache<Long, Optional<Usuario>> estados = Caffeine.newBuilder()
            .expireAfterWrite(VALIDADE_CACHE)
            .maximumSize(1_000)
            .build();

    public SessaoServiceImpl(UsuarioRepository usuarioRepository, JwtService jwtService, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Override
    public boolean valida(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getInicioSessao() == null) {
            return false;
        }
        Instant limite = usuario.getInicioSessao().plus(jwtService.getDuracaoMaximaSessao());
        if (clock.instant().isAfter(limite)) {
            return false;
        }
        Optional<Usuario> estado = estados.get(usuario.getId(), usuarioRepository::buscarEstadoSessao);
        return estado != null && estado
                .filter(u -> Boolean.TRUE.equals(u.getInAtivo()))
                .filter(u -> Objects.equals(versao(u), usuario.getVersaoSessao()))
                .isPresent();
    }

    @Override
    public void invalidar(Long idUsuario) {
        if (idUsuario != null) {
            estados.invalidate(idUsuario);
        }
    }

    static int versao(Usuario usuario) {
        return usuario.getNrVersaoSessao() == null ? 0 : usuario.getNrVersaoSessao();
    }
}
