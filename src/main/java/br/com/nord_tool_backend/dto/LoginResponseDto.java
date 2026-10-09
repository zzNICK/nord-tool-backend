package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class LoginResponseDto {
    private String token;
    /** Instante de expiração do token (ISO-8601, UTC). */
    private String expiraEm;
    /** Duração do token em minutos (o frontend renova antes de expirar enquanto houver atividade). */
    private long inatividadeMinutos;
    /** Fim absoluto da sessão (ISO-8601, UTC): depois disso só com novo login. */
    private String sessaoExpiraEm;
    private UsuarioDto usuario;
}
