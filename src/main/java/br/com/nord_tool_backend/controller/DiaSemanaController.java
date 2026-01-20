package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.response.BodyResponse;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.DiaSemanaDto;
import br.com.nord_tool_backend.service.DiaSemanaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/nord-tool")
@Tag(name = "Dia Semana", description = "Endpoints de Dia da Semana")
@RequiredArgsConstructor
public class DiaSemanaController implements BaseResponse {

    private final DiaSemanaService diaSemanaService;

    @Operation(summary = "Lista os dias da semana")
    @GetMapping("/diaSemana")
    public ResponseEntity<BodyResponse<List<DiaSemanaDto>>> listaDiaSemana() {
        return ok(diaSemanaService.listarDiaSemana());
    }
}
