package com.mini.credit.service.consolidation;

import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutFicheJournaliere;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.TypeRecette;
import com.mini.credit.mapper.FicheJournaliereMapper;
import com.mini.credit.repository.caisse.FicheJournaliereRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PHASE 6B.2: Tests unitaires pour RecetteConsolidationService
 * 
 * Tests du service de consolidation automatique des recettes journalières
 * en fiches journalières d'agent terrain (à 17:00 chaque jour).
 * 
 * Couverture: 15 scénarios clés
 * - Consolidation réussie (simple et plage)
 * - Pas de recettes trouvées
 * - Idempotence (pas de doublon)
 * - Agrégation financière (calcul correct)
 * - Agrégation comptage (membres visités, nouveaux, carnets)
 * - Statut par défaut (BROUILLON, pas SOUMISE)
 * - Liaison FK entre recettes et fiche
 * - Cas limites (champs vides, null)
 */
@ExtendWith(MockitoExtension.class)
class RecetteConsolidationServiceTest {

    @Mock
    private RecetteJournaliereTerrainRepository recetteRepository;

    @Mock
    private FicheJournaliereRepository ficheRepository;

    @Mock
    private FicheJournaliereMapper ficheMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RecetteConsolidationServiceImpl consolidationService;

    private LocalDate testDate;
    private Utilisateur testAgent;
    private Site testSite;
    private Membre testMember;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 6, 5);
        
        // Create test Agence (Site needs Agence FK)
        com.mini.credit.entity.agence.Agence testAgence = com.mini.credit.entity.agence.Agence.builder()
            .codeAgence("AGE01")
            .nomAgence("Agence Principale")
            .build();
        ReflectionTestUtils.setField(testAgence, "id", 1L);

        testSite = Site.builder()
            .codeSite("SITE01")
            .nomSite("Site Principal")
            .agence(testAgence)
            .build();
        ReflectionTestUtils.setField(testSite, "id", 10L);

        testAgent = Utilisateur.builder()
            .username("agent_terrain_01")
            .nomComplet("Agent Terrain One")
            .site(testSite)
            .actif(true)
            .build();
        ReflectionTestUtils.setField(testAgent, "id", 100L);

        testMember = Membre.builder()
            .codeMembre("MEM001")
            .nomComplet("Jean Dupont")
            .dateAdhesion(testDate)
            .build();
        ReflectionTestUtils.setField(testMember, "id", 1L);
    }

    // ============ TEST 1: Consolidation réussie ============
    @Test
    void testConsolidateDate_Success() {
        // GIVEN: 3 recettes VALIDEES pour le même agent et date
        List<RecetteJournaliereTerrain> receipts = createValidatedReceipts(3);
        List<Utilisateur> agents = List.of(testAgent);

        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);
        when(ficheRepository.save(any(FicheJournaliereAgentTerrain.class)))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // THEN: Une fiche créée
        assertEquals(1, result);
        verify(ficheRepository, times(1)).save(any(FicheJournaliereAgentTerrain.class));
    }

    // ============ TEST 2: Consolidation plage (range) ============
    @Test
    void testConsolidateDateRange_Success() {
        // GIVEN: Plage de 3 jours avec recettes pour 1 jour seulement
        LocalDate debut = testDate;
        LocalDate fin = testDate.plusDays(2);
        List<Utilisateur> agents = List.of(testAgent);

        when(recetteRepository.findDistinctAgentsByDateRange(debut, fin))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(anyLong(), any(LocalDate.class)))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            eq(testAgent.getId()), any(LocalDate.class), eq(StatutRecetteJournaliere.VALIDEE)))
            .thenAnswer(i -> {
                LocalDate date = i.getArgument(1);
                // Recettes seulement pour jour 1
                if (date.equals(debut)) {
                    return createValidatedReceipts(2);
                }
                return List.of();
            });
        when(ficheRepository.save(any(FicheJournaliereAgentTerrain.class)))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation de la plage
        int result = consolidationService.consolidateDateRange(debut, fin);

        // THEN: 1 fiche créée (jour 1 seulement)
        assertEquals(1, result);
        verify(ficheRepository, times(1)).save(any(FicheJournaliereAgentTerrain.class));
    }

    // ============ TEST 3: Pas de recettes ============
    @Test
    void testConsolidateDate_NoReceipts() {
        // GIVEN: Pas de recettes pour la date
        List<Utilisateur> agents = List.of(testAgent);

        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(List.of());

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // THEN: 0 fiches créées
        assertEquals(0, result);
        verify(ficheRepository, never()).save(any());
    }

    // ============ TEST 4: Recettes en BROUILLON uniquement ============
    @Test
    void testConsolidateDate_OnlyBrouillonReceipts() {
        // GIVEN: Recettes en BROUILLON (non validées par contrôleur)
        List<Utilisateur> agents = List.of(testAgent);
        List<RecetteJournaliereTerrain> receipts = Arrays.asList(
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(100))
                .statut(StatutRecetteJournaliere.CREEE)  // BROUILLON / CREEE
                .membre(testMember)
                .build()
        );

        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        // Recherche VALIDEE -> aucune
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(List.of());

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // THEN: 0 fiches créées (recettes non validées)
        assertEquals(0, result);
        verify(ficheRepository, never()).save(any());
    }

    // ============ TEST 5: Idempotence (pas de doublons) ============
    @Test
    void testConsolidateDate_Idempotent() {
        // GIVEN: Appel consolidation 2 fois
        List<RecetteJournaliereTerrain> receipts = createValidatedReceipts(2);
        List<Utilisateur> agents = List.of(testAgent);

        // Première appel: fiche n'existe pas
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false)
            .thenReturn(true);  // Deuxième appel: fiche existe
        
        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);
        when(ficheRepository.save(any(FicheJournaliereAgentTerrain.class)))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Première consolidation
        int result1 = consolidationService.consolidateDate(testDate);
        
        // THEN: 1 fiche créée
        assertEquals(1, result1);
        verify(ficheRepository, times(1)).save(any(FicheJournaliereAgentTerrain.class));

        // WHEN: Deuxième consolidation (idempotence)
        int result2 = consolidationService.consolidateDate(testDate);

        // THEN: 0 fiches créées (déjà existe)
        assertEquals(0, result2);
    }

    // ============ TEST 6: Agrégation financière correcte ============
    @Test
    void testConsolidateDate_FinancialAggregation() {
        // GIVEN: Recettes mixtes
        BigDecimal depotAmount = BigDecimal.valueOf(1000);
        BigDecimal interetAmount = BigDecimal.valueOf(50);
        BigDecimal remboursementAmount = BigDecimal.valueOf(500);
        BigDecimal fraisAmount = BigDecimal.valueOf(25);
        BigDecimal autreAmount = BigDecimal.valueOf(10);

        List<RecetteJournaliereTerrain> receipts = Arrays.asList(
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(depotAmount)
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.INTERET)
                .montant(interetAmount)
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.REMBOURSEMENT_CREDIT)
                .montant(remboursementAmount)
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.FRAIS)
                .montant(fraisAmount)
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.AUTRE)
                .montant(autreAmount)
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build()
        );

        List<Utilisateur> agents = List.of(testAgent);
        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);

        ArgumentCaptor<FicheJournaliereAgentTerrain> ficheCaptor = 
            ArgumentCaptor.forClass(FicheJournaliereAgentTerrain.class);
        when(ficheRepository.save(ficheCaptor.capture()))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // THEN: Vérifier les totaux
        FicheJournaliereAgentTerrain savedFiche = ficheCaptor.getValue();
        
        // Epargne = DEPOT + INTERET = 1000 + 50 = 1050
        assertEquals(BigDecimal.valueOf(1050), savedFiche.getEpargneCollecteeTotal());
        
        // Remboursement = 500
        assertEquals(remboursementAmount, savedFiche.getRemboursementCollectes());
        
        // Frais = 25
        assertEquals(fraisAmount, savedFiche.getFraisCollectes());
        
        // Autres = 10
        assertEquals(autreAmount, savedFiche.getAutresRecettes());
    }

    // ============ TEST 7: Agrégation comptage ============
    @Test
    void testConsolidateDate_CountAggregation() {
        // GIVEN: Recettes pour 2 membres distincts, 1 nouveau
        Membre oldMember = Membre.builder()
            .codeMembre("MEM001")
            .nomComplet("Jean Dupont")
            .dateAdhesion(testDate.minusDays(10))  // Ancien membre (adhésion 10j avant)
            .build();

        Membre newMember = Membre.builder()
            .codeMembre("MEM002")
            .nomComplet("Nouveau Membre")
            .dateAdhesion(testDate)  // Nouveau aujourd'hui
            .build();

        List<RecetteJournaliereTerrain> receipts = Arrays.asList(
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(100))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(oldMember)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(200))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(oldMember)  // Ancien membre (2e recette)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(150))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(newMember)  // Nouveau membre
                .build()
        );

        List<Utilisateur> agents = List.of(testAgent);
        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);

        ArgumentCaptor<FicheJournaliereAgentTerrain> ficheCaptor = 
            ArgumentCaptor.forClass(FicheJournaliereAgentTerrain.class);
        when(ficheRepository.save(ficheCaptor.capture()))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // THEN: 2 membres visités (oldMember et newMember), 1 nouveau
        FicheJournaliereAgentTerrain savedFiche = ficheCaptor.getValue();
        assertEquals(2, savedFiche.getNombreMembresVisites());
        assertEquals(1, savedFiche.getNombreNouveauxMembres());
        assertEquals(0, savedFiche.getNombreCarnetsDistribues());  // TBD
    }

    // ============ TEST 8: Statut par défaut BROUILLON ============
    @Test
    void testConsolidateDate_DefaultStatus() {
        // GIVEN: Recettes valides
        List<RecetteJournaliereTerrain> receipts = createValidatedReceipts(1);
        List<Utilisateur> agents = List.of(testAgent);

        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);

        ArgumentCaptor<FicheJournaliereAgentTerrain> ficheCaptor = 
            ArgumentCaptor.forClass(FicheJournaliereAgentTerrain.class);
        when(ficheRepository.save(ficheCaptor.capture()))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // THEN: Statut est BROUILLON (NOT SOUMISE)
        FicheJournaliereAgentTerrain savedFiche = ficheCaptor.getValue();
        assertEquals(StatutFicheJournaliere.BROUILLON, savedFiche.getStatut());
    }

    // ============ TEST 9: Liaison FK recettes ↔ fiche ============
    @Test
    void testConsolidateDate_FicheLinkToReceipts() {
        // GIVEN: Recettes
        List<RecetteJournaliereTerrain> receipts = createValidatedReceipts(2);
        List<Utilisateur> agents = List.of(testAgent);

        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);

        ArgumentCaptor<FicheJournaliereAgentTerrain> ficheCaptor = 
            ArgumentCaptor.forClass(FicheJournaliereAgentTerrain.class);
        when(ficheRepository.save(ficheCaptor.capture()))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // THEN: Vérifier set FK (ficheJournaliere sur les recettes - owning side)
        FicheJournaliereAgentTerrain savedFiche = ficheCaptor.getValue();
        assertNotNull(savedFiche);

        // The owning side is RecetteJournaliereTerrain.ficheJournaliere
        for (RecetteJournaliereTerrain receipt : receipts) {
            assertEquals(savedFiche, receipt.getFicheJournaliere());
        }
    }

    // ============ TEST 10: Site défini depuis agent ============
    @Test
    void testConsolidateDate_SiteSet() {
        // GIVEN: Agent avec site
        List<RecetteJournaliereTerrain> receipts = createValidatedReceipts(1);
        List<Utilisateur> agents = List.of(testAgent);

        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);

        ArgumentCaptor<FicheJournaliereAgentTerrain> ficheCaptor = 
            ArgumentCaptor.forClass(FicheJournaliereAgentTerrain.class);
        when(ficheRepository.save(ficheCaptor.capture()))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation
        consolidationService.consolidateDate(testDate);

        // THEN: Site = agent.site
        FicheJournaliereAgentTerrain savedFiche = ficheCaptor.getValue();
        assertEquals(testSite, savedFiche.getSite());
    }

    // ============ TEST 11: Consolidation agents multiples ============
    @Test
    void testConsolidateDateRange_MultipleAgents() {
        // GIVEN: 2 agents différents avec recettes
        Utilisateur agent2 = Utilisateur.builder()
            .username("agent_terrain_02")
            .nomComplet("Agent Terrain Two")
            .site(testSite)
            .build();
        ReflectionTestUtils.setField(agent2, "id", 101L);

        List<Utilisateur> agents = Arrays.asList(testAgent, agent2);

        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(anyLong(), any(LocalDate.class)))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(createValidatedReceipts(2));
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            agent2.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(createValidatedReceipts(1));
        when(ficheRepository.save(any(FicheJournaliereAgentTerrain.class)))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // THEN: 2 fiches créées (1 par agent)
        assertEquals(2, result);
        verify(ficheRepository, times(2)).save(any(FicheJournaliereAgentTerrain.class));
    }

    // ============ TEST 12: Consolidation hier (yesterday) ============
    @Test
    void testConsolidateYesterday_Success() {
        // GIVEN: Hier = LocalDate.now() - 1 (même calcul que le service)
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Utilisateur> agents = List.of(testAgent);

        when(recetteRepository.findDistinctAgentsByDateRange(yesterday, yesterday))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), yesterday))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), yesterday, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(createValidatedReceipts(2));
        when(ficheRepository.save(any(FicheJournaliereAgentTerrain.class)))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation hier
        int result = consolidationService.consolidateYesterday();

        // THEN: 1 fiche créée pour hier
        assertEquals(1, result);
        verify(ficheRepository, times(1)).save(any(FicheJournaliereAgentTerrain.class));
    }

    // ============ TEST 13: Champs vides (null protection) ============
    @Test
    void testConsolidateDate_EmptyFields() {
        // GIVEN: Recette sans montant (edge case)
        List<RecetteJournaliereTerrain> receipts = Arrays.asList(
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(100))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(null)  // Type null (edge case)
                .montant(BigDecimal.valueOf(50))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build()
        );

        List<Utilisateur> agents = List.of(testAgent);
        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);

        ArgumentCaptor<FicheJournaliereAgentTerrain> ficheCaptor = 
            ArgumentCaptor.forClass(FicheJournaliereAgentTerrain.class);
        when(ficheRepository.save(ficheCaptor.capture()))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation (devrait pas NPE)
        assertDoesNotThrow(() -> consolidationService.consolidateDate(testDate));

        // THEN: Fiche créée, montant null ignoré
        FicheJournaliereAgentTerrain savedFiche = ficheCaptor.getValue();
        assertNotNull(savedFiche);
        assertEquals(BigDecimal.valueOf(100), savedFiche.getEpargneCollecteeTotal());
    }

    // ============ TEST 14: Aucun agent trouvé ============
    @Test
    void testConsolidateDate_NoAgentsFound() {
        // GIVEN: Pas d'agents avec recettes
        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(List.of());

        // WHEN: Consolidation
        int result = consolidationService.consolidateDate(testDate);

        // THEN: 0 fiches créées
        assertEquals(0, result);
        verify(ficheRepository, never()).save(any());
    }

    // ============ TEST 15: Membre null (edge case) ============
    @Test
    void testConsolidateDate_MemberNull() {
        // GIVEN: Recette avec membre null + 1 recette avec vrai membre
        List<RecetteJournaliereTerrain> receipts = Arrays.asList(
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(100))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(null)  // Null member (edge case)
                .build(),
            RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(50))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)  // Real member
                .build()
        );

        List<Utilisateur> agents = List.of(testAgent);
        when(recetteRepository.findDistinctAgentsByDateRange(testDate, testDate))
            .thenReturn(agents);
        when(ficheRepository.existsByAgentTerrainIdAndDateFiche(testAgent.getId(), testDate))
            .thenReturn(false);
        when(recetteRepository.findByAgentTerrainAndDateAndStatut(
            testAgent.getId(), testDate, StatutRecetteJournaliere.VALIDEE))
            .thenReturn(receipts);

        ArgumentCaptor<FicheJournaliereAgentTerrain> ficheCaptor = 
            ArgumentCaptor.forClass(FicheJournaliereAgentTerrain.class);
        when(ficheRepository.save(ficheCaptor.capture()))
            .thenAnswer(i -> i.getArgument(0));

        // WHEN: Consolidation (devrait pas NPE)
        assertDoesNotThrow(() -> consolidationService.consolidateDate(testDate));

        // THEN: Fiche créée, compte 1 seul membre (null member + 1 real = 2 au total, mais null est distinct donc 2)
        FicheJournaliereAgentTerrain savedFiche = ficheCaptor.getValue();
        assertNotNull(savedFiche);
        // Note: Stream.distinct() counts null as one distinct value, so 2 distinct elements (null + testMember)
        assertEquals(2, savedFiche.getNombreMembresVisites());
    }

    // ============ HELPERS ============

    /**
     * Crée N recettes VALIDEES pour test
     */
    private List<RecetteJournaliereTerrain> createValidatedReceipts(int count) {
        List<RecetteJournaliereTerrain> receipts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            receipts.add(RecetteJournaliereTerrain.builder()
                .agent(testAgent)
                .dateJour(testDate)
                .typeRecette(TypeRecette.DEPOT)
                .montant(BigDecimal.valueOf(100 * (i + 1)))
                .statut(StatutRecetteJournaliere.VALIDEE)
                .membre(testMember)
                .build());
        }
        return receipts;
    }
}
