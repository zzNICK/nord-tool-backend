package br.com.nord_tool_backend.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Principal extraído do JWT: identidade, permissões já resolvidas e dados da sessão. */
public class UsuarioAutenticado implements Serializable {

    private final Long id;
    private final String email;
    private final String perfil;
    private final Map<Modulo, Acao> permissoes;
    private final int versaoSessao;
    private final Instant inicioSessao;

    public UsuarioAutenticado(Long id, String email, String perfil, Map<Modulo, Acao> permissoes,
                              int versaoSessao, Instant inicioSessao) {
        this.id = id;
        this.email = email;
        this.perfil = perfil;
        this.permissoes = permissoes == null || permissoes.isEmpty()
                ? Collections.emptyMap() : Collections.unmodifiableMap(new EnumMap<>(permissoes));
        this.versaoSessao = versaoSessao;
        this.inicioSessao = inicioSessao;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPerfil() { return perfil; }
    public Map<Modulo, Acao> getPermissoes() { return permissoes; }
    public int getVersaoSessao() { return versaoSessao; }
    /** Instante do login (auth_time); preservado nas renovações para o limite absoluto da sessão. */
    public Instant getInicioSessao() { return inicioSessao; }

    public Acao acao(Modulo modulo) {
        return permissoes.getOrDefault(modulo, Acao.NENHUM);
    }

    @Override
    public String toString() { return "usuario#" + id; }
}
