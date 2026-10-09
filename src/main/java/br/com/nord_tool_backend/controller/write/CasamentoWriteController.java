package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.CasamentoResponse;
import br.com.nord_tool_backend.dto.CasamentoAnexoDto;
import br.com.nord_tool_backend.dto.CasamentoConfiguracaoDto;
import br.com.nord_tool_backend.dto.CasamentoConvidadoDto;
import br.com.nord_tool_backend.dto.CasamentoFornecedorDto;
import br.com.nord_tool_backend.dto.CasamentoMarcoDto;
import br.com.nord_tool_backend.dto.ImportacaoConvidadosDto;
import br.com.nord_tool_backend.form.CasamentoConfiguracaoForm;
import br.com.nord_tool_backend.form.CasamentoConvidadoForm;
import br.com.nord_tool_backend.form.CasamentoFornecedorForm;
import br.com.nord_tool_backend.form.CasamentoMarcoConcluidoForm;
import br.com.nord_tool_backend.form.CasamentoMarcoForm;
import br.com.nord_tool_backend.service.CasamentoConvidadoService;
import br.com.nord_tool_backend.service.CasamentoFornecedorService;
import br.com.nord_tool_backend.service.CasamentoMarcoService;
import br.com.nord_tool_backend.service.CasamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/casamento")
@Tag(name = "Casamento", description = "Configuração, dashboard, fornecedores, convidados e marcos")
public class CasamentoWriteController implements CasamentoResponse {

    private final CasamentoService casamentoService;
    private final CasamentoFornecedorService fornecedorService;
    private final CasamentoConvidadoService convidadoService;
    private final CasamentoMarcoService marcoService;

    @Operation(summary = "Salva casal e data do casamento")
    @PutMapping("/configuracao")
    public ResponseEntity<ApiResponseBody<CasamentoConfiguracaoDto>> salvarConfiguracao(@Valid @RequestBody CasamentoConfiguracaoForm form) {
        return ok(casamentoService.salvarConfiguracao(form));
    }

    // ---------- fornecedores ----------

    @Operation(summary = "Cria um fornecedor")
    @PostMapping("/fornecedores")
    public ResponseEntity<ApiResponseBody<CasamentoFornecedorDto>> criarFornecedor(@Valid @RequestBody CasamentoFornecedorForm form) {
        return created(fornecedorService.criar(form));
    }

    @Operation(summary = "Edita um fornecedor")
    @PutMapping("/fornecedores/{id}")
    public ResponseEntity<ApiResponseBody<CasamentoFornecedorDto>> alterarFornecedor(@PathVariable Long id, @Valid @RequestBody CasamentoFornecedorForm form) {
        return ok(fornecedorService.alterar(id, form));
    }

    @Operation(summary = "Exclui o fornecedor, seus anexos e os arquivos")
    @DeleteMapping("/fornecedores/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarFornecedor(@PathVariable Long id) {
        fornecedorService.deletar(id);
        return noContent();
    }

    @Operation(summary = "Anexa contrato/comprovante (PDF ou imagem, até 15 MB)")
    @PostMapping(value = "/fornecedores/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<CasamentoAnexoDto>> anexar(
            @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "descricao", required = false) String descricao) {
        return created(fornecedorService.anexar(id, nome(arquivo), bytes(arquivo), descricao));
    }

    @Operation(summary = "Exclui um anexo e o arquivo")
    @DeleteMapping("/fornecedores/anexos/{idAnexo}")
    public ResponseEntity<ApiResponseBody<Void>> excluirAnexo(@PathVariable Long idAnexo) {
        fornecedorService.excluirAnexo(idAnexo);
        return noContent();
    }

    // ---------- convidados ----------

    @Operation(summary = "Cria um convidado")
    @PostMapping("/convidados")
    public ResponseEntity<ApiResponseBody<CasamentoConvidadoDto>> criarConvidado(@Valid @RequestBody CasamentoConvidadoForm form) {
        return created(convidadoService.criar(form));
    }

    @Operation(summary = "Edita um convidado")
    @PutMapping("/convidados/{id}")
    public ResponseEntity<ApiResponseBody<CasamentoConvidadoDto>> alterarConvidado(@PathVariable Long id, @Valid @RequestBody CasamentoConvidadoForm form) {
        return ok(convidadoService.alterar(id, form));
    }

    @Operation(summary = "Exclui um convidado")
    @DeleteMapping("/convidados/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarConvidado(@PathVariable Long id) {
        convidadoService.deletar(id);
        return noContent();
    }

    @Operation(summary = "Importa convidados de uma planilha .xlsx (devolve o relatório de linhas rejeitadas)")
    @PostMapping(value = "/convidados/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<ImportacaoConvidadosDto>> importarConvidados(@RequestParam("planilha") MultipartFile planilha) {
        return ok(convidadoService.importar(nome(planilha), bytes(planilha)));
    }

    // ---------- marcos ----------

    @Operation(summary = "Cria um marco")
    @PostMapping("/marcos")
    public ResponseEntity<ApiResponseBody<CasamentoMarcoDto>> criarMarco(@Valid @RequestBody CasamentoMarcoForm form) {
        return created(marcoService.criar(form));
    }

    @Operation(summary = "Edita título, prazo e observações do marco")
    @PutMapping("/marcos/{id}")
    public ResponseEntity<ApiResponseBody<CasamentoMarcoDto>> alterarMarco(@PathVariable Long id, @Valid @RequestBody CasamentoMarcoForm form) {
        return ok(marcoService.alterar(id, form));
    }

    @Operation(summary = "Marca ou desmarca o marco como concluído")
    @PutMapping("/marcos/{id}/concluido")
    public ResponseEntity<ApiResponseBody<CasamentoMarcoDto>> concluirMarco(@PathVariable Long id, @Valid @RequestBody CasamentoMarcoConcluidoForm form) {
        return ok(marcoService.concluir(id, form.getConcluido()));
    }

    @Operation(summary = "Exclui um marco")
    @DeleteMapping("/marcos/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarMarco(@PathVariable Long id) {
        marcoService.deletar(id);
        return noContent();
    }

    @Operation(summary = "Cria os 10 marcos padrão (somente se não houver marcos)")
    @PostMapping("/marcos/padrao")
    public ResponseEntity<ApiResponseBody<List<CasamentoMarcoDto>>> marcosPadrao() {
        return created(marcoService.criarPadrao());
    }

    private static String nome(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        return nome == null ? "arquivo" : nome;
    }

    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new EntradaInvalidaException("Não foi possível ler o arquivo enviado", ex);
        }
    }
}
