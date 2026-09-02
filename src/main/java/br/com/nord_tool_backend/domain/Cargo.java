package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class Cargo extends GlobalDomain {
    private Long id;
    private String nome;
}
