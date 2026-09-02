package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Cargo;
import br.com.nord_tool_backend.domain.Empresa;
import br.com.nord_tool_backend.domain.Permissao;
import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.repository.CargoRepository;
import br.com.nord_tool_backend.repository.EmpresaRepository;
import br.com.nord_tool_backend.repository.PermissaoRepository;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogoColaboradorServiceImplTest {
    @Test
    void deveListarEmpresas() {
        EmpresaRepository repository = mock(EmpresaRepository.class);
        when(repository.listarEmpresas()).thenReturn(Collections.singletonList(Empresa.builder().id(1L).nome("Alfa").build()));
        assertEquals(Collections.singletonList(EmpresaDto.builder().id(1L).nome("Alfa").build()),
                new EmpresaServiceImpl(repository).listarEmpresas());
        verify(repository).listarEmpresas();
    }

    @Test
    void deveListarCargos() {
        CargoRepository repository = mock(CargoRepository.class);
        when(repository.listarCargos()).thenReturn(Collections.singletonList(Cargo.builder().id(2L).nome("Analista").build()));
        assertEquals(Collections.singletonList(CargoDto.builder().id(2L).nome("Analista").build()),
                new CargoServiceImpl(repository).listarCargos());
        verify(repository).listarCargos();
    }

    @Test
    void deveListarPermissoes() {
        PermissaoRepository repository = mock(PermissaoRepository.class);
        when(repository.listarPermissoes()).thenReturn(Collections.singletonList(Permissao.builder().id(3L).nome("Administrador").build()));
        assertEquals(Collections.singletonList(PermissaoDto.builder().id(3L).nome("Administrador").build()),
                new PermissaoServiceImpl(repository).listarPermissoes());
        verify(repository).listarPermissoes();
    }
}
