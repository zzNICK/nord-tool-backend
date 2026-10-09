package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 409: conflito de versão otimista (o frontend recarrega os dados). */
public class ConflitoException extends NordException {

    public static final String CD_ERRO = "CONFLITO_VERSAO";

    public ConflitoException(String mensagem) {
        super(NordHttpEnum.HTTP_409, CD_ERRO, mensagem, null);
    }

    public ConflitoException(String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_409, CD_ERRO, mensagem, causa);
    }

    public ConflitoException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_409, cdErro, mensagem, null);
    }

    public ConflitoException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_409, cdErro, mensagem, causa);
    }
}
