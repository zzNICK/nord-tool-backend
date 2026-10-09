package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 429: limite de tentativas excedido. */
public class LimiteRequisicoesException extends NordException {

    public static final String CD_ERRO = "LIMITE_TENTATIVAS";

    public LimiteRequisicoesException(String mensagem) {
        super(NordHttpEnum.HTTP_429, CD_ERRO, mensagem, null);
    }

    public LimiteRequisicoesException(String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_429, CD_ERRO, mensagem, causa);
    }

    public LimiteRequisicoesException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_429, cdErro, mensagem, null);
    }

    public LimiteRequisicoesException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_429, cdErro, mensagem, causa);
    }
}
