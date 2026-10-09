package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.form.PermissaoForm;
import br.com.nord_tool_backend.repository.PermissaoRepository;
import br.com.nord_tool_backend.service.PermissaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PermissaoServiceImpl implements PermissaoService {
    private final PermissaoRepository permissaoRepository;

    private final AutorizacaoService autorizacao;

    public List<PermissaoDto> listarPermissoes() {
        autorizacao.exigirAutenticado();
        return permissaoRepository.listarPermissoes().stream()
                .map(PermissaoDto::converterToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissaoDto salvarPermissao(PermissaoForm permissaoForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        return PermissaoDto.converterToDto(permissaoRepository.salvarPermissao(permissaoForm.converterToDomain(null)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissaoDto alterarPermissao(Long id, PermissaoForm permissaoForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        return PermissaoDto.converterToDto(permissaoRepository.alterarPermissao(permissaoForm.converterToDomain(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarPermissao(Long id) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        permissaoRepository.deletarPermissao(id);
    }
}
