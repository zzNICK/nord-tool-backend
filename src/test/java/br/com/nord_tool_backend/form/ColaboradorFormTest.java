package br.com.nord_tool_backend.form;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColaboradorFormTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void close() {
        validatorFactory.close();
    }

    @Test
    void deveAceitarContratoValido() {
        assertTrue(validator.validate(formValido()).isEmpty());
    }

    @Test
    void deveRejeitarCamposObrigatoriosAusentes() {
        Set<ConstraintViolation<ColaboradorForm>> erros = validator.validate(new ColaboradorForm());
        assertEquals(4, erros.size());
    }

    @Test
    void deveRejeitarCelularForaDoContrato() {
        ColaboradorForm form = formValido();
        form.setCelular("11999A99999");
        assertEquals(1, validator.validate(form).size());
    }

    private ColaboradorForm formValido() {
        return ColaboradorForm.builder()
                .nome("João Silva")
                .celular("11999999999")
                .idEmpresa(1)
                .idCargo(1)
                .idPermissao(1)
                .build();
    }
}
