package br.com.nord_tool_backend.domain.enums;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Colunas aceitas na ordenação da listagem de apartamentos. O nome da constante é a coluna SQL; o campo é o
 * nome recebido da API. Só essas colunas (e as direções ASC/DESC) chegam ao ORDER BY.
 */
public enum OrdenacaoApartamentoVistoriaEnum {
    id_apartamento_vistoria("idApartamentoVistoria"),
    nm_apartamento_vistoria("nmApartamentoVistoria"),
    nm_dia_semana("nmDiaSemana"),
    dt_apartamento_vigente("dtApartamentoVigente"),
    nm_horario_vistoria("nmHorarioVistoria"),
    nm_status_vistoria("nmStatusVistoria"),
    tx_observacao_revistoria("txObservacaoRevistoria"),
    dt_revistoria_vigente("dtRevistoriaVigente");

    private final String campo;

    OrdenacaoApartamentoVistoriaEnum(String campo) {
        this.campo = campo;
    }

    public static Optional<OrdenacaoApartamentoVistoriaEnum> porCampo(String campo) {
        return Stream.of(values()).filter(c -> c.campo.equals(campo)).findFirst();
    }
}
