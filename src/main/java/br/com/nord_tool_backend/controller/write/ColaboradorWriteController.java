package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.form.ColaboradorForm;
import br.com.nord_tool_backend.service.ColaboradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/colaboradores")
@Tag(name = "Colaboradores", description = "Endpoints para criar, alterar e deletar colaboradores")
public class ColaboradorWriteController implements BaseResponse {
    private final ColaboradorService colaboradorService;

    @Operation(summary = "Criar colaborador")
    @PostMapping
    public ResponseEntity<ApiResponseBody<ColaboradorDto>> criarColaborador(
            @Valid @RequestBody ColaboradorForm colaboradorForm) {
        return created(colaboradorService.salvarColaborador(colaboradorForm));
    }

    @Operation(summary = "Alterar colaborador")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<ColaboradorDto>> alterarColaborador(
            @PathVariable Long id, @Valid @RequestBody ColaboradorForm colaboradorForm) {
        return ok(colaboradorService.alterarColaborador(id, colaboradorForm));
    }

    @Operation(summary = "Excluir colaborador")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarColaborador(@PathVariable Long id) {
        colaboradorService.deletarColaborador(id);
        return noContent();
    }
}
