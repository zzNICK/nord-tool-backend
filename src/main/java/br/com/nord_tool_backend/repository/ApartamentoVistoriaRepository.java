package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.InfoGeralApartamentoVistoria;
import br.com.nord_tool_backend.domain.OrdenacaoApartamentoVistoria;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaDto;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaFiltroDto;

import java.time.LocalDate;
import java.util.List;

public interface ApartamentoVistoriaRepository {
    ApartamentoVistoriaDto salvarApartamentoVistoria(ApartamentoVistoria apartamentoVistoria);
    ApartamentoVistoriaDto alterarApartamentoVistoria(ApartamentoVistoria apartamentoVistoria);
    void deletarApartamentoVistoria(Long id);
    ApartamentoVistoria buscarApartamentoVistoria(Long id);
    List<ApartamentoVistoria> listarApartamentoVistoria();
    void salvarEmLote(List<ApartamentoVistoria> lsApartamentoVistoria);
    List<ApartamentoVistoriaDto> listarApartamentoVistoriaFiltrado(ApartamentoVistoriaFiltroDto apartamentoVistoriaFiltroDto,
                                                                   String filtrarTodos, int nrPagina, int nrQuantidadePorPagina,
                                                                   OrdenacaoApartamentoVistoria ordenacao);
    List<InfoGeralApartamentoVistoria> listarInfoGeralApartamentoVistoria(String dtiApartamentoVistoriaFiltro, String dtfApartamentoVistoriaFiltro);
}
