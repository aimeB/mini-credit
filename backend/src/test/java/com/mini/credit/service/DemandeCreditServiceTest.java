package com.mini.credit.service;

import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.entity.credit.AnalyseRisque;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutDemandeCredit;
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
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.impl.DemandeCreditServiceImpl;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.GarantieCreditWorkflowService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandeCreditService - workflow 3N")
class DemandeCreditServiceTest {

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
    private AuditService auditService;

    @InjectMocks
    private DemandeCreditServiceImpl service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void soumise_to_preAnalyse_shouldBeAllowed() {
        Utilisateur user = Utilisateur.builder().username("gestionnaire").build();
        user.setId(11L);
        authenticate(user);

        DemandeCredit demande = demande(1L, StatutDemandeCredit.SOUMISE, new BigDecimal("0"), new BigDecimal("10000"));
        when(demandeCreditRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.preAnalyser(1L, "ok");

        assertThat(demande.getStatut()).isEqualTo(StatutDemandeCredit.EN_ANALYSE);
        assertThat(demande.getDecidedBy()).isEqualTo(user);
    }

    @Test
    void observationRisque_shouldNotAdvanceStatus() {
        Utilisateur user = Utilisateur.builder().username("controleur").build();
        user.setId(12L);
        authenticate(user);

        DemandeCredit demande = demande(2L, StatutDemandeCredit.EN_ANALYSE, new BigDecimal("10000"), new BigDecimal("10000"));
        demande.setAnalyseRisque(new AnalyseRisque());

        when(demandeCreditRepository.findById(2L)).thenReturn(Optional.of(demande));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.enregistrerObservationRisque(2L, "observation risque");

        assertThat(demande.getStatut()).isEqualTo(StatutDemandeCredit.EN_ANALYSE);
        assertThat(demande.getCommentaireDecision()).isEqualTo("observation risque");
    }

    @Test
    void validerAnalyseRisque_shouldRequireNonEmptyComment() {
        Utilisateur user = Utilisateur.builder().username("controleur").build();
        user.setId(12L);
        authenticate(user);

        AnalyseRisque analyse = new AnalyseRisque();
        analyse.setDateVisite(java.time.LocalDate.now());
        analyse.setLieuVisite("Site");
        analyse.setActiviteVerifiee(Boolean.TRUE);
        analyse.setCapaciteRemboursement(new BigDecimal("100"));
        analyse.setRecommandation(com.mini.credit.enums.RecommandationRisque.FAVORABLE);
        analyse.setRisqueNiveau(com.mini.credit.enums.NiveauRisque.FAIBLE);
        analyse.setScoreRisque(new BigDecimal("80"));

        DemandeCredit demande = demande(22L, StatutDemandeCredit.EN_ANALYSE, new BigDecimal("10000"), new BigDecimal("10000"));
        demande.setAnalyseRisque(analyse);

        when(demandeCreditRepository.findById(22L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(22L)).thenReturn(true);

        assertThatThrownBy(() -> service.validerAnalyseRisque(22L, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("commentaire de validation est obligatoire");
    }

    @Test
    void enAnalyse_to_validerAnalyseRisque_shouldAdvanceWhenAnalyseComplete() {
        Utilisateur user = Utilisateur.builder().username("controleur").build();
        user.setId(12L);
        authenticate(user);

        AnalyseRisque analyse = new AnalyseRisque();
        analyse.setDateVisite(java.time.LocalDate.now());
        analyse.setLieuVisite("Site");
        analyse.setActiviteVerifiee(Boolean.TRUE);
        analyse.setCapaciteRemboursement(new BigDecimal("100"));
        analyse.setRecommandation(com.mini.credit.enums.RecommandationRisque.FAVORABLE);
        analyse.setRisqueNiveau(com.mini.credit.enums.NiveauRisque.FAIBLE);
        analyse.setScoreRisque(new BigDecimal("80"));

        DemandeCredit demande = demande(2L, StatutDemandeCredit.EN_ANALYSE, new BigDecimal("10000"), new BigDecimal("10000"));
        demande.setAnalyseRisque(analyse);

        when(demandeCreditRepository.findById(2L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(2L)).thenReturn(true);
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.validerAnalyseRisque(2L, "controle ok");

        assertThat(demande.getStatut()).isEqualTo(StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE);
    }

    @Test
    void garantieSuffisante_shouldPassToValidationChef() {
        Utilisateur user = Utilisateur.builder().username("controleur").build();
        user.setId(12L);
        authenticate(user);

        DemandeCredit demande = demande(
                3L,
                StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE,
                new BigDecimal("20000"),
                new BigDecimal("20000")
        );

        when(demandeCreditRepository.findById(3L)).thenReturn(Optional.of(demande));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));
        when(garantieCreditWorkflowService.isGarantieValidee(3L)).thenReturn(true);

        service.controlerGarantie(3L, "garantie complete");

        assertThat(demande.getStatut()).isEqualTo(StatutDemandeCredit.VALIDATION_CHEF);
    }

    @Test
    void garantieInsuffisante_shouldBeRejectedBeforeValidationChef() {
        DemandeCredit demande = demande(
                4L,
                StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE,
                new BigDecimal("15000"),
                new BigDecimal("20000")
        );

        when(demandeCreditRepository.findById(4L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieValidee(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.controlerGarantie(4L, "insuffisant"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Garantie insuffisante");
    }

    private DemandeCredit demande(Long id, StatutDemandeCredit statut, BigDecimal depotPaye, BigDecimal depotRequis) {
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

    private void authenticate(Utilisateur user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }
}
