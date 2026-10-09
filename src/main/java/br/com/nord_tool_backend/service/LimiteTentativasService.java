package br.com.nord_tool_backend.service;

/**
 * Limite de tentativas por chave (IP, e-mail) numa janela de um minuto. Em memória: vale para uma instância
 * da aplicação; com várias instâncias o contador precisa ir para um armazenamento compartilhado.
 */
public interface LimiteTentativasService {

    /** Conta a tentativa; lança 429 quando a chave passa do limite na janela atual. */
    void registrar(String chave, int limitePorJanela);
}
