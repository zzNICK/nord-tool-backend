package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.Permissao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class PermissaoDto {
    private Long id;
    private String nome;

    public static PermissaoDto converterToDomain(Permissao permissao) {
        return PermissaoDto.builder().id(permissao.getId()).nome(permissao.getNome()).build();
    }
}
