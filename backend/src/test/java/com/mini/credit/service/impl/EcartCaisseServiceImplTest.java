package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.EcartCaisseDTO;
import com.mini.credit.entity.caisse.EcartCaisse;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.TypeEcartCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.EcartCaisseMapper;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.service.EcartThresholdConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PATCH 8B — Tests unitaires EcartCaisseServiceImpl.
 *
 * RBAC (ADMIN, CONTROLEUR, CHEF_BUREAU, CAISSIER) est appliqué au niveau du controller
 * par Spring Security (@PreAuthorize). Les tests unitaires de service vérifient uniquement
 * la logique métier et les transitions de statut.
 *
 * Seuils (PATCH 8B) :
 *   Le flag seuilDépassé est calculé à la création via EcartThresholdConfigService.
 *   Aucun seuil numérique n'est codé en dur dans l'entité ou le service.
 *   La valeur du seuil n'est pas définie dans les documents 3N fournis.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PATCH 8 — EcartCaisseServiceImpl — Tests unitaires")
class EcartCaisseServiceImplTest {

    @Mock private EcartCaisseRepository ecartRepository;
    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private RecetteJournaliereTerrainRepository recetteRepository;
    @Mock private EcartCaisseMapper ecartMapper;
    /** PATCH 8B — Mock du service de seuils paramétrables. */
    @Mock private EcartThresholdConfigService thresholdService;

    @InjectMocks
    private EcartCaisseServiceImpl service;

    // ===== Fixtures =====

    private EcartCaisse buildEcart(StatutEcartCaisse statut, BigDecimal montant, boolean seuilDepassé) {
        SessionCaisse session = new SessionCaisse();
        session.setId(1L);

        EcartCaisse e = EcartCaisse.builder()
                .sessionCaisse(session)
                .dateJour(LocalDate.now())
                .typeEcart(TypeEcartCaisse.DEFICIT)
                .montantEcart(montant)
                .description("Écart détecté lors de la clôture")
                .statut(statut)
                .seuilDepassé(seuilDepassé)
                .build();
        e.setId(42L);
        return e;
    }

    private EcartCaisseDTO buildDTO(Long id, String statut) {
        return EcartCaisseDTO.builder().id(id).statut(statut).build();
    }

    // ===== T1 : getBySession =====

    /**
     * PATCH 8 — getBySession_shouldReturnEcarts
     * Vérifie que getBySessionCaisseId() retourne les écarts d'une session.
     */
    @Test
    @DisplayName("T1 — getBySession: retourne les écarts de la session")
    void getBySession_shouldReturnEcarts() {
        EcartCaisse e1 = buildEcart(StatutEcartCaisse.DETECTE, BigDecimal.valueOf(5000), false);
        EcartCaisse e2 = buildEcart(StatutEcartCaisse.EN_INVESTIGATION, BigDecimal.valueOf(200), false);

        when(ecartRepository.findBySessionCaisseIdOrderByDateCreationAsc(1L)).thenReturn(List.of(e1, e2));
        when(ecartMapper.toDTO(e1)).thenReturn(buildDTO(42L, "DETECTE"));
        when(ecartMapper.toDTO(e2)).thenReturn(buildDTO(43L, "EN_INVESTIGATION"));

        List<EcartCaisseDTO> result = service.getBySessionCaisseId(1L);

        assertEquals(2, result.size());
        verify(ecartRepository).findBySessionCaisseIdOrderByDateCreationAsc(1L);
    }

    // ===== T2 : justifier — validation =====

    /**
     * PATCH 8 — justifier_shouldFail_whenJustificationEmpty
     * Justification vide → BusinessException.
     */
    @Test
    @DisplayName("T2a — justifier: échoue si justification vide")
    void justifier_shouldFail_whenJustificationEmpty() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.justifier(1L, ""));
        assertTrue(ex.getMessage().contains("10 caractères"), "Message doit mentionner le minimum");
    }

    /**
     * PATCH 8 — justifier_shouldFail_whenJustificationTooShort
     * Justification < 10 caractères → BusinessException.
     * NOTE: Validation technique provisoire — règle non définie dans les documents 3N.
     */
    @Test
    @DisplayName("T2b — justifier: échoue si justification < 10 caractères [règle provisoire]")
    void justifier_shouldFail_whenJustificationTooShort() {
        assertThrows(BusinessException.class,
                () -> service.justifier(1L, "Court"),
                "Justification trop courte doit lever BusinessException");
    }

    /**
     * PATCH 8 — justifier_shouldFail_whenEcartAlreadyResolved
     * Un écart RESOLU ne peut plus être justifié.
     */
    @Test
    @DisplayName("T2c — justifier: échoue si écart déjà RESOLU")
    void justifier_shouldFail_whenEcartAlreadyResolved() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.RESOLU, BigDecimal.valueOf(1000), false);
        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));

        assertThrows(BusinessException.class,
                () -> service.justifier(42L, "Justification suffisamment longue pour passer"));
    }

    // ===== T3 : justifier — succès =====

    /**
     * PATCH 8 — justifier_shouldPass_whenJustificationValid
     * Justification valide → sauvegardée dans notesInvestigation, statut inchangé.
     */
    @Test
    @DisplayName("T3 — justifier: succès avec justification valide")
    void justifier_shouldPass_whenJustificationValid() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.DETECTE, BigDecimal.valueOf(500), false);
        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));
        when(ecartRepository.save(any(EcartCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ecartMapper.toDTO(any(EcartCaisse.class))).thenReturn(buildDTO(42L, "DETECTE"));

        service.justifier(42L, "Erreur de comptage à l'ouverture de session.");

        // Statut inchangé
        assertEquals(StatutEcartCaisse.DETECTE, ecart.getStatut());

        // Justification ajoutée aux notes
        assertNotNull(ecart.getNotesInvestigation());
        assertTrue(ecart.getNotesInvestigation().contains("[Justification]"),
                "Les notes doivent contenir le marqueur de justification");

        verify(ecartRepository).save(ecart);
    }

    // ===== T4 : ouvrir enquête =====

    /**
     * PATCH 8 — ouvrirEnquete_shouldSetStatusEnInvestigation
     * enqueterEcart() passe le statut à EN_INVESTIGATION et enregistre les notes.
     */
    @Test
    @DisplayName("T4 — ouvrirEnquete: statut → EN_INVESTIGATION")
    void ouvrirEnquete_shouldSetStatusEnInvestigation() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.DETECTE, BigDecimal.valueOf(3000), false);
        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));
        when(ecartRepository.save(any(EcartCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ecartMapper.toDTO(any(EcartCaisse.class))).thenReturn(buildDTO(42L, "EN_INVESTIGATION"));

        service.enqueterEcart(42L, "Investigation formelle ouverte par le contrôleur.");

        assertEquals(StatutEcartCaisse.EN_INVESTIGATION, ecart.getStatut());
        assertEquals("Investigation formelle ouverte par le contrôleur.", ecart.getNotesInvestigation());
        verify(ecartRepository).save(ecart);
    }

    // ===== T5 : résoudre =====

    /**
     * PATCH 8 — resoudre_shouldFail_whenEcartAlreadyResolved
     * Un écart déjà RESOLU ne peut plus être résolu (canBeResolved() = false).
     */
    @Test
    @DisplayName("T5a — resoudre: échoue si statut RESOLU")
    void resoudre_shouldFail_whenEcartAlreadyResolved() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.RESOLU, BigDecimal.valueOf(1000), false);
        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));

        assertThrows(BusinessException.class,
                () -> service.resoudreEcart(42L, "Raison de résolution"),
                "Doit rejeter la résolution d'un écart déjà résolu");
    }

    /**
     * PATCH 8 — resoudre_shouldPass_whenEcartIsDetecte
     * Un écart DETECTE peut être résolu directement.
     * NOTE : Le RBAC (seul CONTROLEUR peut résoudre) est contrôlé par @PreAuthorize au controller.
     */
    @Test
    @DisplayName("T5b — resoudre: succès pour statut DETECTE")
    void resoudre_shouldPass_whenEcartIsDetecte() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.DETECTE, BigDecimal.valueOf(1000), false);
        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));

        ArgumentCaptor<EcartCaisse> captor = ArgumentCaptor.forClass(EcartCaisse.class);
        when(ecartRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(ecartMapper.toDTO(any(EcartCaisse.class))).thenReturn(buildDTO(42L, "RESOLU"));

        service.resoudreEcart(42L, "Différence de monnaie rendue résolue à la réouverture.");

        EcartCaisse saved = captor.getValue();
        assertEquals(StatutEcartCaisse.RESOLU, saved.getStatut());
        assertEquals("Différence de monnaie rendue résolue à la réouverture.", saved.getRaisonResolution());
    }

    // ===== T6 : accepter =====

    /**
     * PATCH 8B — accepter_shouldFail_whenSeuilNotDepassé
     * Si seuilDepassé = false, accepterEcart() doit rejeter.
     * Après PATCH 8B : requiresRCIValidation() vérifie uniquement le flag seuilDepassé,
     * aucune comparaison à une constante 10 000 dans l'entité.
     */
    @Test
    @DisplayName("T6a — accepter: échoue si seuil non dépassé")
    void accepter_shouldFail_whenSeuilNotDepassé() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.RESOLU, BigDecimal.valueOf(500), false);
        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));

        assertThrows(BusinessException.class,
                () -> service.accepterEcart(42L),
                "Doit rejeter si seuil non dépassé");
    }

    /**
     * PATCH 8 — accepter_shouldPass_whenSeuilDepassé
     * Si seuilDepassé = true, accepterEcart() doit passer → ACCEPTE.
    * NOTE : Le RBAC (seul CHEF_BUREAU peut accepter) est contrôlé par @PreAuthorize.
     */
    @Test
    @DisplayName("T6b — accepter: succès si seuil dépassé")
    void accepter_shouldPass_whenSeuilDepassé() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.RESOLU, BigDecimal.valueOf(15000), true);
        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));

        ArgumentCaptor<EcartCaisse> captor = ArgumentCaptor.forClass(EcartCaisse.class);
        when(ecartRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(ecartMapper.toDTO(any(EcartCaisse.class))).thenReturn(buildDTO(42L, "ACCEPTE"));

        service.accepterEcart(42L);

        assertEquals(StatutEcartCaisse.ACCEPTE, captor.getValue().getStatut());
    }

    // ===== T7 : getAll =====

    /**
     * PATCH 8 — getAll_shouldReturnAllEcarts
     * Vérifie que getAll() délègue à ecartRepository.findAll().
     */
    @Test
    @DisplayName("T7 — getAll: retourne tous les écarts")
    void getAll_shouldReturnAllEcarts() {
        EcartCaisse e = buildEcart(StatutEcartCaisse.DETECTE, BigDecimal.valueOf(1000), false);
        when(ecartRepository.findAll()).thenReturn(List.of(e));
        when(ecartMapper.toDTO(e)).thenReturn(buildDTO(42L, "DETECTE"));

        List<EcartCaisseDTO> result = service.getAll();

        assertEquals(1, result.size());
        verify(ecartRepository).findAll();
    }

    // ===== T8 : justifier — append =====

    /**
     * PATCH 8 — justifier_shouldAppend_whenNotesAlreadyExist
     * Si des notes existent déjà, la justification doit être ajoutée (append), pas remplacée.
     */
    @Test
    @DisplayName("T8 — justifier: ajoute à la suite des notes existantes")
    void justifier_shouldAppend_whenNotesAlreadyExist() {
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.DETECTE, BigDecimal.valueOf(200), false);
        ecart.setNotesInvestigation("Note initiale de l'enquête.");

        when(ecartRepository.findById(42L)).thenReturn(Optional.of(ecart));
        when(ecartRepository.save(any(EcartCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ecartMapper.toDTO(any())).thenReturn(buildDTO(42L, "DETECTE"));

        service.justifier(42L, "Justification complémentaire du caissier.");

        assertTrue(ecart.getNotesInvestigation().contains("Note initiale"),
                "La note initiale doit être conservée");
        assertTrue(ecart.getNotesInvestigation().contains("[Justification]"),
                "La nouvelle justification doit être ajoutée");
    }

    // ===== T9 PATCH 8B : seuil via config service =====

    /**
     * PATCH 8B — requiresRCIValidation_shouldUseConfigThreshold
     * Quand EcartThresholdConfigService retourne true pour un montant donné,
     * detecterEcart() doit créer l'écart avec seuilDepassé = true.
     * Vérifie que le service délègue la décision à EcartThresholdConfigService,
     * sans comparaison à une constante codée en dur.
     *
     * NOTE : La valeur du seuil n'est pas définie dans les documents 3N fournis.
     */
    @Test
    @DisplayName("T9 PATCH 8B — detecterEcart: seuilDepassé calculé via EcartThresholdConfigService")
    void requiresRCIValidation_shouldUseConfigThreshold() {
        BigDecimal montant = BigDecimal.valueOf(8000);

        // Le service de seuils dit que ce montant dépasse le seuil configuré
        when(thresholdService.necessiteValidationRCI(montant)).thenReturn(true);

        // Aucune session ni recette n'est nécessaire pour ce test
        when(ecartRepository.save(any(EcartCaisse.class))).thenAnswer(inv -> {
            EcartCaisse saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(ecartMapper.toDTO(any(EcartCaisse.class))).thenReturn(buildDTO(99L, "DETECTE"));

        service.detecterEcart(null, null, LocalDate.now(), "DEFICIT", montant,
                "Test seuil configurable", null); // seuilDepassé = null → calculé par service

        ArgumentCaptor<EcartCaisse> captor = ArgumentCaptor.forClass(EcartCaisse.class);
        verify(ecartRepository).save(captor.capture());

        assertTrue(captor.getValue().getSeuilDepassé(),
                "seuilDepassé doit être true car EcartThresholdConfigService retourne true");
        verify(thresholdService).necessiteValidationRCI(montant);
    }

    /**
     * PATCH 8B — noHardcodedThresholdInEcartCaisseEntity
     * Quand EcartThresholdConfigService retourne false ET seuilDepassé fourni est false,
     * requiresRCIValidation() doit retourner false (le flag seul décide, pas une constante).
     * Avant PATCH 8B, un montant de 15 000 déclenchait la validation via montant > 10 000.
     * Après PATCH 8B, seul le flag seuilDepassé compte dans l'entité.
     */
    @Test
    @DisplayName("T10 PATCH 8B — requiresRCIValidation: basé sur flag, pas sur constante 10 000")
    void noHardcodedThresholdInEcartCaisseEntity() {
        // Crée un écart avec un grand montant MAIS seuilDepassé = false
        EcartCaisse ecart = buildEcart(StatutEcartCaisse.RESOLU, BigDecimal.valueOf(15000), false);

        // Avant PATCH 8B : requiresRCIValidation() aurait retourné true (15000 > 10000).
        // Après PATCH 8B : doit retourner false car seuilDepassé = false.
        assertFalse(ecart.requiresRCIValidation(),
                "PATCH 8B: requiresRCIValidation() doit dépendre uniquement du flag seuilDepassé, " +
                "pas d'une constante 10 000 codée en dur");
    }

    // ===== T11 PATCH 11 : RoleCode.RCI existe =====

    /**
     * PATCH 11 — rciRoleCode_shouldExistInEnum
     * Vérifie que RoleCode.RCI est bien présent dans l'énumération.
     * Avant PATCH 11, RCI était une dette technique non implémentée.
     *
    * RCI ≠ Chef de Bureau ≠ Contrôleur (CONTROLEUR).
     * Ces trois rôles sont distincts dans les documents 3N.
     */
    @Test
    @DisplayName("T11 PATCH 11 — RoleCode.RCI existe et est distinct des autres rôles")
    void rciRoleCode_shouldExistInEnum() {
        com.mini.credit.enums.security.RoleCode rci =
                com.mini.credit.enums.security.RoleCode.RCI;

        assertNotNull(rci, "RoleCode.RCI doit exister (PATCH 11)");

        // RCI est distinct de Chef de Bureau et CONTROLEUR
        assertNotEquals(rci, com.mini.credit.enums.security.RoleCode.CHEF_BUREAU,
            "RCI ne doit pas être confondu avec CHEF_BUREAU");
        assertNotEquals(rci, com.mini.credit.enums.security.RoleCode.CONTROLEUR,
                "RCI ne doit pas être confondu avec CONTROLEUR");

        // Vérification de la description
        assertTrue(rci.getDescription().toLowerCase().contains("audit") ||
                   rci.getDescription().toLowerCase().contains("contr"),
                "La description de RCI doit mentionner audit ou contrôle interne");
    }
}
