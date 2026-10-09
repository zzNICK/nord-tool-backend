package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.domain.FinanceiroAtivo;
import br.com.nord_tool_backend.domain.FinanceiroOperacao;
import br.com.nord_tool_backend.domain.FinanceiroProvento;
import br.com.nord_tool_backend.dto.FinanceiroAtivoDto;
import br.com.nord_tool_backend.dto.FinanceiroInvestimentoDto;
import br.com.nord_tool_backend.form.FinanceiroAtivoForm;
import br.com.nord_tool_backend.form.FinanceiroOperacaoForm;
import br.com.nord_tool_backend.form.FinanceiroProventoForm;
import br.com.nord_tool_backend.repository.FinanceiroInvestimentoRepository;
import br.com.nord_tool_backend.repository.FinanceiroRepository;
import br.com.nord_tool_backend.service.FinanceiroInvestimentoService;
import br.com.nord_tool_backend.service.investimento.Cotacao;
import br.com.nord_tool_backend.service.CotacaoService;
import br.com.nord_tool_backend.service.investimento.PosicaoCalculator;
import br.com.nord_tool_backend.service.investimento.PosicaoCalculator.Posicao;
import br.com.nord_tool_backend.service.investimento.ProventoCotado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FinanceiroInvestimentoServiceImpl implements FinanceiroInvestimentoService {

    static final String MSG_CONFLITO = "O fundo mudou. Sincronize e tente novamente.";
    private static final Pattern TICKER = Pattern.compile("^[A-Z]{4}[0-9]{1,2}$");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final FinanceiroInvestimentoRepository repository;
    private final FinanceiroRepository financeiroRepository;
    private final CotacaoService cotacaoService;
    private final Clock clock;
    private final AutorizacaoService autorizacao;

    public FinanceiroInvestimentoServiceImpl(FinanceiroInvestimentoRepository repository, FinanceiroRepository financeiroRepository,
                                             CotacaoService cotacaoService, Clock clock, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.financeiroRepository = financeiroRepository;
        this.cotacaoService = cotacaoService;
        this.clock = clock;
        this.autorizacao = autorizacao;
    }

    // ---------- leitura ----------

    @Override
    public FinanceiroInvestimentoDto listar(Long idPessoa) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
        List<FinanceiroAtivo> ativos = repository.listarAtivos(idPessoa);
        List<FinanceiroAtivoDto> dtos = montar(ativos, true);
        BigDecimal investido = soma(dtos, FinanceiroAtivoDto::getVlInvestido);
        BigDecimal patrimonio = soma(dtos, FinanceiroAtivoDto::getVlPatrimonio);
        BigDecimal aReceber = soma(dtos, FinanceiroAtivoDto::getVlAReceber);
        YearMonth mes = YearMonth.from(hoje());
        BigDecimal aReceberMes = dtos.stream().flatMap(a -> a.getProventos().stream())
                .filter(p -> !p.isRecebido() && YearMonth.from(p.getDtPagamento()).equals(mes))
                .map(FinanceiroAtivoDto.Provento::getVlTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FinanceiroInvestimentoDto(dtos, investido, patrimonio, patrimonio.subtract(investido),
                percentual(patrimonio.subtract(investido), investido), aReceber, aReceberMes,
                dtos.stream().anyMatch(FinanceiroAtivoDto::isCotacaoAoVivo));
    }

    /** Monta os DTOs; com `ao vivo` busca (e guarda) as cotações do provedor, senão usa a última guardada. */
    private List<FinanceiroAtivoDto> montar(List<FinanceiroAtivo> ativos, boolean aoVivo) {
        if (ativos.isEmpty()) return new ArrayList<>();
        List<Long> ids = ativos.stream().map(FinanceiroAtivo::getId).collect(Collectors.toList());
        Map<Long, List<FinanceiroOperacao>> operacoes = repository.listarOperacoes(ids).stream()
                .collect(Collectors.groupingBy(FinanceiroOperacao::getIdAtivo));
        Map<Long, List<FinanceiroProvento>> proventos = repository.listarProventos(ids).stream()
                .collect(Collectors.groupingBy(FinanceiroProvento::getIdAtivo));
        Map<String, Cotacao> cotacoes = aoVivo ? cotacoesAoVivo(ativos, operacoes) : Collections.emptyMap();

        List<FinanceiroAtivoDto> dtos = new ArrayList<>();
        for (FinanceiroAtivo a : ativos) {
            Cotacao viva = cotacoes.get(a.getCdTicker());
            if (viva != null) {
                LocalDateTime quando = LocalDateTime.ofInstant(viva.getMomento(), FUSO);
                repository.atualizarCotacao(a.getId(), viva.getPreco(), quando);
                a.setVlCotacao(viva.getPreco());
                a.setDhCotacao(quando);
            }
            dtos.add(dto(a, operacoes.getOrDefault(a.getId(), Collections.emptyList()),
                    proventos.getOrDefault(a.getId(), Collections.emptyList()), viva != null));
        }
        return dtos;
    }

    private Map<String, Cotacao> cotacoesAoVivo(List<FinanceiroAtivo> ativos, Map<Long, List<FinanceiroOperacao>> operacoes) {
        Set<String> tickers = new LinkedHashSet<>();
        for (FinanceiroAtivo a : ativos) {
            if (!operacoes.getOrDefault(a.getId(), Collections.emptyList()).isEmpty()) tickers.add(a.getCdTicker());
        }
        return tickers.isEmpty() ? Collections.emptyMap() : cotacaoService.cotar(tickers);
    }

    private FinanceiroAtivoDto dto(FinanceiroAtivo a, List<FinanceiroOperacao> operacoes, List<FinanceiroProvento> proventos, boolean aoVivo) {
        Posicao posicao = PosicaoCalculator.calcular(operacoes);
        int cotas = posicao == null ? 0 : posicao.getCotas();
        BigDecimal custo = posicao == null ? BigDecimal.ZERO : posicao.getCusto();
        BigDecimal preco = a.getVlCotacao();
        BigDecimal patrimonio = preco == null ? custo : preco.multiply(BigDecimal.valueOf(cotas)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal resultado = patrimonio.subtract(custo);

        LocalDate hoje = hoje();
        BigDecimal aReceber = BigDecimal.ZERO;
        List<FinanceiroAtivoDto.Provento> itens = new ArrayList<>();
        for (FinanceiroProvento p : proventos) {
            int cotasNaData = PosicaoCalculator.cotasEm(operacoes, p.getDtCom());
            BigDecimal total = p.getVlPorCota().multiply(BigDecimal.valueOf(cotasNaData)).setScale(2, RoundingMode.HALF_UP);
            boolean recebido = p.getDtPagamento().isBefore(hoje);
            if (!recebido) aReceber = aReceber.add(total);
            itens.add(new FinanceiroAtivoDto.Provento(p.getId(), p.getDtCom(), p.getDtPagamento(), p.getVlPorCota(),
                    cotasNaData, total, recebido, p.getCdOrigem()));
        }
        List<FinanceiroAtivoDto.Operacao> ops = operacoes.stream()
                .map(o -> new FinanceiroAtivoDto.Operacao(o.getId(), o.getDtOperacao(), o.getCdTipo(), o.getQtCotas(), o.getVlPreco()))
                .collect(Collectors.toList());
        Collections.reverse(ops);
        return new FinanceiroAtivoDto(a.getId(), a.getCdTicker(), a.getNmAtivo(), a.getIdPessoa(), a.getNmPessoa(), cotas,
                posicao == null ? BigDecimal.ZERO : posicao.getPrecoMedio(), custo, preco,
                a.getDhCotacao() == null ? null : a.getDhCotacao().format(HORA), aoVivo, patrimonio, resultado,
                percentual(resultado, custo), aReceber, ops, itens, a.getNrVersao());
    }

    // ---------- fundos ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroAtivoDto criarAtivo(FinanceiroAtivoForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        String ticker = ticker(form.getCdTicker());
        if (form.getIdPessoa() == null || !financeiroRepository.buscarPessoa(form.getIdPessoa()).isPresent()) {
            throw invalido("Escolha de quem é o fundo.");
        }
        FinanceiroAtivo existente = repository.buscarAtivoPorTicker(ticker, form.getIdPessoa()).orElse(null);
        if (existente != null) {
            if (Boolean.FALSE.equals(existente.getInAtivo())) {
                existente.setInAtivo(true);
                repository.alterarAtivo(existente, existente.getNrVersao());
                return um(existente.getId());
            }
            throw invalido("Esse fundo já está na carteira dessa pessoa.");
        }
        FinanceiroAtivo novo = new FinanceiroAtivo();
        novo.setCdTicker(ticker);
        novo.setNmAtivo(texto(form.getNmAtivo()));
        novo.setIdPessoa(form.getIdPessoa());
        return um(repository.inserirAtivo(novo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroAtivoDto atualizarAtivo(Long id, FinanceiroAtivoForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroAtivo atual = ativo(id);
        if (form.getNrVersao() == null) throw invalido("Informe a versão do fundo (nrVersao).");
        if (form.getNmAtivo() != null) atual.setNmAtivo(texto(form.getNmAtivo()));
        if (form.getInAtivo() != null) atual.setInAtivo(form.getInAtivo());
        if (repository.alterarAtivo(atual, form.getNrVersao()) == 0) {
            throw new ConflitoException(MSG_CONFLITO);
        }
        return um(id);
    }

    // ---------- operações ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroAtivoDto registrarOperacao(Long idAtivo, FinanceiroOperacaoForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroAtivo ativo = ativo(idAtivo);
        String requisicao = requisicao(form.getCdRequisicao());
        if (repository.buscarAtivoDaRequisicao(requisicao).isPresent()) return um(idAtivo);

        String tipo = form.getCdTipo() == null ? "" : form.getCdTipo().trim().toUpperCase();
        if (!PosicaoCalculator.COMPRA.equals(tipo) && !PosicaoCalculator.VENDA.equals(tipo)) throw invalido("Informe se é compra ou venda.");
        if (form.getQtCotas() == null || form.getQtCotas() < 1) throw invalido("A quantidade de cotas deve ser de pelo menos 1.");
        if (form.getVlPreco() == null || form.getVlPreco().signum() <= 0) throw invalido("O preço por cota deve ser maior que zero.");

        FinanceiroOperacao nova = new FinanceiroOperacao();
        nova.setCdRequisicao(requisicao);
        nova.setIdAtivo(ativo.getId());
        nova.setDtOperacao(data(form.getDtOperacao(), "operação"));
        nova.setCdTipo(tipo);
        nova.setQtCotas(form.getQtCotas());
        nova.setVlPreco(form.getVlPreco().setScale(2, RoundingMode.HALF_UP));
        List<FinanceiroOperacao> todas = new ArrayList<>(repository.listarOperacoes(Collections.singletonList(idAtivo)));
        todas.add(nova);
        if (PosicaoCalculator.calcular(todas) == null) throw invalido("Não há cotas suficientes para essa venda.");
        repository.inserirOperacao(nova);
        return um(idAtivo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroAtivoDto excluirOperacao(Long idOperacao) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroOperacao op = repository.buscarOperacao(idOperacao)
                .orElseThrow(() -> new NaoEncontradoException("Operação não encontrada"));
        List<FinanceiroOperacao> restantes = repository.listarOperacoes(Collections.singletonList(op.getIdAtivo())).stream()
                .filter(o -> !o.getId().equals(idOperacao)).collect(Collectors.toList());
        if (PosicaoCalculator.calcular(restantes) == null) throw invalido("Excluir essa compra deixaria uma venda sem cotas. Exclua a venda antes.");
        repository.excluirOperacao(idOperacao);
        return um(op.getIdAtivo());
    }

    // ---------- proventos ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroAtivoDto registrarProvento(Long idAtivo, FinanceiroProventoForm form) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        ativo(idAtivo);
        FinanceiroProvento p = new FinanceiroProvento();
        p.setIdAtivo(idAtivo);
        p.setDtCom(data(form.getDtCom(), "data-com"));
        p.setDtPagamento(data(form.getDtPagamento(), "pagamento"));
        if (p.getDtPagamento().isBefore(p.getDtCom())) throw invalido("O pagamento não pode ser antes da data-com.");
        if (form.getVlPorCota() == null || form.getVlPorCota().signum() <= 0) throw invalido("O valor por cota deve ser maior que zero.");
        p.setVlPorCota(form.getVlPorCota().setScale(6, RoundingMode.HALF_UP));
        repository.gravarProventoManual(p);
        return um(idAtivo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceiroAtivoDto excluirProvento(Long idProvento) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroProvento p = repository.buscarProvento(idProvento)
                .orElseThrow(() -> new NaoEncontradoException("Provento não encontrado"));
        repository.excluirProvento(idProvento);
        return um(p.getIdAtivo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sincronizarProventos(Long idAtivo) {
        autorizacao.exigir(Modulo.FINANCEIRO, Acao.ESCRITA);
        FinanceiroAtivo ativo = ativo(idAtivo);
        List<FinanceiroOperacao> operacoes = repository.listarOperacoes(Collections.singletonList(idAtivo));
        if (operacoes.isEmpty()) throw invalido("Registre uma compra antes de importar os proventos.");
        LocalDate primeira = operacoes.stream().map(FinanceiroOperacao::getDtOperacao).min(LocalDate::compareTo).orElse(hoje());
        List<ProventoCotado> cotados = cotacaoService.proventos(ativo.getCdTicker());
        int gravados = 0;
        for (ProventoCotado c : cotados) {
            if (c.getDtPagamento().isBefore(primeira) || PosicaoCalculator.cotasEm(operacoes, c.getDtCom()) == 0) continue;
            FinanceiroProvento p = new FinanceiroProvento();
            p.setIdAtivo(idAtivo);
            p.setDtCom(c.getDtCom());
            p.setDtPagamento(c.getDtPagamento());
            p.setVlPorCota(c.getValorPorCota().setScale(6, RoundingMode.HALF_UP));
            if (repository.gravarProventoImportado(p)) gravados++;
        }
        return gravados;
    }

    // ---------- apoio ----------

    private FinanceiroAtivoDto um(Long id) {
        return montar(Collections.singletonList(ativo(id)), false).get(0);
    }

    private FinanceiroAtivo ativo(Long id) {
        return repository.buscarAtivo(id)
                .orElseThrow(() -> new NaoEncontradoException("Fundo não encontrado"));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock.withZone(FUSO));
    }

    private static String ticker(String bruto) {
        String t = bruto == null ? "" : bruto.trim().toUpperCase();
        if (!TICKER.matcher(t).matches()) throw invalido("Informe o código do fundo, como HGLG11.");
        return t;
    }

    private static String texto(String bruto) {
        return bruto == null || bruto.trim().isEmpty() ? null : bruto.trim();
    }

    private static String requisicao(String bruto) {
        try {
            return UUID.fromString(bruto == null ? "" : bruto.trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw invalido("Identificador da requisição inválido.");
        }
    }

    private static LocalDate data(String bruto, String campo) {
        try {
            return LocalDate.parse(bruto == null ? "" : bruto.trim());
        } catch (DateTimeParseException ex) {
            throw invalido("Data da " + campo + " inválida (use yyyy-MM-dd).");
        }
    }

    private static NordException invalido(String mensagem) {
        return new EntradaInvalidaException(mensagem);
    }

    private static BigDecimal soma(List<FinanceiroAtivoDto> itens, java.util.function.Function<FinanceiroAtivoDto, BigDecimal> campo) {
        return itens.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal base) {
        return base.signum() == 0 ? BigDecimal.ZERO : parte.multiply(CEM).divide(base, 2, RoundingMode.HALF_UP);
    }
}
