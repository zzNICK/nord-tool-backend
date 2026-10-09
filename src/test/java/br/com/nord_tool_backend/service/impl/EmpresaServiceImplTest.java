package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.Empresa;
import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.form.EmpresaForm;
import br.com.nord_tool_backend.repository.EmpresaRepository;
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
class EmpresaServiceImplTest {
    @Mock
    private EmpresaRepository empresaRepository;

    @Mock


    private AutorizacaoService autorizacao;



    @InjectMocks
    private EmpresaServiceImpl empresaService;

    @Test
    void deveListarEmpresasConvertidasParaDto() {
        Empresa empresa = Empresa.builder().id(1L).nmEmpresa("Alfa").build();
        when(empresaRepository.listarEmpresas()).thenReturn(Collections.singletonList(empresa));

        assertEquals(Collections.singletonList(EmpresaDto.builder().id(1L).nmEmpresa("Alfa").build()),
                empresaService.listarEmpresas());
        verify(empresaRepository).listarEmpresas();
    }

    @Test
    void deveSalvarEmpresaConvertendoFormEDominioParaDto() {
        EmpresaForm form = EmpresaForm.builder().nmEmpresa("Alfa").build();
        Empresa empresaSalva = Empresa.builder().id(7L).nmEmpresa("Alfa").build();
        when(empresaRepository.salvarEmpresa(Empresa.builder().nmEmpresa("Alfa").build())).thenReturn(empresaSalva);

        assertEquals(EmpresaDto.builder().id(7L).nmEmpresa("Alfa").build(), empresaService.salvarEmpresa(form));
        verify(empresaRepository).salvarEmpresa(Empresa.builder().nmEmpresa("Alfa").build());
    }

    @Test
    void deveAlterarEmpresaComIdInformado() {
        EmpresaForm form = EmpresaForm.builder().nmEmpresa("Beta").build();
        Empresa empresaAlterada = Empresa.builder().id(9L).nmEmpresa("Beta").build();
        when(empresaRepository.alterarEmpresa(Empresa.builder().id(9L).nmEmpresa("Beta").build())).thenReturn(empresaAlterada);

        assertEquals(EmpresaDto.builder().id(9L).nmEmpresa("Beta").build(), empresaService.alterarEmpresa(9L, form));
        verify(empresaRepository).alterarEmpresa(Empresa.builder().id(9L).nmEmpresa("Beta").build());
    }

    @Test
    void deveDelegarExclusaoAoRepositorio() {
        empresaService.deletarEmpresa(11L);

        verify(empresaRepository).deletarEmpresa(11L);
    }

    // ---------- autorização ----------

    @Test
    void semAutenticacaoNaoConsultaODado() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("Autenticação necessária"));
        assertThrows(NaoAutenticadoException.class, () -> empresaService.listarEmpresas());
        verifyNoInteractions(empresaRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> empresaService.deletarEmpresa(1L));
        verifyNoInteractions(empresaRepository);
    }
}
