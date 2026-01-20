package br.com.nord_tool_backend.excepetion;

public class ValidacaoException extends RuntimeException {

    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}