package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.Cargo;
import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.form.CargoForm;
import br.com.nord_tool_backend.repository.CargoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CargoServiceImplTest {
    @Mock
    private CargoRepository cargoRepository;

    @Mock


    private AutorizacaoService autorizacao;



    @InjectMocks
    private CargoServiceImpl cargoService;

    @Test
    void deveListarCargosConvertidosParaDto() {
        Cargo cargo = Cargo.builder().id(2L).nmCargo("Analista").build();
        when(cargoRepository.listarCargos()).thenReturn(Collections.singletonList(cargo));

        assertEquals(Collections.singletonList(CargoDto.builder().id(2L).nmCargo("Analista").build()),
                cargoService.listarCargos());
        verify(cargoRepository).listarCargos();
    }

    @Test
    void deveSalvarCargoConvertendoFormEDominioParaDto() {
        CargoForm form = CargoForm.builder().nmCargo("Analista").build();
        Cargo cargoSalvo = Cargo.builder().id(7L).nmCargo("Analista").build();
        when(cargoRepository.salvarCargo(Cargo.builder().nmCargo("Analista").build())).thenReturn(cargoSalvo);

        assertEquals(CargoDto.builder().id(7L).nmCargo("Analista").build(), cargoService.salvarCargo(form));
        verify(cargoRepository).salvarCargo(Cargo.builder().nmCargo("Analista").build());
    }

    @Test
    void deveAlterarCargoComIdInformado() {
        CargoForm form = CargoForm.builder().nmCargo("Coordenador").build();
        Cargo cargoAlterado = Cargo.builder().id(9L).nmCargo("Coordenador").build();
        when(cargoRepository.alterarCargo(Cargo.builder().id(9L).nmCargo("Coordenador").build())).thenReturn(cargoAlterado);

        assertEquals(CargoDto.builder().id(9L).nmCargo("Coordenador").build(), cargoService.alterarCargo(9L, form));
        verify(cargoRepository).alterarCargo(Cargo.builder().id(9L).nmCargo("Coordenador").build());
    }

    @Test
    void deveDelegarExclusaoAoRepositorio() {
        cargoService.deletarCargo(11L);

        verify(cargoRepository).deletarCargo(11L);
    }

    // ---------- autorização ----------

    @Test
    void semAutenticacaoNaoConsultaODado() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("Autenticação necessária"));
        assertThrows(NaoAutenticadoException.class, () -> cargoService.listarCargos());
        verifyNoInteractions(cargoRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cargoService.deletarCargo(1L));
        verifyNoInteractions(cargoRepository);
    }
}
