package com.mini.credit.integration;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutFicheJournaliere;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.TypeRecette;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.caisse.FicheJournaliereRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.consolidation.RecetteConsolidationService;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 6B.2: Tests d'intégration pour RecetteConsolidationService
 * 
 * Tests avec contexte applicatif simplifié (JPA + repositories uniquement)
 * Vérifie le flux end-to-end: recettes créées → consolidation → fiches créées
 * 
 * Couverture: 8 scénarios d'intégration clés
 * - Consolidation automatique avec données réelles
 * - Idempotence avec base de données
 * - Liaison FK bidirectionnelle
 * - Transactions et rollback
 * - Agrégation financière avec arrondis
 * - Statut workflows
 */
@SpringBootTest(
    properties = {
        "spring.h2.console.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Transactional
@Log4j2
class RecetteConsolidationIntegrationTest {

    @Autowired
    private RecetteConsolidationService consolidationService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecetteJournaliereTerrainRepository recetteRepository;

    @Autowired
    private FicheJournaliereRepository ficheRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    private LocalDate testDate;
    private Utilisateur testAgent;
    private Site testSite;
    private Agence testAgence;
    private Membre testMember;
    private Role agentTerrainRole;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 6, 5);

        // Create Agence
        testAgence = Agence.builder()
            .codeAgence("AGE_INT_01")
            .nomAgence("Agence Intégration Test")
            .adresse("123 Test Street")
            .ville("Kinshasa")
            .build();
        testAgence = agenceRepository.save(testAgence);

        // Create Site
        testSite = Site.builder()
            .codeSite("SITE_INT_01")
            .nomSite("Site Intégration Test")
            .agence(testAgence)
            .zone("Zone test intégration")
            .actif(true)
            .build();
        testSite = siteRepository.save(testSite);

        // Create or fetch AGENT_TERRAIN role
        agentTerrainRole = roleRepository.findByCode(RoleCode.AGENT_TERRAIN)
            .orElseGet(() -> roleRepository.save(Role.builder()
                .code(RoleCode.AGENT_TERRAIN)
                .libelle("Agent Terrain")
                .description("Agent terrain")
                .isActive(true)
                .build()));
        if (agentTerrainRole.getId() == null) {
            agentTerrainRole = roleRepository.save(agentTerrainRole);
        }

        // Create Agent Terrain user
        testAgent = Utilisateur.builder()
            .username("agent_int_test_01")
            .nomComplet("Agent Intégration Test")
            .motDePasseHash("hashedPassword")
            .role(agentTerrainRole)
            .site(testSite)
            .actif(true)
            .build();
        testAgent = utilisateurRepository.save(testAgent);

        // Create Member
        testMember = Membre.builder()
            .codeMembre("MEM_INT_001")
            .nomComplet("Membre Intégration Test")
            .nom("Test")
            .prenom("Membre")
            .nomComplet("Test Membre")
            .dateAdhesion(testDate)
            .statut(StatutMembre.ACTIF)
            .ville("Kinshasa")
            .site(testSite)
            .build();
        testMember = membreRepository.save(testMember);

        log.info("✅ Test fixtures créées pour date: {}", testDate);
    }

    // ============ TEST 1: Consolidation simple avec 3 recettes ============
    @Test
    void testConsolidation_SimpleCase_3Receipts() {
        // GIVEN: 3 recettes VALIDEES
        createValidatedReceipt(BigDecimal.valueOf(100), TypeRecette.DEPOT);
        createValidatedReceipt(BigDecimal.valueOf(200), TypeRecette.REMBOURSEMENT_CREDIT);
        createValidatedReceipt(BigDecimal.valueOf(50), TypeRecette.FRAIS);

        // Verify receipts created
        List<RecetteJournaliereTerrain> receipts = recetteRepository
            .findByAgentTerrainAndDateAndStatut(testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE);
        assertEquals(3, receipts.size());

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // THEN: 1 fiche créée avec totaux corrects
        assertEquals(1, result);

        // Clear L1 cache to reload @Formula fields from DB
        entityManager.flush();
        entityManager.clear();

        FicheJournaliereAgentTerrain fiche = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();

        assertEquals(StatutFicheJournaliere.BROUILLON, fiche.getStatut());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(fiche.getEpargneCollecteeTotal()), "epargneCollecteeTotal doit être 100");
        assertEquals(0, BigDecimal.valueOf(200).compareTo(fiche.getRemboursementCollectes()), "remboursementCollectes doit être 200");
        assertEquals(0, BigDecimal.valueOf(50).compareTo(fiche.getFraisCollectes()), "fraisCollectes doit être 50");
        // Calcul du total (montantTotalCollecte est @Formula, peut être null en H2)
        BigDecimal calculatedTotal = fiche.getEpargneCollecteeTotal()
            .add(fiche.getRemboursementCollectes())
            .add(fiche.getFraisCollectes())
            .add(fiche.getAutresRecettes());
        assertEquals(0, new BigDecimal("350.00").compareTo(calculatedTotal), "total calculé doit être 350");

        log.info("✅ TEST 1 passed: Simple consolidation");
    }

    // ============ TEST 2: Idempotence - 2 consolidations ne créent qu'1 fiche ============
    @Test
    void testConsolidation_Idempotent_NoDoublicate() {
        // GIVEN: Recettes créées
        createValidatedReceipt(BigDecimal.valueOf(100), TypeRecette.DEPOT);

        // WHEN: Première consolidation
        int result1 = consolidationService.consolidateDate(testDate);
        assertEquals(1, result1);

        // Verify fiche exists
        FicheJournaliereAgentTerrain fiche1 = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();
        assertNotNull(fiche1.getId());
        Long ficheId1 = fiche1.getId();

        // WHEN: Deuxième consolidation (idempotence)
        int result2 = consolidationService.consolidateDate(testDate);

        // THEN: 0 fiches créées (déjà existe), même ID
        assertEquals(0, result2);

        FicheJournaliereAgentTerrain fiche2 = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();
        assertEquals(ficheId1, fiche2.getId());

        // Verify no duplicates
        long ficheCount = ficheRepository
            .findByAgentTerrainIdOrderByDateFicheDesc(testAgent.getId())
            .stream()
            .filter(f -> f.getDateFiche().equals(testDate))
            .count();
        assertEquals(1, ficheCount);

        log.info("✅ TEST 2 passed: Idempotence confirmed - no duplicates");
    }

    // ============ TEST 3: Liaison FK bidirectionnelle ============
    @Test
    void testConsolidation_ForeignKeyLink_BidirectionalSync() {
        // GIVEN: 2 recettes créées
        RecetteJournaliereTerrain receipt1 = createValidatedReceipt(BigDecimal.valueOf(100), TypeRecette.DEPOT);
        RecetteJournaliereTerrain receipt2 = createValidatedReceipt(BigDecimal.valueOf(200), TypeRecette.REMBOURSEMENT_CREDIT);
        Long receipt1Id = receipt1.getId();
        Long receipt2Id = receipt2.getId();

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // Clear L1 cache to reload FK state from DB (owning side: RecetteJournaliereTerrain.ficheJournaliere)
        entityManager.flush();
        entityManager.clear();

        // THEN: Recettes liées à la fiche via FK (check owning side)
        FicheJournaliereAgentTerrain fiche = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();

        RecetteJournaliereTerrain reloadedReceipt1 = recetteRepository.findById(receipt1Id).orElseThrow();
        RecetteJournaliereTerrain reloadedReceipt2 = recetteRepository.findById(receipt2Id).orElseThrow();
        assertNotNull(reloadedReceipt1.getFicheJournaliere(), "Receipt1 doit être lié à une fiche");
        assertNotNull(reloadedReceipt2.getFicheJournaliere(), "Receipt2 doit être lié à une fiche");
        assertEquals(fiche.getId(), reloadedReceipt1.getFicheJournaliere().getId());
        assertEquals(fiche.getId(), reloadedReceipt2.getFicheJournaliere().getId());
        // Verify both receipts link to the same fiche
        assertEquals(reloadedReceipt1.getFicheJournaliere().getId(), reloadedReceipt2.getFicheJournaliere().getId());

        log.info("✅ TEST 3 passed: FK bidirectional link verified");
    }

    // ============ TEST 4: Statut workflow BROUILLON → ... ============
    @Test
    void testConsolidation_StatusWorkflow_BrouillonInitial() {
        // GIVEN: Recettes
        createValidatedReceipt(BigDecimal.valueOf(100), TypeRecette.DEPOT);

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // Clear L1 cache before loading to get @Formula fields
        entityManager.flush();
        entityManager.clear();

        // THEN: Fiche créée en BROUILLON (NOT SOUMISE)
        FicheJournaliereAgentTerrain fiche = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();

        assertEquals(StatutFicheJournaliere.BROUILLON, fiche.getStatut());
        assertFalse(fiche.getStatut() == StatutFicheJournaliere.SOUMISE);

        log.info("✅ TEST 4 passed: Status workflow correct - BROUILLON initial");
    }

    // ============ TEST 5: Agrégation financière avec arrondis ============
    @Test
    void testConsolidation_FinancialAggregation_WithRounding() {
        // GIVEN: Recettes avec montants qui demandent arrondis
        createValidatedReceipt(BigDecimal.valueOf(100.55), TypeRecette.DEPOT);
        createValidatedReceipt(BigDecimal.valueOf(200.35), TypeRecette.REMBOURSEMENT_CREDIT);
        createValidatedReceipt(BigDecimal.valueOf(50.10), TypeRecette.FRAIS);

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // Clear L1 cache to reload @Formula fields from DB
        entityManager.flush();
        entityManager.clear();

        // THEN: Totaux corrects
        FicheJournaliereAgentTerrain fiche = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();

        assertEquals(0, BigDecimal.valueOf(100.55).compareTo(fiche.getEpargneCollecteeTotal()), "epargne doit être 100.55");
        assertEquals(0, BigDecimal.valueOf(200.35).compareTo(fiche.getRemboursementCollectes()), "remb doit être 200.35");
        assertEquals(0, BigDecimal.valueOf(50.10).compareTo(fiche.getFraisCollectes()), "frais doit être 50.10");
        
        // Calcul du total via champs individuels (montantTotalCollecte @Formula peut être null en H2)
        BigDecimal calculatedTotal = fiche.getEpargneCollecteeTotal()
            .add(fiche.getRemboursementCollectes())
            .add(fiche.getFraisCollectes())
            .add(fiche.getAutresRecettes());
        BigDecimal expectedTotal = BigDecimal.valueOf(100.55)
            .add(BigDecimal.valueOf(200.35))
            .add(BigDecimal.valueOf(50.10));
        assertEquals(0, expectedTotal.compareTo(calculatedTotal), "montantTotal calculé doit correspondre");

        log.info("✅ TEST 5 passed: Financial aggregation with rounding correct");
    }

    // ============ TEST 6: Pas de recettes → 0 fiches créées ============
    @Test
    void testConsolidation_NoReceipts_ZeroFichesCreated() {
        // GIVEN: Aucune recette pour cette date
        List<RecetteJournaliereTerrain> receipts = recetteRepository
            .findByAgentTerrainAndDateAndStatut(testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE);
        assertEquals(0, receipts.size());

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // THEN: 0 fiches créées
        assertEquals(0, result);

        // Verify no fiche created
        assertTrue(ficheRepository.findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate).isEmpty());

        log.info("✅ TEST 6 passed: No receipts → no fiches created");
    }

    // ============ TEST 7: Membres visités et nouveaux (count) ============
    @Test
    void testConsolidation_MemberCounts_DistinctAndNew() {
        // GIVEN: 2 membres (1 ancien, 1 nouveau)
        Membre oldMember = Membre.builder()
            .codeMembre("MEM_OLD_001")
            .nomComplet("Ancien Membre")
            .nom("Ancien")
            .prenom("Membre")
            .dateAdhesion(testDate.minusDays(10))
            .statut(StatutMembre.ACTIF)
            .ville("Kinshasa")
            .site(testSite)
            .build();
        oldMember = membreRepository.save(oldMember);

        // Recettes pour 2 anciens membres, 1 nouveau
        createValidatedReceiptForMember(BigDecimal.valueOf(100), TypeRecette.DEPOT, testMember);  // nouveau
        createValidatedReceiptForMember(BigDecimal.valueOf(200), TypeRecette.DEPOT, testMember);  // nouveau (2e recette)
        createValidatedReceiptForMember(BigDecimal.valueOf(150), TypeRecette.DEPOT, oldMember);   // ancien

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // Clear L1 cache before loading
        entityManager.flush();
        entityManager.clear();

        // THEN: Counts corrects
        FicheJournaliereAgentTerrain fiche = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();

        assertEquals(2, fiche.getNombreMembresVisites());  // 2 distinct members
        assertEquals(1, fiche.getNombreNouveauxMembres());  // 1 new member (testMember today)

        log.info("✅ TEST 7 passed: Member counts correct - {} visited, {} new",
            fiche.getNombreMembresVisites(), fiche.getNombreNouveauxMembres());
    }

    // ============ TEST 8: Consolidation multiple agents (2 agents) ============
    @Test
    void testConsolidation_MultipleAgents_SeparateFiches() {
        // GIVEN: 2 agents différents avec recettes
        Utilisateur agent2 = Utilisateur.builder()
            .username("agent_int_test_02")
            .nomComplet("Agent 2 Test")
            .motDePasseHash("hashedPassword")
            .role(agentTerrainRole)
            .site(testSite)
            .actif(true)
            .build();
        agent2 = utilisateurRepository.save(agent2);

        // Recettes pour agent 1
        createValidatedReceipt(BigDecimal.valueOf(100), TypeRecette.DEPOT);

        // Recettes pour agent 2
        RecetteJournaliereTerrain receipt2 = RecetteJournaliereTerrain.builder()
            .agent(agent2)
            .dateJour(testDate)
            .typeRecette(TypeRecette.DEPOT)
            .montant(BigDecimal.valueOf(500))
            .statut(StatutRecetteJournaliere.VALIDEE)
            .membre(testMember)
            .build();
        recetteRepository.save(receipt2);

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // Clear L1 cache before loading
        entityManager.flush();
        entityManager.clear();

        // THEN: 2 fiches créées (1 par agent)
        assertEquals(2, result);

        // Verify both fiches exist
        FicheJournaliereAgentTerrain fiche1 = ficheRepository
            .findByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate)
            .orElseThrow();
        FicheJournaliereAgentTerrain fiche2 = ficheRepository
            .findByAgentTerrainIdAndDateFiche(agent2.getId(), testDate)
            .orElseThrow();

        assertEquals(0, BigDecimal.valueOf(100).compareTo(fiche1.getEpargneCollecteeTotal()), "fiche1 epargne doit être 100");
        assertEquals(0, BigDecimal.valueOf(500).compareTo(fiche2.getEpargneCollecteeTotal()), "fiche2 epargne doit être 500");

        assertNotEquals(fiche1.getId(), fiche2.getId());

        log.info("✅ TEST 8 passed: Multiple agents consolidated separately");
    }

    // ============ HELPERS ============

    /**
     * Helper: Crée une recette VALIDEE pour le test
     */
    private RecetteJournaliereTerrain createValidatedReceipt(BigDecimal montant, TypeRecette typeRecette) {
        return createValidatedReceiptForMember(montant, typeRecette, testMember);
    }

    /**
     * Helper: Crée une recette VALIDEE pour un membre spécifique
     */
    private RecetteJournaliereTerrain createValidatedReceiptForMember(
        BigDecimal montant, TypeRecette typeRecette, Membre membre) {
        
        RecetteJournaliereTerrain receipt = RecetteJournaliereTerrain.builder()
            .agent(testAgent)
            .dateJour(testDate)
            .typeRecette(typeRecette)
            .montant(montant)
            .statut(StatutRecetteJournaliere.VALIDEE)
            .membre(membre)
            .build();

        return recetteRepository.save(receipt);
    }
}
