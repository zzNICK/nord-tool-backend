package br.com.nord_tool_backend.handler;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.exception.FalhaInternaException;
import br.com.nord_tool_backend.exception.LimiteRequisicoesException;
import br.com.nord_tool_backend.exception.NordException;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Converte exceções em respostas padronizadas ({@link ApiResponseBody#erro}). A causa técnica (mensagem do
 * banco, do Jackson, de I/O, stack trace) vai somente para o log, com o mesmo {@code idCorrelacao} da resposta;
 * o cliente recebe a mensagem pública e um {@code cdErro} estável.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String MSG_CORPO_INVALIDO = "Corpo da requisição inválido";
    static final String CD_ENTRADA_INVALIDA = "ENTRADA_INVALIDA";
    private static final String PACOTE_APLICACAO = "br.com.nord_tool_backend.";

    @ExceptionHandler(NordException.class)
    public ResponseEntity<ApiResponseBody<String>> handleNordException(NordException ex) {
        NordHttpEnum status = ex.getStatus();
        if (status.getStatus().is5xxServerError()) {
            log.error("{} [{}]", ex.getMessage(), status.getStatus().value(), ex);
        } else if (ex.getCause() != null) {
            log.warn("{} ({})", ex.getMessage(), status.getStatus().value(), ex.getCause());
        }
        ResponseEntity<ApiResponseBody<String>> resposta = responder(status, ex.getCdErro(), ex.getMessage());
        if (ex instanceof LimiteRequisicoesException) {
            return ResponseEntity.status(status.getStatus())
                    .header(HttpHeaders.RETRY_AFTER, "60")
                    .body(resposta.getBody());
        }
        return resposta;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse(NordHttpEnum.HTTP_400.getMensagem());
        return responder(NordHttpEnum.HTTP_400, CD_ENTRADA_INVALIDA, mensagem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseBody<String>> handleConstraintViolation(ConstraintViolationException ex) {
        String mensagem = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> ultimoNo(v) + ": " + v.getMessage())
                .orElse(NordHttpEnum.HTTP_400.getMensagem());
        return responder(NordHttpEnum.HTTP_400, CD_ENTRADA_INVALIDA, mensagem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseBody<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return responder(NordHttpEnum.HTTP_400, CD_ENTRADA_INVALIDA, "Valor inválido para o parâmetro '" + ex.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return responder(NordHttpEnum.HTTP_400, CD_ENTRADA_INVALIDA,
                "Parâmetro obrigatório '" + ex.getParameterName() + "' não informado");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMissingPart(MissingServletRequestPartException ex) {
        return responder(NordHttpEnum.HTTP_400, CD_ENTRADA_INVALIDA,
                "Arquivo obrigatório '" + ex.getRequestPartName() + "' não enviado");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseBody<String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Corpo da requisição não pôde ser lido", ex);
        return responder(NordHttpEnum.HTTP_400, CD_ENTRADA_INVALIDA, mensagemPublica(ex));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return responder(NordHttpEnum.HTTP_400, CD_ENTRADA_INVALIDA, "Tipo de conteúdo não suportado");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return responder(NordHttpEnum.HTTP_413, "ARQUIVO_MUITO_GRANDE", NordHttpEnum.HTTP_413.getMensagem());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponseBody<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return responder(NordHttpEnum.HTTP_405, "METODO_NAO_PERMITIDO", NordHttpEnum.HTTP_405.getMensagem());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseBody<String>> handleAuthentication(AuthenticationException ex) {
        return responder(NordHttpEnum.HTTP_401, "NAO_AUTENTICADO", "Autenticação necessária");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseBody<String>> handleAccessDenied(AccessDeniedException ex) {
        return responder(NordHttpEnum.HTTP_403, "ACESSO_NEGADO", NordHttpEnum.HTTP_403.getMensagem());
    }

    /** Qualquer outra falha: 500 com mensagem genérica; o detalhe fica no log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseBody<String>> handleInesperada(Exception ex) {
        log.error("Erro não tratado", ex);
        return responder(NordHttpEnum.HTTP_500, FalhaInternaException.CD_ERRO, NordHttpEnum.HTTP_500.getMensagem());
    }

    private static ResponseEntity<ApiResponseBody<String>> responder(NordHttpEnum status, String cdErro, String mensagem) {
        return ResponseEntity.status(status.getStatus()).body(ApiResponseBody.erro(status, cdErro, mensagem));
    }

    private static String ultimoNo(ConstraintViolation<?> violacao) {
        return StreamSupport.stream(violacao.getPropertyPath().spliterator(), false)
                .reduce((primeiro, segundo) -> segundo)
                .map(Object::toString)
                .orElse("valor");
    }

    /**
     * Mensagens de validação lançadas pelo próprio código (desserializadores e enums do projeto) são públicas;
     * as do Jackson citam classes internas e são trocadas por uma mensagem com o campo afetado.
     */
    static String mensagemPublica(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getMostSpecificCause();
        if (causa instanceof IllegalArgumentException && lancadaPelaAplicacao(causa) && causa.getMessage() != null) {
            return causa.getMessage();
        }
        if (ex.getCause() instanceof JsonMappingException) {
            String campo = ((JsonMappingException) ex.getCause()).getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."))
                    .replace(".[", "[");
            if (!campo.isEmpty()) {
                return "Valor inválido no campo '" + campo + "'";
            }
        }
        return MSG_CORPO_INVALIDO;
    }

    private static boolean lancadaPelaAplicacao(Throwable causa) {
        StackTraceElement[] pilha = causa.getStackTrace();
        return pilha.length > 0 && pilha[0].getClassName().startsWith(PACOTE_APLICACAO);
    }
}
