package com.mini.credit.service.impl;

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
import com.mini.credit.service.CreditValidationService;
import com.mini.credit.service.GarantieCreditWorkflowService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
