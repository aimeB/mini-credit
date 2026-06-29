package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.AnalyseRisque;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.GarantieCreditWorkflowService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.QuittanceService;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.enums.security.RoleCode;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditServiceImpl - garde garantie validée")
class CreditServiceImplTest {

        @AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

    @Mock
    private CreditRepository creditRepository;
    @Mock
    private DemandeCreditRepository demandeCreditRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private EcheanceCreditRepository echeanceCreditRepository;
    @Mock
    private RemboursementCreditRepository remboursementCreditRepository;
    @Mock
    private SessionCaisseRepository sessionCaisseRepository;
    @Mock
    private AgentTerrainRepository agentTerrainRepository;
    @Mock
    private MembreRepository membreRepository;
    @Mock
    private CreditMapper creditMapper;
    @Mock
    private OperationCaisseService operationCaisseService;
    @Mock
    private QuittanceService quittanceService;
    @Mock
    private GarantieCreditWorkflowService garantieCreditWorkflowService;
    @Mock
    private ScopeService scopeService;
        @Mock
        private AuditService auditService;

    @InjectMocks
    private CreditServiceImpl service;

    @Test
    void approbation_shouldBeBlockedWhenGarantieNotValidated() {
        Membre membre = Membre.builder().nomComplet("Membre A").build();
        membre.setId(501L);

        DemandeCredit demande = DemandeCredit.builder()
                .numeroDemande("DCR-APP-001")
                .membre(membre)
                .dateDemande(LocalDate.now())
                .montantDemande(new BigDecimal("50000"))
                .devise("CDF")
                .dureeValeur(3)
                .dureeUnite(DureeUnite.MOIS)
                .periodiciteRemboursement(PeriodiciteRemboursement.MENSUEL)
                .tauxInteret(new BigDecimal("5"))
                .objetCredit("Stock")
                .statut(StatutDemandeCredit.VALIDATION_CHEF)
                .analyseRisque(new AnalyseRisque())
                .depotGarantieRequis(new BigDecimal("10000"))
                .depotGarantiePaye(new BigDecimal("10000"))
                .build();
        demande.setId(11L);

        Utilisateur decideur = Utilisateur.builder().username("chef.bureau").build();
        decideur.setId(900L);

        ApprobationCreditRequest request = new ApprobationCreditRequest();
        request.setDecidedBy(900L);
        request.setGenererEcheancier(false);

        when(demandeCreditRepository.findById(11L)).thenReturn(Optional.of(demande));
        when(utilisateurRepository.findById(900L)).thenReturn(Optional.of(decideur));
        when(creditRepository.existsByMembreIdAndStatutIn(any(), any())).thenReturn(false);
        when(garantieCreditWorkflowService.isGarantieValidee(11L)).thenReturn(false);

        assertThatThrownBy(() -> service.approuverDemande(11L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("garantie validée");

        verify(creditRepository, never()).save(any(Credit.class));
    }

    @Test
    void decaissement_shouldBeBlockedWhenGarantieNotValidated() {
        Utilisateur caissier = Utilisateur.builder()
                .username("caissier")
                .role(com.mini.credit.entity.referentiel.Role.builder().code(RoleCode.CAISSIER).build())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caissier, null, caissier.getAuthorities())
        );

        Membre membre = Membre.builder().nomComplet("Membre B").build();
        membre.setId(601L);

        DemandeCredit demande = DemandeCredit.builder()
                .numeroDemande("DCR-DEC-001")
                .membre(membre)
                .dateDemande(LocalDate.now())
                .montantDemande(new BigDecimal("50000"))
                .devise("CDF")
                .dureeValeur(3)
                .dureeUnite(DureeUnite.MOIS)
                .periodiciteRemboursement(PeriodiciteRemboursement.MENSUEL)
                .tauxInteret(new BigDecimal("5"))
                .objetCredit("Stock")
                .statut(StatutDemandeCredit.APPROUVEE)
                .build();
        demande.setId(12L);

        Credit credit = Credit.builder()
                .numeroCredit("CR-001")
                .demandeCredit(demande)
                .membre(membre)
                .statut(StatutCredit.APPROUVE)
                .montantOctroye(new BigDecimal("50000"))
                .devise("CDF")
                .build();
        credit.setId(77L);

        Caisse caisse = Caisse.builder().actif(true).build();
        caisse.setId(3L);

        SessionCaisse session = SessionCaisse.builder()
                .statut(StatutSessionCaisse.OUVERTE)
                .caisse(caisse)
                .build();
        session.setId(44L);

        DecaissementCreditRequest request = new DecaissementCreditRequest();
        request.setSessionCaisseId(44L);
        request.setDateDecaissement(LocalDateTime.now());
        request.setModePaiement(ModePaiement.ESPECES);
        request.setCreatedBy(900L);

        when(creditRepository.findById(77L)).thenReturn(Optional.of(credit));
        when(sessionCaisseRepository.findById(44L)).thenReturn(Optional.of(session));
        when(garantieCreditWorkflowService.isGarantieValidee(12L)).thenReturn(false);

        assertThatThrownBy(() -> service.decaisserCredit(77L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("garantie validée");
    }
}
