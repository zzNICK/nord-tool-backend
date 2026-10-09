package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAtualProvider;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutorizacaoServiceImplTest {

    private final UsuarioAtualProvider usuarioAtual = mock(UsuarioAtualProvider.class);
    private final AutorizacaoServiceImpl service = new AutorizacaoServiceImpl(usuarioAtual);

    private void logado(Map<Modulo, Acao> permissoes) {
        when(usuarioAtual.atual()).thenReturn(Optional.of(new UsuarioAutenticado(1L, "a@b.com", "OPERADOR", permissoes, 0, Instant.now())));
    }

    private static Map<Modulo, Acao> com(Modulo modulo, Acao acao) {
        Map<Modulo, Acao> mapa = new EnumMap<>(Modulo.class);
        mapa.put(modulo, acao);
        return mapa;
    }

    @Test
    void nivelSuperiorAtendeOsInferiores() {
        logado(com(Modulo.FINANCEIRO, Acao.ADMIN));
        assertNotNull(service.exigir(Modulo.FINANCEIRO, Acao.LEITURA));
        assertNotNull(service.exigir(Modulo.FINANCEIRO, Acao.ESCRITA));
        assertNotNull(service.exigir(Modulo.FINANCEIRO, Acao.ADMIN));
    }

    @Test
    void leituraNaoPermiteEscritaEOutroModuloEhNegado() {
        logado(com(Modulo.CASAMENTO, Acao.LEITURA));
        assertNotNull(service.exigir(Modulo.CASAMENTO, Acao.LEITURA));
        assertThrows(AcessoNegadoException.class, () -> service.exigir(Modulo.CASAMENTO, Acao.ESCRITA));
        assertThrows(AcessoNegadoException.class, () -> service.exigir(Modulo.FINANCEIRO, Acao.LEITURA));
    }

    @Test
    void semUsuarioEhNaoAutenticado() {
        when(usuarioAtual.atual()).thenReturn(Optional.empty());
        assertThrows(NaoAutenticadoException.class, () -> service.exigir(Modulo.VISTORIA, Acao.LEITURA));
        assertThrows(NaoAutenticadoException.class, service::exigirAutenticado);
    }

    @Test
    void autenticadoSemModuloAindaAcessaDadosDeApoio() {
        logado(new EnumMap<>(Modulo.class));
        assertEquals(1L, service.exigirAutenticado().getId());
        assertThrows(AcessoNegadoException.class, () -> service.exigir(Modulo.VISTORIA, Acao.LEITURA));
    }
}
