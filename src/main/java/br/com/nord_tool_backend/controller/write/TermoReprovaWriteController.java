package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.TermoFotoDto;
import br.com.nord_tool_backend.dto.TermoReprovaDto;
import br.com.nord_tool_backend.form.OrdemFotoForm;
import br.com.nord_tool_backend.form.SituacaoTermoForm;
import br.com.nord_tool_backend.service.TermoReprovaService;
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
@RequestMapping("/api/v1/nord-tool/apartamentoVistoria")
@Tag(name = "Termo de reprova", description = "Termos de reprova (PDF) e fotos por apartamento")
public class TermoReprovaWriteController implements BaseResponse {

    private final TermoReprovaService termoReprovaService;

    @Operation(summary = "Anexa um novo termo (numeração automática: max + 1)")
    @PostMapping(value = "/{idApartamento}/termos-reprova", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<TermoReprovaDto>> criar(
            @PathVariable Long idApartamento,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("nrPaginas") int nrPaginas) {
        return created(termoReprovaService.criar(idApartamento, nome(arquivo), bytes(arquivo), nrPaginas));
    }

    @Operation(summary = "Troca o PDF de um termo")
    @PutMapping(value = "/termos-reprova/{idTermo}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<TermoReprovaDto>> trocarArquivo(
            @PathVariable Long idTermo,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("nrPaginas") int nrPaginas) {
        return ok(termoReprovaService.trocarArquivo(idTermo, nome(arquivo), bytes(arquivo), nrPaginas));
    }

    @Operation(summary = "Atualiza situação e observação do termo")
    @PutMapping("/termos-reprova/{idTermo}/situacao")
    public ResponseEntity<ApiResponseBody<TermoReprovaDto>> situacao(
            @PathVariable Long idTermo, @Valid @RequestBody SituacaoTermoForm form) {
        return ok(termoReprovaService.atualizarSituacao(idTermo, form));
    }

    @Operation(summary = "Exclui o termo, suas fotos e os arquivos")
    @DeleteMapping("/termos-reprova/{idTermo}")
    public ResponseEntity<ApiResponseBody<Void>> deletar(@PathVariable Long idTermo) {
        termoReprovaService.deletar(idTermo);
        return noContent();
    }

    @Operation(summary = "Adiciona uma foto à página do termo")
    @PostMapping(value = "/termos-reprova/{idTermo}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<TermoFotoDto>> adicionarFoto(
            @PathVariable Long idTermo,
            @RequestParam("imagem") MultipartFile imagem,
            @RequestParam("miniatura") MultipartFile miniatura,
            @RequestParam("nrPagina") int nrPagina,
            @RequestParam(value = "legenda", required = false) String legenda) {
        return created(termoReprovaService.adicionarFoto(idTermo, nome(imagem), bytes(imagem), bytes(miniatura), nrPagina, legenda));
    }

    @Operation(summary = "Edita foto: trocar imagem/miniatura, legenda ou página")
    @PutMapping(value = "/termos-reprova/fotos/{idFoto}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<TermoFotoDto>> editarFoto(
            @PathVariable Long idFoto,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem,
            @RequestParam(value = "miniatura", required = false) MultipartFile miniatura,
            @RequestParam(value = "legenda", required = false) String legenda,
            @RequestParam(value = "nrPagina", required = false) Integer nrPagina) {
        String nomeImagem = imagem == null ? null : nome(imagem);
        return ok(termoReprovaService.editarFoto(idFoto, nomeImagem, bytesOuNulo(imagem), bytesOuNulo(miniatura), legenda, nrPagina));
    }

    @Operation(summary = "Exclui a foto e seus arquivos")
    @DeleteMapping("/termos-reprova/fotos/{idFoto}")
    public ResponseEntity<ApiResponseBody<Void>> excluirFoto(@PathVariable Long idFoto) {
        termoReprovaService.excluirFoto(idFoto);
        return noContent();
    }

    @Operation(summary = "Reordena as fotos do termo")
    @PutMapping("/termos-reprova/{idTermo}/fotos/ordem")
    public ResponseEntity<ApiResponseBody<List<TermoFotoDto>>> ordenar(
            @PathVariable Long idTermo, @Valid @RequestBody List<@Valid OrdemFotoForm> ordem) {
        return ok(termoReprovaService.ordenarFotos(idTermo, ordem));
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

    private static byte[] bytesOuNulo(MultipartFile arquivo) {
        return arquivo == null || arquivo.isEmpty() ? null : bytes(arquivo);
    }
}
