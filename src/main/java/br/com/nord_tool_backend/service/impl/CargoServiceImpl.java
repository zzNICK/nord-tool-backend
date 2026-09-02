package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.repository.CargoRepository;
import br.com.nord_tool_backend.service.CargoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class CargoServiceImpl implements CargoService {
    private final CargoRepository cargoRepository;

    public List<CargoDto> listarCargos() {
        return cargoRepository.listarCargos().stream()
                .map(CargoDto::converterToDomain).collect(Collectors.toList());
    }
}
