package br.com.nord_tool_backend.form;

import br.com.nord_tool_backend.domain.Colaborador;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColaboradorForm {
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Size(max = 11, message = "Celular deve ter no máximo 11 dígitos")
    @Pattern(regexp = "\\d{10,11}", message = "Celular deve conter 10 ou 11 dígitos")
    private String celular;

    @NotNull(message = "Empresa é obrigatória")
    private Integer idEmpresa;

    @NotNull(message = "Cargo é obrigatório")
    private Integer idCargo;

    @NotNull(message = "Permissão é obrigatória")
    private Integer idPermissao;

    public Colaborador converterToDomain(Long id) {
        return Colaborador.builder()
                .id(id)
                .nome(nome)
                .celular(celular)
                .idEmpresa(idEmpresa)
                .idCargo(idCargo)
                .idPermissao(idPermissao)
                .build();
    }
}
