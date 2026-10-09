package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 404: recurso inexistente ou fora do escopo do usuário. */
public class NaoEncontradoException extends NordException {

    public static final String CD_ERRO = "NAO_ENCONTRADO";

    public NaoEncontradoException(String mensagem) {
        super(NordHttpEnum.HTTP_404, CD_ERRO, mensagem, null);
    }

    public NaoEncontradoException(String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_404, CD_ERRO, mensagem, causa);
    }

    public NaoEncontradoException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_404, cdErro, mensagem, null);
    }

    public NaoEncontradoException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_404, cdErro, mensagem, causa);
    }
}
