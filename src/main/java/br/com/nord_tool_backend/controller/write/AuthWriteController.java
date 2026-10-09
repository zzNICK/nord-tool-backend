package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/auth")
@Tag(name = "Autenticação", description = "Login e sessão (JWT)")
public class AuthWriteController implements BaseResponse {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Autentica por e-mail e senha e devolve o JWT")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> login(@Valid @RequestBody LoginForm form,
                                                                   HttpServletRequest request) {
        // Atrás de proxy, o IP real vem de X-Forwarded-For via server.forward-headers-strategy (perfil railway).
        return ok(authService.login(form, request.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token (versão de sessão e limite absoluto desde o login)")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> refresh() {
        return ok(authService.refresh());
    }

    @PostMapping("/alterar-senha")
    @Operation(summary = "Altera a senha (10 caracteres a 72 bytes), encerra as outras sessões e devolve token novo")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> alterarSenha(@Valid @RequestBody AlterarSenhaForm form) {
        return ok(authService.alterarSenha(form));
    }
}
