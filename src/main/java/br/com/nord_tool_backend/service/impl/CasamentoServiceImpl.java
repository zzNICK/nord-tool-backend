package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.dto.CasamentoConfiguracaoDto;
import br.com.nord_tool_backend.dto.CasamentoDashboardDto;
import br.com.nord_tool_backend.dto.CasamentoMarcoDto;
import br.com.nord_tool_backend.dto.CasamentoTotaisDto;
import br.com.nord_tool_backend.form.CasamentoConfiguracaoForm;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import br.com.nord_tool_backend.service.CasamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasamentoServiceImpl implements CasamentoService {

    static final String CHAVE_CASAL = "casal";
    static final String CHAVE_DATA = "dataCasamento";
    static final int QT_PROXIMOS_MARCOS = 5;

    private final CasamentoRepository repository;

    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(readOnly = true)
    public CasamentoConfiguracaoDto buscarConfiguracao() {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        Map<String, String> config = repository.listarConfiguracao();
        return new CasamentoConfiguracaoDto(config.get(CHAVE_CASAL), config.get(CHAVE_DATA));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoConfiguracaoDto salvarConfiguracao(CasamentoConfiguracaoForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        try {
            LocalDate.parse(form.getDataCasamento());
        } catch (DateTimeParseException ex) {
            throw new EntradaInvalidaException("Data do casamento inválida");
        }
        repository.salvarConfiguracao(CHAVE_CASAL, form.getCasal().trim());
        repository.salvarConfiguracao(CHAVE_DATA, form.getDataCasamento());
        return buscarConfiguracao();
    }

    @Override
    @Transactional(readOnly = true)
    public CasamentoDashboardDto dashboard() {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        CasamentoTotaisDto t = repository.buscarTotais();
        List<CasamentoMarcoDto> proximos = repository.listarProximosMarcos(QT_PROXIMOS_MARCOS).stream()
                .map(CasamentoMarcoDto::de).collect(Collectors.toList());
        int marcos = valor(t.getQtMarcos());
        int concluidos = valor(t.getQtMarcosConcluidos());
        int percentual = marcos == 0 ? 0 : (int) Math.round(concluidos * 100.0 / marcos);
        return new CasamentoDashboardDto(
                buscarConfiguracao(),
                valor(t.getQtFornecedores()),
                valor(t.getQtFornecedoresContratados()),
                t.getVlContratado() == null ? BigDecimal.ZERO : t.getVlContratado(),
                valor(t.getQtConvidados()),
                valor(t.getQtConvidadosConfirmados()),
                valor(t.getQtPessoasConfirmadas()),
                marcos,
                concluidos,
                percentual,
                proximos);
    }

    private static int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }

}
