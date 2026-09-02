package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/empresas")
public class EmpresaReadController implements BaseResponse {
    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<ApiResponseBody<List<EmpresaDto>>> listarEmpresas() {
        return ok(empresaService.listarEmpresas());
    }
}
