package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Colaborador;
import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.form.ColaboradorForm;
import br.com.nord_tool_backend.repository.ColaboradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColaboradorServiceImplTest {
    @Mock
    private ColaboradorRepository repository;

    private ColaboradorServiceImpl service;
    private ColaboradorForm form;
    private Colaborador colaborador;
    private ColaboradorDto dto;

    @BeforeEach
    void setup() {
        service = new ColaboradorServiceImpl(repository);
        form = ColaboradorForm.builder()
                .nome("João Silva")
                .celular("11999999999")
                .idEmpresa(1)
                .idCargo(2)
                .idPermissao(3)
                .build();
        colaborador = Colaborador.builder()
                .id(10L).nome("João Silva").celular("11999999999")
                .idEmpresa(1).nomeEmpresa("Empresa")
                .idCargo(2).nomeCargo("Cargo")
                .idPermissao(3).nomePermissao("Permissão").build();
        dto = ColaboradorDto.converterToDomain(colaborador);
    }

    @Test
    void deveSalvarColaborador() {
        when(repository.salvarColaborador(org.mockito.ArgumentMatchers.any())).thenReturn(dto);
        assertEquals(dto, service.salvarColaborador(form));
        ArgumentCaptor<Colaborador> captor = ArgumentCaptor.forClass(Colaborador.class);
        verify(repository).salvarColaborador(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals("João Silva", captor.getValue().getNome());
    }

    @Test
    void deveAlterarColaboradorComIdDoCaminho() {
        when(repository.alterarColaborador(org.mockito.ArgumentMatchers.any())).thenReturn(dto);
        assertEquals(dto, service.alterarColaborador(10L, form));
        ArgumentCaptor<Colaborador> captor = ArgumentCaptor.forClass(Colaborador.class);
        verify(repository).alterarColaborador(captor.capture());
        assertEquals(10L, captor.getValue().getId());
    }

    @Test
    void deveDeletarColaborador() {
        service.deletarColaborador(10L);
        verify(repository).deletarColaborador(10L);
    }

    @Test
    void deveBuscarColaborador() {
        when(repository.buscarPorIdColaborador(10L)).thenReturn(colaborador);
        assertEquals(dto, service.buscarPorIdColaborador(10L));
    }

    @Test
    void deveListarColaboradores() {
        when(repository.listarColaboradores()).thenReturn(Collections.singletonList(colaborador));
        assertEquals(Collections.singletonList(dto), service.listarColaboradores());
    }
}
