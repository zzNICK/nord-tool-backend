package br.com.nord_tool_backend.service;

/**
 * Limpeza de cache pela API (somente ADMIN). Os serviços que precisam invalidar dados chamam o
 * {@link CacheService} diretamente, sem passar por esta autorização.
 */
public interface AdministracaoCacheService {

    void limparCache(String nomeCache);

    void limparTodos();
}
