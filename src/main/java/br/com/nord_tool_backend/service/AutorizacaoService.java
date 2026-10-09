package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;

/**
 * Autorização decidida nos serviços: todo método público de *ServiceImpl chama {@link #exigir} (ou
 * {@link #exigirAutenticado} para dados de apoio) antes de ler ou alterar dados.
 */
public interface AutorizacaoService {

    /** Exige o nível no módulo; lança 401 sem usuário e 403 sem permissão. Devolve o usuário. */
    UsuarioAutenticado exigir(Modulo modulo, Acao acao);

    /** Exige apenas um usuário autenticado (tabelas de apoio usadas por várias telas). */
    UsuarioAutenticado exigirAutenticado();
}
