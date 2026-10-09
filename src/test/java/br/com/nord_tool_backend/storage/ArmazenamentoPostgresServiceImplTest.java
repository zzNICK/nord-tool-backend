package br.com.nord_tool_backend.storage;

import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.domain.ArquivoArmazenado;
import br.com.nord_tool_backend.repository.ArquivoArmazenadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArmazenamentoPostgresServiceImplTest {

    private ArquivoArmazenadoRepository repository;
    private ArmazenamentoPostgresServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ArquivoArmazenadoRepository.class);
        service = new ArmazenamentoPostgresServiceImpl(repository);
    }

    @Test
    void salvaBytesComHashSha256ETamanhoCorretos() {
        when(repository.inserir(any())).thenAnswer(inv -> {
            ArquivoArmazenado a = inv.getArgument(0);
            a.setId(42L);
            return a;
        });
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);

        Long id = service.salvar("termo.pdf", "application/pdf", bytes);

        assertEquals(42L, id);
        ArgumentCaptor<ArquivoArmazenado> captor = ArgumentCaptor.forClass(ArquivoArmazenado.class);
        verify(repository).inserir(captor.capture());
        ArquivoArmazenado salvo = captor.getValue();
        assertEquals("POSTGRES", salvo.getNmProvedor());
        assertEquals("termo.pdf", salvo.getNmArquivo());
        assertEquals("application/pdf", salvo.getNmContentType());
        assertEquals(3L, salvo.getNrTamanhoBytes());
        // SHA-256 conhecido de "abc"
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", salvo.getNmHashSha256());
        assertEquals(64, salvo.getNmHashSha256().length());
        assertArrayEquals(bytes, salvo.getBinConteudo());
    }

    @Test
    void naoSalvaArquivoVazio() {
        assertThrows(NordException.class, () -> service.salvar("a", "x", new byte[0]));
        assertThrows(NordException.class, () -> service.salvar("a", "x", null));
        verifyNoInteractions(repository);
    }

    @Test
    void abreDevolvendoOsMesmosBytes() {
        ArquivoArmazenado a = new ArquivoArmazenado();
        a.setId(1L);
        a.setNmArquivo("foto.jpg");
        a.setNmContentType("image/jpeg");
        a.setNrTamanhoBytes(4L);
        a.setBinConteudo(new byte[]{1, 2, 3, 4});
        when(repository.buscarComConteudo(1L)).thenReturn(Optional.of(a));

        ArquivoConteudo conteudo = service.abrir(1L);

        assertEquals("foto.jpg", conteudo.getNome());
        assertEquals("image/jpeg", conteudo.getContentType());
        assertEquals(4L, conteudo.getTamanho());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, conteudo.getBytes());
    }

    @Test
    void abrirArquivoInexistenteRetorna404() {
        when(repository.buscarComConteudo(9L)).thenReturn(Optional.empty());

        NordException ex = assertThrows(NordException.class, () -> service.abrir(9L));

        assertEquals(404, ex.getStatus().getStatus().value());
    }

    @Test
    void apagaPeloId() {
        service.apagar(5L);

        verify(repository).deletar(5L);
    }

    @Test
    void apagarIdNuloNaoFazNada() {
        service.apagar(null);

        verifyNoInteractions(repository);
    }
}
