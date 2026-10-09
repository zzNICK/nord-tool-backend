package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.CasamentoMarco;
import br.com.nord_tool_backend.dto.CasamentoMarcoDto;
import br.com.nord_tool_backend.form.CasamentoMarcoForm;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import br.com.nord_tool_backend.service.CasamentoMarcoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasamentoMarcoServiceImpl implements CasamentoMarcoService {

    /** Marcos padrão do Lugia Center (mesmos títulos e prazos fixos). */
    static final String[][] MARCOS_PADRAO = {
            {"Definir orçamento total e reserva", "2026-10-12"},
            {"Finalizar lista inicial de convidados", "2026-11-12"},
            {"Contratar espaço da recepção", "2026-10-12"},
            {"Contratar buffet", "2027-01-12"},
            {"Contratar fotografia e vídeo", "2027-02-12"},
            {"Definir cerimônia e celebrante", "2027-03-12"},
            {"Escolher vestido e trajes", "2027-04-12"},
            {"Comprar alianças", "2027-06-12"},
            {"Enviar convites e abrir confirmações", "2027-07-12"},
            {"Confirmar logística e cronograma final", "2027-09-12"},
    };

    private final CasamentoRepository repository;

    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(readOnly = true)
    public List<CasamentoMarcoDto> listar() {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        return repository.listarMarcos().stream().map(CasamentoMarcoDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CasamentoMarcoDto buscar(Long id) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        return CasamentoMarcoDto.de(marco(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoMarcoDto criar(CasamentoMarcoForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        CasamentoMarco novo = converter(form);
        novo.setInConcluido(false);
        return CasamentoMarcoDto.de(marco(repository.inserirMarco(novo)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoMarcoDto alterar(Long id, CasamentoMarcoForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        marco(id);
        CasamentoMarco novo = converter(form);
        novo.setId(id);
        repository.alterarMarco(novo);
        return CasamentoMarcoDto.de(marco(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoMarcoDto concluir(Long id, boolean concluido) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        marco(id);
        repository.concluirMarco(id, concluido);
        return CasamentoMarcoDto.de(marco(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletar(Long id) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        marco(id);
        repository.deletarMarco(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CasamentoMarcoDto> criarPadrao() {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        if (repository.contarMarcos() > 0) {
            throw new EntradaInvalidaException("Os marcos padrão só podem ser criados quando não há marcos cadastrados");
        }
        for (String[] padrao : MARCOS_PADRAO) {
            CasamentoMarco m = new CasamentoMarco();
            m.setNmTitulo(padrao[0]);
            m.setDtPrazo(LocalDate.parse(padrao[1]));
            m.setInConcluido(false);
            repository.inserirMarco(m);
        }
        return listar();
    }

    private CasamentoMarco marco(Long id) {
        return repository.buscarMarco(id)
                .orElseThrow(() -> new NaoEncontradoException("Marco não encontrado"));
    }

    private CasamentoMarco converter(CasamentoMarcoForm form) {
        CasamentoMarco m = new CasamentoMarco();
        m.setNmTitulo(form.getNmTitulo().trim());
        m.setDtPrazo(form.getDtPrazo());
        m.setTxObservacao(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getTxObservacao()));
        return m;
    }
}
