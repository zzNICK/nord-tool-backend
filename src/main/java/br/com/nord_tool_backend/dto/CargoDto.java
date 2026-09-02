package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Cargo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class CargoDto {
    private Long id;
    private String nome;

    public static CargoDto converterToDomain(Cargo cargo) {
        return CargoDto.builder().id(cargo.getId()).nome(cargo.getNome()).build();
    }
}
