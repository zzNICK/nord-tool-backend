package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.ColaboradorReadController;
import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.write.ColaboradorWriteController;
import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.form.ColaboradorForm;
import br.com.nord_tool_backend.service.ColaboradorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColaboradorControllerTest {
    @Mock
    private ColaboradorService service;
    private ColaboradorReadController readController;
    private ColaboradorWriteController writeController;
    private ColaboradorDto dto;
    private ColaboradorForm form;

    @BeforeEach
    void setup() {
        readController = new ColaboradorReadController(service);
        writeController = new ColaboradorWriteController(service);
        dto = ColaboradorDto.builder().id(1L).nome("João Silva").celular("11999999999")
                .idEmpresa(1).nomeEmpresa("Empresa").idCargo(1).nomeCargo("Cargo")
                .idPermissao(1).nomePermissao("Permissão").build();
        form = ColaboradorForm.builder().nome("João Silva").celular("11999999999")
                .idEmpresa(1).idCargo(1).idPermissao(1).build();
    }

    @Test
    void deveListarComHttp200() {
        when(service.listarColaboradores()).thenReturn(Collections.singletonList(dto));
        ResponseEntity<ApiResponseBody<List<ColaboradorDto>>> response = readController.listarColaboradores();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(Collections.singletonList(dto), response.getBody().getBody());
    }

    @Test
    void deveBuscarComHttp200() {
        when(service.buscarPorIdColaborador(1L)).thenReturn(dto);
        ResponseEntity<ApiResponseBody<ColaboradorDto>> response = readController.buscarColaborador(1L);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody().getBody());
    }

    @Test
    void deveCriarComHttp201() {
        when(service.salvarColaborador(form)).thenReturn(dto);
        ResponseEntity<ApiResponseBody<ColaboradorDto>> response = writeController.criarColaborador(form);
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(dto, response.getBody().getBody());
    }

    @Test
    void deveAlterarComHttp200() {
        when(service.alterarColaborador(1L, form)).thenReturn(dto);
        ResponseEntity<ApiResponseBody<ColaboradorDto>> response = writeController.alterarColaborador(1L, form);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody().getBody());
    }

    @Test
    void deveDeletarComHttp204() {
        ResponseEntity<ApiResponseBody<Void>> response = writeController.deletarColaborador(1L);
        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody().getBody());
        verify(service).deletarColaborador(1L);
    }
}
