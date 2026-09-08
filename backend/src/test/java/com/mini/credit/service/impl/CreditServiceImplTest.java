package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.document.QuittanceCreateRequest;
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
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
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
import com.mini.credit.service.CreditValidationService;
import com.mini.credit.service.WorkflowTaskService;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.mockito.ArgumentCaptor;

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
                private CreditValidationService creditValidationService;
        @Mock
        private WorkflowTaskService workflowTaskService;
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
        when(creditRepository.findByDemandeCreditId(11L)).thenReturn(Optional.empty());
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

        @Test
        void approbation_shouldCreateWorkflowTaskForCaissier() {
                Membre membre = Membre.builder().nomComplet("Membre A").build();
                membre.setId(501L);

                DemandeCredit demande = DemandeCredit.builder()
                                .numeroDemande("DCR-APP-002")
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
                        when(creditRepository.findByDemandeCreditId(11L)).thenReturn(Optional.empty());
                when(utilisateurRepository.findById(900L)).thenReturn(Optional.of(decideur));
                when(creditRepository.existsByMembreIdAndStatutIn(any(), any())).thenReturn(false);
                when(garantieCreditWorkflowService.isGarantieValidee(11L)).thenReturn(true);
                when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> {
                        Credit credit = invocation.getArgument(0);
                        if (credit.getId() == null) {
                                credit.setId(1L);
                        }
                        return credit;
                });
                when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(creditMapper.toResponse(any(Credit.class))).thenReturn(mock(CreditResponse.class));

                service.approuverDemande(11L, request);

                verify(workflowTaskService).onDemandeCreditApprouvee(eq(11L), eq(1L), anyString(), isNull(), isNull());
        }

        @Test
        void approbationCreeCreditAvecDureeDeLaDemande() {
                Membre membre = Membre.builder().nomComplet("Membre A").build();
                membre.setId(501L);

                DemandeCredit demande = DemandeCredit.builder()
                                .numeroDemande("DCR-APP-DUREE")
                                .membre(membre)
                                .dateDemande(LocalDate.now())
                                .montantDemande(new BigDecimal("50000"))
                                .devise("CDF")
                                .dureeValeur(6)
                                .dureeUnite(DureeUnite.MOIS)
                                .periodiciteRemboursement(PeriodiciteRemboursement.MENSUEL)
                                .tauxInteret(new BigDecimal("5"))
                                .objetCredit("Stock")
                                .statut(StatutDemandeCredit.VALIDATION_CHEF)
                                .analyseRisque(new AnalyseRisque())
                                .fraisDemande(new BigDecimal("7500"))
                                .fraisDemandePayes(new BigDecimal("7500"))
                                .depotGarantieRequis(new BigDecimal("10000"))
                                .depotGarantiePaye(new BigDecimal("10000"))
                                .build();
                demande.setId(14L);
                Utilisateur decideur = Utilisateur.builder().username("chef.bureau").build();
                decideur.setId(900L);
                ApprobationCreditRequest request = new ApprobationCreditRequest();
                request.setDecidedBy(900L);
                request.setGenererEcheancier(false);

                when(demandeCreditRepository.findById(14L)).thenReturn(Optional.of(demande));
                when(creditRepository.findByDemandeCreditId(14L)).thenReturn(Optional.empty());
                when(utilisateurRepository.findById(900L)).thenReturn(Optional.of(decideur));
                when(creditRepository.existsByMembreIdAndStatutIn(any(), any())).thenReturn(false);
                when(garantieCreditWorkflowService.isGarantieValidee(14L)).thenReturn(true);
                when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> {
                        Credit credit = invocation.getArgument(0);
                        if (credit.getId() == null) {
                                credit.setId(14L);
                        }
                        return credit;
                });
                when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(creditMapper.toResponse(any(Credit.class))).thenReturn(mock(CreditResponse.class));

                service.approuverDemande(14L, request);

                ArgumentCaptor<Credit> creditCaptor = ArgumentCaptor.forClass(Credit.class);
                verify(creditRepository, org.mockito.Mockito.atLeastOnce()).save(creditCaptor.capture());
                assertThat(creditCaptor.getAllValues().get(0).getDureeValeur()).isEqualTo(6);
        }

        @Test
        void decaissementRefuseSiFraisIncomplets() {
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
                                .numeroDemande("DCR-DEC-FRAIS")
                                .membre(membre)
                                .statut(StatutDemandeCredit.APPROUVEE)
                                .fraisDemande(new BigDecimal("7500"))
                                .fraisDemandePayes(new BigDecimal("3000"))
                                .build();
                demande.setId(15L);
                Credit credit = Credit.builder()
                                .numeroCredit("CR-FRAIS")
                                .demandeCredit(demande)
                                .membre(membre)
                                .statut(StatutCredit.APPROUVE)
                                .montantOctroye(new BigDecimal("50000"))
                                .devise("CDF")
                                .build();
                credit.setId(78L);
                SessionCaisse session = SessionCaisse.builder()
                                .statut(StatutSessionCaisse.OUVERTE)
                                .caisse(Caisse.builder().actif(true).build())
                                .soldeTheorique(new BigDecimal("100000"))
                                .build();
                session.setId(44L);
                DecaissementCreditRequest request = new DecaissementCreditRequest();
                request.setSessionCaisseId(44L);
                request.setDateDecaissement(LocalDateTime.now());
                request.setModePaiement(ModePaiement.ESPECES);
                request.setCreatedBy(900L);

                when(creditRepository.findById(78L)).thenReturn(Optional.of(credit));
                when(sessionCaisseRepository.findById(44L)).thenReturn(Optional.of(session));
                doThrow(new BusinessException("Les frais de demande doivent être intégralement payés"))
                                .when(creditValidationService).verifierFraisDemandeIntegralementPayes(demande);

                assertThatThrownBy(() -> service.decaisserCredit(78L, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("frais");
        }

        @Test
        void decaissementRefuseSiCreditNonApprouve() {
                authenticateCaissier();
                Credit credit = creditPourDecaissement(79L, StatutCredit.EN_COURS, StatutDemandeCredit.APPROUVEE);
                DecaissementCreditRequest request = decaissementRequest();

                when(creditRepository.findById(79L)).thenReturn(Optional.of(credit));

                assertThatThrownBy(() -> service.decaisserCredit(79L, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("crédit approuvé");
                verify(operationCaisseService, never()).enregistrer(any(OperationCaisseRequest.class));
        }

        @Test
        void decaissementRefuseSiGarantieNonValidee() {
                authenticateCaissier();
                Credit credit = creditPourDecaissement(80L, StatutCredit.APPROUVE, StatutDemandeCredit.APPROUVEE);
                SessionCaisse session = sessionOuverteAvecSolde("100000");
                DecaissementCreditRequest request = decaissementRequest();

                when(creditRepository.findById(80L)).thenReturn(Optional.of(credit));
                when(sessionCaisseRepository.findById(44L)).thenReturn(Optional.of(session));
                when(garantieCreditWorkflowService.isGarantieValidee(credit.getDemandeCredit().getId())).thenReturn(false);

                assertThatThrownBy(() -> service.decaisserCredit(80L, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("garantie validée");
                verify(operationCaisseService, never()).enregistrer(any(OperationCaisseRequest.class));
        }

        @Test
        void decaissementOKSiConditionsCompletes() {
                authenticateCaissier();
                Credit credit = creditPourDecaissement(81L, StatutCredit.APPROUVE, StatutDemandeCredit.APPROUVEE);
                SessionCaisse session = sessionOuverteAvecSolde("100000");
                DecaissementCreditRequest request = decaissementRequest();

                when(creditRepository.findById(81L)).thenReturn(Optional.of(credit));
                when(sessionCaisseRepository.findById(44L)).thenReturn(Optional.of(session));
                when(garantieCreditWorkflowService.isGarantieValidee(credit.getDemandeCredit().getId())).thenReturn(true);
                when(utilisateurRepository.findById(900L)).thenReturn(Optional.of(Utilisateur.builder().username("caissier").build()));
                when(operationCaisseService.enregistrer(any(OperationCaisseRequest.class))).thenReturn(OperationCaisseResponse.builder().id(500L).build());
                when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(creditMapper.toResponse(any(Credit.class))).thenReturn(mock(CreditResponse.class));

                service.decaisserCredit(81L, request);

                ArgumentCaptor<OperationCaisseRequest> operationCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
                verify(operationCaisseService).enregistrer(operationCaptor.capture());
                OperationCaisseRequest operation = operationCaptor.getValue();
                assertThat(operation.getTypeOperation()).isEqualTo(TypeOperationCaisse.SORTIE);
                assertThat(operation.getCategorieOperation()).isEqualTo(CategorieOperationCaisse.DECAISSEMENT_CREDIT);
                assertThat(operation.getSource()).isEqualTo(SourceOperationCaisse.CREDIT_DECAISSEMENT);
                assertThat(operation.getMontant()).isEqualByComparingTo("50000.00");
                assertThat(operation.getCreditId()).isEqualTo(81L);
                assertThat(operation.getSessionCaisseId()).isEqualTo(44L);
                assertThat(operation.getCaisseId()).isEqualTo(3L);
                assertThat(credit.getStatut()).isEqualTo(StatutCredit.DECAISSE);
                verify(quittanceService).create(any(QuittanceCreateRequest.class));
                verify(workflowTaskService).onCreditDecaisse(eq(81L), eq("CR-DEC-81"), isNull(), isNull());
                verify(creditRepository).save(credit);
        }

        @Test
        void approbation_shouldReturnExistingCreditWithoutCreatingDuplicate() {
                Membre membre = Membre.builder().nomComplet("Membre A").build();
                membre.setId(501L);

                DemandeCredit demande = DemandeCredit.builder()
                                .numeroDemande("DCR-APP-003")
                                .membre(membre)
                                .statut(StatutDemandeCredit.APPROUVEE)
                                .build();
                demande.setId(11L);

                Credit existingCredit = Credit.builder()
                                .numeroCredit("CR-2026-1")
                                .demandeCredit(demande)
                                .membre(membre)
                                .statut(StatutCredit.APPROUVE)
                                .build();
                existingCredit.setId(1L);

                CreditResponse response = mock(CreditResponse.class);

                when(demandeCreditRepository.findById(11L)).thenReturn(Optional.of(demande));
                when(creditRepository.findByDemandeCreditId(11L)).thenReturn(Optional.of(existingCredit));
                when(creditMapper.toResponse(existingCredit)).thenReturn(response);

                CreditResponse result = service.approuverDemande(11L, new ApprobationCreditRequest());

                assertThat(result).isSameAs(response);
                verify(creditRepository, never()).save(any(Credit.class));
                verify(workflowTaskService, never()).onDemandeCreditApprouvee(any(), any(), any(), any(), any());
        }

        private void authenticateCaissier() {
                Utilisateur caissier = Utilisateur.builder()
                                .username("caissier")
                                .role(com.mini.credit.entity.referentiel.Role.builder().code(RoleCode.CAISSIER).build())
                                .build();
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(caissier, null, caissier.getAuthorities())
                );
        }

        private DecaissementCreditRequest decaissementRequest() {
                DecaissementCreditRequest request = new DecaissementCreditRequest();
                request.setSessionCaisseId(44L);
                request.setDateDecaissement(LocalDateTime.of(2026, 7, 24, 10, 0));
                request.setModePaiement(ModePaiement.ESPECES);
                request.setCreatedBy(900L);
                return request;
        }

        private SessionCaisse sessionOuverteAvecSolde(String solde) {
                Caisse caisse = Caisse.builder().actif(true).build();
                caisse.setId(3L);
                SessionCaisse session = SessionCaisse.builder()
                                .statut(StatutSessionCaisse.OUVERTE)
                                .caisse(caisse)
                                .soldeTheorique(new BigDecimal(solde))
                                .build();
                session.setId(44L);
                return session;
        }

        private Credit creditPourDecaissement(Long id, StatutCredit statutCredit, StatutDemandeCredit statutDemande) {
                Membre membre = Membre.builder().nomComplet("Membre B").build();
                membre.setId(601L);
                DemandeCredit demande = DemandeCredit.builder()
                                .numeroDemande("DCR-DEC-" + id)
                                .membre(membre)
                                .statut(statutDemande)
                                .fraisDemande(new BigDecimal("7500"))
                                .fraisDemandePayes(new BigDecimal("7500"))
                                .build();
                demande.setId(id + 1000L);
                Credit credit = Credit.builder()
                                .numeroCredit("CR-DEC-" + id)
                                .demandeCredit(demande)
                                .membre(membre)
                                .statut(statutCredit)
                                .montantOctroye(new BigDecimal("50000"))
                                .devise("CDF")
                                .build();
                credit.setId(id);
                return credit;
        }
}
