package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.dto.FerramentaDto;
import br.com.nord_tool_backend.form.FerramentaForm;
import br.com.nord_tool_backend.repository.FerramentaRepository;
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
class FerramentaServiceImplTest {
    @Mock
    private FerramentaRepository ferramentaRepository;

    @Mock


    private AutorizacaoService autorizacao;



    @InjectMocks
    private FerramentaServiceImpl ferramentaService;

    @Test
    void deveSalvarFerramentaConvertendoFormEAplicandoAtivoPorPadrao() {
        FerramentaForm form = FerramentaForm.builder()
                .nmFerramenta("Furadeira")
                .nmCategoria("Elétrica")
                .cdPatrimonio("PAT-1")
                .build();
        FerramentaDto resposta = FerramentaDto.builder().id(7L).nmFerramenta("Furadeira")
                .nmCategoria("Elétrica").cdPatrimonio("PAT-1").flAtivo(true).build();
        when(ferramentaRepository.salvarFerramenta(Ferramenta.builder()
                .nmFerramenta("Furadeira").nmFerramentaCategoria("Elétrica")
                .cdFerramentaPatrimonio("PAT-1").inFerramentaAtivo(true).build())).thenReturn(resposta);

        assertEquals(resposta, ferramentaService.salvarFerramenta(form));
        verify(ferramentaRepository).salvarFerramenta(Ferramenta.builder()
                .nmFerramenta("Furadeira").nmFerramentaCategoria("Elétrica")
                .cdFerramentaPatrimonio("PAT-1").inFerramentaAtivo(true).build());
    }

    @Test
    void deveAlterarFerramentaComIdEAtivoInformados() {
        FerramentaForm form = FerramentaForm.builder().nmFerramenta("Furadeira")
                .nmCategoria("Elétrica").cdPatrimonio("PAT-2").flAtivo(false).build();
        FerramentaDto resposta = FerramentaDto.builder().id(9L).nmFerramenta("Furadeira")
                .nmCategoria("Elétrica").cdPatrimonio("PAT-2").flAtivo(false).build();
        when(ferramentaRepository.alterarFerramenta(Ferramenta.builder().id(9L)
                .nmFerramenta("Furadeira").nmFerramentaCategoria("Elétrica")
                .cdFerramentaPatrimonio("PAT-2").inFerramentaAtivo(false).build())).thenReturn(resposta);

        assertEquals(resposta, ferramentaService.alterarFerramenta(9L, form));
        verify(ferramentaRepository).alterarFerramenta(Ferramenta.builder().id(9L)
                .nmFerramenta("Furadeira").nmFerramentaCategoria("Elétrica")
                .cdFerramentaPatrimonio("PAT-2").inFerramentaAtivo(false).build());
    }

    @Test
    void deveDelegarExclusaoAoRepositorio() {
        ferramentaService.deletarFerramenta(11L);

        verify(ferramentaRepository).deletarFerramenta(11L);
    }

    @Test
    void deveBuscarPorIdEConverterDominioParaDto() {
        when(ferramentaRepository.buscarPorIdFerramenta(5L)).thenReturn(Ferramenta.builder()
                .id(5L).nmFerramenta("Furadeira").nmFerramentaCategoria("Elétrica")
                .cdFerramentaPatrimonio("PAT-5").inFerramentaAtivo(true).build());

        assertEquals(FerramentaDto.builder().id(5L).nmFerramenta("Furadeira")
                        .nmCategoria("Elétrica").cdPatrimonio("PAT-5").flAtivo(true).build(),
                ferramentaService.buscarPorIdFerramenta(5L));
        verify(ferramentaRepository).buscarPorIdFerramenta(5L);
    }

    @Test
    void deveListarFerramentasConvertidasParaDto() {
        Ferramenta ferramenta = Ferramenta.builder().id(3L).nmFerramenta("Furadeira")
                .nmFerramentaCategoria("Elétrica").cdFerramentaPatrimonio("PAT-3")
                .inFerramentaAtivo(true).build();
        when(ferramentaRepository.listarFerramentas()).thenReturn(Collections.singletonList(ferramenta));

        assertEquals(Collections.singletonList(FerramentaDto.builder().id(3L).nmFerramenta("Furadeira")
                        .nmCategoria("Elétrica").cdPatrimonio("PAT-3").flAtivo(true).build()),
                ferramentaService.listarFerramentas());
        verify(ferramentaRepository).listarFerramentas();
    }

    // ---------- autorização ----------

    @Test
    void semAutenticacaoNaoConsultaODado() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("Autenticação necessária"));
        assertThrows(NaoAutenticadoException.class, () -> ferramentaService.listarFerramentas());
        verifyNoInteractions(ferramentaRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> ferramentaService.deletarFerramenta(1L));
        verifyNoInteractions(ferramentaRepository);
    }
}
