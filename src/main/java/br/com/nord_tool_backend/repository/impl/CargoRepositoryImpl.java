package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Cargo;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.CargoRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository @Slf4j
@PropertySource("classpath:query/cargo.properties")
public class CargoRepositoryImpl extends RepositoryJdbcOperationsSql<Cargo> implements CargoRepository {
    @Value("${SPS.CARGO.LISTAR}") private String queryListar;

    public List<Cargo> listarCargos() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Cargo.class));
        } catch (Exception ex) {
            log.error("Erro ao listar cargos", ex);
            throw new ValidacaoException(NordHttpEnum.HTTP_400, "Erro ao listar cargos", ExceptionUtils.getMessage(ex));
        }
    }
}
