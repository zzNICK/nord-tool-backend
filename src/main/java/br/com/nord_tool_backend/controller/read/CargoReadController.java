package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.service.CargoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/cargos")
public class CargoReadController implements BaseResponse {
    private final CargoService cargoService;

    @GetMapping
    public ResponseEntity<ApiResponseBody<List<CargoDto>>> listarCargos() {
        return ok(cargoService.listarCargos());
    }
}
