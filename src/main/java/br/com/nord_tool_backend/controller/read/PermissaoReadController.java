package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.service.PermissaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/permissoes")
public class PermissaoReadController implements BaseResponse {
    private final PermissaoService permissaoService;

    @GetMapping
    public ResponseEntity<ApiResponseBody<List<PermissaoDto>>> listarPermissoes() {
        return ok(permissaoService.listarPermissoes());
    }
}
