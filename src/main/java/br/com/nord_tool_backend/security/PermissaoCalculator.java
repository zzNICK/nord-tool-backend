package br.com.nord_tool_backend.security;

import br.com.nord_tool_backend.domain.PerfilPermissao;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolve a permissão efetiva de cada módulo a partir das linhas do perfil e das exceções do usuário,
 * da mais específica para a menos: usuário+módulo, perfil+módulo, usuário+{@code *}, perfil+{@code *}.
 * Módulos que terminam em {@code NENHUM} ficam fora do mapa.
 */
public final class PermissaoCalculator {

    private PermissaoCalculator() {
    }

    public static Map<Modulo, Acao> resolver(Collection<PerfilPermissao> doPerfil, Collection<PerfilPermissao> doUsuario) {
        Map<Modulo, Acao> efetivas = new EnumMap<>(Modulo.class);
        for (Modulo modulo : Modulo.values()) {
            Acao acao = buscar(doUsuario, modulo.name())
                    .orElseGet(() -> buscar(doPerfil, modulo.name())
                            .orElseGet(() -> buscar(doUsuario, Modulo.CURINGA)
                                    .orElseGet(() -> buscar(doPerfil, Modulo.CURINGA).orElse(Acao.NENHUM))));
            if (acao != Acao.NENHUM) {
                efetivas.put(modulo, acao);
            }
        }
        return efetivas;
    }

    private static Optional<Acao> buscar(Collection<PerfilPermissao> linhas, String codigoModulo) {
        if (linhas == null) return Optional.empty();
        return linhas.stream()
                .filter(l -> l.getCdModulo() != null && l.getCdModulo().trim().equalsIgnoreCase(codigoModulo))
                .map(l -> Acao.de(l.getCdAcao()))
                .findFirst();
    }
}
