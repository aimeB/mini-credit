package com.mini.credit.service;

import com.mini.credit.dto.garantie.AjouterGarantieMaterielleRequest;
import com.mini.credit.dto.garantie.BloquerGarantieEpargneRequest;
import com.mini.credit.dto.garantie.GarantieCreditResponse;
import com.mini.credit.dto.garantie.GarantieMaterielleResponse;
import com.mini.credit.dto.garantie.ValiderGarantieRequest;
import com.mini.credit.dto.garantie.VerifierGarantieCreditRequest;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.GarantieCredit;
import com.mini.credit.entity.credit.GarantieMaterielle;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutGarantieCredit;
import com.mini.credit.enums.StatutGarantieMaterielle;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.GarantieCreditRepository;
import com.mini.credit.repository.credit.GarantieMaterielleRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.impl.GarantieCreditWorkflowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GarantieCreditWorkflowService - cas ciblés 3N")
class GarantieCreditServiceTest {

    @Mock
    private DemandeCreditRepository demandeCreditRepository;
    @Mock
    private GarantieCreditRepository garantieCreditRepository;
    @Mock
    private GarantieMaterielleRepository garantieMaterielleRepository;
    @Mock
    private CompteEpargneRepository compteEpargneRepository;
    @Mock
    private OperationEpargneRepository operationEpargneRepository;
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ParametreMetierService parametreMetierService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private GarantieCreditWorkflowServiceImpl service;

    @BeforeEach
    void setupCommonStubs() {
        lenient().when(garantieCreditRepository.save(any(GarantieCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(compteEpargneRepository.save(any(CompteEpargne.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(operationEpargneRepository.save(any(OperationEpargne.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(garantieMaterielleRepository.save(any(GarantieMaterielle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(garantieMaterielleRepository.findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(anyLong())).thenReturn(List.of());
        lenient().when(parametreMetierService.getDecimal("POURCENTAGE_GARANTIE")).thenReturn(new BigDecimal("20"));

        Utilisateur user = Utilisateur.builder().username("controleur.3n").build();
        user.setId(99L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
        lenient().when(utilisateurRepository.findById(99L)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculGarantieEpargne_shouldUse20PercentFor50000() {
        DemandeCredit demande = demande(10L, new BigDecimal("50000"));
        CompteEpargne compte = compte(demande.getMembre(), new BigDecimal("15000"), new BigDecimal("0"));

        when(garantieCreditRepository.findByDemandeCreditId(10L)).thenReturn(Optional.empty());
        when(demandeCreditRepository.findById(10L)).thenReturn(Optional.of(demande));
        when(compteEpargneRepository.findFirstByMembreIdAndStatut(demande.getMembre().getId(), StatutCompte.ACTIF))
                .thenReturn(Optional.of(compte));

        GarantieCreditResponse response = service.verifier(10L, new VerifierGarantieCreditRequest());

        assertThat(response.getMontantCredit()).isEqualByComparingTo("50000.00");
        assertThat(response.getMontantGarantieRequis()).isEqualByComparingTo("10000.00");
        assertThat(response.getMontantGarantieManquant()).isEqualByComparingTo("0.00");
        assertThat(response.getStatutGarantieEpargne()).isEqualTo(StatutGarantieCredit.SUFFISANTE);
    }

        @Test
        void verifier_shouldExposeExistingBlockedBalanceAsPartialGuarantee() {
                DemandeCredit demande = demande(11L, new BigDecimal("50000"));
                CompteEpargne compte = compte(demande.getMembre(), BigDecimal.ZERO, new BigDecimal("5000"));

                when(garantieCreditRepository.findByDemandeCreditId(11L)).thenReturn(Optional.empty());
                when(demandeCreditRepository.findById(11L)).thenReturn(Optional.of(demande));
                when(compteEpargneRepository.findFirstByMembreIdAndStatut(demande.getMembre().getId(), StatutCompte.ACTIF))
                                .thenReturn(Optional.of(compte));

                GarantieCreditResponse response = service.verifier(11L, new VerifierGarantieCreditRequest());

                assertThat(response.getMontantGarantieBloque()).isEqualByComparingTo("5000.00");
                assertThat(response.getMontantGarantieManquant()).isEqualByComparingTo("5000.00");
                assertThat(response.getStatutGarantieEpargne()).isEqualTo(StatutGarantieCredit.INSUFFISANTE);
        }

    @Test
    void blocageEpargne_shouldUpdateBalancesCreateOperationAndAudit() {
        DemandeCredit demande = demande(20L, new BigDecimal("50000"));
        CompteEpargne compte = compte(demande.getMembre(), new BigDecimal("15000"), new BigDecimal("0"));
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.NON_VERIFIEE, BigDecimal.ZERO, BigDecimal.ZERO);

        when(garantieCreditRepository.findByDemandeCreditId(20L)).thenReturn(Optional.of(garantie));
        when(compteEpargneRepository.findFirstByMembreIdAndStatut(demande.getMembre().getId(), StatutCompte.ACTIF))
                .thenReturn(Optional.of(compte));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GarantieCreditResponse response = service.bloquerEpargne(20L, new BloquerGarantieEpargneRequest());

        assertThat(compte.getSoldeDisponible()).isEqualByComparingTo("5000.00");
        assertThat(compte.getSoldeBloque()).isEqualByComparingTo("10000.00");
        assertThat(response.getMontantGarantieBloque()).isEqualByComparingTo("10000.00");
        assertThat(response.getStatutGarantieEpargne()).isIn(StatutGarantieCredit.BLOQUEE, StatutGarantieCredit.VALIDEE);

        ArgumentCaptor<OperationEpargne> operationCaptor = ArgumentCaptor.forClass(OperationEpargne.class);
        verify(operationEpargneRepository).save(operationCaptor.capture());
        OperationEpargne operation = operationCaptor.getValue();
        assertThat(operation.getTypeOperation()).isEqualTo(TypeOperationEpargne.BLOCAGE_GARANTIE);
        assertThat(operation.getMontant()).isEqualByComparingTo("10000.00");
        assertThat(operation.getSens()).isEqualTo(SensOperation.SORTIE);
        assertThat(operation.getDemandeCredit()).isEqualTo(demande);

        verify(auditService).logSuccess(eq(AuditAction.GARANTIE_BLOCKED), eq("GarantieCredit"), any(), any());
    }

        @Test
        void blocageEpargne_shouldCompleteExistingBlockedBalanceOnlyForMissingAmount() {
                DemandeCredit demande = demande(21L, new BigDecimal("50000"));
                CompteEpargne compte = compte(demande.getMembre(), new BigDecimal("7000"), new BigDecimal("5000"));
                GarantieCredit garantie = garantie(demande, StatutGarantieCredit.INSUFFISANTE, BigDecimal.ZERO, new BigDecimal("5000"));

                when(garantieCreditRepository.findByDemandeCreditId(21L)).thenReturn(Optional.of(garantie));
                when(compteEpargneRepository.findFirstByMembreIdAndStatut(demande.getMembre().getId(), StatutCompte.ACTIF))
                                .thenReturn(Optional.of(compte));
                when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));

                GarantieCreditResponse response = service.bloquerEpargne(21L, new BloquerGarantieEpargneRequest());

                assertThat(compte.getSoldeDisponible()).isEqualByComparingTo("2000.00");
                assertThat(compte.getSoldeBloque()).isEqualByComparingTo("10000.00");
                assertThat(response.getMontantGarantieBloque()).isEqualByComparingTo("10000.00");

                ArgumentCaptor<OperationEpargne> operationCaptor = ArgumentCaptor.forClass(OperationEpargne.class);
                verify(operationEpargneRepository).save(operationCaptor.capture());
                assertThat(operationCaptor.getValue().getMontant()).isEqualByComparingTo("5000.00");
        }

    @Test
    void soldeInsuffisant_shouldRefuseBlocageAndSetManquant() {
        DemandeCredit demande = demande(30L, new BigDecimal("50000"));
        CompteEpargne compte = compte(demande.getMembre(), BigDecimal.ZERO, BigDecimal.ZERO);
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.NON_VERIFIEE, BigDecimal.ZERO, BigDecimal.ZERO);

        when(garantieCreditRepository.findByDemandeCreditId(30L)).thenReturn(Optional.of(garantie));
        when(compteEpargneRepository.findFirstByMembreIdAndStatut(demande.getMembre().getId(), StatutCompte.ACTIF))
                .thenReturn(Optional.of(compte));

        assertThatThrownBy(() -> service.bloquerEpargne(30L, new BloquerGarantieEpargneRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insuffisant")
                .hasMessageContaining("10000.00 CDF");

        assertThat(garantie.getMontantGarantieManquant()).isEqualByComparingTo("10000.00");
        assertThat(garantie.getStatutGarantieEpargne()).isEqualTo(StatutGarantieCredit.INSUFFISANTE);
        verify(operationEpargneRepository, never()).save(any(OperationEpargne.class));
    }

        @Test
        void soldeInsuffisant_shouldKeepExistingBlockedAmountAndSetRemainingManquant() {
                DemandeCredit demande = demande(31L, new BigDecimal("50000"));
                CompteEpargne compte = compte(demande.getMembre(), new BigDecimal("1000"), new BigDecimal("5000"));
                GarantieCredit garantie = garantie(demande, StatutGarantieCredit.NON_VERIFIEE, BigDecimal.ZERO, BigDecimal.ZERO);

                when(garantieCreditRepository.findByDemandeCreditId(31L)).thenReturn(Optional.of(garantie));
                when(compteEpargneRepository.findFirstByMembreIdAndStatut(demande.getMembre().getId(), StatutCompte.ACTIF))
                                .thenReturn(Optional.of(compte));

                assertThatThrownBy(() -> service.bloquerEpargne(31L, new BloquerGarantieEpargneRequest()))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("insuffisant")
                                .hasMessageContaining("4000.00 CDF");

                assertThat(garantie.getMontantGarantieBloque()).isEqualByComparingTo("5000.00");
                assertThat(garantie.getMontantGarantieManquant()).isEqualByComparingTo("4000.00");
                assertThat(garantie.getStatutGarantieEpargne()).isEqualTo(StatutGarantieCredit.INSUFFISANTE);
                verify(operationEpargneRepository, never()).save(any(OperationEpargne.class));
        }

    @Test
    void ajouterGarantieMaterielle_shouldCreateDeclaredTvAndAudit() {
        DemandeCredit demande = demande(40L, new BigDecimal("50000"));
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.BLOQUEE, new BigDecimal("10000"), BigDecimal.ZERO);

        when(garantieCreditRepository.findByDemandeCreditId(40L)).thenReturn(Optional.of(garantie));

        AjouterGarantieMaterielleRequest request = new AjouterGarantieMaterielleRequest();
        request.setTypeBien("TELEVISION");
        request.setDescription("TV écran plat");
        request.setValeurEstimee(new BigDecimal("2000000"));
        request.setDevise("CDF");

        GarantieMaterielleResponse response = service.ajouterGarantieMaterielle(40L, request);

        assertThat(response.getTypeBien()).isEqualTo("TELEVISION");
        assertThat(response.getValeurEstimee()).isEqualByComparingTo("2000000.00");
        assertThat(response.getDevise()).isEqualTo("CDF");
        assertThat(response.getStatut()).isEqualTo(StatutGarantieMaterielle.DECLAREE);
        verify(auditService).logSuccess(eq(AuditAction.GARANTIE_MATERIAL_ADDED), eq("GarantieMaterielle"), any(), any());
    }

    @Test
    void validationGarantieMaterielle_shouldSupportAcceptRejectAndRequireCommentOnReject() {
        DemandeCredit demande = demande(50L, new BigDecimal("50000"));
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.BLOQUEE, new BigDecimal("10000"), BigDecimal.ZERO);

        GarantieMaterielle materielle = GarantieMaterielle.builder()
                .garantieCredit(garantie)
                .typeBien("TELEVISION")
                .description("TV")
                .valeurEstimee(new BigDecimal("2000000"))
                .devise("CDF")
                .statut(StatutGarantieMaterielle.DECLAREE)
                .build();
        materielle.setId(501L);

        when(garantieMaterielleRepository.findById(501L)).thenReturn(Optional.of(materielle));

        GarantieMaterielleResponse accepted = service.accepterGarantieMaterielle(50L, 501L, "contrôle ok");
        assertThat(accepted.getStatut()).isEqualTo(StatutGarantieMaterielle.ACCEPTEE);

        verify(auditService).logSuccess(eq(AuditAction.GARANTIE_MATERIAL_ACCEPTED), eq("GarantieMaterielle"), eq(501L), any());

        assertThatThrownBy(() -> service.refuserGarantieMaterielle(50L, 501L, "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("commentaire est obligatoire");

        GarantieMaterielleResponse refused = service.refuserGarantieMaterielle(50L, 501L, "document manquant");
        assertThat(refused.getStatut()).isEqualTo(StatutGarantieMaterielle.REFUSEE);
        assertThat(refused.getCommentaire()).contains("document manquant");

        verify(auditService).logSuccess(eq(AuditAction.GARANTIE_MATERIAL_REJECTED), eq("GarantieMaterielle"), eq(501L), any());
    }

    @Test
    void validationGlobale_shouldSetGarantieValideeWhenMaterielleConforme() {
        DemandeCredit demande = demande(60L, new BigDecimal("50000"));
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.BLOQUEE, new BigDecimal("10000"), BigDecimal.ZERO);

        GarantieMaterielle materielle = GarantieMaterielle.builder()
                .garantieCredit(garantie)
                .typeBien("TELEVISION")
                .description("TV")
                .valeurEstimee(new BigDecimal("2000000"))
                .devise("CDF")
                .statut(StatutGarantieMaterielle.ACCEPTEE)
                .build();

        when(garantieCreditRepository.findByDemandeCreditId(60L)).thenReturn(Optional.of(garantie));
        when(parametreMetierService.getTexte("AUTORISER_GARANTIE_MATERIELLE_OBLIGATOIRE")).thenReturn("true");
        when(garantieMaterielleRepository.findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(60L)).thenReturn(List.of(materielle));

        ValiderGarantieRequest request = new ValiderGarantieRequest();
        request.setCommentaire("Validation globale 3N");

        GarantieCreditResponse response = service.valider(60L, request);

        assertThat(response.getStatutGarantieEpargne()).isEqualTo(StatutGarantieCredit.VALIDEE);
        verify(auditService).logSuccess(eq(AuditAction.GARANTIE_VALIDATED), eq("GarantieCredit"), any(), any());
    }

    @Test
    void validationGarantieMaterielleRefuseSiMoinsDeDeuxFoisMontant() {
        DemandeCredit demande = demande(61L, new BigDecimal("50000"));
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.BLOQUEE, new BigDecimal("10000"), BigDecimal.ZERO);
        GarantieMaterielle materielle = GarantieMaterielle.builder()
                .garantieCredit(garantie)
                .typeBien("TELEVISION")
                .description("TV")
                .valeurEstimee(new BigDecimal("80000"))
                .devise("CDF")
                .statut(StatutGarantieMaterielle.ACCEPTEE)
                .build();

        when(garantieCreditRepository.findByDemandeCreditId(61L)).thenReturn(Optional.of(garantie));
        when(parametreMetierService.getTexte("AUTORISER_GARANTIE_MATERIELLE_OBLIGATOIRE")).thenReturn("true");
        when(garantieMaterielleRepository.findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(61L)).thenReturn(List.of(materielle));

        ValiderGarantieRequest request = new ValiderGarantieRequest();
        request.setCommentaire("Validation globale 3N");

        assertThatThrownBy(() -> service.valider(61L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("garanties matérielles acceptées")
                .hasMessageContaining("100000.00");
    }

    @Test
    void validationGarantieMaterielleOKSiDeuxFoisMontant() {
        DemandeCredit demande = demande(62L, new BigDecimal("50000"));
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.BLOQUEE, new BigDecimal("10000"), BigDecimal.ZERO);
        GarantieMaterielle materielle = GarantieMaterielle.builder()
                .garantieCredit(garantie)
                .typeBien("TELEVISION")
                .description("TV")
                .valeurEstimee(new BigDecimal("100000"))
                .devise("CDF")
                .statut(StatutGarantieMaterielle.ACCEPTEE)
                .build();

        when(garantieCreditRepository.findByDemandeCreditId(62L)).thenReturn(Optional.of(garantie));
        when(parametreMetierService.getTexte("AUTORISER_GARANTIE_MATERIELLE_OBLIGATOIRE")).thenReturn("true");
        when(garantieMaterielleRepository.findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(62L)).thenReturn(List.of(materielle));

        ValiderGarantieRequest request = new ValiderGarantieRequest();
        request.setCommentaire("Validation globale 3N");

        GarantieCreditResponse response = service.valider(62L, request);

        assertThat(response.getStatutGarantieEpargne()).isEqualTo(StatutGarantieCredit.VALIDEE);
    }

    @Test
    void idempotence_shouldNotCreateSecondBlocageOperationForSameDemande() {
        DemandeCredit demande = demande(70L, new BigDecimal("50000"));
        CompteEpargne compte = compte(demande.getMembre(), new BigDecimal("15000"), BigDecimal.ZERO);
        GarantieCredit garantie = garantie(demande, StatutGarantieCredit.NON_VERIFIEE, BigDecimal.ZERO, BigDecimal.ZERO);

        when(garantieCreditRepository.findByDemandeCreditId(70L)).thenReturn(Optional.of(garantie));
        when(compteEpargneRepository.findFirstByMembreIdAndStatut(demande.getMembre().getId(), StatutCompte.ACTIF))
                .thenReturn(Optional.of(compte));
        when(compteEpargneRepository.findById(compte.getId())).thenReturn(Optional.of(compte));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.bloquerEpargne(70L, new BloquerGarantieEpargneRequest());

        GarantieCreditResponse response = service.bloquerEpargne(70L, new BloquerGarantieEpargneRequest());

        assertThat(response.getStatutGarantieEpargne()).isEqualTo(StatutGarantieCredit.BLOQUEE);

        verify(operationEpargneRepository, times(1)).save(any(OperationEpargne.class));
    }

    private DemandeCredit demande(Long demandeId, BigDecimal montant) {
        Membre membre = Membre.builder().nomComplet("Membre Test").build();
        membre.setId(900L + demandeId);

        DemandeCredit demande = DemandeCredit.builder()
                .numeroDemande("DCR-" + demandeId)
                .membre(membre)
                .dateDemande(LocalDate.now())
                .montantDemande(montant)
                .devise("CDF")
                .dureeValeur(3)
                .dureeUnite(com.mini.credit.enums.DureeUnite.MOIS)
                .periodiciteRemboursement(com.mini.credit.enums.PeriodiciteRemboursement.MENSUEL)
                .tauxInteret(new BigDecimal("5"))
                .objetCredit("Stock")
                .fraisDemande(new BigDecimal("1000"))
                .depotGarantieRequis(BigDecimal.ZERO)
                .depotGarantiePaye(BigDecimal.ZERO)
                .statut(StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE)
                .build();
        demande.setId(demandeId);
        return demande;
    }

    private CompteEpargne compte(Membre membre, BigDecimal soldeDisponible, BigDecimal soldeBloque) {
        CompteEpargne compte = CompteEpargne.builder()
                .membre(membre)
                .numeroCompte("EPN-" + membre.getId())
                .typeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE)
                .soldeDisponible(soldeDisponible)
                .soldeBloque(soldeBloque)
                .statut(StatutCompte.ACTIF)
                .dateOuverture(LocalDate.now().minusMonths(1))
                .build();
        compte.setId(800L + membre.getId());
        return compte;
    }

    private GarantieCredit garantie(DemandeCredit demande,
                                    StatutGarantieCredit statut,
                                    BigDecimal montantBloque,
                                    BigDecimal montantManquant) {
        GarantieCredit garantie = GarantieCredit.builder()
                .demandeCredit(demande)
                .membre(demande.getMembre())
                .montantCredit(demande.getMontantDemande())
                .devise(demande.getDevise())
                .montantGarantieRequis(new BigDecimal("10000.00"))
                .montantGarantieBloque(montantBloque)
                .montantGarantieManquant(montantManquant)
                .statutGarantieEpargne(statut)
                .build();
        garantie.setId(700L + demande.getId());
        return garantie;
    }
}
