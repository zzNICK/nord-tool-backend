package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Colaborador extends GlobalDomain {
    private Long id;
    private String nome;
    private String celular;
    private Integer idEmpresa;
    private String nomeEmpresa;
    private Integer idCargo;
    private String nomeCargo;
    private Integer idPermissao;
    private String nomePermissao;
}
