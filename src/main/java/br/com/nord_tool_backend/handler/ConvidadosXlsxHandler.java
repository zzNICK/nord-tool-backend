package br.com.nord_tool_backend.handler;

import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.enums.StatusConvidadoEnum;
import br.com.nord_tool_backend.form.CasamentoConvidadoForm;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê a planilha de convidados (.xlsx). A primeira linha é o cabeçalho; colunas reconhecidas
 * (sem diferenciar caixa/acentos): Nome, Grupo, Telefone, Relação, Status, Acompanhantes, Mesa.
 */
@Component
public class ConvidadosXlsxHandler {

    public static final int MAX_LINHAS = 2000;

    public static class LinhaLida {
        private final int linha;
        private final CasamentoConvidadoForm form;
        private final String erro;

        LinhaLida(int linha, CasamentoConvidadoForm form, String erro) {
            this.linha = linha;
            this.form = form;
            this.erro = erro;
        }

        public int getLinha() { return linha; }
        public CasamentoConvidadoForm getForm() { return form; }
        public String getErro() { return erro; }
        public boolean valida() { return erro == null; }
    }

    public List<LinhaLida> ler(byte[] bytes) {
        try (Workbook planilha = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet aba = planilha.getSheetAt(0);
            Row cabecalho = aba.getRow(aba.getFirstRowNum());
            Map<String, Integer> colunas = mapearColunas(cabecalho);
            if (!colunas.containsKey("nome")) {
                throw new EntradaInvalidaException("A planilha precisa ter a coluna Nome na primeira linha");
            }
            if (aba.getLastRowNum() - aba.getFirstRowNum() > MAX_LINHAS) {
                throw new EntradaInvalidaException("A planilha tem linhas demais (máximo " + MAX_LINHAS + ")");
            }
            DataFormatter formatador = new DataFormatter();
            List<LinhaLida> linhas = new ArrayList<>();
            for (int i = aba.getFirstRowNum() + 1; i <= aba.getLastRowNum(); i++) {
                Row row = aba.getRow(i);
                if (row == null) continue;
                Map<String, String> valores = new HashMap<>();
                boolean algum = false;
                for (Map.Entry<String, Integer> coluna : colunas.entrySet()) {
                    String texto = row.getCell(coluna.getValue()) == null ? "" : formatador.formatCellValue(row.getCell(coluna.getValue())).trim();
                    valores.put(coluna.getKey(), texto);
                    if (!texto.isEmpty()) algum = true;
                }
                if (!algum) continue;
                linhas.add(converter(i + 1, valores));
            }
            return linhas;
        } catch (NordException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EntradaInvalidaException("Planilha inválida. Envie um arquivo .xlsx", ex);
        }
    }

    private LinhaLida converter(int numeroLinha, Map<String, String> v) {
        String nome = v.getOrDefault("nome", "");
        if (nome.isEmpty()) return erro(numeroLinha, "Nome é obrigatório");
        if (nome.length() > 150) return erro(numeroLinha, "Nome com mais de 150 caracteres");
        String grupo = v.getOrDefault("grupo", "");
        if (grupo.length() > 80) return erro(numeroLinha, "Grupo com mais de 80 caracteres");
        String telefone = v.getOrDefault("telefone", "");
        if (telefone.length() > 30) return erro(numeroLinha, "Telefone com mais de 30 caracteres");
        String relacao = v.getOrDefault("relacao", "");
        if (relacao.length() > 80) return erro(numeroLinha, "Relação com mais de 80 caracteres");
        String mesa = v.getOrDefault("mesa", "");
        if (mesa.length() > 40) return erro(numeroLinha, "Mesa com mais de 40 caracteres");

        String status = v.getOrDefault("status", "");
        StatusConvidadoEnum statusEnum = StatusConvidadoEnum.NAO_CONVIDADO;
        if (!status.isEmpty()) {
            statusEnum = StatusConvidadoEnum.de(status).orElse(null);
            if (statusEnum == null) return erro(numeroLinha, "Status inválido: " + status);
        }

        int acompanhantes = 0;
        String textoAc = v.getOrDefault("acompanhantes", "");
        if (!textoAc.isEmpty()) {
            try {
                double numero = Double.parseDouble(textoAc.replace(",", "."));
                if (numero != Math.floor(numero)) return erro(numeroLinha, "Acompanhantes deve ser um número inteiro");
                acompanhantes = (int) numero;
            } catch (NumberFormatException ex) {
                return erro(numeroLinha, "Acompanhantes inválido: " + textoAc);
            }
            if (acompanhantes < 0 || acompanhantes > 50) return erro(numeroLinha, "Acompanhantes deve estar entre 0 e 50");
        }

        CasamentoConvidadoForm form = new CasamentoConvidadoForm();
        form.setNmConvidado(nome);
        form.setNmGrupo(grupo.isEmpty() ? null : grupo);
        form.setNrTelefone(telefone.isEmpty() ? null : telefone);
        form.setNmRelacao(relacao.isEmpty() ? null : relacao);
        form.setNmStatus(statusEnum.name());
        form.setNrAcompanhantes(acompanhantes);
        form.setNmMesa(mesa.isEmpty() ? null : mesa);
        return new LinhaLida(numeroLinha, form, null);
    }

    private static LinhaLida erro(int linha, String motivo) {
        return new LinhaLida(linha, null, motivo);
    }

    private static Map<String, Integer> mapearColunas(Row cabecalho) {
        Map<String, Integer> colunas = new HashMap<>();
        if (cabecalho == null) return colunas;
        DataFormatter formatador = new DataFormatter();
        for (int c = cabecalho.getFirstCellNum(); c >= 0 && c < cabecalho.getLastCellNum(); c++) {
            if (cabecalho.getCell(c) == null) continue;
            String chave = normalizar(formatador.formatCellValue(cabecalho.getCell(c)));
            if (chave.equals("convidado")) chave = "nome";
            if (chave.equals("acompanhante")) chave = "acompanhantes";
            if (!chave.isEmpty()) colunas.putIfAbsent(chave, c);
        }
        return colunas;
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
