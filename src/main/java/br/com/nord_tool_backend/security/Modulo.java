package br.com.nord_tool_backend.security;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Módulos sob controle de acesso. O nome da constante é o {@code cd_modulo} gravado em
 * {@code perfil_permissao}/{@code usuario_permissao}; {@code *} (curinga) vale para todos.
 */
public enum Modulo {
    VISTORIA,
    TERMO_REPROVA,
    CRONOGRAMA,
    CONTROLE_CHAVES,
    CAIXINHA,
    FINANCEIRO,
    CASAMENTO,
    CADASTROS,
    ADMINISTRACAO;

    public static final String CURINGA = "*";

    public static Optional<Modulo> de(String codigo) {
        if (codigo == null) return Optional.empty();
        String chave = codigo.trim();
        return Stream.of(values()).filter(m -> m.name().equalsIgnoreCase(chave)).findFirst();
    }
}
