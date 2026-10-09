package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final CacheManager cacheManager;

    @Override
    public void limparCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new NaoEncontradoException("Cache não encontrado: " + cacheName);
        }
        cache.clear();
    }

    @Override
    public void limparTodos() {
        cacheManager.getCacheNames()
                .forEach(nome -> {
                    Cache cache = cacheManager.getCache(nome);
                    if (cache != null) {
                        cache.clear();
                    }
                });
    }
}
