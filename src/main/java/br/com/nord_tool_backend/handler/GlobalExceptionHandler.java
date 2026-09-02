package br.com.nord_tool_backend.handler;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse(NordHttpEnum.HTTP_400.getMensagem());

        return ResponseEntity
                .badRequest()
                .body(new ApiResponseBody<>(NordHttpEnum.HTTP_400, mensagem, null));
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ApiResponseBody<String>> handleValidacaoException(
            ValidacaoException ex) {

        NordHttpEnum status = ex.getHttpEnum();

        return ResponseEntity
                .status(status.getStatus())
                .body(
                        new ApiResponseBody<>(
                                status,
                                ex.getMenssage(),
                                ex.getException()
                        )
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseBody<String>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        Throwable causa = ex.getMostSpecificCause();

        return ResponseEntity
                .badRequest()
                .body(
                        new ApiResponseBody<>(
                                NordHttpEnum.HTTP_400,
                                causa.getMessage(),
                                causa.getClass().getSimpleName()
                        )
                );
    }
}
