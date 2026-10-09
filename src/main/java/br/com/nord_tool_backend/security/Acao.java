package br.com.nord_tool_backend.security;

import java.util.stream.Stream;

/** Nível de acesso hierárquico: cada nível inclui os anteriores. {@code NENHUM} nega o módulo. */
public enum Acao {
    NENHUM(0),
    LEITURA(1),
    ESCRITA(2),
    ADMIN(3);

    private final int nivel;

    Acao(int nivel) {
        this.nivel = nivel;
    }

    public boolean atende(Acao exigida) {
        return nivel >= exigida.nivel;
    }

    /** Código desconhecido vale {@code NENHUM} (nega). */
    public static Acao de(String codigo) {
        if (codigo == null) return NENHUM;
        String chave = codigo.trim();
        return Stream.of(values()).filter(a -> a.name().equalsIgnoreCase(chave)).findFirst().orElse(NENHUM);
    }
}
