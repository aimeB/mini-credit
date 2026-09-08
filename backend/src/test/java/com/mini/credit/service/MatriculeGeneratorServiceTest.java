package com.mini.credit.service;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.repository.EmployeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour MatriculeGeneratorService.
 *
 * Vérifie le format AGENCE-FONCTION-AA-SEQ et l'incrémentation de la séquence.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatriculeGeneratorService — Tests de génération du matricule")
class MatriculeGeneratorServiceTest {

    @Mock
    private EmployeRepository employeRepository;

    @InjectMocks
    private MatriculeGeneratorService service;

    private Agence agenceDelvaux;

    @BeforeEach
    void setUp() {
        agenceDelvaux = new Agence();
        agenceDelvaux.setId(1L);
        agenceDelvaux.setNomAgence("Agence Delvaux");
        agenceDelvaux.setCodeAgence("DEL1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Format du matricule
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generer_shouldUseAgenceCodeAsPrefix — le code agence constitue le premier segment")
    void generer_shouldUseAgenceCodeAsPrefix() {
        when(employeRepository.findMatriculesByPrefix(startsWith("DEL1-GES-")))
                .thenReturn(Collections.emptyList());

        String matricule = service.generer(agenceDelvaux, PosteEmploye.GESTIONNAIRE, 2026);

        assertThat(matricule).startsWith("DEL1-");
    }

    @Test
    @DisplayName("generer_shouldUseFonctionCodeGES — GESTIONNAIRE génère le code GES")
    void generer_shouldUseFonctionCodeGES() {
        when(employeRepository.findMatriculesByPrefix("DEL1-GES-26-"))
                .thenReturn(Collections.emptyList());

        String matricule = service.generer(agenceDelvaux, PosteEmploye.GESTIONNAIRE, 2026);

        assertThat(matricule).contains("-GES-");
    }

    @Test
    @DisplayName("generer_shouldUseFonctionCodeAT — AGENT_TERRAIN génère le code AT")
    void generer_shouldUseFonctionCodeAT() {
        when(employeRepository.findMatriculesByPrefix("DEL1-AT-26-"))
                .thenReturn(Collections.emptyList());

        String matricule = service.generer(agenceDelvaux, PosteEmploye.AGENT_TERRAIN, 2026);

        assertThat(matricule).contains("-AT-");
    }

    @Test
    @DisplayName("generer_shouldUseTwoDigitYear — l'année est sur 2 chiffres (2026 → 26)")
    void generer_shouldUseTwoDigitYear() {
        when(employeRepository.findMatriculesByPrefix("DEL1-GES-26-"))
                .thenReturn(Collections.emptyList());

        String matricule = service.generer(agenceDelvaux, PosteEmploye.GESTIONNAIRE, 2026);

        // Le troisième segment doit être "26"
        String[] parts = matricule.split("-");
        assertThat(parts).hasSize(4);
        assertThat(parts[2]).isEqualTo("26");
    }

    @Test
    @DisplayName("generer_firstEmploye_shouldReturnSeq001 — premier employé → séquence 001")
    void generer_firstEmploye_shouldReturnSeq001() {
        when(employeRepository.findMatriculesByPrefix("DEL1-GES-26-"))
                .thenReturn(Collections.emptyList());

        String matricule = service.generer(agenceDelvaux, PosteEmploye.GESTIONNAIRE, 2026);

        assertThat(matricule).isEqualTo("DEL1-GES-26-001");
    }

    @Test
    @DisplayName("generer_secondEmploye_shouldReturnSeq002 — deuxième employé même agence/fonction/année → 002")
    void generer_secondEmploye_shouldReturnSeq002() {
        // Simule un premier employé déjà créé
        when(employeRepository.findMatriculesByPrefix("DEL1-GES-26-"))
                .thenReturn(List.of("DEL1-GES-26-001"));

        String matricule = service.generer(agenceDelvaux, PosteEmploye.GESTIONNAIRE, 2026);

        assertThat(matricule).isEqualTo("DEL1-GES-26-002");
    }

    @Test
    @DisplayName("generer_withGapsInSeq_shouldReturnMaxPlusOne — reprend après le max, pas après le dernier connu")
    void generer_withGapsInSeq_shouldReturnMaxPlusOne() {
        // 001 existe, 002 manque, 003 existe → prochain doit être 004
        when(employeRepository.findMatriculesByPrefix("DEL1-GES-26-"))
                .thenReturn(List.of("DEL1-GES-26-001", "DEL1-GES-26-003"));

        String matricule = service.generer(agenceDelvaux, PosteEmploye.GESTIONNAIRE, 2026);

        assertThat(matricule).isEqualTo("DEL1-GES-26-004");
    }

    @Test
    @DisplayName("generer_differentFunctions_haveIndependentSeq — séquences indépendantes par fonction")
    void generer_differentFunctions_haveIndependentSeq() {
        // Il y a déjà 2 GES → prochain sera 003
        when(employeRepository.findMatriculesByPrefix("DEL1-GES-26-"))
                .thenReturn(List.of("DEL1-GES-26-001", "DEL1-GES-26-002"));
        // Mais pas encore de CAI → premier sera 001
        when(employeRepository.findMatriculesByPrefix("DEL1-CAI-26-"))
                .thenReturn(Collections.emptyList());

        String ges = service.generer(agenceDelvaux, PosteEmploye.GESTIONNAIRE, 2026);
        String cai = service.generer(agenceDelvaux, PosteEmploye.CAISSIER, 2026);

        assertThat(ges).isEqualTo("DEL1-GES-26-003");
        assertThat(cai).isEqualTo("DEL1-CAI-26-001");
    }

    @Test
    @DisplayName("generer_allFunctionCodes_shouldBeNonNull — tous les postes ont un code court défini")
    void generer_allFunctionCodes_shouldBeNonNull() {
        for (PosteEmploye fonction : PosteEmploye.values()) {
            String code = service.getCodeFonction(fonction);
            assertThat(code)
                    .as("Code court pour %s ne doit pas être null/vide", fonction.name())
                    .isNotNull()
                    .isNotEmpty();
        }
    }
}
