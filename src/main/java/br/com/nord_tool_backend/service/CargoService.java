package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CargoDto;
import java.util.List;

public interface CargoService {
    List<CargoDto> listarCargos();
}
