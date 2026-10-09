package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SessaoServiceImplTest {

    private static final Instant AGORA = Instant.parse("2026-10-08T15:00:00Z");

    private UsuarioRepository repository;
    private SessaoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.getDuracaoMaximaSessao()).thenReturn(Duration.ofHours(12));
        service = new SessaoServiceImpl(repository, jwt, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private static Usuario estado(boolean ativo, int versao) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setInAtivo(ativo);
        u.setNrVersaoSessao(versao);
        return u;
    }

    private static UsuarioAutenticado token(int versao, Instant inicio) {
        return new UsuarioAutenticado(1L, "a@b.com", "ADMIN", new EnumMap<>(Modulo.class), versao, inicio);
    }

    @Test
    void sessaoAtivaComMesmaVersaoEhValidaEUsaOCache() {
        when(repository.buscarEstadoSessao(1L)).thenReturn(Optional.of(estado(true, 2)));
        assertTrue(service.valida(token(2, AGORA.minusSeconds(60))));
        assertTrue(service.valida(token(2, AGORA.minusSeconds(60))));
        verify(repository, times(1)).buscarEstadoSessao(1L);
    }

    @Test
    void versaoDiferenteUsuarioInativoOuInexistenteInvalidam() {
        when(repository.buscarEstadoSessao(1L)).thenReturn(Optional.of(estado(true, 3)));
        assertFalse(service.valida(token(2, AGORA)));

        service.invalidar(1L);
        when(repository.buscarEstadoSessao(1L)).thenReturn(Optional.of(estado(false, 2)));
        assertFalse(service.valida(token(2, AGORA)));

        service.invalidar(1L);
        when(repository.buscarEstadoSessao(1L)).thenReturn(Optional.empty());
        assertFalse(service.valida(token(2, AGORA)));
    }

    @Test
    void invalidarFazARevogacaoValerNaHora() {
        when(repository.buscarEstadoSessao(1L)).thenReturn(Optional.of(estado(true, 2)));
        assertTrue(service.valida(token(2, AGORA)));

        when(repository.buscarEstadoSessao(1L)).thenReturn(Optional.of(estado(true, 3)));
        service.invalidar(1L);
        assertFalse(service.valida(token(2, AGORA)));
    }

    @Test
    void sessaoAlemDoLimiteAbsolutoNaoValeNemConsultaOBanco() {
        assertFalse(service.valida(token(0, AGORA.minus(Duration.ofHours(12)).minusSeconds(1))));
        assertFalse(service.valida(null));
        verifyNoInteractions(repository);
    }
}
