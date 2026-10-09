package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.domain.FinanceiroCategoria;
import br.com.nord_tool_backend.domain.FinanceiroConfiguracao;
import br.com.nord_tool_backend.domain.FinanceiroFaturaAberta;
import br.com.nord_tool_backend.domain.FinanceiroLancamento;
import br.com.nord_tool_backend.domain.FinanceiroMes;
import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import br.com.nord_tool_backend.domain.FinanceiroRecorrencia;
import br.com.nord_tool_backend.domain.FinanceiroSomaMes;
import br.com.nord_tool_backend.domain.enums.TipoProjecaoEnum;
import br.com.nord_tool_backend.dto.FinanceiroConfiguracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroFaturaLeituraDto;
import br.com.nord_tool_backend.dto.FinanceiroFechamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroGeracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoLinhaDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.dto.FinanceiroRecorrenciaDto;
import br.com.nord_tool_backend.form.FinanceiroConfiguracaoForm;
import br.com.nord_tool_backend.form.FinanceiroRecorrenciaForm;
import br.com.nord_tool_backend.form.FinanceiroSaldoInicialForm;
import br.com.nord_tool_backend.repository.FinanceiroProjecaoRepository;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import br.com.nord_tool_backend.service.FinanceiroProjecaoService;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator;
import br.com.nord_tool_backend.service.projecao.ProjecaoCalculator.Resultado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinanceiroProjecaoServiceImpl implements FinanceiroProjecaoService {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    static final int MESES_DE_CADEIA = 24;
    static final String MSG_CONFLITO = "O registro mudou. Sincronize e tente novamente.";

    private final FinanceiroRepository repository;
    private final FinanceiroProjecaoRepository projecaoRepository;
    private final Clock clock;
    private final AutorizacaoService autorizacao;

    public FinanceiroProjecaoServiceImpl(FinanceiroRepository repository, FinanceiroProjecaoRepository projecaoRepository, Clock clock, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.projecaoRepository = projecaoRepository;
        this.clock = clock;
        this.autorizacao = autorizacao;
    }

    // ---------- a conta do mês ----------

    @Override
    @Transactional(readOnly = true)
    public FinanceiroProjecaoMesDto obterMes(String competencia, Long idPessoa) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        return projetar(mes(competencia), idPessoa, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroProjecaoMesDto definirSaldoInicial(String competencia, FinanceiroSaldoInicialForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        YearMonth mes = mes(competencia);
        exigirAberto(mes);
        projecaoRepository.garantirMes(mes.atDay(1));
        if (projecaoRepository.definirSaldoInicial(mes.atDay(1), form.getVlSaldoInicial()) == 0) throw mesFechado(mes);
        return projetar(mes, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroFechamentoDto fechar(String competencia, Long idUsuario) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        YearMonth mes = mes(competencia);
        Optional<FinanceiroMes> linha = projecaoRepository.buscarMes(mes.atDay(1));
        if (linha.isPresent() && fechado(linha.get())) throw invalido("O mês de " + rotulo(mes) + " já está fechado");

        Optional<LocalDate> primeira = projecaoRepository.primeiraCompetencia();
        boolean haMesAnterior = primeira.isPresent() && YearMonth.from(primeira.get()).isBefore(mes);
        boolean anteriorFechado = projecaoRepository.buscarMes(mes.minusMonths(1).atDay(1)).map(FinanceiroProjecaoServiceImpl::fechado).orElse(false);
        boolean temSaldoInicial = linha.isPresent() && linha.get().getVlSaldoInicial() != null;
        if (haMesAnterior && !anteriorFechado && !temSaldoInicial) {
            throw invalido("Feche antes o mês de " + rotulo(mes.minusMonths(1)) + " (ou informe o saldo inicial de " + rotulo(mes) + ").");
        }

        // O saldo gravado é o real: saldo anterior + o que foi lançado, sem estimativas.
        FinanceiroProjecaoMesDto real = projetar(mes, null, true);
        projecaoRepository.garantirMes(mes.atDay(1));
        if (projecaoRepository.fecharMes(mes.atDay(1), real.getSaldoFinal(), idUsuario) == 0) throw invalido("O mês de " + rotulo(mes) + " já está fechado");

        int geradas = 0;
        if (!mesFechadoNoBanco(mes.plusMonths(1))) geradas = gerar(mes.plusMonths(1), idUsuario);
        return new FinanceiroFechamentoDto(projetar(mes, null, false), geradas);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroProjecaoMesDto reabrir(String competencia) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        YearMonth mes = mes(competencia);
        if (!mesFechadoNoBanco(mes)) throw invalido("O mês de " + rotulo(mes) + " não está fechado");
        if (projecaoRepository.existeMesFechadoApos(mes.atDay(1))) {
            throw invalido("Reabra primeiro os meses seguintes: só o último mês fechado pode ser reaberto");
        }
        if (projecaoRepository.reabrirMes(mes.atDay(1)) == 0) throw invalido("O mês de " + rotulo(mes) + " não está fechado");
        return projetar(mes, null, false);
    }

    /** Monta a conta de {@code mes}. {@code soReal} ignora estimativas (usado ao fechar). */
    private FinanceiroProjecaoMesDto projetar(YearMonth mes, Long idPessoa, boolean soReal) {
        FinanceiroConfiguracao cfg = configuracao();
        int meses = valor(cfg.getNrMesesMedia(), 3);
        YearMonth atual = YearMonth.now(clock.withZone(FUSO));
        List<FinanceiroCategoria> categorias = repository.listarCategorias();
        List<FinanceiroRecorrencia> recorrencias = projecaoRepository.listarRecorrencias();
        boolean comSaldo = idPessoa == null;

        Optional<LocalDate> primeira = projecaoRepository.primeiraCompetencia();
        YearMonth inicio = primeira.map(YearMonth::from).orElse(mes);
        if (inicio.isBefore(mes.minusMonths(MESES_DE_CADEIA))) inicio = mes.minusMonths(MESES_DE_CADEIA);
        if (inicio.isAfter(mes)) inicio = mes;

        Somas somas = carregarSomas(inicio.minusMonths(meses), mes, idPessoa);
        Map<LocalDate, FinanceiroMes> linhas = new HashMap<>();
        for (FinanceiroMes l : projecaoRepository.listarMeses(inicio.atDay(1), mes.atDay(1))) linhas.put(l.getDtCompetencia(), l);
        FinanceiroMes linhaDoMes = linhas.get(mes.atDay(1));
        boolean fechado = linhaDoMes != null && fechado(linhaDoMes);
        boolean estimar = !soReal && !fechado && !mes.isBefore(atual);

        BigDecimal saldoAnterior = null;
        if (comSaldo) {
            BigDecimal corrente = BigDecimal.ZERO;
            for (YearMonth m = inicio; m.isBefore(mes); m = m.plusMonths(1)) {
                FinanceiroMes linha = linhas.get(m.atDay(1));
                if (linha != null && fechado(linha) && linha.getVlSaldoFinal() != null) {
                    corrente = linha.getVlSaldoFinal();
                    continue;
                }
                BigDecimal inicial = linha != null && linha.getVlSaldoInicial() != null ? linha.getVlSaldoInicial() : corrente;
                Resultado r = calcular(m, !m.isBefore(atual), categorias, recorrencias, somas, cfg, meses, null);
                corrente = inicial.add(r.totalEntradas).subtract(r.totalSaidas);
            }
            saldoAnterior = linhaDoMes != null && linhaDoMes.getVlSaldoInicial() != null ? linhaDoMes.getVlSaldoInicial() : corrente;
        }

        Resultado resultado = calcular(mes, estimar, categorias, recorrencias, somas, cfg, meses, idPessoa);
        BigDecimal saldoFinal = (saldoAnterior == null ? BigDecimal.ZERO : saldoAnterior)
                .add(resultado.totalEntradas).subtract(resultado.totalSaidas);
        if (fechado && comSaldo && linhaDoMes.getVlSaldoFinal() != null) saldoFinal = linhaDoMes.getVlSaldoFinal();

        BigDecimal meta = comSaldo ? valor(cfg.getVlMetaSaldo(), BigDecimal.ZERO) : null;
        BigDecimal folga = meta == null ? null : saldoFinal.subtract(meta);
        boolean verde = meta == null ? saldoFinal.signum() >= 0 : folga.signum() >= 0;
        int previstos = fechado ? 0 : projecaoRepository.contarPrevistos(mes.atDay(1));
        return new FinanceiroProjecaoMesDto(formato(mes), fechado, estimar, comSaldo, saldoAnterior,
                linhas(resultado.entradas), linhas(resultado.saidas), resultado.totalEntradas, resultado.totalSaidas,
                saldoFinal, meta, folga, verde ? "VERDE" : "VERMELHO", previstos);
    }

    private Resultado calcular(YearMonth mes, boolean estimar, List<FinanceiroCategoria> categorias,
                               List<FinanceiroRecorrencia> recorrencias, Somas somas, FinanceiroConfiguracao cfg,
                               int mesesNaMedia, Long idPessoa) {
        List<ProjecaoCalculator.Fatura> faturas = new ArrayList<>();
        Map<Long, BigDecimal> fixas = new HashMap<>();
        if (estimar) {
            for (FinanceiroFaturaAberta f : projecaoRepository.faturasDoMes(mes.atDay(1), idPessoa)) {
                faturas.add(new ProjecaoCalculator.Fatura(f.getIdCategoria(), f.getVlLancamento(), f.getDtLeitura()));
            }
            fixas = somaRecorrencias(recorrencias, mes, idPessoa);
        }
        return ProjecaoCalculator.calcular(new ProjecaoCalculator.Entrada(mes, estimar, categorias,
                somas.total.getOrDefault(mes, new HashMap<>()), somas.quantidade.getOrDefault(mes, new HashMap<>()),
                somas.total, fixas, faturas, mesesNaMedia, valor(cfg.getNrDiaFechamentoFatura(), 8)));
    }

    /** Soma, por categoria, das recorrências ativas que valem no mês. */
    static Map<Long, BigDecimal> somaRecorrencias(List<FinanceiroRecorrencia> recorrencias, YearMonth mes, Long idPessoa) {
        Map<Long, BigDecimal> soma = new HashMap<>();
        for (FinanceiroRecorrencia r : recorrencias) {
            if (!vigente(r, mes) || (idPessoa != null && !idPessoa.equals(r.getIdPessoa()))) continue;
            soma.merge(r.getIdCategoria(), r.getVlRecorrencia(), BigDecimal::add);
        }
        return soma;
    }

    static boolean vigente(FinanceiroRecorrencia r, YearMonth mes) {
        if (Boolean.FALSE.equals(r.getInAtivo())) return false;
        if (YearMonth.from(r.getDtInicio()).isAfter(mes)) return false;
        return r.getDtFim() == null || !YearMonth.from(r.getDtFim()).isBefore(mes);
    }

    private static final class Somas {
        final Map<YearMonth, Map<Long, BigDecimal>> total = new HashMap<>();
        final Map<YearMonth, Map<Long, Integer>> quantidade = new HashMap<>();
    }

    private Somas carregarSomas(YearMonth de, YearMonth ate, Long idPessoa) {
        Somas somas = new Somas();
        for (FinanceiroSomaMes s : projecaoRepository.somarPorCategoria(de.atDay(1), ate.atDay(1), idPessoa)) {
            YearMonth m = YearMonth.from(s.getDtCompetencia());
            somas.total.computeIfAbsent(m, k -> new HashMap<>()).put(s.getIdCategoria(), s.getVlTotal());
            somas.quantidade.computeIfAbsent(m, k -> new HashMap<>()).put(s.getIdCategoria(), s.getQtLancamentos());
        }
        return somas;
    }

    private static List<FinanceiroProjecaoLinhaDto> linhas(List<ProjecaoCalculator.Linha> origem) {
        return origem.stream().map(l -> new FinanceiroProjecaoLinhaDto(l.idCategoria, l.nmCategoria, l.cdTipo, l.cdProjecao,
                l.real, l.projetado, l.origem, l.detalhe)).collect(Collectors.toList());
    }

    // ---------- configuração ----------

    @Override
    @Transactional
    public FinanceiroConfiguracaoDto obterConfiguracao() {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        return FinanceiroConfiguracaoDto.de(configuracao());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroConfiguracaoDto atualizarConfiguracao(FinanceiroConfiguracaoForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        projecaoRepository.garantirConfiguracao();
        FinanceiroConfiguracao c = new FinanceiroConfiguracao();
        c.setVlMetaSaldo(form.getVlMetaSaldo());
        c.setNrMesesMedia(form.getNrMesesMedia());
        c.setNrDiaConferencia(form.getNrDiaConferencia());
        c.setNrDiaFechamentoFatura(form.getNrDiaFechamentoFatura());
        projecaoRepository.alterarConfiguracao(c);
        return FinanceiroConfiguracaoDto.de(configuracao());
    }

    private FinanceiroConfiguracao configuracao() {
        Optional<FinanceiroConfiguracao> existente = projecaoRepository.buscarConfiguracao();
        if (existente.isPresent()) return existente.get();
        projecaoRepository.garantirConfiguracao();
        return projecaoRepository.buscarConfiguracao().orElseGet(() -> {
            FinanceiroConfiguracao padrao = new FinanceiroConfiguracao();
            padrao.setVlMetaSaldo(BigDecimal.ZERO);
            padrao.setNrMesesMedia(3);
            padrao.setNrDiaConferencia(8);
            padrao.setNrDiaFechamentoFatura(8);
            return padrao;
        });
    }

    // ---------- leituras da fatura ----------

    @Override
    @Transactional(readOnly = true)
    public List<FinanceiroFaturaLeituraDto> listarLeituras(Long idLancamento) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        repository.buscarLancamento(idLancamento)
                .orElseThrow(() -> new NaoEncontradoException("Lançamento não encontrado"));
        return projecaoRepository.listarLeituras(idLancamento).stream().map(FinanceiroFaturaLeituraDto::de).collect(Collectors.toList());
    }

    // ---------- recorrências ----------

    @Override
    @Transactional(readOnly = true)
    public List<FinanceiroRecorrenciaDto> listarRecorrencias() {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        return projecaoRepository.listarRecorrencias().stream().map(FinanceiroRecorrenciaDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroRecorrenciaDto criarRecorrencia(FinanceiroRecorrenciaForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        validarRecorrencia(form, null, null);
        Long id = projecaoRepository.inserirRecorrencia(converter(form));
        return FinanceiroRecorrenciaDto.de(recorrencia(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroRecorrenciaDto atualizarRecorrencia(Long id, FinanceiroRecorrenciaForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        if (form.getNrVersao() == null) throw invalido("Informe a versão da recorrência (nrVersao)");
        FinanceiroRecorrencia atual = recorrencia(id);
        validarRecorrencia(form, atual.getIdCategoria(), atual.getIdPessoa());
        FinanceiroRecorrencia nova = converter(form);
        nova.setId(id);
        if (projecaoRepository.alterarRecorrencia(nova, form.getNrVersao()) == 0) {
            throw new ConflitoException(MSG_CONFLITO);
        }
        return FinanceiroRecorrenciaDto.de(recorrencia(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroGeracaoDto gerarRecorrencias(String competencia, Long idUsuario) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        YearMonth mes = mes(competencia);
        exigirAberto(mes);
        return new FinanceiroGeracaoDto(formato(mes), gerar(mes, idUsuario));
    }

    /**
     * Cria os lançamentos das recorrências vigentes no mês. O identificador do lançamento é derivado da recorrência e
     * do mês, então repetir a geração não duplica; um lançamento apagado à mão, porém, volta a ser criado.
     */
    private int gerar(YearMonth mes, Long idUsuario) {
        Map<Long, FinanceiroCategoria> categorias = repository.listarCategorias().stream()
                .collect(Collectors.toMap(FinanceiroCategoria::getId, c -> c));
        Map<Long, FinanceiroPessoa> pessoas = repository.listarPessoas().stream()
                .collect(Collectors.toMap(FinanceiroPessoa::getId, p -> p));
        int criados = 0;
        for (FinanceiroRecorrencia r : projecaoRepository.listarRecorrencias()) {
            if (!vigente(r, mes)) continue;
            FinanceiroCategoria c = categorias.get(r.getIdCategoria());
            FinanceiroPessoa p = pessoas.get(r.getIdPessoa());
            if (c == null || Boolean.FALSE.equals(c.getInAtivo()) || p == null || Boolean.FALSE.equals(p.getInAtivo())) continue;

            FinanceiroLancamento l = new FinanceiroLancamento();
            l.setCdRequisicao(requisicaoDaRecorrencia(r.getId(), mes));
            l.setDtCompetencia(mes.atDay(1));
            l.setDtLancamento(mes.atDay(Math.max(1, Math.min(r.getNrDia() == null ? 1 : r.getNrDia(), mes.lengthOfMonth()))));
            l.setIdCategoria(r.getIdCategoria());
            l.setIdPessoa(r.getIdPessoa());
            l.setDsLancamento(r.getDsRecorrencia());
            l.setVlLancamento(r.getVlRecorrencia());
            l.setInRealizado(false);
            l.setIdUsuarioCriacao(idUsuario);
            l.setIdRecorrencia(r.getId());
            if (repository.inserirLancamento(l).isPresent()) criados++;
        }
        return criados;
    }

    static String requisicaoDaRecorrencia(Long idRecorrencia, YearMonth mes) {
        return UUID.nameUUIDFromBytes(("recorrencia:" + idRecorrencia + ":" + formato(mes)).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void validarRecorrencia(FinanceiroRecorrenciaForm form, Long idCategoriaAtual, Long idPessoaAtual) {
        FinanceiroCategoria c = repository.buscarCategoria(form.getIdCategoria()).orElseThrow(() -> invalido("Categoria não encontrada"));
        if (Boolean.FALSE.equals(c.getInAtivo()) && !form.getIdCategoria().equals(idCategoriaAtual)) throw invalido("A categoria está inativa");
        if (TipoProjecaoEnum.SALDO_ANTERIOR.name().equals(c.getCdProjecao())) {
            throw invalido("O saldo anterior é calculado pelo sistema e não aceita recorrência");
        }
        FinanceiroPessoa p = repository.buscarPessoa(form.getIdPessoa()).orElseThrow(() -> invalido("Pessoa não encontrada"));
        if (Boolean.FALSE.equals(p.getInAtivo()) && !form.getIdPessoa().equals(idPessoaAtual)) throw invalido("A pessoa está inativa");
        if (form.getDtFim() != null && form.getDtFim().isBefore(form.getDtInicio())) {
            throw invalido("O fim da recorrência não pode ser anterior ao começo");
        }
    }

    private static FinanceiroRecorrencia converter(FinanceiroRecorrenciaForm form) {
        FinanceiroRecorrencia r = new FinanceiroRecorrencia();
        r.setIdCategoria(form.getIdCategoria());
        r.setIdPessoa(form.getIdPessoa());
        String descricao = form.getDsRecorrencia() == null ? "" : form.getDsRecorrencia().trim();
        r.setDsRecorrencia(descricao.isEmpty() ? null : descricao);
        r.setVlRecorrencia(form.getVlRecorrencia());
        r.setNrDia(form.getNrDia());
        r.setDtInicio(form.getDtInicio());
        r.setDtFim(form.getDtFim());
        r.setInAtivo(form.getInAtivo() == null || form.getInAtivo());
        return r;
    }

    private FinanceiroRecorrencia recorrencia(Long id) {
        return projecaoRepository.buscarRecorrencia(id)
                .orElseThrow(() -> new NaoEncontradoException("Recorrência não encontrada"));
    }

    // ---------- auxiliares ----------

    private boolean mesFechadoNoBanco(YearMonth mes) {
        return projecaoRepository.buscarMes(mes.atDay(1)).map(FinanceiroProjecaoServiceImpl::fechado).orElse(false);
    }

    private void exigirAberto(YearMonth mes) {
        if (mesFechadoNoBanco(mes)) throw mesFechado(mes);
    }

    private static NordException mesFechado(YearMonth mes) {
        return invalido("O mês de " + rotulo(mes) + " está fechado. Reabra o mês para alterá-lo.");
    }

    private static boolean fechado(FinanceiroMes m) {
        return Boolean.TRUE.equals(m.getInFechado());
    }

    private static YearMonth mes(String competencia) {
        try {
            return YearMonth.parse(competencia == null ? "" : competencia.trim());
        } catch (DateTimeParseException ex) {
            throw invalido("Competência inválida. Use o formato yyyy-MM (ex.: 2026-10).");
        }
    }

    static String formato(YearMonth mes) {
        return String.format("%04d-%02d", mes.getYear(), mes.getMonthValue());
    }

    private static final String[] NOMES = {"janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};

    static String rotulo(YearMonth mes) {
        return NOMES[mes.getMonthValue() - 1] + " de " + mes.getYear();
    }

    private static NordException invalido(String mensagem) {
        return new EntradaInvalidaException(mensagem);
    }

    private static int valor(Integer valor, int padrao) {
        return valor == null ? padrao : valor;
    }

    private static BigDecimal valor(BigDecimal valor, BigDecimal padrao) {
        return valor == null ? padrao : valor;
    }
}
