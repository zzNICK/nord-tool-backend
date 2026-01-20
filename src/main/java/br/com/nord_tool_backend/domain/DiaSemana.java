package br.com.nord_tool_backend.domain;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaSemana implements Serializable {

    private Integer idDiaSemana;
    private String nmDiaSemana;
}
