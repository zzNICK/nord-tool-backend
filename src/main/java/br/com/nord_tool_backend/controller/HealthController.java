package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.dto.HealthDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nord-tool")
@Tag(name = "Health", description = "Endpoint Health")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Testa a se aplicação está funcionando normalmente")
    public HealthDto getCliente() {
        HealthDto health = new HealthDto();
        health.setMessage("Retornando com sucesso");
        return health;

    }
}