package br.com.nord_tool_backend.service;

/**
 * Cria o primeiro ADMIN quando a tabela {@code usuario} está vazia e NORD_ADMIN_EMAIL, NORD_ADMIN_NAME e
 * NORD_ADMIN_PASSWORD estão definidas. Com as variáveis definidas, qualquer falha derruba o startup.
 */
public interface AdminBootstrapService {

    /** Devolve true se criou o ADMIN. */
    boolean executar(String email, String nome, String senha);
}
