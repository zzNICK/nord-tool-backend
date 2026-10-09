package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.domain.CasamentoAnexo;
import br.com.nord_tool_backend.domain.CasamentoFornecedor;
import br.com.nord_tool_backend.dto.CasamentoAnexoDto;
import br.com.nord_tool_backend.dto.CasamentoFornecedorDto;
import br.com.nord_tool_backend.form.CasamentoFornecedorForm;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoConteudo;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import java.util.Collections;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CasamentoFornecedorServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};

    private CasamentoRepository repository;
    private ArmazenamentoService armazenamento;
    private CasamentoFornecedorServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CasamentoRepository.class);
        armazenamento = mock(ArmazenamentoService.class);
        service = new CasamentoFornecedorServiceImpl(repository, armazenamento, autorizacao);
    }

    private CasamentoFornecedor fornecedor(long id) {
        CasamentoFornecedor f = new CasamentoFornecedor();
        f.setId(id);
        f.setNmFornecedor("Buffet");
        f.setNmCategoria("Alimentação");
        f.setNmStatus("CONTRATADO");
        f.setVlValor(new BigDecimal("1000"));
        f.setQtAnexos(0);
        return f;
    }

    private CasamentoAnexo anexo(long id, long idFornecedor, long idArquivo) {
        CasamentoAnexo a = new CasamentoAnexo();
        a.setId(id);
        a.setIdFornecedor(idFornecedor);
        a.setIdArquivo(idArquivo);
        a.setNmArquivo("contrato.pdf");
        a.setNmContentType("application/pdf");
        a.setNrTamanhoBytes(6L);
        a.setDhCriacao(LocalDateTime.now());
        return a;
    }

    private CasamentoFornecedorForm form(String nome, String categoria, String status, String valor) {
        CasamentoFornecedorForm f = new CasamentoFornecedorForm();
        f.setNmFornecedor(nome);
        f.setNmCategoria(categoria);
        f.setNmStatus(status);
        f.setVlValor(valor == null ? null : new BigDecimal(valor));
        return f;
    }

    @Test
    void criaComStatusPadraoValorZeroETextoAparado() {
        when(repository.inserirFornecedor(any())).thenReturn(7L);
        when(repository.buscarFornecedor(7L)).thenReturn(Optional.of(fornecedor(7L)));
        CasamentoFornecedorForm f = form("  Foto & Vídeo ", " Fotografia ", null, null);
        f.setTxContato("   ");

        CasamentoFornecedorDto dto = service.criar(f);

        ArgumentCaptor<CasamentoFornecedor> captor = ArgumentCaptor.forClass(CasamentoFornecedor.class);
        verify(repository).inserirFornecedor(captor.capture());
        CasamentoFornecedor gravado = captor.getValue();
        assertEquals("Foto & Vídeo", gravado.getNmFornecedor());
        assertEquals("Fotografia", gravado.getNmCategoria());
        assertEquals("PESQUISANDO", gravado.getNmStatus());
        assertEquals(BigDecimal.ZERO, gravado.getVlValor());
        assertNull(gravado.getTxContato());
        assertEquals(7L, dto.getIdFornecedor());
    }

    @Test
    void aceitaStatusPeloRotuloCompativelComOLugia() {
        when(repository.inserirFornecedor(any())).thenReturn(1L);
        when(repository.buscarFornecedor(1L)).thenReturn(Optional.of(fornecedor(1L)));

        service.criar(form("A", "B", "Orçamento", "10"));

        ArgumentCaptor<CasamentoFornecedor> captor = ArgumentCaptor.forClass(CasamentoFornecedor.class);
        verify(repository).inserirFornecedor(captor.capture());
        assertEquals("ORCAMENTO", captor.getValue().getNmStatus());
    }

    @Test
    void recusaStatusInvalido() {
        NordException ex = assertThrows(NordException.class, () -> service.criar(form("A", "B", "TALVEZ", "1")));

        assertEquals(400, ex.getStatus().getStatus().value());
        verify(repository, never()).inserirFornecedor(any());
    }

    @Test
    void alterarFornecedorInexistenteRetorna404() {
        when(repository.buscarFornecedor(9L)).thenReturn(Optional.empty());

        NordException ex = assertThrows(NordException.class, () -> service.alterar(9L, form("A", "B", "CONTRATADO", "1")));

        assertEquals(404, ex.getStatus().getStatus().value());
    }

    @Test
    void alteraMantendoOId() {
        when(repository.buscarFornecedor(3L)).thenReturn(Optional.of(fornecedor(3L)));

        service.alterar(3L, form("Novo", "Cat", "CONTRATADO", "25.50"));

        ArgumentCaptor<CasamentoFornecedor> captor = ArgumentCaptor.forClass(CasamentoFornecedor.class);
        verify(repository).alterarFornecedor(captor.capture());
        assertEquals(3L, captor.getValue().getId());
        assertEquals(new BigDecimal("25.50"), captor.getValue().getVlValor());
    }

    @Test
    void excluirApagaAsLinhasEDepoisTodosOsArquivosDosAnexos() {
        when(repository.buscarFornecedor(3L)).thenReturn(Optional.of(fornecedor(3L)));
        when(repository.listarAnexos(3L)).thenReturn(List.of(anexo(1, 3, 100), anexo(2, 3, 200)));

        service.deletar(3L);

        InOrder ordem = inOrder(repository, armazenamento);
        ordem.verify(repository).deletarFornecedor(3L);
        ordem.verify(armazenamento).apagar(100L);
        ordem.verify(armazenamento).apagar(200L);
    }

    @Test
    void anexaPdfGravandoOArquivoEOVinculo() {
        when(repository.buscarFornecedor(3L)).thenReturn(Optional.of(fornecedor(3L)));
        when(armazenamento.salvar(eq("contrato.pdf"), eq("application/pdf"), any())).thenReturn(55L);
        when(repository.inserirAnexo(3L, 55L, "Contrato assinado")).thenReturn(9L);
        when(repository.buscarAnexo(9L)).thenReturn(Optional.of(anexo(9, 3, 55)));

        CasamentoAnexoDto dto = service.anexar(3L, "contrato.pdf", PDF, "  Contrato assinado ");

        assertEquals(9L, dto.getIdAnexo());
        assertEquals("application/pdf", dto.getNmContentType());
        verify(repository).inserirAnexo(3L, 55L, "Contrato assinado");
    }

    @Test
    void anexaImagemPng() {
        when(repository.buscarFornecedor(3L)).thenReturn(Optional.of(fornecedor(3L)));
        when(armazenamento.salvar(anyString(), eq("image/png"), any())).thenReturn(56L);
        when(repository.inserirAnexo(anyLong(), anyLong(), any())).thenReturn(10L);
        when(repository.buscarAnexo(10L)).thenReturn(Optional.of(anexo(10, 3, 56)));

        assertNotNull(service.anexar(3L, "foto.png", PNG, null));
    }

    @Test
    void recusaArquivoDeOutroTipoDescricaoLongaEFornecedorInexistente() {
        when(repository.buscarFornecedor(3L)).thenReturn(Optional.of(fornecedor(3L)));
        assertThrows(NordException.class, () -> service.anexar(3L, "x.txt", "texto".getBytes(), null));
        assertThrows(NordException.class, () -> service.anexar(3L, "c.pdf", PDF, new String(new char[201]).replace('\0', 'x')));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());

        when(repository.buscarFornecedor(4L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(NordException.class, () -> service.anexar(4L, "c.pdf", PDF, null)).getStatus().getStatus().value());
    }

    @Test
    void excluirAnexoApagaALinhaEOArquivo() {
        when(repository.buscarAnexo(9L)).thenReturn(Optional.of(anexo(9, 3, 55)));

        service.excluirAnexo(9L);

        InOrder ordem = inOrder(repository, armazenamento);
        ordem.verify(repository).deletarAnexo(9L);
        ordem.verify(armazenamento).apagar(55L);
    }

    @Test
    void baixarAnexoDevolveOArquivoComVersao() {
        when(repository.buscarAnexo(9L)).thenReturn(Optional.of(anexo(9, 3, 55)));
        when(armazenamento.abrir(55L)).thenReturn(new ArquivoConteudo("contrato.pdf", "application/pdf", 6, PDF));

        ArquivoDownload download = service.baixarAnexo(9L);

        assertEquals("contrato.pdf", download.getConteudo().getNome());
        assertTrue(download.getVersao() > 0);
    }

    // ---------- cotas e limites ----------

    @Test
    void anexoAcimaDaCotaDoFornecedorEhRecusadoSemGravarArquivo() {
        when(repository.buscarFornecedor(3L)).thenReturn(Optional.of(fornecedor(3L)));
        when(repository.listarAnexos(3L)).thenReturn(Collections.nCopies(
                CasamentoFornecedorServiceImpl.MAX_ANEXOS_POR_FORNECEDOR, new CasamentoAnexo()));
        assertThrows(EntradaInvalidaException.class, () -> service.anexar(3L, "c.pdf", new byte[]{1}, null));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.listar());
        verifyNoInteractions(repository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.deletar(1L));
        verifyNoInteractions(repository);
    }
}
