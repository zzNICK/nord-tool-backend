package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapServiceImplTest {

    private UsuarioRepository repository;
    private BCryptPasswordEncoder encoder;
    private AdminBootstrapServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        service = new AdminBootstrapServiceImpl(repository, encoder);
    }

    @Test
    void criaAdminApenasComTabelaVazia() {
        when(repository.contar()).thenReturn(0L);
        when(repository.buscarIdPerfil("ADMIN")).thenReturn(Optional.of(9L));

        assertTrue(service.executar(" admin@nord.com ", " Admin ", "senha-forte-123"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).inserir(captor.capture());
        assertEquals("admin@nord.com", captor.getValue().getNmEmail());
        assertEquals(9L, captor.getValue().getIdPerfil());
        assertTrue(encoder.matches("senha-forte-123", captor.getValue().getNmSenhaHash()));
    }

    @Test
    void naoCriaSeJaExistemUsuarios() {
        when(repository.contar()).thenReturn(1L);
        assertFalse(service.executar("admin@nord.com", "Admin", "senha-forte-123"));
        verify(repository, never()).inserir(any());
    }

    @Test
    void semVariaveisNaoFazNada() {
        assertFalse(service.executar("", " ", null));
        verifyNoInteractions(repository);
    }

    @Test
    void configuracaoIncompletaOuInvalidaDerrubaOStartup() {
        assertThrows(IllegalStateException.class, () -> service.executar("admin@nord.com", "", "senha-forte-123"));
        assertThrows(IllegalStateException.class, () -> service.executar("nao-e-email", "Admin", "senha-forte-123"));
        assertThrows(IllegalStateException.class, () -> service.executar("admin@nord.com", "Admin", "curta"));
        assertThrows(IllegalStateException.class, () -> service.executar("admin@nord.com", "Admin", "ç".repeat(37)));
        verifyNoInteractions(repository);
    }

    @Test
    void perfilAdminAusenteDerrubaOStartup() {
        when(repository.contar()).thenReturn(0L);
        when(repository.buscarIdPerfil("ADMIN")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.executar("admin@nord.com", "Admin", "senha-forte-123"));
        verify(repository, never()).inserir(any());
    }
}
