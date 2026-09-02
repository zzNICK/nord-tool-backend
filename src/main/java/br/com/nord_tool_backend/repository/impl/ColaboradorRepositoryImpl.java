package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Colaborador;
import br.com.nord_tool_backend.dto.ColaboradorDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.ColaboradorRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
@PropertySource("classpath:query/colaborador.properties")
public class ColaboradorRepositoryImpl extends RepositoryJdbcOperationsSql<Colaborador> implements ColaboradorRepository {
    private static final String ERRO_SALVAR = "Erro ao salvar colaborador";
    private static final String ERRO_ALTERAR = "Erro ao alterar colaborador";
    private static final String ERRO_DELETAR = "Erro ao deletar colaborador";
    private static final String ERRO_BUSCAR = "Erro ao buscar colaborador";
    private static final String ERRO_LISTAR = "Erro ao listar colaboradores";

    @Value("${SPI.COLABORADOR.INSERIR}") private String querySalvar;
    @Value("${SPU.COLABORADOR.ALTERAR}") private String queryAlterar;
    @Value("${SPD.COLABORADOR.DELETAR}") private String queryDeletar;
    @Value("${SPS.COLABORADOR.BUSCAR_POR_ID}") private String queryBuscarPorId;
    @Value("${SPS.COLABORADOR.LISTAR}") private String queryListar;

    @Override
    public ColaboradorDto salvarColaborador(Colaborador colaborador) {
        try {
            Colaborador salvo = salvar(querySalvar, colaborador, "id_user");
            return ColaboradorDto.converterToDomain(buscarPorIdInterno(salvo.getId()));
        } catch (Exception ex) {
            throw tratarErro(ERRO_SALVAR, ex);
        }
    }

    @Override
    public ColaboradorDto alterarColaborador(Colaborador colaborador) {
        try {
            alterar(queryAlterar, colaborador);
            return ColaboradorDto.converterToDomain(buscarPorIdInterno(colaborador.getId()));
        } catch (Exception ex) {
            throw tratarErro(ERRO_ALTERAR, ex);
        }
    }

    @Override
    public void deletarColaborador(Long id) {
        try {
            deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro(ERRO_DELETAR, ex);
        }
    }

    @Override
    public Colaborador buscarPorIdColaborador(Long id) {
        try {
            return buscarPorIdInterno(id);
        } catch (Exception ex) {
            throw tratarErro(ERRO_BUSCAR, ex);
        }
    }

    @Override
    public List<Colaborador> listarColaboradores() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Colaborador.class));
        } catch (Exception ex) {
            throw tratarErro(ERRO_LISTAR, ex);
        }
    }

    private Colaborador buscarPorIdInterno(Long id) {
        return buscarPorId(queryBuscarPorId, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(Colaborador.class));
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getMessage(ex));
    }
}
