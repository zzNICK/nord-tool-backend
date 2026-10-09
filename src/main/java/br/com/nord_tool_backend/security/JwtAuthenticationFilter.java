package br.com.nord_tool_backend.security;

import br.com.nord_tool_backend.service.SessaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Autentica a requisição pelo Bearer token. Além da assinatura e da validade, confere a sessão no banco
 * (usuário ativo e versão da sessão, com cache curto): token revogado segue como anônimo e recebe 401.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final SessaoService sessaoService;

    public JwtAuthenticationFilter(JwtService jwtService, SessaoService sessaoService) {
        this.jwtService = jwtService;
        this.sessaoService = sessaoService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            Optional<UsuarioAutenticado> usuario = jwtService.validar(cabecalho.substring(PREFIXO.length()).trim());
            if (usuario.isPresent() && sessaoValida(usuario.get())) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(usuario.get(), null, autoridades(usuario.get()));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean sessaoValida(UsuarioAutenticado usuario) {
        try {
            return sessaoService.valida(usuario);
        } catch (RuntimeException ex) {
            // Sem conseguir conferir a sessão, a requisição não é autenticada (falha fechada).
            log.error("Não foi possível validar a sessão de {}", usuario, ex);
            return false;
        }
    }

    /** ROLE_{perfil} e PERM_{MODULO}_{ACAO} para cada nível atendido (ESCRITA gera também LEITURA). */
    static List<GrantedAuthority> autoridades(UsuarioAutenticado usuario) {
        List<GrantedAuthority> lista = new ArrayList<>();
        if (usuario.getPerfil() != null) {
            lista.add(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil()));
        }
        for (Map.Entry<Modulo, Acao> permissao : usuario.getPermissoes().entrySet()) {
            for (Acao nivel : Acao.values()) {
                if (nivel != Acao.NENHUM && permissao.getValue().atende(nivel)) {
                    lista.add(new SimpleGrantedAuthority("PERM_" + permissao.getKey().name() + "_" + nivel.name()));
                }
            }
        }
        return lista;
    }
}
