package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 400: entrada inválida ou regra de validação não atendida. */
public class EntradaInvalidaException extends NordException {

    public static final String CD_ERRO = "ENTRADA_INVALIDA";

    public EntradaInvalidaException(String mensagem) {
        super(NordHttpEnum.HTTP_400, CD_ERRO, mensagem, null);
    }

    public EntradaInvalidaException(String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_400, CD_ERRO, mensagem, causa);
    }

    public EntradaInvalidaException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_400, cdErro, mensagem, null);
    }

    public EntradaInvalidaException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_400, cdErro, mensagem, causa);
    }
}
