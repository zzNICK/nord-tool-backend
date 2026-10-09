package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.NaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class CacheServiceImplTest {


    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private CacheServiceImpl cacheService;

    @Test
    void deveLimparCachePorNome() {
        when(cacheManager.getCache("diaSemanaCache")).thenReturn(cache);
        cacheService.limparCache("diaSemanaCache");
        verify(cache, times(1)).clear();
    }

    @Test
    void naoDeveLimparChacePorNome() {
        String cacheName = "cacheInexistente";

        when(cacheManager.getCache(cacheName)).thenReturn(null);
        NaoEncontradoException exception = assertThrows(NaoEncontradoException.class, () -> cacheService.limparCache(cacheName));
        assertEquals("Cache não encontrado: " + cacheName, exception.getMessage());
        verify(cacheManager).getCache(cacheName);
    }

    @Test
    void deveLimparTodosOsCaches() {
        when(cacheManager.getCacheNames()).thenReturn(Set.of("cache1", "cache2"));
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        cacheService.limparTodos();
        verify(cache, times(2)).clear();
    }

    @Test
    void naoDeveDarErroSeCacheForNulo() {
        when(cacheManager.getCacheNames()).thenReturn(Set.of("cache1"));
        when(cacheManager.getCache("cache1")).thenReturn(null);
        assertDoesNotThrow(() -> cacheService.limparTodos());
        verify(cache, never()).clear();
    }

}
