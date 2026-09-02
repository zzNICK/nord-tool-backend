package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Colaborador;
import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.form.ColaboradorForm;
import br.com.nord_tool_backend.repository.ColaboradorRepository;
import br.com.nord_tool_backend.service.ColaboradorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ColaboradorServiceImpl implements ColaboradorService {
    private final ColaboradorRepository colaboradorRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ColaboradorDto salvarColaborador(ColaboradorForm colaboradorForm) {
        log.info("Iniciando método para salvar colaborador");
        return colaboradorRepository.salvarColaborador(colaboradorForm.converterToDomain(null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ColaboradorDto alterarColaborador(Long id, ColaboradorForm colaboradorForm) {
        log.info("Iniciando método para alterar colaborador");
        return colaboradorRepository.alterarColaborador(colaboradorForm.converterToDomain(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarColaborador(Long id) {
        log.info("Iniciando método para deletar colaborador");
        colaboradorRepository.deletarColaborador(id);
    }

    @Override
    public ColaboradorDto buscarPorIdColaborador(Long id) {
        Colaborador colaborador = colaboradorRepository.buscarPorIdColaborador(id);
        return ColaboradorDto.converterToDomain(colaborador);
    }

    @Override
    public List<ColaboradorDto> listarColaboradores() {
        return colaboradorRepository.listarColaboradores().stream()
                .map(ColaboradorDto::converterToDomain)
                .collect(Collectors.toList());
    }
}
