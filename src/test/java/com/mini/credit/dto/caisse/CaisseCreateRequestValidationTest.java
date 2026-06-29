package com.mini.credit.dto.caisse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CaisseCreateRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenAgenceIdIsMissing() {
        CaisseCreateRequest request = new CaisseCreateRequest();
        request.setLibelle("Caisse principale");
        request.setDevise("CDF");

        Set<ConstraintViolation<CaisseCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "agenceId".equals(v.getPropertyPath().toString()));
    }

    @Test
    void shouldFailWhenLibelleIsMissing() {
        CaisseCreateRequest request = new CaisseCreateRequest();
        request.setAgenceId(1L);
        request.setSiteId(2L);
        request.setDevise("CDF");

        Set<ConstraintViolation<CaisseCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "libelle".equals(v.getPropertyPath().toString()));
    }

    @Test
    void shouldFailWhenDeviseIsMissing() {
        CaisseCreateRequest request = new CaisseCreateRequest();
        request.setAgenceId(1L);
        request.setSiteId(2L);
        request.setLibelle("Caisse principale");
        request.setDevise(null);

        Set<ConstraintViolation<CaisseCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "devise".equals(v.getPropertyPath().toString()));
    }
}