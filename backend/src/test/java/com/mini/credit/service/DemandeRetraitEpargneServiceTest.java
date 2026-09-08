package com.mini.credit.service;

import com.mini.credit.dto.epargne.DemandeRetraitEpargneDTO;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.document.TicketRecuGenerationRequest;
import com.mini.credit.dto.document.TicketRecuResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutDemandeRetrait;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.DemandeRetraitEpargneMapper;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.TicketRecuService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.impl.DemandeRetraitEpargneServiceImpl;
import com.mini.credit.service.impl.RetraitEpargneCommissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour DemandeRetraitEpargneService
 * 
 * PHASE 6B.3: Vérifier le workflow complet de retraits épargne
 * - Création demande
 * - Validation par Contrôleur
 * - Paiement par Caissier (CRITIQUE: avec OperationEpargne + OperationCaisse)
 */
@ExtendWith(MockitoExtension.class)
class DemandeRetraitEpargneServiceTest {

    @Mock
    private DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;

    @Mock
    private CompteEpargneRepository compteEpargneRepository;

    @Mock
    private OperationEpargneRepository operationEpargneRepository;

    @Mock
    private OperationCaisseService operationCaisseService;

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private OperationCaisseRepository operationCaisseRepository;

    @Mock
    private DemandeRetraitEpargneMapper demandeRetraitEpargneMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private WorkflowTaskService workflowTaskService;

    @Mock
    private TicketRecuService ticketRecuService;

    @Spy
    private RetraitEpargneCommissionServiceImpl retraitEpargneCommissionService = new RetraitEpargneCommissionServiceImpl();

    @InjectMocks
    private DemandeRetraitEpargneServiceImpl service;

    private Membre membre;
    private CompteEpargne compteEpargne;
    private DemandeRetraitEpargne demande;
    private SessionCaisse sessionCaisse;
    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .nomComplet("Controleur Test")
                .username("controleur")
            .role(com.mini.credit.entity.referentiel.Role.builder().code(RoleCode.CAISSIER).build())
                .build();
        utilisateur.setId(1L); // Set ID explicitly
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities())
        );

        membre = Membre.builder()
                .nom("Membre Test")
                .build();
        membre.setId(1L); // Set ID explicitly

        Agence agenceMembre = Agence.builder().build();
        agenceMembre.setId(10L);
        Site siteMembre = Site.builder().agence(agenceMembre).build();
        siteMembre.setId(20L);
        membre.setSite(siteMembre);

        compteEpargne = new CompteEpargne();
        compteEpargne.setId(1L);
        compteEpargne.setMembre(membre);
        compteEpargne.setNumeroCompte("EPARGNE-001");
        compteEpargne.setTypeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE);
        compteEpargne.setSoldeDisponible(new BigDecimal("20000.00"));
        compteEpargne.setSoldeBloque(new BigDecimal("500.00"));
        compteEpargne.setStatut(StatutCompte.ACTIF);
        compteEpargne.setDateOuverture(LocalDate.now().minusYears(1));

        Caisse caisse = new Caisse();
        caisse.setId(1L);

        sessionCaisse = new SessionCaisse();
        sessionCaisse.setId(1L);
        sessionCaisse.setCaisse(caisse);
        sessionCaisse.setStatut(StatutSessionCaisse.OUVERTE);
        sessionCaisse.setDateOuverture(LocalDateTime.now());
        sessionCaisse.setSoldeOuverture(BigDecimal.ZERO);
        sessionCaisse.setTotalEntrees(BigDecimal.ZERO);
        sessionCaisse.setTotalSorties(BigDecimal.ZERO);
        sessionCaisse.setSoldeTheorique(BigDecimal.ZERO);

        demande = new DemandeRetraitEpargne();
        demande.setId(1L);
        demande.setCompteEpargne(compteEpargne);
        demande.setMembre(membre);
        demande.setMontantDemande(new BigDecimal("200.00"));
        demande.setStatut(StatutDemandeRetrait.CREEE);
        demande.setDateDemande(LocalDateTime.now());
    }

    // ========== TEST 1: Création Demande ==========
    @Test
    void testCreerDemande_Success() {
        when(compteEpargneRepository.findById(1L)).thenReturn(Optional.of(compteEpargne));
        when(demandeRetraitEpargneRepository.save(any())).thenAnswer(inv -> {
            DemandeRetraitEpargne saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });
        when(operationCaisseRepository.findByRetraitEpargneIdOrderByDateOperationDesc(1L)).thenReturn(List.of());
        when(demandeRetraitEpargneMapper.toDTO(any(DemandeRetraitEpargne.class))).thenAnswer(inv -> {
            DemandeRetraitEpargne saved = inv.getArgument(0);
            return DemandeRetraitEpargneDTO.builder()
                    .id(saved.getId())
                    .referenceRetrait(saved.getReferenceRetrait())
                    .statut(saved.getStatut().name())
                    .montantDemande(saved.getMontantDemande())
                    .fraisRetrait(saved.getFraisRetrait())
                    .tauxCommissionRetrait(saved.getTauxCommissionRetrait())
                    .montantTotalDebite(saved.getMontantTotalDebite())
                    .build();
        });

        DemandeRetraitEpargneDTO result = service.creerDemande(1L, new BigDecimal("1000.00"), BigDecimal.ZERO, "Test");
        assertNotNull(result);
        assertEquals("CREEE", result.getStatut());
        assertEquals(new BigDecimal("90.00"), result.getFraisRetrait());
        assertEquals(new BigDecimal("9.00"), result.getTauxCommissionRetrait());
        assertEquals(new BigDecimal("1090.00"), result.getMontantTotalDebite());
        assertEquals(currentRetraitReference(), result.getReferenceRetrait());
        verify(demandeRetraitEpargneRepository, times(2)).save(any());
        verify(workflowTaskService).onRetraitDemande(1L, currentRetraitReference(), 10L, 20L);
    }

    @Test
    void retraitCdf999Refuse() {
        assertThrows(BusinessException.class,
                () -> service.creerDemande(1L, new BigDecimal("999.00"), BigDecimal.ZERO, "Test minimum"));
        verify(demandeRetraitEpargneRepository, never()).save(any());
        verify(operationEpargneRepository, never()).save(any());
        verify(operationCaisseService, never()).enregistrer(any());
    }

    @Test
    void testCreerDemande_MontantNegatif() {
        assertThrows(BusinessException.class, 
            () -> service.creerDemande(1L, new BigDecimal("-100"), BigDecimal.ZERO, "Test"));
    }

    private String currentRetraitReference() {
        return "RET-" + LocalDate.now().getYear() + "-0001";
    }

    // ========== TEST 2: Validation Demande ==========
    @Test
    void testValiderDemande_SoldeDisponibleSuffisant() {
        demande.setStatut(StatutDemandeRetrait.CREEE);
        demande.setMontantDemande(new BigDecimal("500.00")); // < 1000 disponible
        
        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .statut("VALIDEE")
                    .build()
        );

        service.validerDemande(1L);

        assertEquals(StatutDemandeRetrait.VALIDEE, demande.getStatut());
        verify(demandeRetraitEpargneRepository).save(any());
        verify(workflowTaskService).onRetraitApprouve(1L, currentRetraitReference(), 10L, 20L);
    }

    @Test
    void testValiderDemande_SoldeDisponibleInsuffisant() {
        demande.setStatut(StatutDemandeRetrait.CREEE);
        demande.setMontantDemande(new BigDecimal("2000.00")); // > 1000 disponible
        compteEpargne.setSoldeDisponible(new BigDecimal("1000.00"));
        
        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .statut("REJETEE")
                    .motifRejet("Solde insuffisant")
                    .build()
        );

        service.validerDemande(1L);

        assertEquals(StatutDemandeRetrait.REJETEE, demande.getStatut());
        assertNotNull(demande.getMotifRejet());
        verify(demandeRetraitEpargneRepository).save(any());
        verify(workflowTaskService).onRetraitRejete(1L, currentRetraitReference(), 10L, 20L);
    }

    @Test
    void testValiderDemande_SoldeBloqueNonRetirable() {
        // Le solde bloqué (500) ne doit jamais être inclu dans soldeDisponible (1000)
        // Test que validerDemande utilise correctement soldeDisponible uniquement
        demande.setStatut(StatutDemandeRetrait.CREEE);
        demande.setMontantDemande(new BigDecimal("800.00")); // OK: 800 <= 1000 disponible
        
        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .statut("VALIDEE")
                    .build()
        );

        service.validerDemande(1L);

        // Doit être VALIDEE car 800 <= 1000 (solde disponible)
        assertEquals(StatutDemandeRetrait.VALIDEE, demande.getStatut());
    }

    // ========== TEST 3: CRITIQUE - Décaissement avec OperationCaisse ==========
    @Test
    void retraitRenseigneCreatedByOperationEpargne() {
        // SETUP: Demande validée prête pour décaissement
        demande.setStatut(StatutDemandeRetrait.VALIDEE);
        demande.setValidePar(utilisateur);
        demande.setDateValidation(LocalDateTime.now());
        demande.setMontantDemande(new BigDecimal("10000.00"));
        demande.setFraisRetrait(new BigDecimal("900.00"));
        demande.setTauxCommissionRetrait(new BigDecimal("9.00"));
        demande.setMontantTotalDebite(new BigDecimal("10900.00"));

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(
                StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(sessionCaisse));
        
        OperationEpargne operationEpargne = new OperationEpargne();
        operationEpargne.setId(1L);
        operationEpargne.setTypeOperation(TypeOperationEpargne.RETRAIT);
        operationEpargne.setMontant(new BigDecimal("10900.00"));
        
        OperationCaisseResponse operationCaisse = OperationCaisseResponse.builder()
            .id(1L)
            .typeOperation(TypeOperationCaisse.SORTIE)
            .categorieOperation(CategorieOperationCaisse.RETRAIT_EPARGNE)
            .montant(new BigDecimal("10000.00"))
            .build();

        when(operationEpargneRepository.save(any())).thenReturn(operationEpargne);
        when(operationCaisseService.enregistrer(any())).thenReturn(operationCaisse);
        when(ticketRecuService.genererDepuisOperation(any())).thenReturn(TicketRecuResponse.builder().id(55L).build());
        when(compteEpargneRepository.save(any())).thenReturn(compteEpargne);
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .statut("DECAISSEE")
                    .build()
        );

        // ACTION
        service.decaisserRetrait(1L);

        // VÉRIFICATIONS CRITIQUES:
        // 1. OperationEpargne créée avec traçabilité utilisateur
        ArgumentCaptor<OperationEpargne> operationEpargneCaptor = ArgumentCaptor.forClass(OperationEpargne.class);
        verify(operationEpargneRepository).save(operationEpargneCaptor.capture());
        OperationEpargne savedOperationEpargne = operationEpargneCaptor.getValue();
        assertEquals(TypeOperationEpargne.RETRAIT, savedOperationEpargne.getTypeOperation());
        assertEquals(new BigDecimal("10900.00"), savedOperationEpargne.getMontant());
        assertNotNull(savedOperationEpargne.getCreatedBy());
        assertEquals(utilisateur.getId(), savedOperationEpargne.getCreatedBy().getId());

        // 2. OperationCaisse créée avec SORTIE pour le principal payé au membre
        // et ENTREE pour la commission
        ArgumentCaptor<OperationCaisseRequest> caisseCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService, times(2)).enregistrer(caisseCaptor.capture());
        List<OperationCaisseRequest> mouvementsCaisse = caisseCaptor.getAllValues();

        OperationCaisseRequest sortiePrincipal = mouvementsCaisse.stream().filter(req ->
            req.getTypeOperation() == TypeOperationCaisse.SORTIE &&
            req.getCategorieOperation() == CategorieOperationCaisse.RETRAIT_EPARGNE &&
            req.getMontant().compareTo(new BigDecimal("10000.00")) == 0 &&
            req.getSessionCaisseId().equals(sessionCaisse.getId()) &&
            req.getCaisseId().equals(sessionCaisse.getCaisse().getId())
        ).findFirst().orElseThrow();
        assertEquals(currentRetraitReference(), sortiePrincipal.getReferenceMetier());
        assertEquals(currentRetraitReference(), sortiePrincipal.getReferenceExterne());

        OperationCaisseRequest entreeCommission = mouvementsCaisse.stream().filter(req ->
            req.getTypeOperation() == TypeOperationCaisse.ENTREE &&
            req.getCategorieOperation() == CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE &&
            req.getMontant().compareTo(new BigDecimal("900.00")) == 0 &&
            req.getSessionCaisseId().equals(sessionCaisse.getId()) &&
            req.getCaisseId().equals(sessionCaisse.getCaisse().getId())
        ).findFirst().orElseThrow();
        assertEquals(currentRetraitReference() + "-FRAIS", entreeCommission.getReferenceMetier());
        assertEquals(currentRetraitReference(), entreeCommission.getReferenceExterne());

        // 3. Compte épargne solde réduit du total débité
        verify(compteEpargneRepository).save(argThat(c ->
            c.getSoldeDisponible().equals(new BigDecimal("9100.00"))
        ));

        // 4. Demande passée à DECAISSEE
        assertEquals(StatutDemandeRetrait.DECAISSEE, demande.getStatut());
        verify(workflowTaskService).onRetraitPaye(1L, currentRetraitReference(), 10L, 20L);

        ArgumentCaptor<TicketRecuGenerationRequest> ticketCaptor = ArgumentCaptor.forClass(TicketRecuGenerationRequest.class);
        verify(ticketRecuService).genererDepuisOperation(ticketCaptor.capture());
        TicketRecuGenerationRequest ticketRequest = ticketCaptor.getValue();
        assertEquals(new BigDecimal("10000.00"), ticketRequest.getMontantPrincipal());
        assertEquals(new BigDecimal("900.00"), ticketRequest.getMontantCommission());
        assertEquals(new BigDecimal("10900.00"), ticketRequest.getMontantTotalDebite());
        assertEquals(new BigDecimal("10000.00"), ticketRequest.getMontantRemisMembre());
        assertEquals(new BigDecimal("20000.00"), ticketRequest.getAncienSolde());
        assertEquals(new BigDecimal("9100.00"), ticketRequest.getNouveauSolde());
    }

    @Test
    void testDecaisserRetrait_SansSessionCaisseActive() {
        // SETUP
        demande.setStatut(StatutDemandeRetrait.VALIDEE);
        demande.setValidePar(utilisateur);
        demande.setDateValidation(LocalDateTime.now());

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(
                StatutSessionCaisse.OUVERTE)).thenReturn(Optional.empty());

        // ACTION & ASSERTION
        assertThrows(BusinessException.class, 
            () -> service.decaisserRetrait(1L),
            "Doit lancer exception si pas de session caisse active");

        // Vérifier que OperationCaisse n'a PAS été créée
        verify(operationCaisseService, never()).enregistrer(any());
    }

    @Test
    void testDecaisserRetrait_Idempotence_DejaDecaissee() {
        // SETUP: Retrait déjà décaissé
        demande.setStatut(StatutDemandeRetrait.DECAISSEE);
        demande.setValidePar(utilisateur);
        demande.setDateValidation(LocalDateTime.now());

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .statut("DECAISSEE")
                    .build()
        );

        // ACTION
        service.decaisserRetrait(1L);

        // VÉRIFICATION IDEMPOTENCE: Aucune nouvelle opération créée
        verify(operationEpargneRepository, never()).save(any());
        verify(operationCaisseService, never()).enregistrer(any());
        verify(compteEpargneRepository, never()).save(any());
        verify(workflowTaskService).onRetraitPaye(1L, currentRetraitReference(), 10L, 20L);
    }

    @Test
    void testDecaisserRetrait_SansValidation_NonAutorise() {
        // SETUP: Demande non validée
        demande.setStatut(StatutDemandeRetrait.CREEE);

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));

        // ACTION & ASSERTION
        assertThrows(BusinessException.class, 
            () -> service.decaisserRetrait(1L),
            "Doit interdire paiement sans validation");

        // Vérifier que AUCUNE opération n'a été créée
        verify(operationEpargneRepository, never()).save(any());
        verify(operationCaisseService, never()).enregistrer(any());
    }

    // ========== TEST 4: Rejet Demande ==========
    @Test
    void testRejeterDemande_Success() {
        demande.setStatut(StatutDemandeRetrait.CREEE);

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .statut("REJETEE")
                    .motifRejet("Solde insuffisant")
                    .build()
        );

        service.rejeterDemande(1L, "Solde insuffisant");

        assertEquals(StatutDemandeRetrait.REJETEE, demande.getStatut());
        assertEquals("Solde insuffisant", demande.getMotifRejet());
        verify(demandeRetraitEpargneRepository).save(any());
        verify(workflowTaskService).onRetraitRejete(1L, currentRetraitReference(), 10L, 20L);
    }

    // ========== TEST 5: Annulation Demande ==========
    @Test
    void testAnnulerDemande_Success() {
        demande.setStatut(StatutDemandeRetrait.VALIDEE);

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .statut("ANNULEE")
                    .build()
        );

        service.annulerDemande(1L);

        assertEquals(StatutDemandeRetrait.ANNULEE, demande.getStatut());
        verify(demandeRetraitEpargneRepository).save(any());
    }

    @Test
    void testAnnulerDemande_ImpossibleSiDecaissee() {
        demande.setStatut(StatutDemandeRetrait.DECAISSEE);

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));

        assertThrows(BusinessException.class, 
            () -> service.annulerDemande(1L),
            "Doit empêcher annulation si retrait déjà payé");
    }

    // ========== TEST 6: Get Demande ==========
    @Test
    void testGetById_Success() {
        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder()
                    .id(1L)
                    .build()
        );

        DemandeRetraitEpargneDTO result = service.getById(1L);

        assertNotNull(result);
        verify(demandeRetraitEpargneRepository).findById(1L);
    }

    @Test
    void testGetById_NotFound() {
        when(demandeRetraitEpargneRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> service.getById(999L));
    }

    // ========== PATCH 6: Source traçabilité ==========

    /**
     * PATCH 6 — retraitEpargne_shouldCreateOperationCaisseWithSourceRetraitEpargne
     * Vérifie que decaisserRetrait() crée l'OperationCaisse avec source=RETRAIT_EPARGNE
     * et referenceExterne non null contenant l'id de la demande.
     */
    @Test
    void retraitEpargne_shouldCreateOperationCaisseWithSourceRetraitEpargne() {
        // SETUP
        demande.setStatut(StatutDemandeRetrait.VALIDEE);
        demande.setValidePar(utilisateur);
        demande.setDateValidation(LocalDateTime.now());

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(
                StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(sessionCaisse));

        OperationEpargne operationEpargne = new OperationEpargne();
        operationEpargne.setId(1L);
        operationEpargne.setTypeOperation(TypeOperationEpargne.RETRAIT);
        operationEpargne.setMontant(new BigDecimal("200.00"));

        when(operationEpargneRepository.save(any())).thenReturn(operationEpargne);
        when(compteEpargneRepository.save(any())).thenReturn(compteEpargne);
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder().id(1L).statut("DECAISSEE").build()
        );

        ArgumentCaptor<OperationCaisseRequest> captor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        OperationCaisseResponse savedOp = OperationCaisseResponse.builder()
            .id(99L)
            .build();
        when(operationCaisseService.enregistrer(captor.capture())).thenReturn(savedOp);

        // ACTION
        service.decaisserRetrait(1L);

        // VÉRIFICATION PATCH 6
        OperationCaisseRequest captured = captor.getValue();
        assertEquals(SourceOperationCaisse.RETRAIT_EPARGNE, captured.getSource(),
            "L'OperationCaisse doit avoir source=RETRAIT_EPARGNE (PATCH 6)");
        assertEquals(sessionCaisse.getId(), captured.getSessionCaisseId(),
            "sessionCaisseId doit être tracé sur l'opération de paiement retrait");
        assertEquals(sessionCaisse.getCaisse().getId(), captured.getCaisseId(),
            "caisseId doit être tracé sur l'opération de paiement retrait");
        assertEquals(utilisateur.getId(), captured.getCreatedBy(),
            "createdBy doit être le caissier/utilisateur courant");
        assertEquals(utilisateur.getId(), captured.getUtilisateurId(),
            "utilisateurId doit être le caissier/utilisateur courant");
        assertEquals(currentRetraitReference(), captured.getReferenceMetier(),
            "referenceMetier doit identifier la demande de retrait");
        assertNotNull(captured.getReferenceExterne(),
            "referenceExterne doit être renseignée pour la traçabilité (PATCH 6)");
        assertTrue(captured.getReferenceExterne().contains("1"),
            "referenceExterne doit contenir l'id de la demande (PATCH 6)");
    }

    /**
     * PATCH 6 — newOperation_shouldNeverUseLegacy
     * Vérifie qu'une nouvelle opération caisse de retrait n'utilise jamais LEGACY.
     */
    @Test
    void newOperation_shouldNeverUseLegacy() {
        demande.setStatut(StatutDemandeRetrait.VALIDEE);
        demande.setValidePar(utilisateur);
        demande.setDateValidation(LocalDateTime.now());

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(
                StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(sessionCaisse));
        when(operationEpargneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(compteEpargneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder().id(1L).statut("DECAISSEE").build()
        );

        ArgumentCaptor<OperationCaisseRequest> captor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        when(operationCaisseService.enregistrer(captor.capture())).thenAnswer(inv -> {
            OperationCaisseRequest req = inv.getArgument(0);
            return OperationCaisseResponse.builder()
                    .id(1L)
                    .typeOperation(req.getTypeOperation())
                    .categorieOperation(req.getCategorieOperation())
                    .montant(req.getMontant())
                    .build();
        });

        service.decaisserRetrait(1L);

        OperationCaisseRequest captured = captor.getValue();
        assertNotNull(captured.getSource(),
            "source ne doit jamais être null pour une nouvelle opération (PATCH 6)");
        // Le retrait doit utiliser RETRAIT_EPARGNE, jamais AUTRE ou null par défaut
        assertEquals(SourceOperationCaisse.RETRAIT_EPARGNE, captured.getSource(),
            "newOperation: source doit être RETRAIT_EPARGNE, pas une valeur générique (PATCH 6)");
    }

    @Test
    void fraisRetraitCreeOperationCaisseEntreeRevenuTracee() {
        demande.setStatut(StatutDemandeRetrait.VALIDEE);
        demande.setValidePar(utilisateur);
        demande.setDateValidation(LocalDateTime.now());
        demande.setFraisRetrait(new BigDecimal("25.00"));

        when(demandeRetraitEpargneRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(
                StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(sessionCaisse));
        when(operationEpargneRepository.save(any())).thenAnswer(inv -> {
            OperationEpargne operation = inv.getArgument(0);
            operation.setId(88L);
            return operation;
        });
        when(compteEpargneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeRetraitEpargneRepository.save(any())).thenReturn(demande);
        when(demandeRetraitEpargneMapper.toDTO(demande)).thenReturn(
            DemandeRetraitEpargneDTO.builder().id(1L).statut("DECAISSEE").build()
        );
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(1L).build());

        service.decaisserRetrait(1L);

        ArgumentCaptor<OperationCaisseRequest> captor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService, times(2)).enregistrer(captor.capture());
        OperationCaisseRequest frais = captor.getAllValues().stream()
                .filter(request -> request.getCategorieOperation() == CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE)
                .findFirst()
                .orElseThrow();
        assertEquals(TypeOperationCaisse.ENTREE, frais.getTypeOperation());
        assertEquals(new BigDecimal("25.00"), frais.getMontant());
        assertEquals(SourceOperationCaisse.RETRAIT_EPARGNE, frais.getSource());
        assertEquals(currentRetraitReference() + "-FRAIS", frais.getReferenceMetier());
        assertEquals(sessionCaisse.getId(), frais.getSessionCaisseId());
        assertEquals(sessionCaisse.getCaisse().getId(), frais.getCaisseId());
        assertEquals(utilisateur.getId(), frais.getUtilisateurId());
    }
}
