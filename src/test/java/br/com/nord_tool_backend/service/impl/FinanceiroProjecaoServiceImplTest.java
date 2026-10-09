package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroConfiguracao;
import br.com.nord_tool_backend.domain.FinanceiroFaturaAberta;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroMes;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.domain.FinanceiroRecorrencia;
import br.com.nord_tool_backend.domain.FinanceiroSomaMes;
import br.com.nord_tool_backend.dto.FinanceiroConfiguracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroFechamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroGeracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoLinhaDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.form.FinanceiroConfiguracaoForm;
import br.com.nord_tool_backend.form.FinanceiroRecorrenciaForm;
import br.com.nord_tool_backend.form.FinanceiroSaldoInicialForm;
import br.com.nord_tool_backend.repository.FinanceiroProjecaoRepository;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Entrada;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Fatura;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Linha;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Resultado;
import java.util.HashMap;
import java.util.Map;
import br.com.nord_tool_backend.service.projecao.*;
import org.junit.jupiter.api.Nested;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.mockito.Mockito.verifyNoInteractions;

class FinanceiroProjecaoServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private static final LocalDate OUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate SET = LocalDate.of(2026, 9, 1);
    private static final LocalDate AGO = LocalDate.of(2026, 8, 1);

    private FinanceiroRepository repository;
    private FinanceiroProjecaoRepository projecao;
    private FinanceiroProjecaoServiceImpl service;

    private FinanceiroCategoria salario, fatura, apto, luz, saldo;

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @BeforeEach
    void setUp() {
        repository = mock(FinanceiroRepository.class);
        projecao = mock(FinanceiroProjecaoRepository.class);
        // 08/10/2026 12:00 em São Paulo: o mês atual é outubro
        service = new FinanceiroProjecaoServiceImpl(repository, projecao, Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("UTC")), autorizacao);

        salario = cat(1, "Salário", "ENTRADA", "FIXA_MEDIA", 1);
        saldo = cat(3, "Saldo anterior", "ENTRADA", "SALDO_ANTERIOR", 3);
        fatura = cat(4, "Fatura", "SAIDA", "RITMO_FATURA", 1);
        apto = cat(5, "Apartamento", "SAIDA", "FIXA_VALOR", 2);
        luz = cat(6, "Conta de luz", "SAIDA", "VARIAVEL_MEDIA", 3);
        when(repository.listarCategorias()).thenReturn(Arrays.asList(salario, saldo, fatura, apto, luz));
        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("500.00", 3, 8)));
        when(projecao.listarRecorrencias()).thenReturn(Collections.emptyList());
        when(projecao.primeiraCompetencia()).thenReturn(Optional.empty());
        when(projecao.listarMeses(any(), any())).thenReturn(Collections.emptyList());
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Collections.emptyList());
        when(projecao.faturasDoMes(any(), any())).thenReturn(Collections.emptyList());
        when(projecao.buscarMes(any())).thenReturn(Optional.empty());
    }

    // ---------- montagem dos dados ----------

    private static FinanceiroCategoria cat(long id, String nome, String tipo, String projecao, int ordem) {
        FinanceiroCategoria c = new FinanceiroCategoria();
        c.setId(id);
        c.setNmCategoria(nome);
        c.setCdTipo(tipo);
        c.setCdProjecao(projecao);
        c.setNrOrdem(ordem);
        c.setInAtivo(true);
        return c;
    }

    private static FinanceiroConfiguracao config(String meta, int meses, int diaFatura) {
        FinanceiroConfiguracao c = new FinanceiroConfiguracao();
        c.setVlMetaSaldo(v(meta));
        c.setNrMesesMedia(meses);
        c.setNrDiaConferencia(8);
        c.setNrDiaFechamentoFatura(diaFatura);
        return c;
    }

    private static FinanceiroSomaMes soma(LocalDate mes, FinanceiroCategoria c, String total, int qt) {
        FinanceiroSomaMes s = new FinanceiroSomaMes();
        s.setDtCompetencia(mes);
        s.setIdCategoria(c.getId());
        s.setVlTotal(v(total));
        s.setQtLancamentos(qt);
        return s;
    }

    private static FinanceiroMes mes(LocalDate competencia, boolean fechado, String saldoFinal, String saldoInicial) {
        FinanceiroMes m = new FinanceiroMes();
        m.setDtCompetencia(competencia);
        m.setInFechado(fechado);
        m.setVlSaldoFinal(saldoFinal == null ? null : v(saldoFinal));
        m.setVlSaldoInicial(saldoInicial == null ? null : v(saldoInicial));
        return m;
    }

    private void mesesNoBanco(FinanceiroMes... meses) {
        when(projecao.listarMeses(any(), any())).thenReturn(Arrays.asList(meses));
        for (FinanceiroMes m : meses) when(projecao.buscarMes(m.getDtCompetencia())).thenReturn(Optional.of(m));
    }

    private static FinanceiroRecorrencia recorrencia(long id, FinanceiroCategoria c, String valor, LocalDate inicio, LocalDate fim) {
        FinanceiroRecorrencia r = new FinanceiroRecorrencia();
        r.setId(id);
        r.setIdCategoria(c.getId());
        r.setIdPessoa(1L);
        r.setDsRecorrencia("Parcela");
        r.setVlRecorrencia(v(valor));
        r.setDtInicio(inicio);
        r.setDtFim(fim);
        r.setInAtivo(true);
        return r;
    }

    private static FinanceiroProjecaoLinhaDto linha(List<FinanceiroProjecaoLinhaDto> linhas, long idCategoria) {
        return linhas.stream().filter(l -> l.getIdCategoria() == idCategoria).findFirst().orElseThrow(AssertionError::new);
    }

    private void esperaErro(NordHttpEnum esperado, Runnable acao) {
        NordException ex = assertThrows(NordException.class, acao::run);
        assertEquals(esperado, ex.getStatus());
    }

    /** Agosto e setembro fechados, outubro (atual) com salário e fatura lançados e o resto estimado. */
    private void cenarioDeOutubro() {
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(AGO));
        mesesNoBanco(mes(AGO, true, "1000.00", null), mes(SET, true, "1500.00", null));
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Arrays.asList(
                soma(AGO, fatura, "2400.00", 1), soma(SET, fatura, "2400.00", 1), soma(SET, luz, "300.00", 1),
                soma(OUT, salario, "5000.00", 1), soma(OUT, fatura, "1200.00", 1)));
        FinanceiroFaturaAberta aberta = new FinanceiroFaturaAberta();
        aberta.setIdLancamento(70L);
        aberta.setIdCategoria(fatura.getId());
        aberta.setVlLancamento(v("1200.00"));
        aberta.setDtLeitura(LocalDate.of(2026, 10, 24));
        when(projecao.faturasDoMes(eq(OUT), any())).thenReturn(Collections.singletonList(aberta));
        when(projecao.listarRecorrencias()).thenReturn(Collections.singletonList(recorrencia(9, apto, "1956.42", LocalDate.of(2026, 1, 1), null)));
        when(projecao.contarPrevistos(OUT)).thenReturn(3);
    }

    // ---------- a conta do mês ----------

    @Test
    void mesAtualSomaOLancadoAsEstimativasEPartedoSaldoDoMesAnteriorFechado() {
        cenarioDeOutubro();

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);

        assertEquals("2026-10", mes.getCompetencia());
        assertFalse(mes.isFechado());
        assertTrue(mes.isEstimado());
        assertTrue(mes.isComSaldoAnterior());
        assertEquals(v("1500.00"), mes.getSaldoAnterior());
        assertEquals(v("5000.00"), mes.getTotalEntradas());
        // fatura de outubro (ciclo 09/10 a 08/11, 31 dias; em 24/10 passaram 16): 1200 + (1 - 16/31) * 2400 = 2361,29; apartamento pela recorrência; luz pela média (300)
        assertEquals(v("2361.29"), linha(mes.getSaidas(), 4).getProjetado());
        assertEquals("RITMO", linha(mes.getSaidas(), 4).getOrigem());
        assertEquals(v("1956.42"), linha(mes.getSaidas(), 5).getProjetado());
        assertEquals("RECORRENCIA", linha(mes.getSaidas(), 5).getOrigem());
        assertEquals(v("300.00"), linha(mes.getSaidas(), 6).getProjetado());
        assertEquals(v("4617.71"), mes.getTotalSaidas());
        assertEquals(v("1882.29"), mes.getSaldoFinal());
        assertEquals(v("500.00"), mes.getMetaSaldo());
        assertEquals(v("1382.29"), mes.getFolga());
        assertEquals("VERDE", mes.getSituacao());
        assertEquals(3, mes.getQtPrevistos());
        // o saldo anterior não é uma linha: vem à parte
        assertEquals(1, mes.getEntradas().size());
        assertEquals(3, mes.getSaidas().size());
    }

    @Test
    void saldoFinalAbaixoDaMetaFicaVermelho() {
        cenarioDeOutubro();
        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("3000.00", 3, 8)));

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);

        assertEquals("VERMELHO", mes.getSituacao());
        assertEquals(v("-1117.71"), mes.getFolga());
    }

    @Test
    void mesFechadoMostraOSaldoGravadoESemEstimativas() {
        cenarioDeOutubro();
        mesesNoBanco(mes(AGO, true, "1000.00", null), mes(SET, true, "1500.00", null));
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Arrays.asList(
                soma(AGO, salario, "3000.00", 1), soma(SET, salario, "3000.00", 1), soma(SET, apto, "1000.00", 1)));

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-09", null);

        assertTrue(mes.isFechado());
        assertFalse(mes.isEstimado());
        assertEquals(v("1500.00"), mes.getSaldoFinal());
        assertEquals(v("1000.00"), mes.getSaldoAnterior());
        assertEquals(0, mes.getQtPrevistos());
        assertEquals("real", "REAL".equals(linha(mes.getSaidas(), 5).getOrigem()) ? "real" : "outra");
    }

    @Test
    void mesesPassadosAindaAbertosContamSoOQueFoiLancadoEOSaldoInicialInformado() {
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(AGO));
        mesesNoBanco(mes(AGO, false, null, "100.00"));
        when(projecao.somarPorCategoria(any(), any(), any())).thenReturn(Arrays.asList(
                soma(AGO, salario, "3000.00", 1), soma(AGO, apto, "1000.00", 1),
                soma(SET, salario, "3000.00", 1), soma(SET, luz, "200.00", 1)));

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);

        // agosto: 100 + 3000 - 1000 = 2100; setembro: 2100 + 3000 - 200 = 4900
        assertEquals(v("4900.00"), mes.getSaldoAnterior());
    }

    @Test
    void saldoInicialDoProprioMesPrevaleceSobreACadeia() {
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(SET));
        mesesNoBanco(mes(OUT, false, null, "777.00"));

        assertEquals(v("777.00"), service.obterMes("2026-10", null).getSaldoAnterior());
    }

    @Test
    void semNenhumDadoOSaldoAnteriorEZero() {
        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", null);
        assertEquals(v("0"), mes.getSaldoAnterior());
        assertEquals(v("0"), mes.getSaldoFinal());
        // sem nada, o saldo (zero) fica abaixo da meta de 500
        assertEquals("VERMELHO", mes.getSituacao());
        assertEquals(v("-500.00"), mes.getFolga());

        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("0.00", 3, 8)));
        assertEquals("VERDE", service.obterMes("2026-10", null).getSituacao());
    }

    @Test
    void comFiltroDePessoaNaoHaSaldoAnteriorNemMeta() {
        cenarioDeOutubro();

        FinanceiroProjecaoMesDto mes = service.obterMes("2026-10", 2L);

        assertFalse(mes.isComSaldoAnterior());
        assertNull(mes.getSaldoAnterior());
        assertNull(mes.getMetaSaldo());
        assertNull(mes.getFolga());
        // a recorrência do apartamento é da pessoa 1: para a pessoa 2 o apartamento fica sem valor
        assertEquals(v("2338.71"), mes.getSaldoFinal());
        assertEquals("SEM_DADOS", linha(mes.getSaidas(), 5).getOrigem());
        assertEquals("VERDE", mes.getSituacao());
        verify(projecao, times(1)).somarPorCategoria(any(), any(), eq(2L));

        // para a pessoa 1 a recorrência entra
        assertEquals(v("382.29"), service.obterMes("2026-10", 1L).getSaldoFinal());
    }

    @Test
    void competenciaInvalidaEhRecusada() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.obterMes("10/2026", null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.obterMes(null, null));
    }

    // ---------- fechar e reabrir ----------

    private void prepararFechamento() {
        cenarioDeOutubro();
        when(projecao.fecharMes(any(), any(), any())).thenReturn(1);
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.listarPessoas()).thenReturn(Collections.singletonList(nick));
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(500L));
    }

    @Test
    void fecharGravaOSaldoRealSemEstimativasEGeraOsFixosDoMesSeguinte() {
        prepararFechamento();

        FinanceiroFechamentoDto r = service.fechar("2026-10", 9L);

        // real: saldo anterior 1500 + salário 5000 - fatura 1200 lançada (sem projetar o resto)
        verify(projecao).fecharMes(OUT, v("5300.00"), 9L);
        assertEquals(1, r.getRecorrenciasGeradas());
        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        verify(repository).inserirLancamento(captor.capture());
        FinanceiroLancamento gerado = captor.getValue();
        assertEquals(LocalDate.of(2026, 11, 1), gerado.getDtCompetencia());
        assertEquals(v("1956.42"), gerado.getVlLancamento());
        assertEquals(9L, gerado.getIdRecorrencia());
        assertEquals(9L, gerado.getIdUsuarioCriacao());
        assertEquals(FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(9L, YearMonth.of(2026, 11)), gerado.getCdRequisicao());
        assertFalse(gerado.getInRealizado());
    }

    @Test
    void fecharExigeOMesAnteriorFechadoQuandoHaDadosAntes() {
        cenarioDeOutubro();
        mesesNoBanco(mes(AGO, true, "1000.00", null), mes(SET, false, null, null));

        esperaErro(NordHttpEnum.HTTP_400, () -> service.fechar("2026-10", 9L));
        verify(projecao, never()).fecharMes(any(), any(), any());
    }

    @Test
    void fecharPodeSerOPrimeiroMesOuTerSaldoInicial() {
        when(projecao.fecharMes(any(), any(), any())).thenReturn(1);
        // primeiro mês: nada antes
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(OUT));
        service.fechar("2026-10", 9L);

        // há dados antes, mas o mês tem saldo inicial informado
        when(projecao.primeiraCompetencia()).thenReturn(Optional.of(AGO));
        mesesNoBanco(mes(OUT, false, null, "250.00"));
        service.fechar("2026-10", 9L);

        verify(projecao, times(2)).fecharMes(eq(OUT), any(), eq(9L));
    }

    @Test
    void fecharMesJaFechadoOuQueFechaEmParaleloEhRecusado() {
        mesesNoBanco(mes(OUT, true, "1.00", null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.fechar("2026-10", 9L));

        when(projecao.buscarMes(OUT)).thenReturn(Optional.empty());
        when(projecao.fecharMes(any(), any(), any())).thenReturn(0);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.fechar("2026-10", 9L));
    }

    @Test
    void reabrirSoOUltimoMesFechado() {
        mesesNoBanco(mes(SET, true, "1500.00", null));
        when(projecao.existeMesFechadoApos(SET)).thenReturn(true);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.reabrir("2026-09"));

        when(projecao.existeMesFechadoApos(SET)).thenReturn(false);
        when(projecao.reabrirMes(SET)).thenReturn(1);
        service.reabrir("2026-09");
        verify(projecao).reabrirMes(SET);

        esperaErro(NordHttpEnum.HTTP_400, () -> service.reabrir("2026-08"));
    }

    @Test
    void saldoInicialNaoMudaMesFechado() {
        mesesNoBanco(mes(SET, true, "1500.00", null));
        FinanceiroSaldoInicialForm f = new FinanceiroSaldoInicialForm();
        f.setVlSaldoInicial(v("10.00"));

        esperaErro(NordHttpEnum.HTTP_400, () -> service.definirSaldoInicial("2026-09", f));

        when(projecao.definirSaldoInicial(OUT, v("10.00"))).thenReturn(1);
        service.definirSaldoInicial("2026-10", f);
        verify(projecao).garantirMes(OUT);
        verify(projecao).definirSaldoInicial(OUT, v("10.00"));
    }

    // ---------- recorrências ----------

    @Test
    void geracaoSoCriaOsLancamentosVigentesAtivosENaoContaOsQueJaExistiam() {
        FinanceiroCategoria inativa = cat(8, "Antiga", "SAIDA", "FIXA_VALOR", 8);
        inativa.setInAtivo(false);
        when(repository.listarCategorias()).thenReturn(Arrays.asList(apto, salario, inativa));
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.listarPessoas()).thenReturn(Collections.singletonList(nick));
        FinanceiroRecorrencia vigente = recorrencia(1, apto, "100.00", LocalDate.of(2026, 1, 1), null);
        vigente.setNrDia(31);
        FinanceiroRecorrencia jaExiste = recorrencia(2, salario, "200.00", LocalDate.of(2026, 1, 1), null);
        FinanceiroRecorrencia acabou = recorrencia(3, apto, "300.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30));
        FinanceiroRecorrencia comecaDepois = recorrencia(4, apto, "400.00", LocalDate.of(2026, 12, 1), null);
        FinanceiroRecorrencia categoriaInativa = recorrencia(5, inativa, "500.00", LocalDate.of(2026, 1, 1), null);
        FinanceiroRecorrencia desligada = recorrencia(6, apto, "600.00", LocalDate.of(2026, 1, 1), null);
        desligada.setInAtivo(false);
        FinanceiroRecorrencia pessoaInativa = recorrencia(7, apto, "700.00", LocalDate.of(2026, 1, 1), null);
        pessoaInativa.setIdPessoa(99L);
        when(projecao.listarRecorrencias()).thenReturn(Arrays.asList(vigente, jaExiste, acabou, comecaDepois, categoriaInativa, desligada, pessoaInativa));
        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        when(repository.inserirLancamento(captor.capture())).thenReturn(Optional.of(1L), Optional.empty());

        FinanceiroGeracaoDto r = service.gerarRecorrencias("2026-10", 9L);

        assertEquals("2026-10", r.getCompetencia());
        assertEquals(1, r.getCriados());
        assertEquals(2, captor.getAllValues().size());
        assertEquals(LocalDate.of(2026, 10, 31), captor.getAllValues().get(0).getDtLancamento());
        assertEquals(LocalDate.of(2026, 10, 1), captor.getAllValues().get(1).getDtLancamento());
    }

    @Test
    void geracaoEmMesFechadoEhRecusada() {
        mesesNoBanco(mes(OUT, true, "1.00", null));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.gerarRecorrencias("2026-10", 9L));
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void identificadorDaRecorrenciaEEstavelPorMes() {
        String a = FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(1L, YearMonth.of(2026, 10));
        assertEquals(a, FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(1L, YearMonth.of(2026, 10)));
        assertFalse(a.equals(FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(1L, YearMonth.of(2026, 11))));
        assertFalse(a.equals(FinanceiroProjecaoServiceImpl.requisicaoDaRecorrencia(2L, YearMonth.of(2026, 10))));
    }

    private FinanceiroRecorrenciaForm formRecorrencia() {
        FinanceiroRecorrenciaForm f = new FinanceiroRecorrenciaForm();
        f.setIdCategoria(apto.getId());
        f.setIdPessoa(1L);
        f.setVlRecorrencia(v("1956.42"));
        f.setDtInicio(LocalDate.of(2026, 1, 1));
        return f;
    }

    @Test
    void criaRecorrenciaValidandoCategoriaPessoaEPeriodo() {
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.buscarCategoria(5L)).thenReturn(Optional.of(apto));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(nick));
        when(projecao.inserirRecorrencia(any())).thenReturn(30L);
        when(projecao.buscarRecorrencia(30L)).thenReturn(Optional.of(recorrencia(30, apto, "1956.42", LocalDate.of(2026, 1, 1), null)));

        assertEquals(30L, service.criarRecorrencia(formRecorrencia()).getIdRecorrencia());

        FinanceiroRecorrenciaForm fimAntes = formRecorrencia();
        fimAntes.setDtFim(LocalDate.of(2025, 12, 31));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarRecorrencia(fimAntes));

        when(repository.buscarCategoria(3L)).thenReturn(Optional.of(saldo));
        FinanceiroRecorrenciaForm doSaldo = formRecorrencia();
        doSaldo.setIdCategoria(3L);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarRecorrencia(doSaldo));

        nick.setInAtivo(false);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarRecorrencia(formRecorrencia()));
    }

    @Test
    void atualizaRecorrenciaComControleDeVersao() {
        FinanceiroPessoa nick = new FinanceiroPessoa();
        nick.setId(1L);
        nick.setInAtivo(true);
        when(repository.buscarCategoria(5L)).thenReturn(Optional.of(apto));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(nick));
        when(projecao.buscarRecorrencia(30L)).thenReturn(Optional.of(recorrencia(30, apto, "1956.42", LocalDate.of(2026, 1, 1), null)));
        FinanceiroRecorrenciaForm f = formRecorrencia();

        esperaErro(NordHttpEnum.HTTP_400, () -> service.atualizarRecorrencia(30L, f));

        f.setNrVersao(2);
        when(projecao.alterarRecorrencia(any(), eq(2))).thenReturn(0);
        esperaErro(NordHttpEnum.HTTP_409, () -> service.atualizarRecorrencia(30L, f));

        when(projecao.alterarRecorrencia(any(), eq(2))).thenReturn(1);
        assertEquals(30L, service.atualizarRecorrencia(30L, f).getIdRecorrencia());
    }

    // ---------- configuração ----------

    @Test
    void configuracaoAusenteEhCriadaComOsPadroes() {
        when(projecao.buscarConfiguracao()).thenReturn(Optional.empty(), Optional.of(config("0.00", 3, 8)));

        FinanceiroConfiguracaoDto c = service.obterConfiguracao();

        verify(projecao).garantirConfiguracao();
        assertEquals(3, c.getNrMesesMedia());
        assertEquals(8, c.getNrDiaFechamentoFatura());
    }

    @Test
    void atualizaAConfiguracao() {
        FinanceiroConfiguracaoForm f = new FinanceiroConfiguracaoForm();
        f.setVlMetaSaldo(v("500.00"));
        f.setNrMesesMedia(6);
        f.setNrDiaConferencia(10);
        f.setNrDiaFechamentoFatura(15);
        when(projecao.buscarConfiguracao()).thenReturn(Optional.of(config("500.00", 6, 15)));

        FinanceiroConfiguracaoDto c = service.atualizarConfiguracao(f);

        ArgumentCaptor<FinanceiroConfiguracao> captor = ArgumentCaptor.forClass(FinanceiroConfiguracao.class);
        verify(projecao).alterarConfiguracao(captor.capture());
        assertEquals(v("500.00"), captor.getValue().getVlMetaSaldo());
        assertEquals(6, captor.getValue().getNrMesesMedia());
        assertEquals(15, captor.getValue().getNrDiaFechamentoFatura());
        assertEquals(15, c.getNrDiaFechamentoFatura());
    }

    @Test
    void leiturasDeLancamentoInexistenteVira404() {
        when(repository.buscarLancamento(99L)).thenReturn(Optional.empty());
        esperaErro(NordHttpEnum.HTTP_404, () -> service.listarLeituras(99L));
        verify(projecao, never()).listarLeituras(anyLong());
    }

    @Nested
    class ProjecaoCalculatorTest {

        private final YearMonth OUT = YearMonth.of(2026, 10);
        private final YearMonth SET = YearMonth.of(2026, 9);

        private BigDecimal v(String valor) {
            return new BigDecimal(valor);
        }

        private FinanceiroCategoria cat(long id, String nome, String tipo, String projecao, int ordem, boolean ativa) {
            FinanceiroCategoria c = new FinanceiroCategoria();
            c.setId(id);
            c.setNmCategoria(nome);
            c.setCdTipo(tipo);
            c.setCdProjecao(projecao);
            c.setNrOrdem(ordem);
            c.setInAtivo(ativa);
            return c;
        }

        private final FinanceiroCategoria SALARIO = cat(1, "Salário", "ENTRADA", "FIXA_MEDIA", 1, true);
        private final FinanceiroCategoria SALDO = cat(3, "Saldo anterior", "ENTRADA", "SALDO_ANTERIOR", 3, true);
        private final FinanceiroCategoria FATURA = cat(4, "Fatura", "SAIDA", "RITMO_FATURA", 1, true);
        private final FinanceiroCategoria APTO = cat(5, "Apartamento", "SAIDA", "FIXA_VALOR", 2, true);
        private final FinanceiroCategoria LUZ = cat(6, "Conta de luz", "SAIDA", "VARIAVEL_MEDIA", 3, true);
        private final FinanceiroCategoria JUAN = cat(7, "Juan", "SAIDA", "MANUAL", 4, true);

        /** Monta a entrada: {@code real} e {@code historico} são "id=valor" (mês anterior = índice 0). */
        private class Montador {
            boolean estimar = true;
            int mesesNaMedia = 3;
            int diaFechamento = 8;
            List<FinanceiroCategoria> categorias = new ArrayList<>();
            Map<Long, BigDecimal> real = new HashMap<>();
            Map<Long, Integer> quantidade = new HashMap<>();
            Map<YearMonth, Map<Long, BigDecimal>> historico = new HashMap<>();
            Map<Long, BigDecimal> recorrencias = new HashMap<>();
            List<Fatura> faturas = new ArrayList<>();

            Montador categorias(FinanceiroCategoria... lista) { categorias.addAll(Arrays.asList(lista)); return this; }
            Montador lancado(FinanceiroCategoria c, String valor) { real.put(c.getId(), v(valor)); quantidade.put(c.getId(), 1); return this; }
            Montador mesAnterior(int mesesAtras, FinanceiroCategoria c, String valor) {
                historico.computeIfAbsent(OUT.minusMonths(mesesAtras), k -> new HashMap<>()).put(c.getId(), v(valor));
                return this;
            }
            Montador recorrencia(FinanceiroCategoria c, String valor) { recorrencias.put(c.getId(), v(valor)); return this; }
            Montador fatura(FinanceiroCategoria c, String valor, LocalDate leitura) { faturas.add(new Fatura(c.getId(), v(valor), leitura)); return this; }
            Resultado calcular() {
                return ProjecaoCalculator.calcular(new Entrada(OUT, estimar, categorias, real, quantidade, historico,
                        recorrencias, faturas, mesesNaMedia, diaFechamento));
            }
        }

        private Linha linha(List<Linha> linhas, long idCategoria) {
            return linhas.stream().filter(l -> l.idCategoria == idCategoria).findFirst().orElseThrow(AssertionError::new);
        }

        // ---------- médias e valores lançados ----------

        @Test
        void salarioSemLancamentoUsaAMediaDosUltimosTresMeses() {
            Resultado r = new Montador().categorias(SALARIO)
                    .mesAnterior(1, SALARIO, "3290.15").mesAnterior(2, SALARIO, "3952.06").mesAnterior(3, SALARIO, "3500.00").calcular();

            Linha l = linha(r.entradas, 1);
            assertEquals(v("3580.74"), l.projetado);
            assertEquals(BigDecimal.ZERO, l.real);
            assertEquals(ProjecaoCalculator.ORIGEM_MEDIA, l.origem);
            assertEquals(v("3580.74"), r.totalEntradas);
        }

        @Test
        void valorLancadoNoMesPrevaleceSobreAMedia() {
            Resultado r = new Montador().categorias(SALARIO).lancado(SALARIO, "4000.00")
                    .mesAnterior(1, SALARIO, "3000.00").calcular();

            assertEquals(v("4000.00"), linha(r.entradas, 1).projetado);
            assertEquals(ProjecaoCalculator.ORIGEM_REAL, linha(r.entradas, 1).origem);
        }

        @Test
        void mediaConsideraSoMesesComLancamentoELimitaOsMesesPedidos() {
            Montador m = new Montador().categorias(LUZ).mesAnterior(1, LUZ, "300.00").mesAnterior(3, LUZ, "500.00");
            // o mês -2 não teve lançamento: média de 2 meses com dado
            assertEquals(v("400.00"), linha(m.calcular().saidas, 6).projetado);

            m.mesesNaMedia = 1;
            assertEquals(v("300.00"), linha(m.calcular().saidas, 6).projetado);

            m.mesesNaMedia = 12;
            m.mesAnterior(8, LUZ, "900.00");
            assertEquals(v("566.67"), linha(m.calcular().saidas, 6).projetado);
        }

        @Test
        void semHistoricoNemLancamentoFicaZeroSemDados() {
            Linha l = linha(new Montador().categorias(LUZ).calcular().saidas, 6);
            assertEquals(BigDecimal.ZERO, l.projetado);
            assertEquals(ProjecaoCalculator.ORIGEM_SEM_DADOS, l.origem);
        }

        @Test
        void valorFixoPrefereLancamentoDepoisRecorrenciaDepoisMedia() {
            Montador m = new Montador().categorias(APTO).mesAnterior(1, APTO, "1900.00");
            assertEquals(v("1900.00"), linha(m.calcular().saidas, 5).projetado);
            assertEquals(ProjecaoCalculator.ORIGEM_MEDIA, linha(m.calcular().saidas, 5).origem);

            m.recorrencia(APTO, "1956.42");
            assertEquals(v("1956.42"), linha(m.calcular().saidas, 5).projetado);
            assertEquals(ProjecaoCalculator.ORIGEM_RECORRENCIA, linha(m.calcular().saidas, 5).origem);

            m.lancado(APTO, "2000.00");
            assertEquals(v("2000.00"), linha(m.calcular().saidas, 5).projetado);
            assertEquals(ProjecaoCalculator.ORIGEM_REAL, linha(m.calcular().saidas, 5).origem);
        }

        @Test
        void categoriaManualSoValeOQueFoiLancado() {
            Montador m = new Montador().categorias(JUAN).mesAnterior(1, JUAN, "500.00");
            assertEquals(BigDecimal.ZERO, linha(m.calcular().saidas, 7).projetado);
            m.lancado(JUAN, "500.00");
            assertEquals(v("500.00"), linha(m.calcular().saidas, 7).projetado);
        }

        @Test
        void semEstimarSoValeOLancado() {
            Montador m = new Montador().categorias(SALARIO, LUZ, FATURA).lancado(LUZ, "355.23")
                    .mesAnterior(1, SALARIO, "3000.00").mesAnterior(1, FATURA, "2000.00");
            m.estimar = false;

            Resultado r = m.calcular();

            assertEquals(BigDecimal.ZERO, linha(r.entradas, 1).projetado);
            assertEquals(v("355.23"), linha(r.saidas, 6).projetado);
            assertEquals(BigDecimal.ZERO, linha(r.saidas, 4).projetado);
            assertEquals(v("355.23"), r.totalSaidas);
        }

        // ---------- filtros de categoria, ordem e totais ----------

        @Test
        void saldoAnteriorSaiDasLinhasEInativaSemLancamentoSome() {
            FinanceiroCategoria inativa = cat(9, "Antiga", "SAIDA", "MANUAL", 9, false);
            FinanceiroCategoria inativaComLancamento = cat(10, "Antiga com uso", "SAIDA", "MANUAL", 10, false);
            Resultado r = new Montador().categorias(SALARIO, SALDO, inativa, inativaComLancamento).lancado(inativaComLancamento, "10.00").calcular();

            assertEquals(1, r.entradas.size());
            assertEquals(1, r.saidas.size());
            assertEquals(10L, (long) r.saidas.get(0).idCategoria);
        }

        @Test
        void linhasSeguemAOrdemDasCategoriasETotaisSeparamEntradaESaida() {
            Resultado r = new Montador().categorias(JUAN, LUZ, APTO, FATURA, SALARIO)
                    .lancado(SALARIO, "5000.00").lancado(FATURA, "2380.38").lancado(APTO, "1956.42").lancado(LUZ, "355.23").lancado(JUAN, "500.00")
                    .calcular();

            assertEquals(Arrays.asList(4L, 5L, 6L, 7L), ids(r.saidas));
            assertEquals(v("5000.00"), r.totalEntradas);
            assertEquals(v("5192.03"), r.totalSaidas);
        }

        private List<Long> ids(List<Linha> linhas) {
            List<Long> ids = new ArrayList<>();
            for (Linha l : linhas) ids.add(l.idCategoria);
            return ids;
        }

        // ---------- fatura pelo ritmo ----------

        @Test
        void faturaComMediaSomaOParcialAoQueFaltaDoCiclo() {
            // fechamento dia 8: ciclo da fatura de setembro = 09/09 a 08/10 (30 dias); em 24/09 passaram 16
            BigDecimal r = ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 9, 24), SET, 8, v("2400.00"));
            assertEquals(v("2320.00"), r); // 1200 + (1 - 16/30) * 2400
        }

        @Test
        void faturaSemHistoricoUsaORitmoDoCiclo() {
            BigDecimal r = ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 9, 24), SET, 8, null);
            assertEquals(v("2250.00"), r); // 1200 * 30 / 16
        }

        @Test
        void faturaNuncaFicaAbaixoDoParcial() {
            BigDecimal r = ProjecaoCalculator.projetarFatura(v("3000.00"), LocalDate.of(2026, 9, 24), SET, 8, v("2400.00"));
            assertEquals(v("4120.00"), r);
            assertTrue(r.compareTo(v("3000.00")) >= 0);
        }

        @Test
        void faturaSemLeituraCicloEncerradoOuLeituraForaDoCicloFicaNoParcial() {
            assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), null, SET, 8, v("2400.00")));
            assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 10, 8), SET, 8, v("2400.00")));
            assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 10, 20), SET, 8, v("2400.00")));
            assertEquals(v("1200.00"), ProjecaoCalculator.projetarFatura(v("1200.00"), LocalDate.of(2026, 9, 1), SET, 8, v("2400.00")));
        }

        @Test
        void primeiroDiaDoCicloPesaQuaseTudoNaMedia() {
            BigDecimal r = ProjecaoCalculator.projetarFatura(v("100.00"), LocalDate.of(2026, 9, 9), SET, 8, v("3000.00"));
            assertEquals(v("3000.00"), r); // d = 1 de 30: 100 + 29/30 * 3000 = 3000
        }

        @Test
        void diaDeFechamentoMaiorQueOMesUsaOUltimoDia() {
            // fechamento "dia 31": a fatura de janeiro/2027 começa em 01/02 (31/01 + 1) e fecha em 28/02 (fevereiro não tem dia 31)
            YearMonth jan = YearMonth.of(2027, 1);
            BigDecimal r = ProjecaoCalculator.projetarFatura(v("1000.00"), LocalDate.of(2027, 2, 14), jan, 31, null);
            assertEquals(v("2000.00"), r); // d = 14 de 28
        }

        @Test
        void linhaDeFaturaEmAbertoUsaOParcialMaisOQueFalta() {
            Montador m = new Montador().categorias(FATURA).lancado(FATURA, "1200.00")
                    .mesAnterior(1, FATURA, "2400.00").mesAnterior(2, FATURA, "2400.00").mesAnterior(3, FATURA, "2400.00")
                    .fatura(FATURA, "1200.00", LocalDate.of(2026, 10, 24));

            Linha l = linha(m.calcular().saidas, 4);

            assertEquals(v("2361.29"), l.projetado); // ciclo de outubro: 09/10 a 08/11 (31 dias), em 24/10 passaram 16
            assertEquals(v("1200.00"), l.real);
            assertEquals(ProjecaoCalculator.ORIGEM_RITMO, l.origem);
        }

        @Test
        void faturaSemLancamentoUsaAMediaEFaturaFechadaFicaNoValorLancado() {
            Montador m = new Montador().categorias(FATURA).mesAnterior(1, FATURA, "2000.00").mesAnterior(2, FATURA, "3000.00");
            assertEquals(v("2500.00"), linha(m.calcular().saidas, 4).projetado);
            assertEquals(ProjecaoCalculator.ORIGEM_MEDIA, linha(m.calcular().saidas, 4).origem);

            Montador fechada = new Montador().categorias(FATURA).lancado(FATURA, "2380.38").mesAnterior(1, FATURA, "2400.00")
                    .fatura(FATURA, "2380.38", LocalDate.of(2026, 11, 9)); // depois do fechamento de 08/11
            Linha l = linha(fechada.calcular().saidas, 4);
            assertEquals(v("2380.38"), l.projetado);
            assertEquals(ProjecaoCalculator.ORIGEM_REAL, l.origem);
        }

        @Test
        void faturaLancadaSemNenhumaFaturaAbertaFicaNoValorLancado() {
            Linha l = linha(new Montador().categorias(FATURA).lancado(FATURA, "900.00").mesAnterior(1, FATURA, "2400.00").calcular().saidas, 4);
            assertEquals(v("900.00"), l.projetado);
        }

        @Test
        void duasFaturasNaMesmaCategoriaDividemAMedia() {
            Montador m = new Montador().categorias(FATURA).lancado(FATURA, "1200.00")
                    .mesAnterior(1, FATURA, "2400.00")
                    .fatura(FATURA, "600.00", LocalDate.of(2026, 10, 24)).fatura(FATURA, "600.00", LocalDate.of(2026, 10, 24));

            assertEquals(v("2361.30"), linha(m.calcular().saidas, 4).projetado); // 2 x (600 + 0,4839 x 1200)
        }

        @Test
        void semEstimarAFaturaFicaNoValorLancado() {
            Montador m = new Montador().categorias(FATURA).lancado(FATURA, "1200.00").mesAnterior(1, FATURA, "2400.00")
                    .fatura(FATURA, "1200.00", LocalDate.of(2026, 10, 24));
            m.estimar = false;
            assertEquals(v("1200.00"), linha(m.calcular().saidas, 4).projetado);
            assertEquals(Collections.emptyList(), new Montador().calcular().entradas);
        }
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.obterConfiguracao());
        verifyNoInteractions(repository, projecao);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.reabrir("2026-10"));
        verifyNoInteractions(repository, projecao);
    }
}
