package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusVistoria {
    private Integer idStatusVistoria;
    private String nmStatusVistoria;
}
