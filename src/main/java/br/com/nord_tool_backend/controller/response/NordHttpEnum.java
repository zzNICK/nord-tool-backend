package br.com.nord_tool_backend.controller.response;

import br.com.nord_tool_backend.utils.ApiResponseMessage;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public enum NordHttpEnum {
    HTTP_200(HttpStatus.OK, ApiResponseMessage.RESPONSE_200),
    HTTP_201(HttpStatus.CREATED, ApiResponseMessage.RESPONSE_201),
    HTTP_204(HttpStatus.OK, ApiResponseMessage.RESPONSE_204),
    HTTP_304(HttpStatus.NOT_MODIFIED, ApiResponseMessage.RESPONSE_304),
    HTTP_400(HttpStatus.BAD_REQUEST, ApiResponseMessage.RESPONSE_400),
    HTTP_401(HttpStatus.UNAUTHORIZED, ApiResponseMessage.RESPONSE_401),
    HTTP_404(HttpStatus.NOT_FOUND, ApiResponseMessage.RESPONSE_404),
    HTTP_500(HttpStatus.INTERNAL_SERVER_ERROR, ApiResponseMessage.RESPONSE_500);

    private HttpStatus status;
    private String mensagem;

    NordHttpEnum(final HttpStatus status, final String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }

    public HttpStatus getStatus() {
        return this.status;
    }

    public String getMensagem() {
        return this.mensagem;
    }
}
