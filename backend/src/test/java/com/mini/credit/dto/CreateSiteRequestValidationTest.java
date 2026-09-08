package com.mini.credit.dto;

import com.mini.credit.dto.referentiel.CreateSiteRequest;
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
 * Tests de validation sur CreateSiteRequest.
 *
 * Champs obligatoires : agenceId, codeSite, nomSite, zone
 * Champs optionnels   : reference, observation
 * Champs absents      : ville, commune, adresse (localisation vient de l'Agence)
 */
@DisplayName("CreateSiteRequest — Validation des champs obligatoires")
class CreateSiteRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CreateSiteRequest validRequest() {
        return CreateSiteRequest.builder()
                .agenceId(1L)
                .codeSite("KIN01")
                .nomSite("Site Place Meteo")
                .zone("De la Place Meteo jusqu a l avenue des Aviateurs")
                .build();
    }

    @Test
    @DisplayName("Requete valide — aucune violation attendue")
    void validRequest_shouldHaveNoViolations() {
        Set<ConstraintViolation<CreateSiteRequest>> violations = validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    // =========================================================
    // createSite_shouldRequireAgence
    // =========================================================

    @Test
    @DisplayName("createSite_shouldRequireAgence — agenceId null provoque violation")
    void createSite_shouldRequireAgence() {
        CreateSiteRequest req = validRequest();
        req.setAgenceId(null);
        Set<ConstraintViolation<CreateSiteRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("agenceId");
    }

    // =========================================================
    // createSite_shouldRequireCode
    // =========================================================

    @Test
    @DisplayName("createSite_shouldRequireCode — codeSite null provoque violation")
    void createSite_shouldRequireCode() {
        CreateSiteRequest req = validRequest();
        req.setCodeSite(null);
        Set<ConstraintViolation<CreateSiteRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("codeSite");
    }

    @Test
    @DisplayName("createSite_shouldRequireCode — codeSite vide provoque violation")
    void createSite_shouldRequireCode_blank() {
        CreateSiteRequest req = validRequest();
        req.setCodeSite("  ");
        Set<ConstraintViolation<CreateSiteRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("codeSite");
    }

    // =========================================================
    // createSite_shouldRequireNom
    // =========================================================

    @Test
    @DisplayName("createSite_shouldRequireNom — nomSite null provoque violation")
    void createSite_shouldRequireNom() {
        CreateSiteRequest req = validRequest();
        req.setNomSite(null);
        Set<ConstraintViolation<CreateSiteRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("nomSite");
    }

    // =========================================================
    // createSite_shouldRequireZone
    // =========================================================

    @Test
    @DisplayName("createSite_shouldRequireZone — zone null provoque violation")
    void createSite_shouldRequireZone() {
        CreateSiteRequest req = validRequest();
        req.setZone(null);
        Set<ConstraintViolation<CreateSiteRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("zone");
    }

    @Test
    @DisplayName("createSite_shouldRequireZone — zone vide provoque violation")
    void createSite_shouldRequireZone_blank() {
        CreateSiteRequest req = validRequest();
        req.setZone("");
        Set<ConstraintViolation<CreateSiteRequest>> violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("zone");
    }

    // =========================================================
    // createSite_shouldNotRequireReference
    // =========================================================

    @Test
    @DisplayName("createSite_shouldNotRequireReference — pas de champ reference dans CreateSiteRequest")
    void createSite_shouldNotRequireReference() {
        boolean hasReference = false;
        for (java.lang.reflect.Field f : CreateSiteRequest.class.getDeclaredFields()) {
            if (f.getName().equals("reference")) hasReference = true;
        }
        assertThat(hasReference).as("Le DTO ne doit pas avoir le champ 'reference'").isFalse();
    }

    // =========================================================
    // createSite_shouldNotRequireVilleCommuneAdresse
    // =========================================================

    @Test
    @DisplayName("createSite_shouldNotRequireVilleCommuneAdresse — pas de champs ville/commune/adresse")
    void createSite_shouldNotRequireVilleCommuneAdresse() {
        // Le DTO ne doit pas avoir de champs ville/commune/adresse
        // Vérification par reflection que ces champs n'existent pas dans CreateSiteRequest
        boolean hasVille = false;
        boolean hasCommune = false;
        boolean hasAdresse = false;

        for (java.lang.reflect.Field f : CreateSiteRequest.class.getDeclaredFields()) {
            if (f.getName().equals("ville"))    hasVille = true;
            if (f.getName().equals("commune"))  hasCommune = true;
            if (f.getName().equals("adresse"))  hasAdresse = true;
        }

        assertThat(hasVille).as("Le DTO ne doit pas avoir le champ 'ville'").isFalse();
        assertThat(hasCommune).as("Le DTO ne doit pas avoir le champ 'commune'").isFalse();
        assertThat(hasAdresse).as("Le DTO ne doit pas avoir le champ 'adresse'").isFalse();
    }
}
