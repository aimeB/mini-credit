package com.mini.credit.service;

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
import com.mini.credit.service.impl.CreditServiceImpl;
import com.mini.credit.service.GarantieCreditWorkflowService;
import com.mini.credit.service.CreditValidationService;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditService - garde workflow approbation/decaissement")
class CreditWorkflowServiceTest {

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
                private CreditValidationService creditValidationService;
    @Mock
    private ScopeService scopeService;
        @Mock
        private AuditService auditService;

    @InjectMocks
    private CreditServiceImpl service;

    @Test
    void approbation_shouldFailWhenGarantieIsInsufficient() {
        Membre membre = Membre.builder().nomComplet("Test").build();
        membre.setId(5L);

        DemandeCredit demande = DemandeCredit.builder()
                .membre(membre)
                .statut(StatutDemandeCredit.VALIDATION_CHEF)
                .montantDemande(new BigDecimal("100000"))
                .tauxInteret(new BigDecimal("5"))
                .dureeValeur(3)
                .dureeUnite(DureeUnite.MOIS)
                .periodiciteRemboursement(PeriodiciteRemboursement.MENSUEL)
                .analyseRisque(new AnalyseRisque())
                .depotGarantieRequis(new BigDecimal("20000"))
                .depotGarantiePaye(new BigDecimal("10000"))
                .build();
        demande.setId(1L);

        ApprobationCreditRequest request = new ApprobationCreditRequest();
        request.setDecidedBy(11L);
        request.setGenererEcheancier(false);

        Utilisateur decideur = Utilisateur.builder().username("chef").build();
        decideur.setId(11L);

        when(demandeCreditRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(utilisateurRepository.findById(11L)).thenReturn(Optional.of(decideur));
        when(creditRepository.existsByMembreIdAndStatutIn(any(), any())).thenReturn(false);
        when(garantieCreditWorkflowService.isGarantieValidee(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.approuverDemande(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépôt de garantie");
    }

    @Test
    void approbation_shouldFailWhenStatutIsNotValidationChefOrControleur() {
        Membre membre = Membre.builder().nomComplet("Test").build();
        membre.setId(5L);

        DemandeCredit demande = DemandeCredit.builder()
                .membre(membre)
                .statut(StatutDemandeCredit.EN_ANALYSE)
                .montantDemande(new BigDecimal("100000"))
                .tauxInteret(new BigDecimal("5"))
                .dureeValeur(3)
                .dureeUnite(DureeUnite.MOIS)
                .periodiciteRemboursement(PeriodiciteRemboursement.MENSUEL)
                .build();
        demande.setId(2L);

        ApprobationCreditRequest request = new ApprobationCreditRequest();
        request.setDecidedBy(11L);

        Utilisateur decideur = Utilisateur.builder().username("chef").build();
        decideur.setId(11L);

        when(demandeCreditRepository.findById(2L)).thenReturn(Optional.of(demande));
        when(utilisateurRepository.findById(11L)).thenReturn(Optional.of(decideur));

        assertThatThrownBy(() -> service.approuverDemande(2L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Statut de demande non pris en charge");
    }

    @Test
    void decaissement_shouldFailWhenDemandeIsNotApprouvee() {
        Utilisateur caissier = Utilisateur.builder()
                .username("caissier")
                .role(com.mini.credit.entity.referentiel.Role.builder().code(RoleCode.CAISSIER).build())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caissier, null, caissier.getAuthorities())
        );

        DemandeCredit demande = DemandeCredit.builder()
                .statut(StatutDemandeCredit.VALIDATION_CHEF)
                .build();
        demande.setId(3L);

        Membre membre = Membre.builder().nomComplet("Test").build();
        membre.setId(12L);

        Credit credit = Credit.builder()
                .statut(StatutCredit.APPROUVE)
                .demandeCredit(demande)
                .membre(membre)
                .montantOctroye(new BigDecimal("50000"))
                .devise("CDF")
                .numeroCredit("CR-001")
                .build();
        credit.setId(9L);

        DecaissementCreditRequest request = new DecaissementCreditRequest();
        request.setSessionCaisseId(1L);
        request.setDateDecaissement(LocalDateTime.now());
        request.setModePaiement(ModePaiement.ESPECES);
        request.setCreatedBy(44L);

        Caisse caisse = Caisse.builder().actif(true).build();
        caisse.setId(1L);
        SessionCaisse session = SessionCaisse.builder()
                .statut(StatutSessionCaisse.OUVERTE)
                .caisse(caisse)
                .build();
        session.setId(1L);

        when(creditRepository.findById(9L)).thenReturn(Optional.of(credit));
        when(sessionCaisseRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.decaisserCredit(9L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("demande est approuvée");
    }
}
