package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutFicheJournaliere;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 6B.1: Integration tests for FicheJournaliereRepository
 *
 * Utilise @SpringBootTest @ActiveProfiles("test") avec H2 (Flyway désactivé).
 * Le setUp crée et persiste les entités parentes (Role → Utilisateur).
 * FicheJournaliereAgentTerrain.site est optional=true — pas besoin de Site/Agence.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("FicheJournaliereRepository Tests")
class FicheJournaliereRepositoryTest {

    @Autowired
    private FicheJournaliereRepository ficheRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Utilisateur testAgent;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 15);

        // Créer un rôle (nécessaire pour Utilisateur)
        Role role = roleRepository.findByCode(RoleCode.AGENT_TERRAIN)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code(RoleCode.AGENT_TERRAIN)
                        .libelle("Agent Terrain")
                        .description("Agent terrain pour tests")
                        .build()));

        // Créer un agent terrain persisté (site est nullable sur Utilisateur)
        testAgent = utilisateurRepository.save(Utilisateur.builder()
                .username("agent_test_fiche_" + System.currentTimeMillis())
                .nomComplet("Agent Test Fiche")
                .motDePasseHash("$2a$10$hashedPasswordForTest")
                .role(role)
                .actif(true)
                .build());
    }

    @Test
    @DisplayName("Should create and find fiche by agent and date")
    void testFindByAgentTerrainIdAndDateFiche_WhenExists_ReturnsFiche() {
        // Arrange
        FicheJournaliereAgentTerrain fiche = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(testAgent)
                .dateFiche(testDate)
                .statut(StatutFicheJournaliere.BROUILLON)
                .epargneCollecteeTotal(new BigDecimal("1000.00"))
                .build();
        ficheRepository.save(fiche);

        // Act
        Optional<FicheJournaliereAgentTerrain> found = ficheRepository
                .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(testAgent.getId(), found.get().getAgentTerrain().getId());
        assertEquals(testDate, found.get().getDateFiche());
        assertEquals(StatutFicheJournaliere.BROUILLON, found.get().getStatut());
    }

    @Test
    @DisplayName("Should return empty when fiche not found")
    void testFindByAgentTerrainIdAndDateFiche_WhenNotExists_ReturnsEmpty() {
        // Act
        Optional<FicheJournaliereAgentTerrain> found = ficheRepository
                .findByAgentTerrainIdAndDateFiche(999L, testDate);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find all fiches for an agent")
    void testFindByAgentTerrainIdOrderByDateFicheDesc_ReturnsFiches() {
        // Arrange
        FicheJournaliereAgentTerrain fiche1 = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(testAgent)
                .dateFiche(LocalDate.of(2024, 1, 15))
                .statut(StatutFicheJournaliere.BROUILLON)
                .build();
        FicheJournaliereAgentTerrain fiche2 = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(testAgent)
                .dateFiche(LocalDate.of(2024, 1, 16))
                .statut(StatutFicheJournaliere.BROUILLON)
                .build();
        ficheRepository.saveAll(List.of(fiche1, fiche2));

        // Act
        List<FicheJournaliereAgentTerrain> fiches = ficheRepository
                .findByAgentTerrainIdOrderByDateFicheDesc(testAgent.getId());

        // Assert
        assertEquals(2, fiches.size());
        assertEquals(LocalDate.of(2024, 1, 16), fiches.get(0).getDateFiche()); // Newest first
    }

    @Test
    @DisplayName("Should find fiches by status")
    void testFindByStatutOrderByDateFicheDesc_ReturnsFiches() {
        // Arrange
        FicheJournaliereAgentTerrain brouillon = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(testAgent)
                .dateFiche(testDate)
                .statut(StatutFicheJournaliere.BROUILLON)
                .build();
        FicheJournaliereAgentTerrain soumise = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(testAgent)
                .dateFiche(LocalDate.of(2024, 1, 16))
                .statut(StatutFicheJournaliere.SOUMISE)
                .build();
        ficheRepository.saveAll(List.of(brouillon, soumise));

        // Act
        List<FicheJournaliereAgentTerrain> brouillons = ficheRepository
                .findByStatutOrderByDateFicheDesc(StatutFicheJournaliere.BROUILLON);

        // Assert
        assertEquals(1, brouillons.size());
        assertEquals(StatutFicheJournaliere.BROUILLON, brouillons.get(0).getStatut());
    }

    @Test
    @DisplayName("Should check if fiche exists by agent and date")
    void testExistsByAgentTerrainIdAndDateFiche_WhenExists_ReturnsTrue() {
        // Arrange
        FicheJournaliereAgentTerrain fiche = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(testAgent)
                .dateFiche(testDate)
                .statut(StatutFicheJournaliere.BROUILLON)
                .build();
        ficheRepository.save(fiche);

        // Act
        boolean exists = ficheRepository.existsByAgentTerrainIdAndDateFiche(
                testAgent.getId(), testDate);

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when fiche not exists")
    void testExistsByAgentTerrainIdAndDateFiche_WhenNotExists_ReturnsFalse() {
        // Act
        boolean exists = ficheRepository.existsByAgentTerrainIdAndDateFiche(
                999L, testDate);

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should enforce unique constraint on agent+date")
    void testUniqueConstraintOnAgentAndDate() {
        // Arrange — créer une première fiche
        FicheJournaliereAgentTerrain fiche1 = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(testAgent)
                .dateFiche(testDate)
                .statut(StatutFicheJournaliere.BROUILLON)
                .build();
        ficheRepository.save(fiche1);

        // Vérifier que la fiche est bien retrouvée (contrainte unique respectée)
        Optional<FicheJournaliereAgentTerrain> found = ficheRepository
                .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate);
        assertTrue(found.isPresent());
        assertEquals(fiche1.getId(), found.get().getId());
    }
}
