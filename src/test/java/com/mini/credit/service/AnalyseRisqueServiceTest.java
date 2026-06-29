package com.mini.credit.service;

import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.credit.AnalyseRisqueRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.GarantieCreditRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.impl.DemandeCreditServiceImpl;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.GarantieCreditWorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyseRisqueService - garde statut")
class AnalyseRisqueServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(String username, Long id, boolean withEmploye) {
        Utilisateur analyste = Utilisateur.builder().username(username).build();
        analyste.setId(id);
        analyste.setRole(com.mini.credit.entity.referentiel.Role.builder().code(com.mini.credit.enums.security.RoleCode.CONTROLEUR).build());
        if (withEmploye) {
            analyste.setEmploye(com.mini.credit.entity.employe.Employe.builder().nomComplet("Employe Controleur").build());
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "pwd", java.util.Collections.emptyList())
        );
        lenient().when(utilisateurRepository.findByUsernameWithValidationContext(username)).thenReturn(Optional.of(analyste));
    }

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
    private CreditMapper creditMapper;
    @Mock
    private ScopeService scopeService;
    @Mock
    private CreditValidationService creditValidationService;
    @Mock
    private GarantieCreditWorkflowService garantieCreditWorkflowService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private DemandeCreditServiceImpl service;

    @Test
    void analyse_shouldBeAllowedOnlyWhenDemandeIsEnAnalyse() {
        DemandeCredit demande = DemandeCredit.builder().statut(StatutDemandeCredit.EN_ANALYSE).build();
        demande.setId(1L);
        demande.setDevise("CDF");
        demande.setDepotGarantieRequis(java.math.BigDecimal.valueOf(10000));
        demande.setDepotGarantiePaye(java.math.BigDecimal.valueOf(10000));
        mockAuthenticatedUser("analyste", 100L, true);

        AnalyseRisqueRequest request = new AnalyseRisqueRequest();
        request.setDateVisite(java.time.LocalDate.now());
        request.setLieuVisite("Antenne 3N");
        request.setCapaciteRemboursement(java.math.BigDecimal.valueOf(50000));
        request.setRevenuNetEstime(java.math.BigDecimal.valueOf(80000));
        request.setChargesMensuelles(java.math.BigDecimal.valueOf(20000));
        request.setActiviteVerifiee(true);
        request.setDescriptionActivite("Commerce local actif");
        request.setRecommandation(com.mini.credit.enums.RecommandationRisque.FAVORABLE);

        when(demandeCreditRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(1L)).thenReturn(true);
        when(analyseRisqueRepository.existsByAnalyseId(anyString())).thenReturn(false);
        when(analyseRisqueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.ajouterAnalyse(1L, request);
        assertThat(demande.getAnalyseRisque()).isNotNull();
        assertThat(demande.getAnalyseRisque().getAnalyseId()).startsWith("ANR-1-");
    }

    @Test
    void analyse_shouldBeRejectedWhenDemandeIsNotEnAnalyse() {
        DemandeCredit demande = DemandeCredit.builder().statut(StatutDemandeCredit.SOUMISE).build();
        demande.setId(2L);

        AnalyseRisqueRequest request = new AnalyseRisqueRequest();

        when(demandeCreditRepository.findById(2L)).thenReturn(Optional.of(demande));

        assertThatThrownBy(() -> service.ajouterAnalyse(2L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("EN_ANALYSE");
    }

    @Test
    void analyse_shouldRejectManualAnalyseIdFromFrontend() {
        DemandeCredit demande = DemandeCredit.builder().statut(StatutDemandeCredit.EN_ANALYSE).build();
        demande.setId(3L);
        mockAuthenticatedUser("analyste", 100L, true);

        AnalyseRisqueRequest request = new AnalyseRisqueRequest();
        request.setAnalyseId("MANUEL-001");
        request.setDateVisite(java.time.LocalDate.now());
        request.setLieuVisite("Antenne 3N");
        request.setCapaciteRemboursement(java.math.BigDecimal.valueOf(5000));
        request.setRecommandation(com.mini.credit.enums.RecommandationRisque.FAVORABLE);

        when(demandeCreditRepository.findById(3L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.ajouterAnalyse(3L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("généré automatiquement");
    }

    @Test
    void analyse_shouldRejectIncoherentScoreAndRiskLevel() {
        DemandeCredit demande = DemandeCredit.builder().statut(StatutDemandeCredit.EN_ANALYSE).build();
        demande.setId(4L);
        mockAuthenticatedUser("analyste", 100L, true);

        AnalyseRisqueRequest request = new AnalyseRisqueRequest();
        request.setDateVisite(java.time.LocalDate.now());
        request.setLieuVisite("Antenne 3N");
        request.setCapaciteRemboursement(java.math.BigDecimal.valueOf(5000));
        request.setScoreRisque(java.math.BigDecimal.ZERO);
        request.setScoreRisqueCorrigeManuellement(true);
        request.setRisqueNiveau(com.mini.credit.enums.NiveauRisque.MOYEN);
        request.setCommentaire("Justification");
        request.setRecommandation(com.mini.credit.enums.RecommandationRisque.FAVORABLE);

        when(demandeCreditRepository.findById(4L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.ajouterAnalyse(4L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Incohérence score/niveau");
    }

    @Test
    void analyse_shouldRequireCommentForDefavorableRecommendation() {
        DemandeCredit demande = DemandeCredit.builder().statut(StatutDemandeCredit.EN_ANALYSE).build();
        demande.setId(5L);
        mockAuthenticatedUser("analyste", 100L, true);

        AnalyseRisqueRequest request = new AnalyseRisqueRequest();
        request.setDateVisite(java.time.LocalDate.now());
        request.setLieuVisite("Antenne 3N");
        request.setCapaciteRemboursement(java.math.BigDecimal.valueOf(5000));
        request.setRisqueNiveau(com.mini.credit.enums.NiveauRisque.MOYEN);
        request.setRecommandation(com.mini.credit.enums.RecommandationRisque.DEFAVORABLE);

        when(demandeCreditRepository.findById(5L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.ajouterAnalyse(5L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Commentaire obligatoire");
    }

    @Test
    void analyse_shouldUseAuthenticatedUserWithoutAnalysteId() {
        DemandeCredit demande = DemandeCredit.builder().statut(StatutDemandeCredit.EN_ANALYSE).build();
        demande.setId(6L);
        demande.setDevise("CDF");
        demande.setDepotGarantieRequis(java.math.BigDecimal.valueOf(10000));
        demande.setDepotGarantiePaye(java.math.BigDecimal.valueOf(10000));
        mockAuthenticatedUser("controleur.3n", 200L, true);

        AnalyseRisqueRequest request = new AnalyseRisqueRequest();
        request.setDateVisite(java.time.LocalDate.now());
        request.setLieuVisite("Antenne 3N");
        request.setCapaciteRemboursement(java.math.BigDecimal.valueOf(50000));
        request.setRevenuNetEstime(java.math.BigDecimal.valueOf(90000));
        request.setChargesMensuelles(java.math.BigDecimal.valueOf(15000));
        request.setActiviteVerifiee(true);
        request.setDescriptionActivite("Activite verifiee");
        request.setRecommandation(com.mini.credit.enums.RecommandationRisque.FAVORABLE);

        when(demandeCreditRepository.findById(6L)).thenReturn(Optional.of(demande));
        when(garantieCreditWorkflowService.isGarantieBloquee(6L)).thenReturn(true);
        when(analyseRisqueRepository.existsByAnalyseId(anyString())).thenReturn(false);
        when(analyseRisqueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditMapper.toResponse(any(DemandeCredit.class))).thenReturn(mock(DemandeCreditResponse.class));

        service.ajouterAnalyse(6L, request);

        assertThat(demande.getAnalyseRisque()).isNotNull();
        assertThat(demande.getAnalyseRisque().getAnalyste()).isNotNull();
        assertThat(demande.getAnalyseRisque().getAnalyste().getUsername()).isEqualTo("controleur.3n");
    }
}
