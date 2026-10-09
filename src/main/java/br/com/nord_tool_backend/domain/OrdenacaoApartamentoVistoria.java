package br.com.nord_tool_backend.domain;

import br.com.nord_tool_backend.domain.enums.OrdenacaoApartamentoVistoriaEnum;

import java.util.Locale;

/**
 * Ordenação já validada da listagem de apartamentos ("campo,direcao" vindo da API). Coluna fora da lista
 * cai no padrão; direção diferente de DESC vira ASC. Nada do texto recebido é concatenado no SQL.
 */
public final class OrdenacaoApartamentoVistoria {

    public static final OrdenacaoApartamentoVistoria PADRAO =
            new OrdenacaoApartamentoVistoria(OrdenacaoApartamentoVistoriaEnum.id_apartamento_vistoria, true);

    private final OrdenacaoApartamentoVistoriaEnum coluna;
    private final boolean descendente;

    public OrdenacaoApartamentoVistoria(OrdenacaoApartamentoVistoriaEnum coluna, boolean descendente) {
        this.coluna = coluna;
        this.descendente = descendente;
    }

    public static OrdenacaoApartamentoVistoria de(String nmOrdenacao) {
        if (nmOrdenacao == null || nmOrdenacao.trim().isEmpty()) {
            return PADRAO;
        }
        String[] partes = nmOrdenacao.split(",");
        OrdenacaoApartamentoVistoriaEnum coluna = OrdenacaoApartamentoVistoriaEnum.porCampo(partes[0].trim())
                .orElse(OrdenacaoApartamentoVistoriaEnum.id_apartamento_vistoria);
        boolean descendente = partes.length > 1 && "DESC".equals(partes[1].trim().toUpperCase(Locale.ROOT));
        return new OrdenacaoApartamentoVistoria(coluna, descendente);
    }

    public OrdenacaoApartamentoVistoriaEnum getColuna() {
        return coluna;
    }

    public boolean isDescendente() {
        return descendente;
    }

    /** Fragmento SQL montado só a partir dos enums. */
    public String sql() {
        return " ORDER BY " + coluna.name() + (descendente ? " DESC " : " ASC ");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrdenacaoApartamentoVistoria)) return false;
        OrdenacaoApartamentoVistoria outra = (OrdenacaoApartamentoVistoria) o;
        return coluna == outra.coluna && descendente == outra.descendente;
    }

    @Override
    public int hashCode() {
        return coluna.hashCode() * 31 + (descendente ? 1 : 0);
    }
}
