package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.DiaSemana;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.DiaSemanaRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import br.com.nord_tool_backend.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
@PropertySource("classpath:query/dia-semana.properties")
public class DiaSemanaRepositoryImpl extends RepositoryJdbcOperationsSql<DiaSemana> implements DiaSemanaRepository {

    private static final String LISTAR_ERRO_GENERICO = "Erro ao listar os dados da tabela Dia semana";

    @Value("${SPS.BUSCAR_TODOS_DIA_SEMANA}")
    private String queryListarTodosDiaSemana;

    @Override
    public List<DiaSemana> listarDiaSemana (){
        try {
            return buscarTodos(queryListarTodosDiaSemana, BeanPropertyRowMapper.newInstance(DiaSemana.class));
        } catch (Exception e) {
            log.error(ExceptionUtils.getMessage(e));
            throw new ValidacaoException(StringUtils.getMensagem(LISTAR_ERRO_GENERICO));
        }
    }
}
