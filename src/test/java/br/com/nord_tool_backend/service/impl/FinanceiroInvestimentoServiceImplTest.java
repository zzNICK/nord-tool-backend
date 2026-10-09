package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.FinanceiroAtivo;
import br.com.nord_tool_backend.domain.FinanceiroOperacao;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.domain.FinanceiroProvento;
import br.com.nord_tool_backend.dto.FinanceiroAtivoDto;
import br.com.nord_tool_backend.dto.FinanceiroInvestimentoDto;
import br.com.nord_tool_backend.form.FinanceiroAtivoForm;
import br.com.nord_tool_backend.form.FinanceiroOperacaoForm;
import br.com.nord_tool_backend.form.FinanceiroProventoForm;
import br.com.nord_tool_backend.repository.FinanceiroInvestimentoRepository;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import br.com.nord_tool_backend.service.investimento.Cotacao;
import br.com.nord_tool_backend.service.CotacaoService;
import br.com.nord_tool_backend.service.investimento.ProventoCotado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import br.com.nord_tool_backend.service.investimento.*;
import org.junit.jupiter.api.Nested;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.mockito.Mockito.verifyNoInteractions;

class FinanceiroInvestimentoServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private static final String UUID1 = "11111111-1111-1111-1111-111111111111";

    private FinanceiroInvestimentoRepository repository;
    private FinanceiroRepository financeiroRepository;
    private CotacaoService cotacaoService;
    private FinanceiroInvestimentoServiceImpl service;
    private FinanceiroAtivo hglg;
    private final List<FinanceiroOperacao> operacoes = new ArrayList<>();
    private final List<FinanceiroProvento> proventos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = mock(FinanceiroInvestimentoRepository.class);
        financeiroRepository = mock(FinanceiroRepository.class);
        cotacaoService = mock(CotacaoService.class);
        Clock relogio = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new FinanceiroInvestimentoServiceImpl(repository, financeiroRepository, cotacaoService, relogio, autorizacao);

        hglg = new FinanceiroAtivo();
        hglg.setId(1L);
        hglg.setCdTicker("HGLG11");
        hglg.setIdPessoa(1L);
        hglg.setNmPessoa("Nick");
        hglg.setInAtivo(true);
        hglg.setNrVersao(1);
        when(repository.listarAtivos(any())).thenReturn(Collections.singletonList(hglg));
        when(repository.buscarAtivo(1L)).thenReturn(Optional.of(hglg));
        when(repository.listarOperacoes(anyCollection())).thenAnswer(i -> new ArrayList<>(operacoes));
        when(repository.listarProventos(anyCollection())).thenAnswer(i -> new ArrayList<>(proventos));
        when(cotacaoService.cotar(anySet())).thenReturn(Collections.emptyMap());
    }

    private static FinanceiroOperacao op(long id, String data, String tipo, int cotas, String preco) {
        FinanceiroOperacao o = new FinanceiroOperacao();
        o.setId(id);
        o.setIdAtivo(1L);
        o.setDtOperacao(LocalDate.parse(data));
        o.setCdTipo(tipo);
        o.setQtCotas(cotas);
        o.setVlPreco(new BigDecimal(preco));
        return o;
    }

    private static FinanceiroProvento prov(long id, String com, String pagamento, String valor) {
        FinanceiroProvento p = new FinanceiroProvento();
        p.setId(id);
        p.setIdAtivo(1L);
        p.setDtCom(LocalDate.parse(com));
        p.setDtPagamento(LocalDate.parse(pagamento));
        p.setVlPorCota(new BigDecimal(valor));
        p.setCdOrigem("MANUAL");
        return p;
    }

    private static FinanceiroOperacaoForm form(String tipo, int cotas, String preco) {
        FinanceiroOperacaoForm f = new FinanceiroOperacaoForm();
        f.setCdRequisicao(UUID1);
        f.setDtOperacao("2026-10-01");
        f.setCdTipo(tipo);
        f.setQtCotas(cotas);
        f.setVlPreco(new BigDecimal(preco));
        return f;
    }

    private static void assertInvalido(Runnable acao, String trecho) {
        NordException ex = assertThrows(NordException.class, acao::run);
        assertEquals(NordHttpEnum.HTTP_400, ex.getStatus());
        assertTrue(ex.getMessage().contains(trecho), ex.getMessage());
    }

    @Test
    void listaComCotacaoAoVivoResultadoEProventosAReceber() {
        operacoes.add(op(1, "2026-01-10", "COMPRA", 10, "150.00"));
        proventos.add(prov(1, "2026-09-30", "2026-10-15", "1.10"));
        proventos.add(prov(2, "2026-08-31", "2026-09-15", "1.00"));
        proventos.add(prov(3, "2026-10-31", "2026-11-14", "1.20"));
        Map<String, Cotacao> cotacoes = new HashMap<>();
        cotacoes.put("HGLG11", new Cotacao(new BigDecimal("160.00"), Instant.parse("2026-10-08T14:30:00Z")));
        when(cotacaoService.cotar(anySet())).thenReturn(cotacoes);

        FinanceiroInvestimentoDto r = service.listar(null);

        FinanceiroAtivoDto a = r.getAtivos().get(0);
        assertEquals(10, a.getQtCotas());
        assertEquals(new BigDecimal("150.00"), a.getVlPrecoMedio());
        assertEquals(new BigDecimal("1600.00"), a.getVlPatrimonio());
        assertEquals(new BigDecimal("100.00"), a.getVlResultado());
        assertEquals(new BigDecimal("6.67"), a.getPcResultado());
        assertTrue(a.isCotacaoAoVivo());
        assertEquals("08/10/2026 11:30", a.getDhCotacao());
        assertEquals(new BigDecimal("23.00"), a.getVlAReceber());
        assertEquals(new BigDecimal("23.00"), r.getVlAReceber());
        assertEquals(new BigDecimal("11.00"), r.getVlAReceberMes());
        assertTrue(a.getProventos().stream().filter(p -> p.getIdProvento() == 2L).findFirst().get().isRecebido());
        assertTrue(r.isCotacaoAoVivo());
        verify(repository).atualizarCotacao(eq(1L), eq(new BigDecimal("160.00")), any());
    }

    @Test
    void semCotacaoDoProvedorValeAUltimaGuardada() {
        operacoes.add(op(1, "2026-01-10", "COMPRA", 10, "150.00"));
        hglg.setVlCotacao(new BigDecimal("155.00"));
        FinanceiroInvestimentoDto r = service.listar(1L);
        assertFalse(r.isCotacaoAoVivo());
        assertEquals(new BigDecimal("1550.00"), r.getAtivos().get(0).getVlPatrimonio());
        verify(repository, never()).atualizarCotacao(anyLong(), any(), any());
    }

    @Test
    void semCotacaoNenhumaOPatrimonioEOCusto() {
        operacoes.add(op(1, "2026-01-10", "COMPRA", 10, "150.00"));
        FinanceiroAtivoDto a = service.listar(null).getAtivos().get(0);
        assertEquals(new BigDecimal("1500.00"), a.getVlPatrimonio());
        assertEquals(new BigDecimal("0.00"), a.getVlResultado());
    }

    @Test
    void fundoSemOperacoesNaoConsultaOProvedor() {
        service.listar(null);
        verify(cotacaoService, never()).cotar(anySet());
    }

    @Test
    void criaFundoNormalizandoOTicker() {
        when(financeiroRepository.buscarPessoa(1L)).thenReturn(Optional.of(new FinanceiroPessoa()));
        when(repository.buscarAtivoPorTicker("HGLG11", 1L)).thenReturn(Optional.empty());
        when(repository.inserirAtivo(any())).thenReturn(1L);
        FinanceiroAtivoForm f = new FinanceiroAtivoForm();
        f.setCdTicker(" hglg11 ");
        f.setIdPessoa(1L);
        assertEquals("HGLG11", service.criarAtivo(f).getCdTicker());
    }

    @Test
    void recusaTickerInvalidoPessoaInexistenteEDuplicado() {
        FinanceiroAtivoForm f = new FinanceiroAtivoForm();
        f.setCdTicker("PETR4X9");
        f.setIdPessoa(1L);
        assertInvalido(() -> service.criarAtivo(f), "código do fundo");
        f.setCdTicker("HGLG11");
        when(financeiroRepository.buscarPessoa(1L)).thenReturn(Optional.empty());
        assertInvalido(() -> service.criarAtivo(f), "de quem");
        when(financeiroRepository.buscarPessoa(1L)).thenReturn(Optional.of(new FinanceiroPessoa()));
        when(repository.buscarAtivoPorTicker("HGLG11", 1L)).thenReturn(Optional.of(hglg));
        assertInvalido(() -> service.criarAtivo(f), "já está");
    }

    @Test
    void fundoArquivadoReativaEmVezDeDuplicar() {
        hglg.setInAtivo(false);
        when(financeiroRepository.buscarPessoa(1L)).thenReturn(Optional.of(new FinanceiroPessoa()));
        when(repository.buscarAtivoPorTicker("HGLG11", 1L)).thenReturn(Optional.of(hglg));
        FinanceiroAtivoForm f = new FinanceiroAtivoForm();
        f.setCdTicker("HGLG11");
        f.setIdPessoa(1L);
        service.criarAtivo(f);
        verify(repository, never()).inserirAtivo(any());
        verify(repository).alterarAtivo(any(), eq(1));
    }

    @Test
    void atualizarComVersaoDivergenteDa409() {
        when(repository.alterarAtivo(any(), anyInt())).thenReturn(0);
        FinanceiroAtivoForm f = new FinanceiroAtivoForm();
        f.setNmAtivo("Logística");
        f.setNrVersao(1);
        NordException ex = assertThrows(NordException.class, () -> service.atualizarAtivo(1L, f));
        assertEquals(NordHttpEnum.HTTP_409, ex.getStatus());
    }

    @Test
    void registraCompraEVendaValida() {
        service.registrarOperacao(1L, form("compra", 10, "150.00"));
        verify(repository).inserirOperacao(any());
        operacoes.add(op(1, "2026-01-10", "COMPRA", 10, "150.00"));
        service.registrarOperacao(1L, form("VENDA", 4, "160.00"));
    }

    @Test
    void recusaVendaSemCotasEDadosInvalidos() {
        assertInvalido(() -> service.registrarOperacao(1L, form("VENDA", 1, "10.00")), "cotas suficientes");
        assertInvalido(() -> service.registrarOperacao(1L, form("TROCA", 1, "10.00")), "compra ou venda");
        assertInvalido(() -> service.registrarOperacao(1L, form("COMPRA", 0, "10.00")), "quantidade");
        assertInvalido(() -> service.registrarOperacao(1L, form("COMPRA", 1, "0")), "preço");
        FinanceiroOperacaoForm ruim = form("COMPRA", 1, "10.00");
        ruim.setCdRequisicao("abc");
        assertInvalido(() -> service.registrarOperacao(1L, ruim), "Identificador");
        FinanceiroOperacaoForm semData = form("COMPRA", 1, "10.00");
        semData.setDtOperacao("31/12/2026");
        assertInvalido(() -> service.registrarOperacao(1L, semData), "Data");
        verify(repository, never()).inserirOperacao(any());
    }

    @Test
    void requisicaoRepetidaNaoDuplica() {
        when(repository.buscarAtivoDaRequisicao(UUID1)).thenReturn(Optional.of(1L));
        service.registrarOperacao(1L, form("COMPRA", 10, "150.00"));
        verify(repository, never()).inserirOperacao(any());
    }

    @Test
    void naoExcluiCompraQueSustentaUmaVenda() {
        operacoes.addAll(Arrays.asList(op(1, "2026-01-10", "COMPRA", 10, "150.00"), op(2, "2026-02-10", "VENDA", 5, "160.00")));
        when(repository.buscarOperacao(1L)).thenReturn(Optional.of(operacoes.get(0)));
        when(repository.buscarOperacao(2L)).thenReturn(Optional.of(operacoes.get(1)));
        assertInvalido(() -> service.excluirOperacao(1L), "sem cotas");
        service.excluirOperacao(2L);
        verify(repository).excluirOperacao(2L);
    }

    @Test
    void registraProventoEValidaDatasEValor() {
        FinanceiroProventoForm f = new FinanceiroProventoForm();
        f.setDtCom("2026-09-30");
        f.setDtPagamento("2026-10-15");
        f.setVlPorCota(new BigDecimal("1.10"));
        service.registrarProvento(1L, f);
        verify(repository).gravarProventoManual(any());
        f.setDtPagamento("2026-09-01");
        assertInvalido(() -> service.registrarProvento(1L, f), "antes da data-com");
        f.setDtPagamento("2026-10-15");
        f.setVlPorCota(BigDecimal.ZERO);
        assertInvalido(() -> service.registrarProvento(1L, f), "maior que zero");
    }

    @Test
    void sincronizaSoOsProventosDeQuemTinhaCotas() {
        operacoes.add(op(1, "2026-09-01", "COMPRA", 10, "150.00"));
        when(cotacaoService.proventos("HGLG11")).thenReturn(Arrays.asList(
                new ProventoCotado(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-15"), new BigDecimal("1.10")),
                new ProventoCotado(LocalDate.parse("2026-07-31"), LocalDate.parse("2026-08-14"), new BigDecimal("1.00")),
                new ProventoCotado(LocalDate.parse("2026-08-20"), LocalDate.parse("2026-09-10"), new BigDecimal("1.05"))));
        when(repository.gravarProventoImportado(any())).thenReturn(true);
        // o 2º é anterior à 1ª compra; o 3º tem pagamento depois da compra mas data-com antes dela (sem cotas)
        assertEquals(1, service.sincronizarProventos(1L));
    }

    @Test
    void sincronizarExigeUmaCompra() {
        assertInvalido(() -> service.sincronizarProventos(1L), "compra");
    }

    @Test
    void fundoInexistenteDa404() {
        when(repository.buscarAtivo(9L)).thenReturn(Optional.empty());
        NordException ex = assertThrows(NordException.class, () -> service.registrarOperacao(9L, form("COMPRA", 1, "1.00")));
        assertEquals(NordHttpEnum.HTTP_404, ex.getStatus());
    }

    @Nested
    class PosicaoCalculatorTest {

        private FinanceiroOperacao op(long id, String data, String tipo, int cotas, String preco) {
            FinanceiroOperacao o = new FinanceiroOperacao();
            o.setId(id);
            o.setDtOperacao(LocalDate.parse(data));
            o.setCdTipo(tipo);
            o.setQtCotas(cotas);
            o.setVlPreco(new BigDecimal(preco));
            return o;
        }

        @Test
        void precoMedioDasCompras() {
            PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Arrays.asList(
                    op(1, "2026-01-10", "COMPRA", 10, "100.00"), op(2, "2026-02-10", "COMPRA", 10, "120.00")));
            assertEquals(20, p.getCotas());
            assertEquals(new BigDecimal("2200.00"), p.getCusto());
            assertEquals(new BigDecimal("110.00"), p.getPrecoMedio());
        }

        @Test
        void vendaMantemOPrecoMedio() {
            PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Arrays.asList(
                    op(1, "2026-01-10", "COMPRA", 10, "100.00"), op(2, "2026-02-10", "COMPRA", 10, "120.00"),
                    op(3, "2026-03-10", "VENDA", 5, "130.00")));
            assertEquals(15, p.getCotas());
            assertEquals(new BigDecimal("110.00"), p.getPrecoMedio());
            assertEquals(new BigDecimal("1650.00"), p.getCusto());
        }

        @Test
        void zeraAoVenderTudo() {
            PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Arrays.asList(
                    op(1, "2026-01-10", "COMPRA", 3, "100.00"), op(2, "2026-01-11", "VENDA", 3, "90.00")));
            assertEquals(0, p.getCotas());
            assertEquals(BigDecimal.ZERO, p.getPrecoMedio());
        }

        @Test
        void vendaAcimaDasCotasInvalida() {
            assertNull(PosicaoCalculator.calcular(Arrays.asList(op(1, "2026-01-10", "COMPRA", 3, "100.00"), op(2, "2026-01-11", "VENDA", 4, "90.00"))));
            // a venda vem antes da compra na linha do tempo
            assertNull(PosicaoCalculator.calcular(Arrays.asList(op(1, "2026-02-10", "COMPRA", 5, "100.00"), op(2, "2026-01-11", "VENDA", 1, "90.00"))));
        }

        @Test
        void mesmoDiaCompraAntesDaVenda() {
            assertNotNull(PosicaoCalculator.calcular(Arrays.asList(op(2, "2026-01-10", "VENDA", 2, "90.00"), op(1, "2026-01-10", "COMPRA", 2, "100.00"))));
        }

        @Test
        void semOperacoes() {
            PosicaoCalculator.Posicao p = PosicaoCalculator.calcular(Collections.emptyList());
            assertEquals(0, p.getCotas());
            assertEquals(0, PosicaoCalculator.cotasEm(Collections.emptyList(), LocalDate.parse("2026-01-01")));
        }

        @Test
        void cotasNaDataComContamOperacoesAteOFimDoDia() {
            java.util.List<FinanceiroOperacao> ops = Arrays.asList(
                    op(1, "2026-01-10", "COMPRA", 10, "100.00"), op(2, "2026-02-10", "COMPRA", 5, "100.00"), op(3, "2026-02-20", "VENDA", 8, "100.00"));
            assertEquals(0, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-01-09")));
            assertEquals(10, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-01-10")));
            assertEquals(15, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-02-10")));
            assertEquals(7, PosicaoCalculator.cotasEm(ops, LocalDate.parse("2026-02-28")));
        }
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.listar(null));
        verifyNoInteractions(repository, financeiroRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.excluirOperacao(1L));
        verifyNoInteractions(repository, financeiroRepository);
    }
}
