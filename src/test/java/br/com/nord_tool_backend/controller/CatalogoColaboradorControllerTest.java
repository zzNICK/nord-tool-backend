package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.CargoReadController;
import br.com.nord_tool_backend.controller.read.EmpresaReadController;
import br.com.nord_tool_backend.controller.read.PermissaoReadController;
import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.service.CargoService;
import br.com.nord_tool_backend.service.EmpresaService;
import br.com.nord_tool_backend.service.PermissaoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogoColaboradorControllerTest {
    @Test
    void deveRetornarEmpresasComHttp200() {
        EmpresaService service = mock(EmpresaService.class);
        List<EmpresaDto> dados = Collections.singletonList(EmpresaDto.builder().id(1L).nome("Alfa").build());
        when(service.listarEmpresas()).thenReturn(dados);
        ResponseEntity<ApiResponseBody<List<EmpresaDto>>> response = new EmpresaReadController(service).listarEmpresas();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dados, response.getBody().getBody());
    }

    @Test
    void deveRetornarCargosComHttp200() {
        CargoService service = mock(CargoService.class);
        List<CargoDto> dados = Collections.singletonList(CargoDto.builder().id(2L).nome("Analista").build());
        when(service.listarCargos()).thenReturn(dados);
        ResponseEntity<ApiResponseBody<List<CargoDto>>> response = new CargoReadController(service).listarCargos();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dados, response.getBody().getBody());
    }

    @Test
    void deveRetornarPermissoesComHttp200() {
        PermissaoService service = mock(PermissaoService.class);
        List<PermissaoDto> dados = Collections.singletonList(PermissaoDto.builder().id(3L).nome("Administrador").build());
        when(service.listarPermissoes()).thenReturn(dados);
        ResponseEntity<ApiResponseBody<List<PermissaoDto>>> response = new PermissaoReadController(service).listarPermissoes();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dados, response.getBody().getBody());
    }
}
