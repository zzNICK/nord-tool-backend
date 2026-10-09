package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class Usuario extends GlobalDomain {
    private String nmEmail;
    private String nmNome;
    private String nmSenhaHash;
    private Long idPerfil;
    private String cdPerfil;
    private Boolean inAtivo;
    private Integer nrFalhasLogin;
    private LocalDateTime dhBloqueadoAte;
    private LocalDateTime dhUltimoLogin;
    /** Incrementada na troca de senha (e em revogações): tokens com versão anterior deixam de valer. */
    private Integer nrVersaoSessao;
}
