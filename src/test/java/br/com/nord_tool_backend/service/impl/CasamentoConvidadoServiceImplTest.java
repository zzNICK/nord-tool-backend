package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.domain.CasamentoConvidado;
import br.com.nord_tool_backend.dto.ImportacaoConvidadosDto;
import br.com.nord_tool_backend.form.CasamentoConvidadoForm;
import br.com.nord_tool_backend.handler.ConvidadosXlsxHandler;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import java.util.List;
import br.com.nord_tool_backend.handler.*;
import org.junit.jupiter.api.Nested;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class CasamentoConvidadoServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private CasamentoRepository repository;
    private CasamentoConvidadoServiceImpl convidados;

    @BeforeEach
    void setUp() {
        repository = mock(CasamentoRepository.class);
        convidados = new CasamentoConvidadoServiceImpl(repository, new ConvidadosXlsxHandler(), autorizacao);
    }

    private CasamentoConvidado convidado(long id) {
        CasamentoConvidado c = new CasamentoConvidado();
        c.setId(id);
        c.setNmConvidado("Ana");
        c.setNmStatus("CONVIDADO");
        c.setNrAcompanhantes(1);
        return c;
    }

    private CasamentoConvidadoForm form(String nome, String status, Integer acompanhantes, String mesa) {
        CasamentoConvidadoForm f = new CasamentoConvidadoForm();
        f.setNmConvidado(nome);
        f.setNmStatus(status);
        f.setNrAcompanhantes(acompanhantes);
        f.setNmMesa(mesa);
        return f;
    }

    @Test
    void criaConvidadoComPadroesMesaEAcompanhantes() {
        when(repository.inserirConvidado(any())).thenReturn(5L);
        when(repository.buscarConvidado(5L)).thenReturn(Optional.of(convidado(5)));

        convidados.criar(form("  Ana Souza ", null, null, " 12 "));

        ArgumentCaptor<CasamentoConvidado> captor = ArgumentCaptor.forClass(CasamentoConvidado.class);
        verify(repository).inserirConvidado(captor.capture());
        assertEquals("Ana Souza", captor.getValue().getNmConvidado());
        assertEquals("NAO_CONVIDADO", captor.getValue().getNmStatus());
        assertEquals(0, captor.getValue().getNrAcompanhantes());
        assertEquals("12", captor.getValue().getNmMesa());
    }

    @Test
    void aceitaStatusPeloRotulo() {
        when(repository.inserirConvidado(any())).thenReturn(5L);
        when(repository.buscarConvidado(5L)).thenReturn(Optional.of(convidado(5)));

        convidados.criar(form("A", "Não irá", 0, null));

        ArgumentCaptor<CasamentoConvidado> captor = ArgumentCaptor.forClass(CasamentoConvidado.class);
        verify(repository).inserirConvidado(captor.capture());
        assertEquals("NAO_IRA", captor.getValue().getNmStatus());
    }

    @Test
    void recusaStatusInvalidoEConvidadoInexistente() {
        assertThrows(NordException.class, () -> convidados.criar(form("A", "TALVEZ", 0, null)));
        when(repository.buscarConvidado(9L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(NordException.class, () -> convidados.deletar(9L)).getStatus().getStatus().value());
        assertEquals(404, assertThrows(NordException.class, () -> convidados.alterar(9L, form("A", null, 0, null))).getStatus().getStatus().value());
    }

    private byte[] xlsx(String[]... linhas) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var aba = wb.createSheet();
            for (int i = 0; i < linhas.length; i++) {
                var row = aba.createRow(i);
                for (int c = 0; c < linhas[i].length; c++) if (linhas[i][c] != null) row.createCell(c).setCellValue(linhas[i][c]);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void importaValidasERelataRejeitadasSemPararAsDemais() throws Exception {
        byte[] bytes = xlsx(new String[]{"Nome", "Status", "Acompanhantes"},
                new String[]{"Ana", "Confirmado", "2"},
                new String[]{null, "Convidado", null},
                new String[]{"Beto", "Talvez", null},
                new String[]{"Carla", "Convidado", "0"});
        when(repository.inserirConvidado(any())).thenReturn(1L);

        ImportacaoConvidadosDto relatorio = convidados.importar("convidados.xlsx", bytes);

        assertEquals(2, relatorio.getImportados());
        assertEquals(2, relatorio.getRejeitados().size());
        assertEquals(3, relatorio.getRejeitados().get(0).getLinha());
        assertEquals("Nome é obrigatório", relatorio.getRejeitados().get(0).getMotivo());
        assertEquals(4, relatorio.getRejeitados().get(1).getLinha());
        verify(repository, times(2)).inserirConvidado(any());
    }

    @Test
    void importacaoRecusaArquivoVazioExtensaoErradaOuGrande() {
        assertThrows(NordException.class, () -> convidados.importar("a.xlsx", new byte[0]));
        assertThrows(NordException.class, () -> convidados.importar("a.csv", new byte[]{1, 2, 3}));
        assertThrows(NordException.class, () -> convidados.importar(null, new byte[]{1, 2, 3}));
        assertThrows(NordException.class, () -> convidados.importar("a.xlsx", new byte[CasamentoConvidadoServiceImpl.MAX_BYTES_PLANILHA + 1]));
        verify(repository, never()).inserirConvidado(any());
    }

    @Nested
    class ConvidadosXlsxHandlerTest {

        private final ConvidadosXlsxHandler handler = new ConvidadosXlsxHandler();

        /** Gera um .xlsx real; cada String[] é uma linha (null = célula vazia). */
        private byte[] planilha(String[]... linhas) throws Exception {
            try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet aba = wb.createSheet("Convidados");
                for (int i = 0; i < linhas.length; i++) {
                    Row row = aba.createRow(i);
                    for (int c = 0; c < linhas[i].length; c++) {
                        if (linhas[i][c] != null) row.createCell(c).setCellValue(linhas[i][c]);
                    }
                }
                wb.write(out);
                return out.toByteArray();
            }
        }

        private final String[] CABECALHO = {"Nome", "Grupo", "Telefone", "Relação", "Status", "Acompanhantes", "Mesa"};

        @Test
        void leLinhasValidasComStatusPorRotuloECodigo() throws Exception {
            byte[] bytes = planilha(CABECALHO,
                    new String[]{"Ana Souza", "Família da noiva", "11 99999-0000", "Prima", "Confirmado", "2", "5"},
                    new String[]{"Bruno", "Trabalho", null, null, "NAO_IRA", null, null},
                    new String[]{"Carla", null, null, null, null, null, null});

            List<ConvidadosXlsxHandler.LinhaLida> linhas = handler.ler(bytes);

            assertEquals(3, linhas.size());
            assertTrue(linhas.stream().allMatch(ConvidadosXlsxHandler.LinhaLida::valida));
            assertEquals("CONFIRMADO", linhas.get(0).getForm().getNmStatus());
            assertEquals(2, linhas.get(0).getForm().getNrAcompanhantes());
            assertEquals("5", linhas.get(0).getForm().getNmMesa());
            assertEquals("NAO_IRA", linhas.get(1).getForm().getNmStatus());
            assertEquals("NAO_CONVIDADO", linhas.get(2).getForm().getNmStatus());
            assertEquals(0, linhas.get(2).getForm().getNrAcompanhantes());
            assertNull(linhas.get(2).getForm().getNmGrupo());
        }

        @Test
        void cabecalhoSemAcentoOuCaixaEOrdemDiferenteFunciona() throws Exception {
            byte[] bytes = planilha(
                    new String[]{"STATUS", "convidado", "RELACAO", "acompanhante"},
                    new String[]{"Convidado", "Dora", "Amiga", "1"});

            ConvidadosXlsxHandler.LinhaLida linha = handler.ler(bytes).get(0);

            assertTrue(linha.valida());
            assertEquals("Dora", linha.getForm().getNmConvidado());
            assertEquals("CONVIDADO", linha.getForm().getNmStatus());
            assertEquals("Amiga", linha.getForm().getNmRelacao());
            assertEquals(1, linha.getForm().getNrAcompanhantes());
        }

        @Test
        void linhasInvalidasViramRejeicoesComONumeroDaLinhaSemPararAsDemais() throws Exception {
            byte[] bytes = planilha(CABECALHO,
                    new String[]{"Ok", null, null, null, "Convidado", null, null},        // linha 2
                    new String[]{null, "Grupo", null, null, null, null, null},            // linha 3: sem nome
                    new String[]{"Status ruim", null, null, null, "Talvez", null, null},  // linha 4
                    new String[]{"Acomp ruim", null, null, null, null, "dois", null},     // linha 5
                    new String[]{"Acomp neg", null, null, null, null, "-1", null},        // linha 6
                    new String[]{"Acomp frac", null, null, null, null, "1.5", null},      // linha 7
                    new String[]{"Segundo ok", null, null, null, null, "3.0", null});     // linha 8

            List<ConvidadosXlsxHandler.LinhaLida> linhas = handler.ler(bytes);

            assertEquals(7, linhas.size());
            assertTrue(linhas.get(0).valida());
            assertEquals(3, linhas.get(1).getLinha());
            assertEquals("Nome é obrigatório", linhas.get(1).getErro());
            assertTrue(linhas.get(2).getErro().startsWith("Status inválido: Talvez"));
            assertTrue(linhas.get(3).getErro().startsWith("Acompanhantes inválido"));
            assertTrue(linhas.get(4).getErro().contains("entre 0 e 50"));
            assertTrue(linhas.get(5).getErro().contains("inteiro"));
            assertTrue(linhas.get(6).valida());
            assertEquals(3, linhas.get(6).getForm().getNrAcompanhantes());
            assertEquals(8, linhas.get(6).getLinha());
        }

        @Test
        void ignoraLinhasTotalmenteVazias() throws Exception {
            byte[] bytes = planilha(CABECALHO, new String[]{}, new String[]{"Zé", null, null, null, null, null, null});

            List<ConvidadosXlsxHandler.LinhaLida> linhas = handler.ler(bytes);

            assertEquals(1, linhas.size());
            assertEquals("Zé", linhas.get(0).getForm().getNmConvidado());
        }

        @Test
        void campoLongoDemaisEhRejeitado() throws Exception {
            String nomeLongo = new String(new char[151]).replace('\0', 'a');
            byte[] bytes = planilha(CABECALHO, new String[]{nomeLongo, null, null, null, null, null, null});

            assertTrue(handler.ler(bytes).get(0).getErro().contains("150"));
        }

        @Test
        void planilhaSemColunaNomeOuQueNaoEhXlsxEhRecusada() throws Exception {
            assertThrows(NordException.class, () -> handler.ler(planilha(new String[]{"Grupo", "Mesa"}, new String[]{"x", "y"})));
            assertThrows(NordException.class, () -> handler.ler("isto não é uma planilha".getBytes()));
        }
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> convidados.listar());
        verifyNoInteractions(repository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> convidados.deletar(1L));
        verifyNoInteractions(repository);
    }
}
