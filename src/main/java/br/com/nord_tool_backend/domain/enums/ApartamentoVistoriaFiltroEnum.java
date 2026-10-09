package br.com.nord_tool_backend.domain.enums;

/** Consulta usada na listagem filtrada de apartamentos: busca em todos os campos ou por atributo. */
public enum ApartamentoVistoriaFiltroEnum {
    QUERY_TODOS("SPS.LISTAR.APARTAMENTO_VISTORIA_FILTRA_TODOS"),
    QUERY_WHERE("SPS.LISTAR.APARTAMENTO_VISTORIA_FILTRA_ATRIBUTO");

    private final String queryProperty;

    ApartamentoVistoriaFiltroEnum(String queryProperty) {
        this.queryProperty = queryProperty;
    }

    public String getQueryProperty() {
        return queryProperty;
    }

    public static ApartamentoVistoriaFiltroEnum para(String filtraTodos) {
        return filtraTodos != null && !filtraTodos.isEmpty() ? QUERY_TODOS : QUERY_WHERE;
    }
}
