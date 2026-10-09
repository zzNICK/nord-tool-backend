package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.dto.FinanceiroLancamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroListaDto;
import br.com.nord_tool_backend.dto.FinanceiroResumoDto;
import br.com.nord_tool_backend.form.FinanceiroCategoriaForm;
import br.com.nord_tool_backend.form.FinanceiroLancamentoForm;
import br.com.nord_tool_backend.form.FinanceiroPessoaForm;
import br.com.nord_tool_backend.form.FinanceiroRealizadoForm;
import br.com.nord_tool_backend.domain.FinanceiroMes;
import br.com.nord_tool_backend.repository.FinanceiroProjecaoRepository;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.mockito.Mockito.verifyNoInteractions;

class FinanceiroServiceImplTest {

    private final AutorizacaoService autorizacao = org.mockito.Mockito.mock(AutorizacaoService.class);

    private static final String UUID1 = "11111111-1111-1111-1111-111111111111";

    private FinanceiroRepository repository;
    private FinanceiroProjecaoRepository projecaoRepository;
    private FinanceiroServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(FinanceiroRepository.class);
        projecaoRepository = mock(FinanceiroProjecaoRepository.class);
        // 08/10/2026 12:00 em São Paulo
        Clock relogio = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("UTC"));
        service = new FinanceiroServiceImpl(repository, projecaoRepository, relogio, autorizacao);
    }

    private void fechado(LocalDate competencia) {
        FinanceiroMes mes = new FinanceiroMes();
        mes.setDtCompetencia(competencia);
        mes.setInFechado(true);
        when(projecaoRepository.buscarMes(competencia)).thenReturn(Optional.of(mes));
    }

    private FinanceiroPessoa pessoa(long id, String nome, boolean ativa) {
        FinanceiroPessoa p = new FinanceiroPessoa();
        p.setId(id);
        p.setNmPessoa(nome);
        p.setInCompartilhado(false);
        p.setNrOrdem(1);
        p.setInAtivo(ativa);
        return p;
    }

    private FinanceiroCategoria categoria(long id, String nome, String tipo, String projecao, boolean ativa) {
        FinanceiroCategoria c = new FinanceiroCategoria();
        c.setId(id);
        c.setNmCategoria(nome);
        c.setCdTipo(tipo);
        c.setCdProjecao(projecao);
        c.setInFixa(false);
        c.setNrOrdem(1);
        c.setInAtivo(ativa);
        return c;
    }

    private FinanceiroLancamento lancamento(long id, int versao) {
        FinanceiroLancamento l = new FinanceiroLancamento();
        l.setId(id);
        l.setDtCompetencia(LocalDate.of(2026, 10, 1));
        l.setDtLancamento(LocalDate.of(2026, 10, 8));
        l.setIdCategoria(1L);
        l.setNmCategoria("Fatura");
        l.setCdTipo("SAIDA");
        l.setIdPessoa(1L);
        l.setNmPessoa("Nick");
        l.setVlLancamento(new BigDecimal("1500.00"));
        l.setInRealizado(false);
        l.setNrVersao(versao);
        return l;
    }

    private FinanceiroLancamentoForm form(String requisicao) {
        FinanceiroLancamentoForm f = new FinanceiroLancamentoForm();
        f.setCdRequisicao(requisicao);
        f.setDtLancamento(LocalDate.of(2026, 10, 8));
        f.setIdCategoria(1L);
        f.setIdPessoa(1L);
        f.setDsLancamento("  Fatura do cartão ");
        f.setVlLancamento(new BigDecimal("1500.00"));
        return f;
    }

    private void cadastrosAtivos() {
        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(categoria(1, "Fatura", "SAIDA", "RITMO_FATURA", true)));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(pessoa(1, "Nick", true)));
    }

    private void esperaErro(NordHttpEnum esperado, Runnable acao) {
        NordException ex = assertThrows(NordException.class, acao::run);
        assertEquals(esperado, ex.getStatus());
    }

    // ---------- criação ----------

    @Test
    void criaLancamentoComAutorCompetenciaEDescricaoLimpa() {
        cadastrosAtivos();
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.empty());
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(5L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));

        List<FinanceiroLancamentoDto> criados = service.criar(form(UUID1), 9L);

        assertEquals(1, criados.size());
        assertEquals(5L, criados.get(0).getIdLancamento());
        assertEquals("2026-10", criados.get(0).getCompetencia());
        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        verify(repository).inserirLancamento(captor.capture());
        FinanceiroLancamento gravado = captor.getValue();
        assertEquals("Fatura do cartão", gravado.getDsLancamento());
        assertEquals(UUID1, gravado.getCdRequisicao());
        assertEquals(LocalDate.of(2026, 10, 1), gravado.getDtCompetencia());
        assertEquals(9L, gravado.getIdUsuarioCriacao());
        assertNull(gravado.getNrParcela());
        assertNull(gravado.getQtParcela());
    }

    @Test
    void competenciaInformadaPrevaleceSobreOMesDaData() {
        cadastrosAtivos();
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.empty());
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(5L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        FinanceiroLancamentoForm f = form(UUID1);
        f.setDtLancamento(LocalDate.of(2026, 11, 3));
        f.setDtCompetencia(LocalDate.of(2026, 10, 25));

        service.criar(f, null);

        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        verify(repository).inserirLancamento(captor.capture());
        assertEquals(LocalDate.of(2026, 10, 1), captor.getValue().getDtCompetencia());
        assertEquals(LocalDate.of(2026, 11, 3), captor.getValue().getDtLancamento());
        assertNull(captor.getValue().getIdUsuarioCriacao());
    }

    @Test
    void reenvioComMesmoCdRequisicaoDevolveOJaCriadoSemInserir() {
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.of(5L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));

        List<FinanceiroLancamentoDto> criados = service.criar(form(UUID1), 9L);

        assertEquals(5L, criados.get(0).getIdLancamento());
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void corridaNaCriacaoDevolveOLancamentoVencedor() {
        cadastrosAtivos();
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.empty(), Optional.of(7L));
        when(repository.inserirLancamento(any())).thenReturn(Optional.empty());
        when(repository.buscarLancamento(7L)).thenReturn(Optional.of(lancamento(7, 1)));

        assertEquals(7L, service.criar(form(UUID1), 9L).get(0).getIdLancamento());
    }

    @Test
    void criacaoExigeUuidValido() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(null), 1L));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form("nao-e-uuid"), 1L));
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void parcelamentoCriaUmLancamentoPorMesComUuidsDerivadosEstaveis() {
        cadastrosAtivos();
        when(repository.buscarLancamentoPorRequisicao(anyString())).thenReturn(Optional.empty());
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(5L), Optional.of(6L), Optional.of(7L));
        when(repository.buscarLancamento(anyLong())).thenReturn(Optional.of(lancamento(5, 1)));
        FinanceiroLancamentoForm f = form(UUID1);
        f.setDtLancamento(LocalDate.of(2026, 10, 31));
        f.setQtParcelas(3);

        List<FinanceiroLancamentoDto> criados = service.criar(f, 9L);

        assertEquals(3, criados.size());
        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        verify(repository, times(3)).inserirLancamento(captor.capture());
        List<FinanceiroLancamento> gravados = captor.getAllValues();
        assertEquals(UUID1, gravados.get(0).getCdRequisicao());
        Set<String> uuids = new HashSet<>();
        for (FinanceiroLancamento g : gravados) uuids.add(g.getCdRequisicao());
        assertEquals(3, uuids.size());
        assertEquals(FinanceiroServiceImpl.requisicaoDaParcela(UUID1, 1), gravados.get(1).getCdRequisicao());
        assertEquals(LocalDate.of(2026, 10, 1), gravados.get(0).getDtCompetencia());
        assertEquals(LocalDate.of(2026, 11, 1), gravados.get(1).getDtCompetencia());
        assertEquals(LocalDate.of(2026, 12, 1), gravados.get(2).getDtCompetencia());
        // 31/10 + 1 mês = 30/11 (sem estourar o mês)
        assertEquals(LocalDate.of(2026, 11, 30), gravados.get(1).getDtLancamento());
        assertEquals(1, gravados.get(0).getNrParcela());
        assertEquals(3, gravados.get(2).getNrParcela());
        assertEquals(3, gravados.get(0).getQtParcela());
    }

    @Test
    void reenvioDeParcelamentoDevolveTodasAsParcelasSemDuplicar() {
        FinanceiroLancamentoForm f = form(UUID1);
        f.setQtParcelas(2);
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.of(5L));
        when(repository.buscarLancamentoPorRequisicao(FinanceiroServiceImpl.requisicaoDaParcela(UUID1, 1))).thenReturn(Optional.of(6L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.buscarLancamento(6L)).thenReturn(Optional.of(lancamento(6, 1)));

        List<FinanceiroLancamentoDto> criados = service.criar(f, 9L);

        assertEquals(2, criados.size());
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void numeroDeParcelasForaDoLimiteEhRecusado() {
        FinanceiroLancamentoForm f = form(UUID1);
        f.setQtParcelas(0);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(f, 1L));
        f.setQtParcelas(121);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(f, 1L));
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void categoriaOuPessoaInexistenteOuInativaSaoRecusadas() {
        when(repository.buscarLancamentoPorRequisicao(anyString())).thenReturn(Optional.empty());
        when(repository.buscarCategoria(1L)).thenReturn(Optional.empty());
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(UUID1), 1L));

        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(categoria(1, "Fatura", "SAIDA", "RITMO_FATURA", false)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(UUID1), 1L));

        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(categoria(1, "Fatura", "SAIDA", "RITMO_FATURA", true)));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.empty());
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(UUID1), 1L));

        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(pessoa(1, "Nick", false)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(UUID1), 1L));
        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void saldoAnteriorNaoAceitaLancamento() {
        when(repository.buscarLancamentoPorRequisicao(anyString())).thenReturn(Optional.empty());
        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(categoria(1, "Saldo anterior", "ENTRADA", "SALDO_ANTERIOR", true)));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(pessoa(1, "Nick", true)));

        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(form(UUID1), 1L));
        verify(repository, never()).inserirLancamento(any());
    }

    // ---------- edição, marcação e exclusão ----------

    @Test
    void alteraComAVersaoDoCliente() {
        cadastrosAtivos();
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 2)));
        when(repository.alterarLancamento(any(), eq(2))).thenReturn(1);
        FinanceiroLancamentoForm f = form(null);
        f.setNrVersao(2);

        assertEquals(5L, service.alterar(5L, f, 9L).getIdLancamento());

        ArgumentCaptor<FinanceiroLancamento> captor = ArgumentCaptor.forClass(FinanceiroLancamento.class);
        verify(repository).alterarLancamento(captor.capture(), eq(2));
        assertEquals(5L, captor.getValue().getId());
        assertEquals(LocalDate.of(2026, 10, 1), captor.getValue().getDtCompetencia());
    }

    @Test
    void versaoDivergenteNaEdicaoMarcacaoEExclusaoViraConflito() {
        cadastrosAtivos();
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 3)));
        when(repository.alterarLancamento(any(), anyInt())).thenReturn(0);
        when(repository.marcarRealizado(anyLong(), anyBoolean(), anyInt())).thenReturn(0);
        when(repository.deletarLancamento(anyLong(), anyInt())).thenReturn(0);
        FinanceiroLancamentoForm f = form(null);
        f.setNrVersao(1);
        FinanceiroRealizadoForm r = new FinanceiroRealizadoForm();
        r.setInRealizado(true);
        r.setNrVersao(1);

        esperaErro(NordHttpEnum.HTTP_409, () -> service.alterar(5L, f, 9L));
        esperaErro(NordHttpEnum.HTTP_409, () -> service.marcarRealizado(5L, r));
        esperaErro(NordHttpEnum.HTTP_409, () -> service.excluir(5L, 1));
    }

    @Test
    void edicaoEExclusaoExigemNrVersao() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.alterar(5L, form(null), 9L));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.excluir(5L, null));
        verify(repository, never()).alterarLancamento(any(), anyInt());
        verify(repository, never()).deletarLancamento(anyLong(), anyInt());
    }

    @Test
    void lancamentoInexistenteVira404() {
        when(repository.buscarLancamento(99L)).thenReturn(Optional.empty());
        FinanceiroRealizadoForm r = new FinanceiroRealizadoForm();
        r.setInRealizado(true);
        r.setNrVersao(1);

        esperaErro(NordHttpEnum.HTTP_404, () -> service.marcarRealizado(99L, r));
        esperaErro(NordHttpEnum.HTTP_404, () -> service.excluir(99L, 1));
    }

    @Test
    void edicaoMantemCategoriaEPessoaInativasJaAssociadas() {
        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(categoria(1, "Fatura", "SAIDA", "RITMO_FATURA", false)));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(pessoa(1, "Nick", false)));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.alterarLancamento(any(), eq(1))).thenReturn(1);
        FinanceiroLancamentoForm f = form(null);
        f.setNrVersao(1);

        assertEquals(5L, service.alterar(5L, f, 9L).getIdLancamento());
    }

    @Test
    void marcaERemoveRealizado() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.marcarRealizado(5L, true, 1)).thenReturn(1);
        FinanceiroRealizadoForm r = new FinanceiroRealizadoForm();
        r.setInRealizado(true);
        r.setNrVersao(1);

        service.marcarRealizado(5L, r);

        verify(repository).marcarRealizado(5L, true, 1);
    }

    @Test
    void excluiComAVersaoCerta() {
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 4)));
        when(repository.deletarLancamento(5L, 4)).thenReturn(1);

        service.excluir(5L, 4);

        verify(repository).deletarLancamento(5L, 4);
    }

    // ---------- mês fechado e leituras da fatura ----------

    @Test
    void mesFechadoTravaCriacaoEdicaoMarcacaoEExclusao() {
        cadastrosAtivos();
        fechado(LocalDate.of(2026, 10, 1));
        when(repository.buscarLancamentoPorRequisicao(anyString())).thenReturn(Optional.empty());
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        FinanceiroLancamentoForm f = form(UUID1);
        f.setNrVersao(1);
        FinanceiroRealizadoForm r = new FinanceiroRealizadoForm();
        r.setInRealizado(true);
        r.setNrVersao(1);

        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(f, 9L));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.alterar(5L, f, 9L));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.marcarRealizado(5L, r));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.excluir(5L, 1));

        verify(repository, never()).inserirLancamento(any());
        verify(repository, never()).alterarLancamento(any(), anyInt());
        verify(repository, never()).marcarRealizado(anyLong(), anyBoolean(), anyInt());
        verify(repository, never()).deletarLancamento(anyLong(), anyInt());
    }

    @Test
    void parcelaEmMesFechadoImpedeACriacaoInteira() {
        cadastrosAtivos();
        fechado(LocalDate.of(2026, 12, 1));
        when(repository.buscarLancamentoPorRequisicao(anyString())).thenReturn(Optional.empty());
        FinanceiroLancamentoForm f = form(UUID1);
        f.setQtParcelas(3);

        esperaErro(NordHttpEnum.HTTP_400, () -> service.criar(f, 9L));

        verify(repository, never()).inserirLancamento(any());
    }

    @Test
    void mudarOLancamentoParaUmMesFechadoEhRecusado() {
        cadastrosAtivos();
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        fechado(LocalDate.of(2026, 9, 1));
        FinanceiroLancamentoForm f = form(null);
        f.setNrVersao(1);
        f.setDtLancamento(LocalDate.of(2026, 9, 20));

        esperaErro(NordHttpEnum.HTTP_400, () -> service.alterar(5L, f, 9L));
        verify(repository, never()).alterarLancamento(any(), anyInt());
    }

    @Test
    void atualizarAFaturaRegistraALeituraDoDia() {
        FinanceiroCategoria fatura = categoria(1, "Fatura", "SAIDA", "RITMO_FATURA", true);
        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(fatura));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(pessoa(1, "Nick", true)));
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.empty());
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(5L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));
        when(repository.alterarLancamento(any(), eq(1))).thenReturn(1);

        service.criar(form(UUID1), 9L);
        FinanceiroLancamentoForm edicao = form(null);
        edicao.setNrVersao(1);
        edicao.setVlLancamento(new BigDecimal("1800.00"));
        service.alterar(5L, edicao, 9L);

        verify(projecaoRepository).registrarLeitura(5L, LocalDate.of(2026, 10, 8), new BigDecimal("1500.00"), 9L);
        verify(projecaoRepository).registrarLeitura(5L, LocalDate.of(2026, 10, 8), new BigDecimal("1800.00"), 9L);
    }

    @Test
    void outrasCategoriasNaoGeramLeitura() {
        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(categoria(1, "Luz", "SAIDA", "VARIAVEL_MEDIA", true)));
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(pessoa(1, "Nick", true)));
        when(repository.buscarLancamentoPorRequisicao(UUID1)).thenReturn(Optional.empty());
        when(repository.inserirLancamento(any())).thenReturn(Optional.of(5L));
        when(repository.buscarLancamento(5L)).thenReturn(Optional.of(lancamento(5, 1)));

        service.criar(form(UUID1), 9L);

        verify(projecaoRepository, never()).registrarLeitura(any(), any(), any(), any());
    }

    // ---------- listagem e resumo ----------

    @Test
    void listagemTrazTotaisPessoasECategorias() {
        when(repository.listarLancamentos(any())).thenReturn(Collections.singletonList(lancamento(5, 1)));
        when(repository.resumir(any())).thenReturn(new FinanceiroRepository.Totais(
                new BigDecimal("5000.00"), new BigDecimal("3500.50"), new BigDecimal("5000.00"), BigDecimal.ZERO, 3, 1));
        when(repository.listarPessoas()).thenReturn(Collections.singletonList(pessoa(1, "Nick", true)));
        when(repository.listarCategorias()).thenReturn(Collections.singletonList(categoria(1, "Fatura", "SAIDA", "RITMO_FATURA", true)));

        FinanceiroListaDto lista = service.listar(new FinanceiroFiltro("2026-10", null, null, null, null, null, null, null));

        assertEquals(1, lista.getLancamentos().size());
        assertEquals(new BigDecimal("1499.50"), lista.getResumo().getSaldo());
        assertEquals(1, lista.getPessoas().size());
        assertEquals(1, lista.getCategorias().size());
    }

    @Test
    void resumoSemLancamentosVemZerado() {
        when(repository.resumir(any())).thenReturn(new FinanceiroRepository.Totais(null, null, null, null, 0, 0));

        FinanceiroResumoDto r = service.resumir(new FinanceiroFiltro());

        assertEquals(BigDecimal.ZERO, r.getSaldo());
        assertEquals(0, r.getQtLancamentos());
    }

    @Test
    void filtrosInvalidosSaoRecusadosAntesDeConsultar() {
        esperaErro(NordHttpEnum.HTTP_400, () -> service.listar(new FinanceiroFiltro("10/2026", null, null, null, null, null, null, null)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.listar(new FinanceiroFiltro(null, null, null, null, null, "DESPESA", null, null)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.listar(new FinanceiroFiltro(null, null, null, null, null, null, "PAGO", null)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.listar(new FinanceiroFiltro(
                null, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null, null, null, null, null)));
        verify(repository, never()).listarLancamentos(any());
    }

    @Test
    void filtrosValidosPassam() {
        when(repository.resumir(any())).thenReturn(new FinanceiroRepository.Totais(null, null, null, null, 0, 0));
        service.resumir(new FinanceiroFiltro("2026-10", null, null, 1L, 2L, "entrada", "realizado", "luz"));
        verify(repository).resumir(any());
    }

    // ---------- pessoas ----------

    @Test
    void criaPessoaComNomeSemEspacosEPadroes() {
        when(repository.listarPessoas()).thenReturn(Collections.emptyList());
        when(repository.inserirPessoa(any())).thenReturn(4L);
        when(repository.buscarPessoa(4L)).thenReturn(Optional.of(pessoa(4, "Thaina", true)));
        FinanceiroPessoaForm f = new FinanceiroPessoaForm();
        f.setNmPessoa("  Thaina ");

        service.criarPessoa(f);

        ArgumentCaptor<FinanceiroPessoa> captor = ArgumentCaptor.forClass(FinanceiroPessoa.class);
        verify(repository).inserirPessoa(captor.capture());
        assertEquals("Thaina", captor.getValue().getNmPessoa());
        assertEquals(true, captor.getValue().getInAtivo());
        assertEquals(false, captor.getValue().getInCompartilhado());
    }

    @Test
    void pessoaExigeNomeEnaoRepeteNome() {
        FinanceiroPessoaForm vazio = new FinanceiroPessoaForm();
        vazio.setNmPessoa("   ");
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarPessoa(vazio));

        when(repository.listarPessoas()).thenReturn(Collections.singletonList(pessoa(1, "Nick", true)));
        FinanceiroPessoaForm repetido = new FinanceiroPessoaForm();
        repetido.setNmPessoa("nick");
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarPessoa(repetido));
        verify(repository, never()).inserirPessoa(any());
    }

    @Test
    void atualizaPessoaSoNoQueFoiInformado() {
        FinanceiroPessoa atual = pessoa(1, "Nick", true);
        when(repository.buscarPessoa(1L)).thenReturn(Optional.of(atual));
        when(repository.listarPessoas()).thenReturn(Collections.singletonList(atual));
        FinanceiroPessoaForm f = new FinanceiroPessoaForm();
        f.setInAtivo(false);
        f.setIdUsuario(7L);

        service.atualizarPessoa(1L, f);

        ArgumentCaptor<FinanceiroPessoa> captor = ArgumentCaptor.forClass(FinanceiroPessoa.class);
        verify(repository).alterarPessoa(captor.capture());
        assertEquals("Nick", captor.getValue().getNmPessoa());
        assertEquals(false, captor.getValue().getInAtivo());
        assertEquals(7L, captor.getValue().getIdUsuario());
    }

    // ---------- categorias ----------

    private FinanceiroCategoriaForm formCategoria(String nome, String tipo, String projecao) {
        FinanceiroCategoriaForm f = new FinanceiroCategoriaForm();
        f.setNmCategoria(nome);
        f.setCdTipo(tipo);
        f.setCdProjecao(projecao);
        return f;
    }

    @Test
    void criaCategoriaComRegraDeData() {
        when(repository.listarCategorias()).thenReturn(Collections.emptyList());
        when(repository.inserirCategoria(any())).thenReturn(8L);
        when(repository.buscarCategoria(8L)).thenReturn(Optional.of(categoria(8, "Salário", "ENTRADA", "FIXA_MEDIA", true)));
        FinanceiroCategoriaForm f = formCategoria(" Salário ", "entrada", "fixa_media");
        f.setCdRegraData("dia_util");
        f.setNrDia(5);

        service.criarCategoria(f);

        ArgumentCaptor<FinanceiroCategoria> captor = ArgumentCaptor.forClass(FinanceiroCategoria.class);
        verify(repository).inserirCategoria(captor.capture());
        assertEquals("Salário", captor.getValue().getNmCategoria());
        assertEquals("ENTRADA", captor.getValue().getCdTipo());
        assertEquals("FIXA_MEDIA", captor.getValue().getCdProjecao());
        assertEquals("DIA_UTIL", captor.getValue().getCdRegraData());
        assertEquals(5, captor.getValue().getNrDia());
    }

    @Test
    void categoriaRecusaCombinacoesInvalidas() {
        when(repository.listarCategorias()).thenReturn(Collections.emptyList());
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(formCategoria("X", "DESPESA", "MANUAL")));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(formCategoria("X", "SAIDA", "OUTRA")));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(formCategoria(" ", "SAIDA", "MANUAL")));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(formCategoria("X", "SAIDA", "SALDO_ANTERIOR")));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(formCategoria("X", "ENTRADA", "RITMO_FATURA")));
        FinanceiroCategoriaForm semDia = formCategoria("X", "ENTRADA", "MANUAL");
        semDia.setCdRegraData("DIA_MES");
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(semDia));
        FinanceiroCategoriaForm diaUtilAlto = formCategoria("X", "ENTRADA", "MANUAL");
        diaUtilAlto.setCdRegraData("DIA_UTIL");
        diaUtilAlto.setNrDia(24);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(diaUtilAlto));
        FinanceiroCategoriaForm diaSemRegra = formCategoria("X", "ENTRADA", "MANUAL");
        diaSemRegra.setNrDia(5);
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(diaSemRegra));
        verify(repository, never()).inserirCategoria(any());
    }

    @Test
    void categoriaNaoRepeteNomeNoMesmoTipoMasPodeEmTiposDiferentes() {
        when(repository.listarCategorias()).thenReturn(Collections.singletonList(categoria(1, "Juan", "SAIDA", "MANUAL", true)));
        esperaErro(NordHttpEnum.HTTP_400, () -> service.criarCategoria(formCategoria("juan", "SAIDA", "MANUAL")));

        when(repository.inserirCategoria(any())).thenReturn(9L);
        when(repository.buscarCategoria(9L)).thenReturn(Optional.of(categoria(9, "Juan", "ENTRADA", "MANUAL", true)));
        assertEquals(9L, service.criarCategoria(formCategoria("Juan", "ENTRADA", "MANUAL")).getIdCategoria());
    }

    @Test
    void tipoDaCategoriaNaoMudaComLancamentos() {
        FinanceiroCategoria atual = categoria(1, "Juan", "SAIDA", "MANUAL", true);
        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(atual));
        when(repository.contarLancamentosDaCategoria(1L)).thenReturn(3);
        FinanceiroCategoriaForm f = new FinanceiroCategoriaForm();
        f.setCdTipo("ENTRADA");

        esperaErro(NordHttpEnum.HTTP_400, () -> service.atualizarCategoria(1L, f));
        verify(repository, never()).alterarCategoria(any());
    }

    @Test
    void atualizaCategoriaSemMudarOTipo() {
        FinanceiroCategoria atual = categoria(1, "Juan", "SAIDA", "MANUAL", true);
        when(repository.buscarCategoria(1L)).thenReturn(Optional.of(atual));
        when(repository.listarCategorias()).thenReturn(Collections.singletonList(atual));
        FinanceiroCategoriaForm f = new FinanceiroCategoriaForm();
        f.setNmCategoria("Juan (obra)");
        f.setCdTipo("SAIDA");
        f.setInAtivo(false);

        service.atualizarCategoria(1L, f);

        ArgumentCaptor<FinanceiroCategoria> captor = ArgumentCaptor.forClass(FinanceiroCategoria.class);
        verify(repository).alterarCategoria(captor.capture());
        assertEquals("Juan (obra)", captor.getValue().getNmCategoria());
        assertEquals(false, captor.getValue().getInAtivo());
        assertTrue(captor.getValue().getCdTipo().equals("SAIDA"));
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.listarPessoas());
        verifyNoInteractions(repository, projecaoRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> service.excluir(1L, 1));
        verifyNoInteractions(repository, projecaoRepository);
    }
}
