package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.Permissao;
import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.form.PermissaoForm;
import br.com.nord_tool_backend.repository.PermissaoRepository;
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
class PermissaoServiceImplTest {
    @Mock
    private PermissaoRepository permissaoRepository;

    @Mock


    private AutorizacaoService autorizacao;



    @InjectMocks
    private PermissaoServiceImpl permissaoService;

    @Test
    void deveListarPermissoesConvertidasParaDto() {
        Permissao permissao = Permissao.builder().id(3L).nmPermissao("Administrador").build();
        when(permissaoRepository.listarPermissoes()).thenReturn(Collections.singletonList(permissao));

        assertEquals(Collections.singletonList(PermissaoDto.builder().id(3L).nmPermissao("Administrador").build()),
                permissaoService.listarPermissoes());
        verify(permissaoRepository).listarPermissoes();
    }

    @Test
    void deveSalvarPermissaoConvertendoFormEDominioParaDto() {
        PermissaoForm form = PermissaoForm.builder().nmPermissao("Administrador").build();
        Permissao permissaoSalva = Permissao.builder().id(7L).nmPermissao("Administrador").build();
        when(permissaoRepository.salvarPermissao(Permissao.builder().nmPermissao("Administrador").build())).thenReturn(permissaoSalva);

        assertEquals(PermissaoDto.builder().id(7L).nmPermissao("Administrador").build(), permissaoService.salvarPermissao(form));
        verify(permissaoRepository).salvarPermissao(Permissao.builder().nmPermissao("Administrador").build());
    }

    @Test
    void deveAlterarPermissaoComIdInformado() {
        PermissaoForm form = PermissaoForm.builder().nmPermissao("Operador").build();
        Permissao permissaoAlterada = Permissao.builder().id(9L).nmPermissao("Operador").build();
        when(permissaoRepository.alterarPermissao(Permissao.builder().id(9L).nmPermissao("Operador").build())).thenReturn(permissaoAlterada);

        assertEquals(PermissaoDto.builder().id(9L).nmPermissao("Operador").build(), permissaoService.alterarPermissao(9L, form));
        verify(permissaoRepository).alterarPermissao(Permissao.builder().id(9L).nmPermissao("Operador").build());
    }

    @Test
    void deveDelegarExclusaoAoRepositorio() {
        permissaoService.deletarPermissao(11L);

        verify(permissaoRepository).deletarPermissao(11L);
    }

    // ---------- autorização ----------

    @Test
    void semAutenticacaoNaoConsultaODado() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("Autenticação necessária"));
        assertThrows(NaoAutenticadoException.class, () -> permissaoService.listarPermissoes());
        verifyNoInteractions(permissaoRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> permissaoService.deletarPermissao(1L));
        verifyNoInteractions(permissaoRepository);
    }
}
