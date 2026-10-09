package br.com.nord_tool_backend.storage;

import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;

/** Validação de uploads por conteúdo (magic bytes), tamanho e nome. */
public final class ArquivoValidador {

    public static final int MAX_PDF_BYTES = 15 * 1024 * 1024;
    public static final int MAX_IMAGEM_BYTES = 5 * 1024 * 1024;
    public static final int MAX_NOME = 180;
    public static final int MAX_CONTRATO_BYTES = 15 * 1024 * 1024;

    public static final String PDF = "application/pdf";
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";

    private ArquivoValidador() {
    }

    /** Valida um PDF e devolve o content-type. */
    public static String validarPdf(String nome, byte[] bytes) {
        validarNome(nome);
        validarTamanho(bytes, MAX_PDF_BYTES, "O PDF deve ter no máximo 15 MB");
        if (!comecaCom(bytes, '%', 'P', 'D', 'F')) {
            throw erro("O arquivo enviado não é um PDF válido");
        }
        return PDF;
    }

    /** Valida uma imagem JPEG ou PNG e devolve o content-type detectado pelo conteúdo. */
    public static String validarImagem(String nome, byte[] bytes) {
        validarNome(nome);
        validarTamanho(bytes, MAX_IMAGEM_BYTES, "A imagem deve ter no máximo 5 MB");
        if (comecaCom(bytes, 0xFF, 0xD8, 0xFF)) return JPEG;
        if (comecaCom(bytes, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
        throw erro("A imagem deve ser JPEG ou PNG");
    }

    /** Contrato/comprovante: PDF ou imagem JPEG/PNG de até 15 MB. Devolve o content-type detectado pelo conteúdo. */
    public static String validarContrato(String nome, byte[] bytes) {
        validarNome(nome);
        validarTamanho(bytes, MAX_CONTRATO_BYTES, "O arquivo deve ter no máximo 15 MB");
        if (comecaCom(bytes, '%', 'P', 'D', 'F')) return PDF;
        if (comecaCom(bytes, 0xFF, 0xD8, 0xFF)) return JPEG;
        if (comecaCom(bytes, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
        throw erro("O arquivo deve ser um PDF ou uma imagem JPEG/PNG");
    }

    /**
     * Comprovante da Caixinha: somente PDF, por assinatura `%PDF-` no início e `%%EOF` nos últimos 1024 bytes
     * (a mesma checagem do Lugia). O limite de tamanho é configurável.
     */
    public static String validarComprovantePdf(String nome, byte[] bytes, int maxBytes) {
        validarNome(nome);
        validarTamanho(bytes, maxBytes, "O comprovante deve ter no máximo " + (maxBytes / (1024 * 1024)) + " MB");
        if (!comecaCom(bytes, '%', 'P', 'D', 'F', '-') || !terminaComEof(bytes)) {
            throw erro("O comprovante deve ser um PDF válido");
        }
        return PDF;
    }

    private static boolean terminaComEof(byte[] bytes) {
        int inicio = Math.max(0, bytes.length - 1024);
        String cauda = new String(bytes, inicio, bytes.length - inicio, java.nio.charset.StandardCharsets.ISO_8859_1);
        return cauda.contains("%%EOF");
    }

    /** Nome com no máximo 180 caracteres e sem caracteres de controle. */
    public static void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw erro("Informe o nome do arquivo");
        }
        if (nome.length() > MAX_NOME) {
            throw erro("O nome do arquivo deve ter no máximo " + MAX_NOME + " caracteres");
        }
        for (int i = 0; i < nome.length(); i++) {
            if (Character.isISOControl(nome.charAt(i))) {
                throw erro("O nome do arquivo contém caracteres inválidos");
            }
        }
    }

    private static void validarTamanho(byte[] bytes, int max, String mensagemMax) {
        if (bytes == null || bytes.length == 0) throw erro("Arquivo vazio");
        if (bytes.length > max) throw erro(mensagemMax);
    }

    private static boolean comecaCom(byte[] bytes, int... assinatura) {
        if (bytes.length < assinatura.length) return false;
        for (int i = 0; i < assinatura.length; i++) {
            if ((bytes[i] & 0xFF) != assinatura[i]) return false;
        }
        return true;
    }

    private static NordException erro(String mensagem) {
        return new EntradaInvalidaException(mensagem);
    }
}
