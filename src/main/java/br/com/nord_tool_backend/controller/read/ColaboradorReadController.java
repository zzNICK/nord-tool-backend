package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.service.ColaboradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/colaboradores")
@Tag(name = "Colaboradores", description = "Endpoints para buscar e listar colaboradores")
public class ColaboradorReadController implements BaseResponse {
    private final ColaboradorService colaboradorService;

    @Operation(summary = "Listar colaboradores")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<ColaboradorDto>>> listarColaboradores() {
        return ok(colaboradorService.listarColaboradores());
    }

    @Operation(summary = "Buscar colaborador por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<ColaboradorDto>> buscarColaborador(@PathVariable Long id) {
        return ok(colaboradorService.buscarPorIdColaborador(id));
    }
}
