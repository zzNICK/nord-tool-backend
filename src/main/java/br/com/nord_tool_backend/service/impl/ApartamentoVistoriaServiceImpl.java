package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.builder.ApartamentoVistoriaHistoricoBuilder;
import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.ApartamentoVistoriaHistorico;
import br.com.nord_tool_backend.domain.InfoGeralApartamentoVistoria;
import br.com.nord_tool_backend.domain.OrdenacaoApartamentoVistoria;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaDto;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaFiltroDto;
import br.com.nord_tool_backend.dto.InfoGeralApartamentoVistoriaDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.ApartamentoVistoriaForm;
import br.com.nord_tool_backend.handler.XlsxExtractorHandlerApartamento;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaHistoricoRepository;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaRepository;
import br.com.nord_tool_backend.service.ApartamentoVistoriaService;
import br.com.nord_tool_backend.service.CacheService;
import br.com.nord_tool_backend.service.TermoReprovaService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApartamentoVistoriaServiceImpl implements ApartamentoVistoriaService {
    private final Logger log = LogManager.getLogger(ApartamentoVistoriaServiceImpl.class);

    private final ApartamentoVistoriaRepository apartamentoVistoriaRepository;

    private final CacheService cacheService;

    private final ApartamentoVistoriaHistoricoRepository apartamentoVistoriaHistoricoRepository;

    private final XlsxExtractorHandlerApartamento xlsxExtractorHandlerApartamento;

    private final TermoReprovaService termoReprovaService;

    private final AutorizacaoService autorizacao;

    static final int MAX_POR_PAGINA = 100;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApartamentoVistoriaDto salvarApartamentoVistoria(ApartamentoVistoriaForm apartamentoVistoriaForm){
        autorizacao.exigir(Modulo.VISTORIA, Acao.ESCRITA);
        log.info("Iniciando método para salvar um Apartamento Vistoria");
        ApartamentoVistoria apartamentoVistoria = apartamentoVistoriaForm.converterToDomain();
        ApartamentoVistoriaDto apartamentoVistoriaDto = apartamentoVistoriaRepository.salvarApartamentoVistoria(apartamentoVistoria);
        log.info("Iniciando método limpar o cache após salvar");
        cacheService.limparTodos();
        log.info("Finalizando método que salva um Apartamento Vistoria");
        return apartamentoVistoriaDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApartamentoVistoriaDto alterarApartamentoVistoria(ApartamentoVistoriaForm apartamentoVistoriaForm) {
        autorizacao.exigir(Modulo.VISTORIA, Acao.ESCRITA);
        log.info("Iniciando método para alterar um Apartamento Vistoria");
        ApartamentoVistoria apartamentoVistoriaAtual = apartamentoVistoriaForm.converterToDomain();
        log.info("Iniciando método para gerar historia dos Apartamentos");
        ApartamentoVistoria ApartamentoVistoriaAnterior = apartamentoVistoriaRepository.buscarApartamentoVistoria(apartamentoVistoriaAtual.getId());
        List<Integer> lsNrVersaoHistorico = apartamentoVistoriaHistoricoRepository.buscarNrVersaoHistorico(apartamentoVistoriaAtual.getId());
        List<ApartamentoVistoriaHistorico> lsApVistoriaHistorico = ApartamentoVistoriaHistoricoBuilder.gerarHistorico(ApartamentoVistoriaAnterior, apartamentoVistoriaAtual, lsNrVersaoHistorico);
        apartamentoVistoriaHistoricoRepository.salvarTodosHistoricos(lsApVistoriaHistorico);
        ApartamentoVistoriaDto apartamentoVistoriaDto = apartamentoVistoriaRepository.alterarApartamentoVistoria(apartamentoVistoriaAtual);
        log.info("Iniciando método limpar o cache após alterar");
        cacheService.limparTodos();
        log.info("Finalizando método que altera um Apartamento Vistoria");
        return apartamentoVistoriaDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarApartamentoVistoria(Long id) {
        autorizacao.exigir(Modulo.VISTORIA, Acao.ESCRITA);
        log.info("Iniciando método para deletar um Apartamento Vistoria");
        // O ON DELETE CASCADE do banco não apaga os arquivos armazenados: remove termos/fotos antes.
        termoReprovaService.apagarPorApartamento(id);
        this.apartamentoVistoriaRepository.deletarApartamentoVistoria(id);
        log.info("Iniciando método limpar o cache após deletar");
        cacheService.limparTodos();
        log.info("Finalizando método que deleta um Apartamento Vistoria");
    }

    @Override
    public ApartamentoVistoriaDto buscarApartamentoVistoria(Long id) {
        autorizacao.exigir(Modulo.VISTORIA, Acao.LEITURA);
        log.info("Iniciando método para buscar um Apartamento Vistoria");
        ApartamentoVistoria apartamentoVistoria = apartamentoVistoriaRepository.buscarApartamentoVistoria(id);
        ApartamentoVistoriaDto apartamentoVistoriaDto = ApartamentoVistoriaDto.converterToDto(apartamentoVistoria);
        log.info("Finalizando método que busca um Apartamento Vistoria");
        return apartamentoVistoriaDto;
    }

    @Override
    //@Cacheable("apartamentoVistoriaDto")
    public List<ApartamentoVistoriaDto> listarApartamentoVistoria() {
        autorizacao.exigir(Modulo.VISTORIA, Acao.LEITURA);
        log.info("Iniciando método para listar Apartamentos Vistoria");
        List<ApartamentoVistoria> lsApartamentoVistoria = apartamentoVistoriaRepository.listarApartamentoVistoria();
        List<ApartamentoVistoriaDto> lsApartamentoVistoriaDto = lsApartamentoVistoria.stream().map(ApartamentoVistoriaDto::converterToDto).collect(Collectors.toList());
        log.info("Finalizando método que lista Apartamentos Vistoria");
        return lsApartamentoVistoriaDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importarPlanilha(MultipartFile arquivo) throws Exception {
        autorizacao.exigir(Modulo.VISTORIA, Acao.ESCRITA);
        log.info("Iniciando método para importar planilha de Apartamentos Vistoria");
        xlsxExtractorHandlerApartamento.init(arquivo);
        log.info("Iniciando método limpar o cache após importar");
        cacheService.limparTodos();
    }

    @Override
    public List<ApartamentoVistoriaDto> listarApartamentoVistoriaFiltrado(ApartamentoVistoriaFiltroDto apartamentoVistoriaFiltroDto, String filtraTodos, int nrPagina, int nrQuantidadePorPagina, String nmOrdenacao) {
        autorizacao.exigir(Modulo.VISTORIA, Acao.LEITURA);
        log.info("Iniciando método para filtrar listas de Apartamentos");
        if (nrPagina < 0 || nrQuantidadePorPagina < 1 || nrQuantidadePorPagina > MAX_POR_PAGINA) {
            throw new EntradaInvalidaException("Paginação inválida: página a partir de 0 e de 1 a " + MAX_POR_PAGINA + " itens por página");
        }
        List<ApartamentoVistoriaDto> lsApartamentoVistoriaDto = apartamentoVistoriaRepository.listarApartamentoVistoriaFiltrado(
                apartamentoVistoriaFiltroDto, filtraTodos, nrPagina, nrQuantidadePorPagina, OrdenacaoApartamentoVistoria.de(nmOrdenacao));
        log.info("Finalizando método que filtrar listas de Apartamentos");
        return lsApartamentoVistoriaDto;
    }

    @Override
    public List<InfoGeralApartamentoVistoriaDto> listarInfoGeralApartamentoVistoria(String dtiApartamentoVistoria, String dtfApartamentoVistoria){
        autorizacao.exigir(Modulo.VISTORIA, Acao.LEITURA);
        log.info("Iniciando método para listar Informações Gerais dos Apartamentos");
        List<InfoGeralApartamentoVistoria> lsInfoGeralApartamentoVistoria =  apartamentoVistoriaRepository.listarInfoGeralApartamentoVistoria(dtiApartamentoVistoria, dtfApartamentoVistoria);
        List<InfoGeralApartamentoVistoriaDto> lsInfoGeralApartamentoVistoriaDto = lsInfoGeralApartamentoVistoria.stream().map(InfoGeralApartamentoVistoriaDto::converterToDTO).collect(Collectors.toList());
        log.info("Finalizando método para listar Informações Gerais dos Apartamentos");
        return lsInfoGeralApartamentoVistoriaDto;
    }

}
