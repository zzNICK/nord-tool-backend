package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Empresa;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.EmpresaRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository @Slf4j
@PropertySource("classpath:query/empresa.properties")
public class EmpresaRepositoryImpl extends RepositoryJdbcOperationsSql<Empresa> implements EmpresaRepository {
    @Value("${SPS.EMPRESA.LISTAR}") private String queryListar;

    public List<Empresa> listarEmpresas() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Empresa.class));
        } catch (Exception ex) {
            log.error("Erro ao listar empresas", ex);
            throw new ValidacaoException(NordHttpEnum.HTTP_400, "Erro ao listar empresas", ExceptionUtils.getMessage(ex));
        }
    }
}
