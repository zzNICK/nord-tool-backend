package br.com.nord_tool_backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BodyResponse<T> {
    private static final long serialVersionUID = 1L;
    private LocalDateTime timestamp;
    private Integer nrStatus;
    private transient T body;
    private String txMensagem;

    public BodyResponse(final NordHttpEnum status, final T body) {
        this.timestamp = LocalDateTime.now();
        this.nrStatus = status.getStatus().value();
        this.txMensagem = status.getMensagem();
        this.body = body;
    }
}
