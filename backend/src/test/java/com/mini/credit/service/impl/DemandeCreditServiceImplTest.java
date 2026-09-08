package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.DemandeCreditCreateRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.entity.credit.AnalyseRisque;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.RecommandationRisque;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.credit.AnalyseRisqueRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.GarantieCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.CreditValidationService;
import com.mini.credit.service.GarantieCreditWorkflowService;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.security.ScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandeCreditServiceImpl - garde validation garantie")
class DemandeCreditServiceImplTest {

    @Mock
    private DemandeCreditRepository demandeCreditRepository;
    @Mock
    private MembreRepository membreRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private AgentTerrainRepository agentTerrainRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private AnalyseRisqueRepository analyseRisqueRepository;
        @Mock
        private GarantieCreditRepository garantieCreditRepository;
        @Mock
        private CompteEpargneRepository compteEpargneRepository;
    @Mock
    private CreditMapper creditMapper;
    @Mock
    private ScopeService scopeService;
    @Mock
    private CreditValidationService creditValidationService;
    @Mock
    private GarantieCreditWorkflowService garantieCreditWorkflowService;
    @Mock
    private WorkflowTaskService workflowTaskService;
    @Mock
    private ParametreMetierService parametreMetierService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private DemandeCreditServiceImpl service;

    @Test
    void fraisDemandeSaisiDoitEtreConserveEtTacheGestionnaireCreee() {
        Membre membre = membreActif(1L);
        Site site = site(2L);
        DemandeCreditCreateRequest request = demandeCreateRequest();
        request.setFraisDemande(new BigDecimal("7500"));
        request.setDureeValeur(3);

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(site));
        when(scopeService.canCreateDemandeCredit(1L)).thenReturn(true);
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> {
            DemandeCredit saved = invocation.getArgument(0);
            saved.setId(1000L);
            return saved;
        });
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.create(request);

        ArgumentCaptor<DemandeCredit> demandeCaptor = ArgumentCaptor.forClass(DemandeCredit.class);
        verify(demandeCreditRepository).save(demandeCaptor.capture());
        DemandeCredit saved = demandeCaptor.getValue();
        assertThat(saved.getFraisDemande()).isEqualByComparingTo("7500.00");
        assertThat(saved.getDureeValeur()).isEqualTo(3);
        assertThat(saved.getStatut()).isEqualTo(StatutDemandeCredit.SOUMISE);
        verify(workflowTaskService).onDemandeCreditSoumise(eq(1000L), anyString(), isNull(), eq(2L));
    }

    @Test
    void fraisDemandeSaisiDoitEtreConserve() {
        Membre membre = membreActif(1L);
        Site site = site(2L);
        DemandeCreditCreateRequest request = demandeCreateRequest();
        request.setFraisDemande(new BigDecimal("7500"));

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(site));
        when(scopeService.canCreateDemandeCredit(1L)).thenReturn(true);
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.create(request);

        ArgumentCaptor<DemandeCredit> demandeCaptor = ArgumentCaptor.forClass(DemandeCredit.class);
        verify(demandeCreditRepository).save(demandeCaptor.capture());
        assertThat(demandeCaptor.getValue().getFraisDemande()).isEqualByComparingTo("7500.00");
    }

    @Test
    void fraisDemandeParDefautSeulementSiAbsent() {
        Membre membre = membreActif(1L);
        Site site = site(2L);
        DemandeCreditCreateRequest request = demandeCreateRequest();
        request.setFraisDemande(null);

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(site));
        when(scopeService.canCreateDemandeCredit(1L)).thenReturn(true);
        when(parametreMetierService.getDecimal("FRAIS_ANALYSE_DEMANDE")).thenReturn(new BigDecimal("5000"));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.create(request);

        ArgumentCaptor<DemandeCredit> demandeCaptor = ArgumentCaptor.forClass(DemandeCredit.class);
        verify(demandeCreditRepository).save(demandeCaptor.capture());
        assertThat(demandeCaptor.getValue().getFraisDemande()).isEqualByComparingTo("5000.00");
    }

    @Test
    void dureeCreditSaisieDoitEtreConservee() {
        Membre membre = membreActif(1L);
        Site site = site(2L);
        DemandeCreditCreateRequest request = demandeCreateRequest();
        request.setDureeValeur(3);

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(site));
        when(scopeService.canCreateDemandeCredit(1L)).thenReturn(true);
        when(parametreMetierService.getDecimal("FRAIS_ANALYSE_DEMANDE")).thenReturn(new BigDecimal("5000"));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.create(request);

        ArgumentCaptor<DemandeCredit> demandeCaptor = ArgumentCaptor.forClass(DemandeCredit.class);
        verify(demandeCreditRepository).save(demandeCaptor.capture());
        assertThat(demandeCaptor.getValue().getDureeValeur()).isEqualTo(3);
    }

    @Test
    void creationDemandeCreditCreeTacheGestionnaire() {
        Membre membre = membreActif(1L);
        Site site = site(2L);
        DemandeCreditCreateRequest request = demandeCreateRequest();

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(site));
        when(scopeService.canCreateDemandeCredit(1L)).thenReturn(true);
        when(parametreMetierService.getDecimal("FRAIS_ANALYSE_DEMANDE")).thenReturn(new BigDecimal("5000"));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> {
            DemandeCredit saved = invocation.getArgument(0);
            saved.setId(1000L);
            return saved;
        });
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.create(request);

        verify(workflowTaskService).onDemandeCreditSoumise(eq(1000L), anyString(), isNull(), eq(2L));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void controlerGarantie_shouldRefuseWhenGarantieNotValidated() {
        DemandeCredit demande = demande(100L, StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE,
                new BigDecimal("10000"), new BigDecimal("10000"));

        when(demandeCreditRepository.findById(100L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieValidee(100L)).thenReturn(false);

        assertThatThrownBy(() -> service.controlerGarantie(100L, "controle garantie"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("garantie doit être validée");
    }

    @Test
    void controlerGarantie_shouldPassToValidationChefWhenGarantieValidated() {
        Utilisateur controleur = Utilisateur.builder().username("controleur.3n").build();
        controleur.setId(333L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(controleur, null, controleur.getAuthorities())
        );

        DemandeCredit demande = demande(101L, StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE,
                new BigDecimal("10000"), new BigDecimal("10000"));

        when(demandeCreditRepository.findById(101L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieValidee(101L)).thenReturn(true);
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.controlerGarantie(101L, "garantie validée");

        assertThat(demande.getStatut()).isEqualTo(StatutDemandeCredit.VALIDATION_CHEF);
        assertThat(demande.getDecidedBy()).isEqualTo(controleur);
        verify(workflowTaskService).onDemandeCreditValidationChef(101L, null, null, null);
    }

    @Test
    void rejeter_shouldCloseActiveCreditTasks() {
        DemandeCredit demande = demande(102L, StatutDemandeCredit.EN_ANALYSE,
                new BigDecimal("10000"), new BigDecimal("10000"));

        when(demandeCreditRepository.findById(102L)).thenReturn(Optional.of(demande));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.rejeter(102L, "motif rejet");

        verify(workflowTaskService).onDemandeCreditRejetee(102L, null, null, null);
    }

    @Test
    void montantFaibleProduitScoreRisqueDifferentMontantEleve() {
        AnalyseRisque low = enregistrerAnalyseEtCapturer(demandeAnalyse(201L, "50000", "10000", "10000"), analyseRequest("200000", "50000", "150000"));
        AnalyseRisque high = enregistrerAnalyseEtCapturer(demandeAnalyse(202L, "1000000", "200000", "200000"), analyseRequest("200000", "50000", "150000"));

        assertThat(low.getScoreRisque()).isNotEqualByComparingTo(high.getScoreRisque());
        assertThat(low.getScoreRisque()).isGreaterThan(high.getScoreRisque());
        assertThat(low.getRisqueNiveau()).isEqualTo(com.mini.credit.enums.NiveauRisque.FAIBLE);
        assertThat(high.getRisqueNiveau().ordinal()).isGreaterThan(low.getRisqueNiveau().ordinal());
    }

    @Test
    void memeProfilMontantPlusEleveAugmenteRisque() {
        AnalyseRisque low = enregistrerAnalyseEtCapturer(demandeAnalyse(203L, "50000", "10000", "10000"), analyseRequest("200000", "50000", "150000"));
        AnalyseRisque high = enregistrerAnalyseEtCapturer(demandeAnalyse(204L, "1000000", "200000", "200000"), analyseRequest("200000", "50000", "150000"));

        assertThat(high.getScoreRisque()).isLessThan(low.getScoreRisque());
        assertThat(high.getRisqueNiveau().ordinal()).isGreaterThan(low.getRisqueNiveau().ordinal());
    }

    @Test
    void montantDemandeEstPrisEnCompteDansScore() {
        AnalyseRisque high = enregistrerAnalyseEtCapturer(demandeAnalyse(205L, "1000000", "200000", "200000"), analyseRequest("200000", "50000", "150000"));

        assertThat(high.getScoreRisque()).isEqualByComparingTo("50.00");
    }

    @Test
    void revenusPlusElevesReduisentRisquePourMemeMontant() {
        DemandeCredit demandeA = demandeAnalyse(206L, "1000000", "200000", "200000");
        DemandeCredit demandeB = demandeAnalyse(207L, "1000000", "200000", "200000");

        AnalyseRisque revenuFaible = enregistrerAnalyseEtCapturer(demandeA, analyseRequest("200000", "50000", "150000"));
        AnalyseRisque revenuEleve = enregistrerAnalyseEtCapturer(demandeB, analyseRequest("1000000", "50000", "950000"));

        assertThat(revenuEleve.getScoreRisque()).isGreaterThan(revenuFaible.getScoreRisque());
    }

    @Test
    void chargesPlusEleveesAugmententRisque() {
        DemandeCredit demandeA = demandeAnalyse(208L, "500000", "100000", "100000");
        DemandeCredit demandeB = demandeAnalyse(209L, "500000", "100000", "100000");

        AnalyseRisque chargesFaibles = enregistrerAnalyseEtCapturer(demandeA, analyseRequest("500000", "50000", "450000"));
        AnalyseRisque chargesElevees = enregistrerAnalyseEtCapturer(demandeB, analyseRequest("500000", "400000", "100000"));

        assertThat(chargesElevees.getScoreRisque()).isLessThan(chargesFaibles.getScoreRisque());
    }

    @Test
    void mensualiteSuperieureCapaciteAugmenteRisque() {
        DemandeCredit demandeA = demandeAnalyse(210L, "1000000", "200000", "200000");
        DemandeCredit demandeB = demandeAnalyse(211L, "1000000", "200000", "200000");

        AnalyseRisque capaciteHaute = enregistrerAnalyseEtCapturer(demandeA, analyseRequest("1000000", "50000", "950000"));
        AnalyseRisque capaciteBasse = enregistrerAnalyseEtCapturer(demandeB, analyseRequest("1000000", "50000", "150000"));

        assertThat(capaciteBasse.getScoreRisque()).isLessThan(capaciteHaute.getScoreRisque());
    }

    @Test
    void garantieEpargneInsuffisanteAugmenteRisque() {
        DemandeCredit garantieComplete = demandeAnalyse(212L, "50000", "10000", "10000");
        DemandeCredit garantieInsuffisante = demandeAnalyse(213L, "50000", "0", "10000");

        AnalyseRisque complete = enregistrerAnalyseEtCapturer(garantieComplete, analyseRequest("200000", "50000", "150000"));
        AnalyseRisque insuffisante = enregistrerAnalyseEtCapturer(garantieInsuffisante, analyseRequest("200000", "50000", "150000"));

        assertThat(insuffisante.getScoreRisque()).isLessThan(complete.getScoreRisque());
    }

    @Test
    void garantieEpargne20PourcentCalculeeCorrectement() {
        Membre membre = membreActif(1L);
        Site site = site(2L);
        DemandeCreditCreateRequest request = demandeCreateRequest();
        request.setMontantDemande(new BigDecimal("1000000"));

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(site));
        when(scopeService.canCreateDemandeCredit(1L)).thenReturn(true);
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.create(request);

        ArgumentCaptor<DemandeCredit> demandeCaptor = ArgumentCaptor.forClass(DemandeCredit.class);
        verify(demandeCreditRepository).save(demandeCaptor.capture());
        assertThat(demandeCaptor.getValue().getDepotGarantieRequis()).isEqualByComparingTo("200000.00");
    }

    @Test
    void frontendScoreIgnoreBackendRecalcule() {
        AnalyseRisqueRequest request = analyseRequest("200000", "50000", "150000");
        request.setScoreRisque(new BigDecimal("100"));
        request.setScoreRisqueCorrigeManuellement(false);

        AnalyseRisque saved = enregistrerAnalyseEtCapturer(demandeAnalyse(214L, "1000000", "200000", "200000"), request);

        assertThat(saved.getScoreRisque()).isEqualByComparingTo("50.00");
        assertThat(saved.getScoreRisque()).isNotEqualByComparingTo("100.00");
    }

    @Test
    void detailCriteresRetourneAuFrontend() {
        DemandeCredit demande = demandeAnalyse(217L, "1000000", "200000", "200000");
        AnalyseRisque analyse = new AnalyseRisque();
        analyse.setScoreRisque(new BigDecimal("50.00"));
        analyse.setRisqueNiveau(com.mini.credit.enums.NiveauRisque.MOYEN);
        analyse.setRevenuNetEstime(new BigDecimal("200000"));
        analyse.setChargesMensuelles(new BigDecimal("50000"));
        analyse.setCapaciteRemboursement(new BigDecimal("150000"));
        analyse.setActiviteVerifiee(true);
        analyse.setDateVisite(LocalDate.of(2026, 7, 30));
        analyse.setLieuVisite("Marche central");

        DemandeCreditResponse mapped = DemandeCreditResponse.builder()
                .id(217L)
                .montantDemande(new BigDecimal("1000000"))
                .devise("CDF")
                .dureeValeur(4)
                .dureeUnite(DureeUnite.MOIS)
                .periodiciteRemboursement(PeriodiciteRemboursement.MENSUEL)
                .tauxInteret(BigDecimal.ZERO)
                .depotGarantieRequis(new BigDecimal("200000"))
                .depotGarantiePaye(new BigDecimal("200000"))
                .statut(StatutDemandeCredit.EN_ANALYSE)
                .build();

        when(demandeCreditRepository.findById(217L)).thenReturn(Optional.of(demande));
        when(creditMapper.toResponse(demande)).thenReturn(mapped);
        when(garantieCreditRepository.findByDemandeCreditId(217L)).thenReturn(Optional.empty());
        when(analyseRisqueRepository.findByDemandeCreditId(217L)).thenReturn(analyse);

        DemandeCreditResponse response = service.getById(217L);

        assertThat(response.getAnalyseRisque()).isNotNull();
        assertThat(response.getAnalyseRisque().getScoreTotal()).isEqualByComparingTo("50.00");
        assertThat(response.getAnalyseRisque().getRatioMontantRevenu()).isEqualByComparingTo("5.0000");
        assertThat(response.getAnalyseRisque().getCriteres())
                .extracting("codeCritere")
                .contains("MONTANT_REVENU", "MENSUALITE_CAPACITE", "GARANTIE_EPARGNE_20");
    }

    @Test
    void scoreReproductiblePourMemesDonnees() {
        AnalyseRisque first = enregistrerAnalyseEtCapturer(demandeAnalyse(215L, "50000", "10000", "10000"), analyseRequest("200000", "50000", "150000"));
        AnalyseRisque second = enregistrerAnalyseEtCapturer(demandeAnalyse(216L, "50000", "10000", "10000"), analyseRequest("200000", "50000", "150000"));

        assertThat(second.getScoreRisque()).isEqualByComparingTo(first.getScoreRisque());
        assertThat(second.getRisqueNiveau()).isEqualTo(first.getRisqueNiveau());
    }

    private AnalyseRisque enregistrerAnalyseEtCapturer(DemandeCredit demande, AnalyseRisqueRequest request) {
        authenticateAdmin();
        when(demandeCreditRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(demande.getId())).thenReturn(true);
        lenient().when(analyseRisqueRepository.existsByAnalyseId(anyString())).thenReturn(false);
        when(analyseRisqueRepository.save(any(AnalyseRisque.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));
        clearInvocations(analyseRisqueRepository);

        service.ajouterAnalyse(demande.getId(), request);

        ArgumentCaptor<AnalyseRisque> analyseCaptor = ArgumentCaptor.forClass(AnalyseRisque.class);
        verify(analyseRisqueRepository).save(analyseCaptor.capture());
        return analyseCaptor.getValue();
    }

    private DemandeCredit demandeAnalyse(Long id, String montantDemande, String depotPaye, String depotRequis) {
        DemandeCredit demande = demande(id, StatutDemandeCredit.EN_ANALYSE, new BigDecimal(depotPaye), new BigDecimal(depotRequis));
        demande.setMontantDemande(new BigDecimal(montantDemande));
        demande.setDevise("CDF");
        demande.setDureeValeur(4);
        demande.setDureeUnite(DureeUnite.MOIS);
        demande.setPeriodiciteRemboursement(PeriodiciteRemboursement.MENSUEL);
        demande.setTauxInteret(BigDecimal.ZERO);
        demande.setObjetCredit("Stock boutique");
        return demande;
    }

    private AnalyseRisqueRequest analyseRequest(String revenu, String charges, String capacite) {
        AnalyseRisqueRequest request = new AnalyseRisqueRequest();
        request.setDateVisite(LocalDate.of(2026, 7, 30));
        request.setLieuVisite("Marche central");
        request.setActiviteVerifiee(true);
        request.setDescriptionActivite("Activite verifiee");
        request.setRevenuNetEstime(new BigDecimal(revenu));
        request.setChargesMensuelles(new BigDecimal(charges));
        request.setCapaciteRemboursement(new BigDecimal(capacite));
        request.setRecommandation(RecommandationRisque.FAVORABLE);
        request.setCommentaire("Analyse risque documentee");
        return request;
    }

    private void authenticateAdmin() {
        Role role = Role.builder().code(RoleCode.ADMIN).libelle("Admin").build();
        Utilisateur admin = Utilisateur.builder()
                .username("admin.3n")
                .nomComplet("Admin 3N")
                .role(role)
                .build();
        admin.setId(999L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );
    }

    private DemandeCredit demande(Long id,
                                  StatutDemandeCredit statut,
                                  BigDecimal depotPaye,
                                  BigDecimal depotRequis) {
        DemandeCredit demande = DemandeCredit.builder()
                .statut(statut)
                .depotGarantiePaye(depotPaye)
                .depotGarantieRequis(depotRequis)
                .fraisDemande(new BigDecimal("1000"))
                .fraisDemandePayes(BigDecimal.ZERO)
                .build();
        demande.setId(id);
        return demande;
    }

    private DemandeCreditCreateRequest demandeCreateRequest() {
        DemandeCreditCreateRequest request = new DemandeCreditCreateRequest();
        request.setMembreId(1L);
        request.setSiteId(2L);
        request.setMontantDemande(new BigDecimal("500000"));
        request.setDevise("CDF");
        request.setDureeValeur(3);
        request.setDureeUnite(DureeUnite.MOIS);
        request.setPeriodiciteRemboursement(PeriodiciteRemboursement.MENSUEL);
        request.setTauxInteret(new BigDecimal("5"));
        request.setObjetCredit("Stock boutique");
        request.setRevenusEstimes(BigDecimal.ZERO);
        request.setChargesEstimees(BigDecimal.ZERO);
        return request;
    }

    private Membre membreActif(Long id) {
        Membre membre = Membre.builder()
                .nomComplet("Membre Test")
                .statut(StatutMembre.ACTIF)
                .build();
        membre.setId(id);
        return membre;
    }

    private Site site(Long id) {
        Site site = Site.builder().nomSite("Site Test").build();
        site.setId(id);
        return site;
    }
}
