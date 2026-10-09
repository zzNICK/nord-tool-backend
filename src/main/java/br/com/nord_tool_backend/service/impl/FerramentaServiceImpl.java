package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.dto.FerramentaDto;
import br.com.nord_tool_backend.form.FerramentaForm;
import br.com.nord_tool_backend.repository.FerramentaRepository;
import br.com.nord_tool_backend.service.FerramentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FerramentaServiceImpl implements FerramentaService {
    private final FerramentaRepository ferramentaRepository;

    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FerramentaDto salvarFerramenta(FerramentaForm ferramentaForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        log.info("Iniciando método para salvar ferramenta");
        return ferramentaRepository.salvarFerramenta(ferramentaForm.converterToDomain(null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FerramentaDto alterarFerramenta(Long id, FerramentaForm ferramentaForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        log.info("Iniciando método para alterar ferramenta");
        return ferramentaRepository.alterarFerramenta(ferramentaForm.converterToDomain(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarFerramenta(Long id) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        log.info("Iniciando método para deletar ferramenta");
        ferramentaRepository.deletarFerramenta(id);
    }

    @Override
    public FerramentaDto buscarPorIdFerramenta(Long id) {
        autorizacao.exigirAutenticado();
        Ferramenta ferramenta = ferramentaRepository.buscarPorIdFerramenta(id);
        return FerramentaDto.converterToDto(ferramenta);
    }

    @Override
    public List<FerramentaDto> listarFerramentas() {
        autorizacao.exigirAutenticado();
        return ferramentaRepository.listarFerramentas().stream()
                .map(FerramentaDto::converterToDto)
                .collect(Collectors.toList());
    }
}
