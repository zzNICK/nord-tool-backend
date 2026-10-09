package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.CacheService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AdministracaoCacheServiceImplTest {

    private final CacheService cacheService = mock(CacheService.class);
    private final AutorizacaoService autorizacao = mock(AutorizacaoService.class);
    private final AdministracaoCacheServiceImpl service = new AdministracaoCacheServiceImpl(cacheService, autorizacao);

    @Test
    void adminLimpaUmCacheOuTodos() {
        service.limparCache("apartamentoVistoriaDto");
        service.limparTodos();
        verify(autorizacao, times(2)).exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        verify(cacheService).limparCache("apartamentoVistoriaDto");
        verify(cacheService).limparTodos();
    }

    @Test
    void semPermissaoDeAdminNaoLimpaNada() {
        when(autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.limparCache("x"));
        assertThrows(AcessoNegadoException.class, service::limparTodos);
        verifyNoInteractions(cacheService);
    }
}
