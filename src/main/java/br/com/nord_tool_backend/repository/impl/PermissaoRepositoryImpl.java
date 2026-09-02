package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Permissao;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.PermissaoRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository @Slf4j
@PropertySource("classpath:query/permissao.properties")
public class PermissaoRepositoryImpl extends RepositoryJdbcOperationsSql<Permissao> implements PermissaoRepository {
    @Value("${SPS.PERMISSAO.LISTAR}") private String queryListar;

    public List<Permissao> listarPermissoes() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Permissao.class));
        } catch (Exception ex) {
            log.error("Erro ao listar permissões", ex);
            throw new ValidacaoException(NordHttpEnum.HTTP_400, "Erro ao listar permissões", ExceptionUtils.getMessage(ex));
        }
    }
}
