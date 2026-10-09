package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 401: requisição sem autenticação válida. */
public class NaoAutenticadoException extends NordException {

    public static final String CD_ERRO = "NAO_AUTENTICADO";

    public NaoAutenticadoException(String mensagem) {
        super(NordHttpEnum.HTTP_401, CD_ERRO, mensagem, null);
    }

    public NaoAutenticadoException(String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_401, CD_ERRO, mensagem, causa);
    }

    public NaoAutenticadoException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_401, cdErro, mensagem, null);
    }

    public NaoAutenticadoException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_401, cdErro, mensagem, causa);
    }
}
