package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.ObraControleChaves;
import br.com.nord_tool_backend.domain.RequisicaoChave;
import br.com.nord_tool_backend.domain.RequisicaoChaveConsulta;
import br.com.nord_tool_backend.domain.enums.StatusRequisicaoChaveEnum;
import br.com.nord_tool_backend.domain.enums.TipoItemControleChavesEnum;
import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.DashboardControleChavesDto;
import br.com.nord_tool_backend.dto.FerramentaControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.dto.RetiradaControleChavesDto;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;
import br.com.nord_tool_backend.repository.ControleChavesRepository;
import br.com.nord_tool_backend.service.ControleChavesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ControleChavesServiceImpl implements ControleChavesService {

    private static final String CODIGO_RETIRADA_PREFIXO = "RET-";
    private static final Pattern PADRAO_NOME_OBRA = Pattern.compile("\\s*[-_]?\\s*[0-9].*$");
    private static final int QUANTIDADE_POR_PAGINA_PADRAO = 20;
    static final int MAX_POR_PAGINA = 100;
    static final int MAX_RECENTES = 50;
    private final ControleChavesRepository controleChavesRepository;

    private final AutorizacaoService autorizacao;

    @Override
    public List<ObraControleChavesDto> listarObras() {
        autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.LEITURA);
        return controleChavesRepository.listarObras().stream()
                .map(ObraControleChaves::getNmApartamentoVistoria)
                .map(this::extrairNomeObra)
                .filter(nmObra -> !nmObra.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(ObraControleChavesDto::converterToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApartamentoControleChavesDto> listarApartamentos(String nmBusca, int nrQuantidadePorPagina, int nrPagina) {
        autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.LEITURA);
        return controleChavesRepository.listarApartamentos().stream()
                .filter(apartamento -> correspondeBusca(apartamento.getNmApartamentoVistoria(), nmBusca))
                .skip(obterOffset(nrQuantidadePorPagina, nrPagina))
                .limit(obterQuantidadePorPagina(nrQuantidadePorPagina))
                .map(ApartamentoControleChavesDto::converterToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FerramentaControleChavesDto> listarFerramentas(String nmBusca, int nrQuantidadePorPagina, int nrPagina) {
        autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.LEITURA);
        return controleChavesRepository.listarFerramentas().stream()
                .filter(ferramenta -> correspondeBusca(ferramenta.getNmFerramenta(), nmBusca))
                .skip(obterOffset(nrQuantidadePorPagina, nrPagina))
                .limit(obterQuantidadePorPagina(nrQuantidadePorPagina))
                .map(FerramentaControleChavesDto::converterToDto)
                .collect(Collectors.toList());
    }

    @Override
    public DashboardControleChavesDto buscarDashboard(int nrLimiteRecentes, String idObra) {
        autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.LEITURA);
        int nrQuantidadePorPagina = nrLimiteRecentes > 0 ? Math.min(nrLimiteRecentes, MAX_RECENTES) : 5;
        List<RetiradaControleChavesDto> retiradasRecentes = controleChavesRepository.listarHistorico().stream()
                .filter(requisicao -> correspondeObra(requisicao, idObra))
                .limit(nrQuantidadePorPagina)
                .map(this::converterRetiradaParaDto)
                .collect(Collectors.toList());

        return DashboardControleChavesDto.builder()
                .qtChavesEmCampo(controleChavesRepository.contarChavesEmCampo())
                .qtChavesNoQuadro(controleChavesRepository.contarChavesNoQuadro())
                .qtChavesEntregues(controleChavesRepository.contarChavesEntregues())
                .retiradasRecentes(retiradasRecentes)
                .build();
    }

    @Override
    public List<RetiradaControleChavesDto> listarHistorico(String nmBusca, String nmStatusRequisicao, String idObra, int nrQuantidadePorPagina, int nrPagina) {
        autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.LEITURA);
        final String nmStatusRequisicaoNormalizado = nmStatusRequisicao != null && !nmStatusRequisicao.isBlank() ? validarStatus(nmStatusRequisicao).name() : nmStatusRequisicao;

        return controleChavesRepository.listarHistorico().stream()
                .filter(requisicao -> correspondeBusca(requisicao.getNmApartamentoVistoria(), nmBusca) || correspondeBusca(requisicao.getNmFerramenta(), nmBusca)
                        || correspondeBusca(requisicao.getNmPessoaRetirante(), nmBusca) || correspondeBusca(requisicao.getCdRetirada(), nmBusca))
                .filter(requisicao -> nmStatusRequisicaoNormalizado == null || nmStatusRequisicaoNormalizado.isBlank()
                        || nmStatusRequisicaoNormalizado.equalsIgnoreCase(requisicao.getNmStatusRequisicao()))
                .filter(requisicao -> correspondeObra(requisicao, idObra))
                .skip(obterOffset(nrQuantidadePorPagina, nrPagina))
                .limit(obterQuantidadePorPagina(nrQuantidadePorPagina))
                .map(this::converterRetiradaParaDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetiradaControleChavesDto criarRetirada(NovaRetiradaControleChavesForm novaRetiradaControleChavesForm) {
        autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.ESCRITA);
        if (novaRetiradaControleChavesForm == null
                || !idValido(novaRetiradaControleChavesForm.getIdUserRetirada())
                || !idValido(novaRetiradaControleChavesForm.getIdUserLiberacao())) {
            throw erroValidacao("Retirante e liberador são obrigatórios e devem ser válidos");
        }
        validarItemRetirada(novaRetiradaControleChavesForm);

        RequisicaoChave requisicaoChave = novaRetiradaControleChavesForm.converterToDomain();
        requisicaoChave.setCdRetirada(gerarCodigoRetirada());
        requisicaoChave.setDtRetirada(LocalDateTime.now());
        requisicaoChave.setNmStatusRequisicao(StatusRequisicaoChaveEnum.ABERTO.name());
        Long idRequisicao = controleChavesRepository.criarRetirada(requisicaoChave);
        return converterRetiradaParaDto(controleChavesRepository.buscarPorId(idRequisicao));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetiradaControleChavesDto receberRetirada(Long idRequisicao,
                                                       RecebimentoControleChavesForm recebimentoControleChavesForm) {
        autorizacao.exigir(Modulo.CONTROLE_CHAVES, Acao.ESCRITA);
        if (!idValido(idRequisicao) || recebimentoControleChavesForm == null
                || !idValido(recebimentoControleChavesForm.getIdUserRecebimento())) {
            throw erroValidacao("Retirada e usuário recebedor são obrigatórios e devem ser válidos");
        }

        RequisicaoChaveConsulta atual = controleChavesRepository.buscarPorId(idRequisicao);
        validarConsistencia(atual);
        if (validarStatus(atual.getNmStatusRequisicao()) != StatusRequisicaoChaveEnum.ABERTO) {
            throw erroValidacao("Somente retiradas abertas podem ser recebidas");
        }

        controleChavesRepository.receberRetirada(idRequisicao, recebimentoControleChavesForm.getIdUserRecebimento(), LocalDateTime.now(),
                StatusRequisicaoChaveEnum.RECEBIDO.name());
        RequisicaoChaveConsulta recebida = controleChavesRepository.buscarPorId(idRequisicao);
        validarConsistencia(recebida);
        if (recebida.getIdUserRecebimento() == null
                || !recebida.getIdUserRecebimento().equals(recebimentoControleChavesForm.getIdUserRecebimento())) {
            throw erroValidacao("A retirada já foi recebida por outro usuário");
        }
        return RetiradaControleChavesDto.converterToDto(recebida);
    }

    private String extrairNomeObra(String nmApartamentoVistoria) {
        if (nmApartamentoVistoria == null) {
            return "";
        }
        return PADRAO_NOME_OBRA.matcher(nmApartamentoVistoria).replaceFirst("").trim();
    }

    private boolean correspondeObra(RequisicaoChaveConsulta requisicaoChaveConsulta, String idObra) {
        return idObra == null || idObra.isBlank()
                || extrairNomeObra(requisicaoChaveConsulta.getNmApartamentoVistoria()).equalsIgnoreCase(idObra.trim());
    }

    private boolean correspondeBusca(String nmValor, String nmBusca) {
        return nmBusca == null || nmBusca.isBlank()
                || (nmValor != null && nmValor.toLowerCase(Locale.ROOT).contains(nmBusca.trim().toLowerCase(Locale.ROOT)));
    }

    private int obterQuantidadePorPagina(int nrQuantidadePorPagina) {
        return nrQuantidadePorPagina > 0 ? Math.min(nrQuantidadePorPagina, MAX_POR_PAGINA) : QUANTIDADE_POR_PAGINA_PADRAO;
    }

    private long obterOffset(int nrQuantidadePorPagina, int nrPagina) {
        return (long) Math.max(0, nrPagina) * obterQuantidadePorPagina(nrQuantidadePorPagina);
    }

    private RetiradaControleChavesDto converterRetiradaParaDto(RequisicaoChaveConsulta requisicaoChaveConsulta) {
        validarConsistencia(requisicaoChaveConsulta);
        return RetiradaControleChavesDto.converterToDto(requisicaoChaveConsulta);
    }

    private void validarItemRetirada(NovaRetiradaControleChavesForm novaRetiradaControleChavesForm) {
        TipoItemControleChavesEnum tipoItem;
        try {
            tipoItem = TipoItemControleChavesEnum.from(novaRetiradaControleChavesForm.getNmTipoItem());
        } catch (IllegalArgumentException ex) {
            throw erroValidacao("Tipo do item inválido: " + novaRetiradaControleChavesForm.getNmTipoItem());
        }

        boolean temApartamento = idValido(novaRetiradaControleChavesForm.getIdApartamentoVistoria());
        boolean temFerramenta = idValido(novaRetiradaControleChavesForm.getIdFerramenta());

        if (tipoItem == TipoItemControleChavesEnum.APARTAMENTO && (!temApartamento || temFerramenta)) {
            throw erroValidacao("Informe apenas o apartamento para uma retirada do tipo Apartamento");
        }
        if (tipoItem == TipoItemControleChavesEnum.FERRAMENTA && (!temFerramenta || temApartamento)) {
            throw erroValidacao("Informe apenas a ferramenta para uma retirada do tipo Ferramenta");
        }
    }

    private StatusRequisicaoChaveEnum validarStatus(String nmStatusRequisicao) {
        try {
            return StatusRequisicaoChaveEnum.from(nmStatusRequisicao);
        } catch (IllegalArgumentException ex) {
            throw erroValidacao("Status de retirada inválido: " + nmStatusRequisicao);
        }
    }

    private void validarConsistencia(RequisicaoChaveConsulta requisicaoChaveConsulta) {
        StatusRequisicaoChaveEnum status = validarStatus(requisicaoChaveConsulta.getNmStatusRequisicao());
        boolean recebimentoInformado = requisicaoChaveConsulta.getDtRecebimento() != null
                && requisicaoChaveConsulta.getIdUserRecebimento() != null;
        boolean recebimentoAusente = requisicaoChaveConsulta.getDtRecebimento() == null
                && requisicaoChaveConsulta.getIdUserRecebimento() == null;

        if ((status == StatusRequisicaoChaveEnum.ABERTO && !recebimentoAusente)
                || (status == StatusRequisicaoChaveEnum.RECEBIDO && !recebimentoInformado)) {
            throw erroValidacao("Status e dados de recebimento da retirada são inconsistentes");
        }
    }

    private String gerarCodigoRetirada() {
        return CODIGO_RETIRADA_PREFIXO + (System.currentTimeMillis() % 1000000);
    }

    private boolean idValido(Long id) {
        return id != null && id > 0;
    }

    private NordException erroValidacao(String mensagem) {
        return new EntradaInvalidaException(mensagem);
    }
}
