package com.mini.credit.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de validation sur CreateAgenceRequest.
 *
 * Vérifie que les contraintes Bean Validation (@NotBlank, @Size)
 * sont correctement définies sur les champs obligatoires.
 *
 * Champs obligatoires : codeAgence, nomAgence, ville, commune, quartier, adresse, actif
 * Champs optionnels  : reference, telephone, description
 * Champ absent       : email (retiré du flux principal)
 */
@DisplayName("CreateAgenceRequest — Validation des champs obligatoires")
class AgenceRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Requête valide de référence
    // ──────────────────────────────────────────────────────────────────────

    private CreateAgenceRequest validRequest() {
        return CreateAgenceRequest.builder()
                .codeAgence("KIN001")
                .nomAgence("Agence Centrale Kinshasa")
                .ville("Kinshasa")
                .commune("Gombe")
                .quartier("Centre-Ville")
                .adresse("Avenue de l'Equateur, n°12")
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("Requête valide — aucune violation attendue")
    void validRequest_shouldHaveNoViolations() {
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // createAgence_shouldRequireVille
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAgence_shouldRequireVille — ville null provoque une violation")
    void createAgence_shouldRequireVille() {
        CreateAgenceRequest req = validRequest();
        req.setVille(null);
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("ville");
    }

    @Test
    @DisplayName("createAgence_shouldRequireVille — ville vide provoque une violation")
    void createAgence_shouldRequireVille_blank() {
        CreateAgenceRequest req = validRequest();
        req.setVille("   ");
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("ville");
    }

    // ──────────────────────────────────────────────────────────────────────
    // createAgence_shouldRequireCommune
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAgence_shouldRequireCommune — commune null provoque une violation")
    void createAgence_shouldRequireCommune() {
        CreateAgenceRequest req = validRequest();
        req.setCommune(null);
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("commune");
    }

    @Test
    @DisplayName("createAgence_shouldRequireCommune — commune vide provoque une violation")
    void createAgence_shouldRequireCommune_blank() {
        CreateAgenceRequest req = validRequest();
        req.setCommune("");
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("commune");
    }

    // ──────────────────────────────────────────────────────────────────────
    // createAgence_shouldRequireQuartier
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAgence_shouldRequireQuartier — quartier null provoque une violation")
    void createAgence_shouldRequireQuartier() {
        CreateAgenceRequest req = validRequest();
        req.setQuartier(null);
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("quartier");
    }

    // ──────────────────────────────────────────────────────────────────────
    // createAgence_shouldRequireAdresse
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAgence_shouldRequireAdresse — adresse null provoque une violation")
    void createAgence_shouldRequireAdresse() {
        CreateAgenceRequest req = validRequest();
        req.setAdresse(null);
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("adresse");
    }

    // ──────────────────────────────────────────────────────────────────────
    // createAgence_shouldAcceptReferenceOptional
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAgence_shouldAcceptReferenceOptional — reference null est accepté")
    void createAgence_shouldAcceptReferenceOptional() {
        CreateAgenceRequest req = validRequest();
        req.setReference(null);
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .doesNotContain("reference");
    }

    @Test
    @DisplayName("createAgence_shouldAcceptReferenceOptional — reference vide est accepté")
    void createAgence_shouldAcceptReferenceOptional_empty() {
        CreateAgenceRequest req = validRequest();
        req.setReference("");
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .doesNotContain("reference");
    }

    // ──────────────────────────────────────────────────────────────────────
    // createAgence_shouldNotRequireEmail
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAgence_shouldNotRequireEmail — email n'existe plus dans CreateAgenceRequest")
    void createAgence_shouldNotRequireEmail() {
        // Vérifier par réflexion que le champ email n'existe pas dans CreateAgenceRequest
        boolean hasEmailField = false;
        try {
            CreateAgenceRequest.class.getDeclaredField("email");
            hasEmailField = true;
        } catch (NoSuchFieldException e) {
            hasEmailField = false;
        }
        assertThat(hasEmailField)
                .as("Le champ email ne doit plus être présent dans CreateAgenceRequest (retiré du flux principal)")
                .isFalse();
    }

    @Test
    @DisplayName("Telephone optionnel — telephone null est accepté")
    void createAgence_shouldAcceptTelephoneOptional() {
        CreateAgenceRequest req = validRequest();
        req.setTelephone(null);
        Set<ConstraintViolation<CreateAgenceRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .doesNotContain("telephone");
    }
}
