package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.form.EmpresaForm;
import br.com.nord_tool_backend.repository.EmpresaRepository;
import br.com.nord_tool_backend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class EmpresaServiceImpl implements EmpresaService {
    private final EmpresaRepository empresaRepository;

    private final AutorizacaoService autorizacao;

    public List<EmpresaDto> listarEmpresas() {
        autorizacao.exigirAutenticado();
        return empresaRepository.listarEmpresas().stream()
                .map(EmpresaDto::converterToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmpresaDto salvarEmpresa(EmpresaForm empresaForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        return EmpresaDto.converterToDto(empresaRepository.salvarEmpresa(empresaForm.converterToDomain(null)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmpresaDto alterarEmpresa(Long id, EmpresaForm empresaForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        return EmpresaDto.converterToDto(empresaRepository.alterarEmpresa(empresaForm.converterToDomain(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarEmpresa(Long id) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        empresaRepository.deletarEmpresa(id);
    }
}
