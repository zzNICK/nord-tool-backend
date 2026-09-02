package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Colaborador;
import br.com.nord_tool_backend.dto.ColaboradorDto;

import java.util.List;

public interface ColaboradorRepository {
    ColaboradorDto salvarColaborador(Colaborador colaborador);
    ColaboradorDto alterarColaborador(Colaborador colaborador);
    void deletarColaborador(Long id);
    Colaborador buscarPorIdColaborador(Long id);
    List<Colaborador> listarColaboradores();
}
