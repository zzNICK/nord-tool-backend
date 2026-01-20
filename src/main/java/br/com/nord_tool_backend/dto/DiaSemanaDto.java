package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.DiaSemana;
import lombok.*;

import java.io.Serializable;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DiaSemanaDto implements Serializable {

    private Integer idDiaSemana;
    private String nmDiaSemana;

    public static DiaSemanaDto converterToDomain(DiaSemana diaSemana ) {
        return DiaSemanaDto.builder()
                .idDiaSemana(diaSemana.getIdDiaSemana())
                .nmDiaSemana(diaSemana.getNmDiaSemana())
                .build();
    }
}
