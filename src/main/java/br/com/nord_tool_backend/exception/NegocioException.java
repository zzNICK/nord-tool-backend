package br.com.nord_tool_backend.exception;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;

/** 422: regra de negócio violada por uma entrada bem formada. */
public class NegocioException extends NordException {

    public static final String CD_ERRO = "REGRA_NEGOCIO";

    public NegocioException(String mensagem) {
        super(NordHttpEnum.HTTP_422, CD_ERRO, mensagem, null);
    }

    public NegocioException(String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_422, CD_ERRO, mensagem, causa);
    }

    public NegocioException(String cdErro, String mensagem) {
        super(NordHttpEnum.HTTP_422, cdErro, mensagem, null);
    }

    public NegocioException(String cdErro, String mensagem, Throwable causa) {
        super(NordHttpEnum.HTTP_422, cdErro, mensagem, causa);
    }
}
