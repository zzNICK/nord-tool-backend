package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AdministracaoCacheService;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdministracaoCacheServiceImpl implements AdministracaoCacheService {

    private final CacheService cacheService;
    private final AutorizacaoService autorizacao;

    @Override
    public void limparCache(String nomeCache) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        cacheService.limparCache(nomeCache);
    }

    @Override
    public void limparTodos() {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        cacheService.limparTodos();
    }
}
