package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.service.AdministracaoCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/cache")
@Tag(name = "Cache Manager", description = "Endpoints para limpar cache (somente ADMIN)")
public class CacheWriteController implements BaseResponse {

    private final AdministracaoCacheService administracaoCacheService;

    @Operation(summary = "Limpar todos os caches")
    @PutMapping("/limparTodos")
    public ResponseEntity<ApiResponseBody<Void>> limparTodos() {
        administracaoCacheService.limparTodos();
        return noContent();
    }

    @Operation(summary = "Limpar um cache específico")
    @PutMapping("/{cacheName}")
    public ResponseEntity<ApiResponseBody<Void>> limparCache(@PathVariable String cacheName) {
        administracaoCacheService.limparCache(cacheName);
        return noContent();
    }
}
