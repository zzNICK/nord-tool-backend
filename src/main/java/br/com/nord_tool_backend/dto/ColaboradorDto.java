package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Colaborador;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColaboradorDto {
    private Long id;
    private String nome;
    private String celular;
    private Integer idEmpresa;
    private String nomeEmpresa;
    private Integer idCargo;
    private String nomeCargo;
    private Integer idPermissao;
    private String nomePermissao;

    public static ColaboradorDto converterToDomain(Colaborador colaborador) {
        return ColaboradorDto.builder()
                .id(colaborador.getId())
                .nome(colaborador.getNome())
                .celular(colaborador.getCelular())
                .idEmpresa(colaborador.getIdEmpresa())
                .nomeEmpresa(colaborador.getNomeEmpresa())
                .idCargo(colaborador.getIdCargo())
                .nomeCargo(colaborador.getNomeCargo())
                .idPermissao(colaborador.getIdPermissao())
                .nomePermissao(colaborador.getNomePermissao())
                .build();
    }
}
