package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.CaixinhaResponse;
import br.com.nord_tool_backend.dto.CaixinhaComprovanteDto;
import br.com.nord_tool_backend.dto.CaixinhaLancamentoDto;
import br.com.nord_tool_backend.dto.CaixinhaResponsavelDto;
import br.com.nord_tool_backend.form.CaixinhaLancamentoForm;
import br.com.nord_tool_backend.form.CaixinhaMarcacaoForm;
import br.com.nord_tool_backend.form.CaixinhaResponsavelForm;
import br.com.nord_tool_backend.service.CaixinhaService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/caixinha")
@Tag(name = "Caixinha", description = "Controle de despesas, responsáveis e comprovantes em PDF")
public class CaixinhaWriteController implements CaixinhaResponse {

    private final CaixinhaService service;

    @Operation(summary = "Cria um lançamento (cdRequisicao UUID obrigatório; repetido devolve o já criado)")
    @PostMapping("/lancamentos")
    public ResponseEntity<ApiResponseBody<CaixinhaLancamentoDto>> criar(@Valid @RequestBody CaixinhaLancamentoForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Edita um lançamento; exige nrVersao (divergente → 409)")
    @PutMapping("/lancamentos/{id}")
    public ResponseEntity<ApiResponseBody<CaixinhaLancamentoDto>> alterar(@PathVariable Long id, @Valid @RequestBody CaixinhaLancamentoForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Marca/desmarca lançado e/ou pago; exige nrVersao (divergente → 409)")
    @PutMapping("/lancamentos/{id}/marcacao")
    public ResponseEntity<ApiResponseBody<CaixinhaLancamentoDto>> marcar(@PathVariable Long id, @Valid @RequestBody CaixinhaMarcacaoForm form) {
        return ok(service.marcar(id, form));
    }

    @Operation(summary = "Exclui o lançamento e apaga também os PDFs dos comprovantes (nrVersao divergente → 409)")
    @DeleteMapping("/lancamentos/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id, @RequestParam(value = "nrVersao", required = false) Integer nrVersao) {
        service.excluir(id, nrVersao);
        return noContent();
    }

    @Operation(summary = "Cria um responsável")
    @PostMapping("/responsaveis")
    public ResponseEntity<ApiResponseBody<CaixinhaResponsavelDto>> criarResponsavel(@Valid @RequestBody CaixinhaResponsavelForm form) {
        return created(service.criarResponsavel(form));
    }

    @Operation(summary = "Renomeia e/ou ativa/desativa um responsável")
    @PutMapping("/responsaveis/{id}")
    public ResponseEntity<ApiResponseBody<CaixinhaResponsavelDto>> atualizarResponsavel(@PathVariable Long id, @Valid @RequestBody CaixinhaResponsavelForm form) {
        return ok(service.atualizarResponsavel(id, form));
    }

    @Operation(summary = "Anexa um comprovante em PDF (até 5 MB; cdRequisicao torna o envio idempotente)")
    @PostMapping(value = "/lancamentos/{id}/comprovantes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<CaixinhaComprovanteDto>> anexar(
            @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "cdRequisicao", required = false) String cdRequisicao) {
        return created(service.anexarComprovante(id, nome(arquivo), bytes(arquivo), cdRequisicao));
    }

    @Operation(summary = "Exclui o comprovante e o arquivo")
    @DeleteMapping("/comprovantes/{idComprovante}")
    public ResponseEntity<ApiResponseBody<Void>> excluirComprovante(@PathVariable Long idComprovante) {
        service.excluirComprovante(idComprovante);
        return noContent();
    }

    private static String nome(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        return nome == null ? "comprovante.pdf" : nome;
    }

    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new EntradaInvalidaException("Não foi possível ler o arquivo enviado", ex);
        }
    }
}
