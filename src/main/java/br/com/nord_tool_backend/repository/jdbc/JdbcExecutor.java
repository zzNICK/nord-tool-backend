package br.com.nord_tool_backend.repository.jdbc;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.FalhaInternaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.exception.NordException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Executa operações JDBC e traduz falhas do banco para o contrato de erro da API. Nenhuma mensagem do banco
 * (SQL, constraint, tabela) chega ao cliente; o log recebe a operação e o SQLState, nunca os parâmetros.
 *
 * <p>Violação de unicidade ou de chave estrangeira vira 422 ({@link NegocioException}), não 409: no frontend o
 * 409 significa conflito de versão (fecha o modal e recarrega), e esses casos devem manter o modal aberto.</p>
 */
@Slf4j
@Component
public class JdbcExecutor {

    public static final String CD_REGISTRO_DUPLICADO = "REGISTRO_DUPLICADO";
    public static final String CD_REGISTRO_EM_USO = "REGISTRO_EM_USO";
    public static final String CD_DADO_INVALIDO = "DADO_INVALIDO";
    public static final String CD_NAO_ENCONTRADO = "NAO_ENCONTRADO";

    static final String UNIQUE_VIOLATION = "23505";
    static final String FOREIGN_KEY_VIOLATION = "23503";
    static final String NOT_NULL_VIOLATION = "23502";
    static final String CHECK_VIOLATION = "23514";
    static final String STRING_TOO_LONG = "22001";

    public <T> T executar(String operacao, Supplier<T> acao) {
        try {
            return acao.get();
        } catch (NordException ex) {
            throw ex;
        } catch (EmptyResultDataAccessException ex) {
            throw new NaoEncontradoException(CD_NAO_ENCONTRADO, NordHttpEnum.HTTP_404.getMensagem(), ex);
        } catch (DataIntegrityViolationException ex) {
            throw traduzirIntegridade(operacao, ex);
        } catch (RuntimeException ex) {
            log.warn("Falha em '{}' (SQLState {})", operacao, sqlState(ex));
            throw new FalhaInternaException(ex);
        }
    }

    public void executar(String operacao, Runnable acao) {
        executar(operacao, () -> {
            acao.run();
            return null;
        });
    }

    private static NordException traduzirIntegridade(String operacao, DataIntegrityViolationException ex) {
        String estado = sqlState(ex);
        log.info("Violação de integridade em '{}' (SQLState {})", operacao, estado);
        if (UNIQUE_VIOLATION.equals(estado)) {
            return new NegocioException(CD_REGISTRO_DUPLICADO, "Já existe um registro com esses dados", ex);
        }
        if (FOREIGN_KEY_VIOLATION.equals(estado)) {
            return new NegocioException(CD_REGISTRO_EM_USO,
                    "O registro está em uso ou referencia um registro inexistente", ex);
        }
        if (NOT_NULL_VIOLATION.equals(estado) || CHECK_VIOLATION.equals(estado) || STRING_TOO_LONG.equals(estado)) {
            return new EntradaInvalidaException(CD_DADO_INVALIDO, "Dados inválidos ou incompletos", ex);
        }
        return new FalhaInternaException(ex);
    }

    /** SQLState da primeira {@link SQLException} na cadeia de causas, ou {@code null}. */
    static String sqlState(Throwable ex) {
        for (Throwable causa = ex; causa != null && causa.getCause() != causa; causa = causa.getCause()) {
            if (causa instanceof SQLException && ((SQLException) causa).getSQLState() != null) {
                return ((SQLException) causa).getSQLState();
            }
        }
        return null;
    }
}
