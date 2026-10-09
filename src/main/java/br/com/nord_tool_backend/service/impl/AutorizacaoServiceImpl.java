package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAtualProvider;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutorizacaoServiceImpl implements AutorizacaoService {

    static final String MSG_SEM_PERMISSAO = "Você não tem permissão para esta ação";

    private final UsuarioAtualProvider usuarioAtual;

    @Override
    public UsuarioAutenticado exigir(Modulo modulo, Acao acao) {
        UsuarioAutenticado usuario = exigirAutenticado();
        if (!usuario.acao(modulo).atende(acao)) {
            log.info("Acesso negado: {} exige {}:{} (tem {})", usuario, modulo, acao, usuario.acao(modulo));
            throw new AcessoNegadoException(MSG_SEM_PERMISSAO);
        }
        return usuario;
    }

    @Override
    public UsuarioAutenticado exigirAutenticado() {
        return usuarioAtual.atual().orElseThrow(() -> new NaoAutenticadoException("Autenticação necessária"));
    }
}
