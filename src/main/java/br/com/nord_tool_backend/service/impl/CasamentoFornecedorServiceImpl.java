package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.CasamentoAnexo;
import br.com.nord_tool_backend.domain.CasamentoFornecedor;
import br.com.nord_tool_backend.domain.enums.StatusFornecedorEnum;
import br.com.nord_tool_backend.dto.CasamentoAnexoDto;
import br.com.nord_tool_backend.dto.CasamentoFornecedorDto;
import br.com.nord_tool_backend.form.CasamentoFornecedorForm;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import br.com.nord_tool_backend.service.CasamentoFornecedorService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasamentoFornecedorServiceImpl implements CasamentoFornecedorService {

    static final int MAX_DESCRICAO = 200;
    static final int MAX_ANEXOS_POR_FORNECEDOR = 20;

    private final CasamentoRepository repository;
    private final ArmazenamentoService armazenamento;

    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(readOnly = true)
    public List<CasamentoFornecedorDto> listar() {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        return repository.listarFornecedores().stream().map(CasamentoFornecedorDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CasamentoFornecedorDto buscar(Long id) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        return CasamentoFornecedorDto.de(fornecedor(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoFornecedorDto criar(CasamentoFornecedorForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        Long id = repository.inserirFornecedor(converter(form));
        return CasamentoFornecedorDto.de(fornecedor(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoFornecedorDto alterar(Long id, CasamentoFornecedorForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        fornecedor(id);
        CasamentoFornecedor novo = converter(form);
        novo.setId(id);
        repository.alterarFornecedor(novo);
        return CasamentoFornecedorDto.de(fornecedor(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletar(Long id) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        fornecedor(id);
        List<Long> arquivos = repository.listarAnexos(id).stream()
                .map(CasamentoAnexo::getIdArquivo).collect(Collectors.toList());
        // As linhas dos anexos saem em cascata; os arquivos armazenados são apagados explicitamente.
        repository.deletarFornecedor(id);
        arquivos.forEach(armazenamento::apagar);
    }

    // ---------- anexos ----------

    @Override
    @Transactional(readOnly = true)
    public List<CasamentoAnexoDto> listarAnexos(Long idFornecedor) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        fornecedor(idFornecedor);
        return repository.listarAnexos(idFornecedor).stream().map(CasamentoAnexoDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoAnexoDto anexar(Long idFornecedor, String nomeArquivo, byte[] bytes, String descricao) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        fornecedor(idFornecedor);
        if (repository.listarAnexos(idFornecedor).size() >= MAX_ANEXOS_POR_FORNECEDOR) {
            throw new EntradaInvalidaException("O fornecedor já tem o máximo de " + MAX_ANEXOS_POR_FORNECEDOR + " anexos");
        }
        String contentType = ArquivoValidador.validarContrato(nomeArquivo, bytes);
        String texto = descricao == null || descricao.trim().isEmpty() ? null : descricao.trim();
        if (texto != null && texto.length() > MAX_DESCRICAO) {
            throw new EntradaInvalidaException("A descrição deve ter no máximo " + MAX_DESCRICAO + " caracteres");
        }
        Long idArquivo = armazenamento.salvar(nomeArquivo, contentType, bytes);
        Long idAnexo = repository.inserirAnexo(idFornecedor, idArquivo, texto);
        return CasamentoAnexoDto.de(anexo(idAnexo));
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarAnexo(Long idAnexo) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        CasamentoAnexo anexo = anexo(idAnexo);
        long versao = anexo.getDhCriacao() == null ? 0L
                : anexo.getDhCriacao().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new ArquivoDownload(armazenamento.abrir(anexo.getIdArquivo()), versao);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirAnexo(Long idAnexo) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        CasamentoAnexo anexo = anexo(idAnexo);
        repository.deletarAnexo(idAnexo);
        armazenamento.apagar(anexo.getIdArquivo());
    }

    // ---------- auxiliares ----------

    private CasamentoFornecedor fornecedor(Long id) {
        return repository.buscarFornecedor(id)
                .orElseThrow(() -> new NaoEncontradoException("Fornecedor não encontrado"));
    }

    private CasamentoAnexo anexo(Long id) {
        return repository.buscarAnexo(id)
                .orElseThrow(() -> new NaoEncontradoException("Anexo não encontrado"));
    }

    CasamentoFornecedor converter(CasamentoFornecedorForm form) {
        StatusFornecedorEnum status = form.getNmStatus() == null || form.getNmStatus().trim().isEmpty()
                ? StatusFornecedorEnum.PESQUISANDO
                : StatusFornecedorEnum.de(form.getNmStatus()).orElseThrow(() -> new EntradaInvalidaException("Status inválido. Use PESQUISANDO, ORCAMENTO ou CONTRATADO."));
        CasamentoFornecedor f = new CasamentoFornecedor();
        f.setNmFornecedor(form.getNmFornecedor().trim());
        f.setNmCategoria(form.getNmCategoria().trim());
        f.setTxContato(vazioParaNulo(form.getTxContato()));
        f.setNmStatus(status.name());
        f.setVlValor(form.getVlValor() == null ? BigDecimal.ZERO : form.getVlValor());
        f.setTxObservacao(vazioParaNulo(form.getTxObservacao()));
        return f;
    }

    static String vazioParaNulo(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }
}
