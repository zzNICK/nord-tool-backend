package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaHistoricoConsultaDto;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaHistoricoDto;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaHistoricoRepository;
import br.com.nord_tool_backend.service.ApartamentoVistoriaHistoricoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ApartamentoVistoriaHistoricoServiceImplTest {

    @Mock

    private AutorizacaoService autorizacao;


    @InjectMocks
    private ApartamentoVistoriaHistoricoServiceImpl apartamentoVistoriaHistoricoService;

    @Mock
    private ApartamentoVistoriaHistoricoRepository apartamentoVistoriaHistoricoRepository;

    private List<ApartamentoVistoriaHistoricoConsultaDto> lsApartamentoVistoriaHistoricoConsultaDto = new ArrayList<>();

    @BeforeEach
    void setup() {

        lsApartamentoVistoriaHistoricoConsultaDto.add(ApartamentoVistoriaHistoricoConsultaDto.builder()
                .nrVersao(1)
                .dtAlteracao(LocalDateTime.now())
                .nmUsuario("usuario.teste")
                .nmAtributo("Status")
                .txAnterior("Pendente")
                .txAtual("Aprovado")
                .build());

    }

    @Test
    void deveBuscarHistoricoApartamentoVistoria() {
        when(apartamentoVistoriaHistoricoRepository.buscarHistorico(1L)).thenReturn(lsApartamentoVistoriaHistoricoConsultaDto);
        List<ApartamentoVistoriaHistoricoDto> apartamentoVistoriaHistoricoDto = apartamentoVistoriaHistoricoService.buscarHistoricoApartamentoVistoria(1L);
        assertNotNull(apartamentoVistoriaHistoricoDto);
        assertEquals(1, apartamentoVistoriaHistoricoDto.size());
        assertEquals(1, apartamentoVistoriaHistoricoDto.get(0).getNrVersao());
        verify(apartamentoVistoriaHistoricoRepository)
                .buscarHistorico(1L);
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.VISTORIA, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> apartamentoVistoriaHistoricoService.buscarHistoricoApartamentoVistoria(1L));
        verifyNoInteractions(apartamentoVistoriaHistoricoRepository);
    }
}
