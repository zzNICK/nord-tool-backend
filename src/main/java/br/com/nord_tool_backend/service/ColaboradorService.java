package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.form.ColaboradorForm;

import java.util.List;

public interface ColaboradorService {
    ColaboradorDto salvarColaborador(ColaboradorForm colaboradorForm);
    ColaboradorDto alterarColaborador(Long id, ColaboradorForm colaboradorForm);
    void deletarColaborador(Long id);
    ColaboradorDto buscarPorIdColaborador(Long id);
    List<ColaboradorDto> listarColaboradores();
}
