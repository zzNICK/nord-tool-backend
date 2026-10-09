package br.com.nord_tool_backend.storage;

import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.ArquivoArmazenado;
import br.com.nord_tool_backend.repository.ArquivoArmazenadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Provisório: guarda os bytes em arquivo_armazenado.bin_conteudo (BYTEA). */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nord-tool.storage.provider", havingValue = "POSTGRES", matchIfMissing = true)
public class ArmazenamentoPostgresServiceImpl implements ArmazenamentoService {

    static final String PROVEDOR = "POSTGRES";

    private final ArquivoArmazenadoRepository repository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long salvar(String nome, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new EntradaInvalidaException("Arquivo vazio");
        }
        ArquivoArmazenado arquivo = new ArquivoArmazenado();
        arquivo.setNmProvedor(PROVEDOR);
        arquivo.setNmArquivo(nome);
        arquivo.setNmContentType(contentType);
        arquivo.setNrTamanhoBytes((long) bytes.length);
        arquivo.setNmHashSha256(sha256(bytes));
        arquivo.setBinConteudo(bytes);
        return repository.inserir(arquivo).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoConteudo abrir(Long idArquivo) {
        ArquivoArmazenado arquivo = repository.buscarComConteudo(idArquivo)
                .orElseThrow(() -> new NaoEncontradoException("Arquivo não encontrado"));
        return new ArquivoConteudo(arquivo.getNmArquivo(), arquivo.getNmContentType(),
                arquivo.getNrTamanhoBytes() == null ? 0 : arquivo.getNrTamanhoBytes(), arquivo.getBinConteudo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apagar(Long idArquivo) {
        if (idArquivo != null) repository.deletar(idArquivo);
    }

    static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }
}
