package br.com.nord_tool_backend.controller.response;

import org.springframework.http.ResponseEntity;

public interface BaseResponse {

    default <T> ResponseEntity<BodyResponse<T>> ok(T body) {
        return ResponseEntity
                .status(NordHttpEnum.HTTP_200.getStatus())
                .body(new BodyResponse<>(NordHttpEnum.HTTP_200, body));
    }

    default <T> ResponseEntity<BodyResponse<T>> created(T body) {
        return ResponseEntity
                .status(NordHttpEnum.HTTP_201.getStatus())
                .body(new BodyResponse<>(NordHttpEnum.HTTP_201, body));
    }

    default ResponseEntity<BodyResponse<Void>> noContent() {
        return ResponseEntity
                .status(NordHttpEnum.HTTP_204.getStatus())
                .body(new BodyResponse<>(NordHttpEnum.HTTP_204, null));
    }

    default ResponseEntity<BodyResponse<Void>> badRequest(String mensagem) {
        return ResponseEntity
                .status(NordHttpEnum.HTTP_400.getStatus())
                .body(
                        BodyResponse.<Void>builder()
                                .timestamp(java.time.LocalDateTime.now())
                                .nrStatus(NordHttpEnum.HTTP_400.getStatus().value())
                                .txMensagem(mensagem)
                                .build()
                );
    }

    default ResponseEntity<BodyResponse<Void>> notFound() {
        return ResponseEntity
                .status(NordHttpEnum.HTTP_404.getStatus())
                .body(new BodyResponse<>(NordHttpEnum.HTTP_404, null));
    }

    default ResponseEntity<BodyResponse<Void>> internalError() {
        return ResponseEntity
                .status(NordHttpEnum.HTTP_500.getStatus())
                .body(new BodyResponse<>(NordHttpEnum.HTTP_500, null));
    }
}