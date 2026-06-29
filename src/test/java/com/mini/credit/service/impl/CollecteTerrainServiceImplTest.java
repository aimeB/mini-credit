package com.mini.credit.service.impl;

import com.mini.credit.dto.referentiel.CreateCollecteMembreLigneRequest;
import com.mini.credit.dto.referentiel.CreateCollecteTerrainRequest;
import com.mini.credit.dto.referentiel.ConfirmerBilletageRequest;
import com.mini.credit.dto.referentiel.ValidateCollecteTerrainRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.*;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.collecteTerrain.CollecteOperationGenereeRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.service.CreditService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.OperationEpargneService;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.credit.RemboursementRequest;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import org.springframework.data.domain.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollecteTerrainServiceImplTest {

    @Mock private CollecteJournaliereTerrainRepository collecteRepository;
    @Mock private CollecteMembreLigneRepository ligneRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AgentTerrainRepository agentTerrainRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private CompteEpargneRepository compteEpargneRepository;
    @Mock private CreditRepository creditRepository;
    @Mock private DemandeCreditRepository demandeCreditRepository;
    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private CollecteOperationGenereeRepository collecteOperationGenereeRepository;
    @Mock private OperationEpargneService operationEpargneService;
    @Mock private CreditService creditService;
    @Mock private OperationCaisseService operationCaisseService;
    @Mock private ParametreMetierService parametreMetierService;
    @Mock private RemboursementCreditRepository remboursementCreditRepository;
    @Mock private AuditService auditService;

    @InjectMocks private CollecteTerrainServiceImpl service;

    private Utilisateur agentUser;
    private AgentTerrain agent;
    private Site site;
    private CollecteJournaliereTerrain collecte;
    private Membre membre;

    @BeforeEach
    void init() {
        Role role = new Role();
        role.setCode(RoleCode.AGENT_TERRAIN);

        agentUser = new Utilisateur();
        agentUser.setId(1L);
        agentUser.setUsername("agent01");
        agentUser.setRole(role);

        site = new Site();
        site.setId(10L);
        site.setActif(true);
        Agence agence = new Agence();
        agence.setId(99L);
        site.setAgence(agence);

        agent = new AgentTerrain();
        agent.setId(100L);
        agent.setUtilisateur(agentUser);
        agent.setSite(site);

        membre = new Membre();
        membre.setId(200L);
        membre.setCodeMembre("MB001");
        membre.setNomComplet("Membre Test");
        membre.setSite(site);

        collecte = CollecteJournaliereTerrain.builder()
            .agentTerrain(agent)
            .site(site)
            .antenneId(99L)
            .dateCollecte(LocalDate.now())
            .statut(RecetteStatut.BROUILLON)
            .especesRemises(BigDecimal.ZERO)
            .build();
        collecte.setId(500L);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("agent01", null, List.of(new SimpleGrantedAuthority("ROLE_AGENT_TERRAIN")))
        );
        lenient().when(utilisateurRepository.findByUsername("agent01")).thenReturn(Optional.of(agentUser));
        lenient().when(agentTerrainRepository.findByUtilisateurId(1L)).thenReturn(Optional.of(agent));
        lenient().when(collecteOperationGenereeRepository.findByCollecteId(any(Long.class))).thenReturn(List.of());
        lenient().when(parametreMetierService.getDecimal("PRIX_CARNET")).thenReturn(new BigDecimal("1000"));
        lenient().when(parametreMetierService.getDecimal("FRAIS_ANALYSE_DEMANDE")).thenReturn(new BigDecimal("5000"));
        lenient().when(parametreMetierService.getDecimal("TAUX_INTERET_CREDIT_MAX")).thenReturn(new BigDecimal("20"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void markBilletageConfirme(CollecteJournaliereTerrain c, BigDecimal montantConfirme) {
        c.setBilletageConfirme(true);
        c.setEspecesConfirmeesCaissier(montantConfirme);
        c.setEspecesRemises(montantConfirme);
        c.setDateConfirmationBilletage(LocalDateTime.now());
        c.setConfirmeParCaissierId(3L);
        c.setObservationBilletage("RAS");
    }

    @Test
    void agentCreeSaCollecteDuJour() {
        when(collecteRepository.findByAgentTerrainIdAndDateCollecte(100L, LocalDate.now())).thenReturn(Optional.empty());
        when(collecteRepository.save(any())).thenAnswer(inv -> {
            CollecteJournaliereTerrain c = inv.getArgument(0);
            c.setId(600L);
            return c;
        });
        when(ligneRepository.findByCollecteId(any(Long.class))).thenReturn(List.of());

        CreateCollecteTerrainRequest request = CreateCollecteTerrainRequest.builder()
            .especesRemises(new BigDecimal("100"))
            .totalGeneralCalcule(new BigDecimal("999999"))
            .build();

        var response = service.create(request);
        assertThat(response.getId()).isEqualTo(600L);
        assertThat(response.getSiteId()).isEqualTo(10L);
        assertThat(response.getAgentTerrainId()).isEqualTo(100L);
        assertThat(response.getAntenneId()).isEqualTo(99L);
        assertThat(response.getDateCollecte()).isEqualTo(LocalDate.now());
    }

    @Test
    void creationDeduitToujoursAgentEtSiteServeur() {
        Site autreSite = new Site();
        autreSite.setId(999L);
        agent.setSite(autreSite);
        Agence autreAgence = new Agence();
        autreAgence.setId(123L);
        autreSite.setAgence(autreAgence);

        when(collecteRepository.findByAgentTerrainIdAndDateCollecte(100L, LocalDate.now())).thenReturn(Optional.empty());
        when(collecteRepository.save(any())).thenAnswer(inv -> {
            CollecteJournaliereTerrain c = inv.getArgument(0);
            c.setId(601L);
            return c;
        });
        when(ligneRepository.findByCollecteId(any(Long.class))).thenReturn(List.of());

        var response = service.create(CreateCollecteTerrainRequest.builder().totalGeneralCalcule(new BigDecimal("9999")).build());
        assertThat(response.getAgentTerrainId()).isEqualTo(100L);
        assertThat(response.getSiteId()).isEqualTo(999L);
        assertThat(response.getAntenneId()).isEqualTo(123L);
    }

    @Test
    void agentAjouteLigneEpargnePourMembre() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);

        when(ligneRepository.save(any())).thenAnswer(inv -> {
            CollecteMembreLigne l = inv.getArgument(0);
            l.setId(700L);
            return l;
        });
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of());
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(compteEpargneRepository.findFirstByMembreIdAndStatut(200L, StatutCompte.ACTIF)).thenReturn(Optional.of(compte));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("25"))
            .build();

        var response = service.addLigne(500L, request);
        assertThat(response.getId()).isEqualTo(700L);
        assertThat(response.getTypeLigne()).isEqualTo(TypeLigneCollecte.EPARGNE);
    }

    @Test
    void remboursementRefuseSiCreditNonActif() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        Credit credit = new Credit();
        credit.setId(400L);
        credit.setStatut(StatutCredit.REMBOURSE);
        when(creditRepository.findById(400L)).thenReturn(Optional.of(credit));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.REMBOURSEMENT_CREDIT)
            .montant(new BigDecimal("10"))
            .creditId(400L)
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Crédit actif obligatoire");
    }

    @Test
    void remboursementAutoDeduitSiUnSeulCreditActifExiste() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        Credit credit = new Credit();
        credit.setId(401L);
        credit.setStatut(StatutCredit.EN_COURS);
        when(creditRepository.findByMembreId(200L)).thenReturn(List.of(credit));

        when(ligneRepository.save(any())).thenAnswer(inv -> {
            CollecteMembreLigne l = inv.getArgument(0);
            l.setId(701L);
            return l;
        });
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of());
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.REMBOURSEMENT_CREDIT)
            .montant(new BigDecimal("10"))
            .build();

        var response = service.addLigne(500L, request);
        assertThat(response.getId()).isEqualTo(701L);
        assertThat(response.getCreditId()).isEqualTo(401L);
    }

    @Test
    void remboursementRefuseSiAucunCreditActif() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));
        when(creditRepository.findByMembreId(200L)).thenReturn(List.of());

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.REMBOURSEMENT_CREDIT)
            .montant(new BigDecimal("10"))
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("aucun crédit en cours");
    }

    @Test
    void remboursementRefuseSiPlusieursCreditsActifs() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        Credit c1 = new Credit();
        c1.setId(410L);
        c1.setStatut(StatutCredit.EN_COURS);
        Credit c2 = new Credit();
        c2.setId(411L);
        c2.setStatut(StatutCredit.DECAISSE);
        when(creditRepository.findByMembreId(200L)).thenReturn(List.of(c1, c2));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.REMBOURSEMENT_CREDIT)
            .montant(new BigDecimal("10"))
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Plusieurs crédits actifs");
    }

    @Test
    void epargneRefuseSiCompteInactif() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(301L);
        compte.setStatut(StatutCompte.FERME);
        when(compteEpargneRepository.findById(301L)).thenReturn(Optional.of(compte));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("25"))
            .compteEpargneId(301L)
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Compte épargne actif obligatoire");
    }

    @Test
    void epargneRefuseSiAucunCompteActifMembre() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));
        when(compteEpargneRepository.findFirstByMembreIdAndStatut(200L, StatutCompte.ACTIF)).thenReturn(Optional.empty());

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("25"))
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("n'a pas de compte épargne actif");
    }

    @Test
    void carnetRefuseSiMontantNonPositif() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));
        when(ligneRepository.findByCollecteIdAndMembreIdAndTypeLigne(500L, 200L, TypeLigneCollecte.CARNET)).thenReturn(List.of());
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of());
        when(ligneRepository.save(any())).thenAnswer(inv -> {
            CollecteMembreLigne l = inv.getArgument(0);
            l.setId(710L);
            return l;
        });
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(BigDecimal.ZERO)
            .quantite(2)
            .build();

        var response = service.addLigne(500L, request);
        assertThat(response.getQuantite()).isEqualTo(1);
        assertThat(response.getMontant()).isEqualByComparingTo("1000");
    }

    @Test
    void fraisAnalyseRefuseSiMontantNonPositif() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.FRAIS_ANALYSE)
            .montant(BigDecimal.ZERO)
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("FRAIS_ANALYSE n'est pas autorisé");
    }

    @Test
    void carnetRefuseDoublonPourMemeMembreDansCollecte() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        CollecteMembreLigne existing = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("1000"))
            .quantite(1)
            .build();
        existing.setId(900L);

        when(ligneRepository.findByCollecteIdAndMembreIdAndTypeLigne(500L, 200L, TypeLigneCollecte.CARNET)).thenReturn(List.of(existing));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("déjà été enregistré");
    }

    @Test
    void demandeCreditGenereeOfficiellementApresValidationCollecte() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("250000"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.DEMANDE_CREDIT)
            .montant(BigDecimal.ZERO)
            .montantSouhaite(new BigDecimal("250000"))
            .objetCredit("Renforcement stock")
            .gagePropose("Equipement")
            .modaliteRemboursement(com.mini.credit.enums.ModaliteRemboursementCollecte.MENSUELLE)
            .build();
        ligne.setId(2L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(ligne));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString())).thenReturn(false);
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(901L).build());
        when(demandeCreditRepository.save(any())).thenAnswer(inv -> {
            com.mini.credit.entity.credit.DemandeCredit d = inv.getArgument(0);
            d.setId(321L);
            return d;
        });
        when(ligneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());
        assertThat(response.getStatut()).isEqualTo(RecetteStatut.VALIDEE);
        verify(creditService, never()).enregistrerRemboursement(anyLong(), any());
        verify(operationEpargneService, never()).enregistrer(any());
        verify(demandeCreditRepository, times(1)).save(any());
        verify(collecteOperationGenereeRepository, atLeastOnce()).save(any());

        ArgumentCaptor<DemandeCredit> demandeCaptor = ArgumentCaptor.forClass(DemandeCredit.class);
        verify(demandeCreditRepository).save(demandeCaptor.capture());
        assertThat(demandeCaptor.getValue().getStatut()).isEqualTo(StatutDemandeCredit.SOUMISE);
    }

    @Test
    void demandeCreditRefuseSiChampsObligatoiresAbsents() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.DEMANDE_CREDIT)
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("montant demandé du crédit");
    }

    @Test
    void totauxSontCalculesParBackendEtIgnorentPayloadFrauduleux() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CollecteMembreLigne l1 = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.EPARGNE).montant(new BigDecimal("100")).quantite(1).build();
        CollecteMembreLigne l2 = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.REMBOURSEMENT_CREDIT).montant(new BigDecimal("50")).quantite(1).build();
        CollecteMembreLigne l3 = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.CARNET).montant(new BigDecimal("10")).quantite(2).build();
        CollecteMembreLigne l4 = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.FRAIS_ANALYSE).montant(new BigDecimal("5")).quantite(1).build();
        l1.setId(1001L); l2.setId(1002L); l3.setId(1003L); l4.setId(1004L);

        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(l1, l2, l3, l4));

        service.soumettre(500L, CreateCollecteTerrainRequest.builder().especesRemises(new BigDecimal("165")).totalGeneralCalcule(new BigDecimal("99999")).build());

        assertThat(collecte.getTotalEpargneCalcule()).isEqualByComparingTo("100");
        assertThat(collecte.getTotalRemboursementsCalcule()).isEqualByComparingTo("50");
        assertThat(collecte.getTotalFraisCalcule()).isEqualByComparingTo("15");
        assertThat(collecte.getTotalGeneralCalcule()).isEqualByComparingTo("165");
    }

    @Test
    void ecartExigeObservationAuMomentSoumission() {
        collecte.setEspecesRemises(new BigDecimal("50"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("100"))
            .build();
        ligne.setId(1L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(ligne));

        assertThatThrownBy(() -> service.soumettre(500L, CreateCollecteTerrainRequest.builder().especesRemises(new BigDecimal("50")).build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Observation obligatoire");
    }

    @Test
    void agentNeModifieQueBrouillon() {
        collecte.setStatut(RecetteStatut.SOUMISE);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("5"))
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Interdiction de modifier");
    }

    @Test
    void soumissionPossibleSiEcartZeroEtBloqueEditionEnsuite() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("100"))
            .build();
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(ligne));

        var submitted = service.soumettre(500L, CreateCollecteTerrainRequest.builder().especesRemises(new BigDecimal("100")).build());
        assertThat(submitted.getStatut()).isEqualTo(RecetteStatut.SOUMISE);

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("5"))
            .build();
        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Interdiction de modifier");
    }

    @Test
    void controleurValideEtGenereUneSeuleFois() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("10"));
        collecte.setOperationsGeneratedAt(null);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("10"))
            .quantite(1)
            .build();
        ligne.setId(1L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(ligne));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(java.time.LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);

        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE))
            .thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));

        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString()))
            .thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString()))
            .thenReturn(false);
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(900L).build());

        var first = service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());
        assertThat(first.getStatut()).isEqualTo(RecetteStatut.VALIDEE);
        assertThat(first.getOperationsGeneratedAt()).isNotNull();

        var second = service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());
        assertThat(second.getStatut()).isEqualTo(RecetteStatut.VALIDEE);

        verify(operationCaisseService, times(1)).enregistrer(any()); // une seule écriture caisse globale malgré revalidation
        verify(auditService, atLeastOnce()).logSuccess(
            eq(AuditAction.RECETTE_JOURNALIERE_VALIDATED),
            eq("CollecteJournaliereTerrain"),
            eq(500L),
            contains("Validation déjà effectuée")
        );
    }

    @Test
    void validationEpargneConserveReferenceEtLienSourceCollecte() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("100"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);
        CollecteMembreLigne epargne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("100"))
            .compteEpargne(compte)
            .build();
        epargne.setId(11L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(epargne));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString())).thenReturn(false);
        when(operationEpargneService.enregistrer(any())).thenReturn(OperationEpargneResponse.builder().id(1000L).typeOperation(TypeOperationEpargne.EPARGNE).build());
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(2000L).build());

        service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());

        ArgumentCaptor<OperationEpargneRequest> epargneCaptor = ArgumentCaptor.forClass(OperationEpargneRequest.class);
        verify(operationEpargneService, times(1)).enregistrer(epargneCaptor.capture());
        OperationEpargneRequest savedEpargne = epargneCaptor.getValue();
        assertThat(savedEpargne.getReferenceExterne()).isEqualTo("COLLECTE-500-LIGNE-11");
        assertThat(savedEpargne.getCreatedBy()).isEqualTo(2L);
        assertThat(savedEpargne.getObservation()).contains("Collecte validée #500");
        assertThat(savedEpargne.getSessionCaisseId()).isNull();

        ArgumentCaptor<OperationCaisseRequest> caisseCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService, times(1)).enregistrer(caisseCaptor.capture());
        OperationCaisseRequest caisseRequest = caisseCaptor.getValue();
        assertThat(caisseRequest.getSource()).isEqualTo(SourceOperationCaisse.RECETTE_JOURNALIERE);
        assertThat(caisseRequest.getRecetteId()).isEqualTo(500L);
        assertThat(caisseRequest.getMontant()).isEqualByComparingTo("100");

        ArgumentCaptor<CollecteOperationGeneree> traceCaptor = ArgumentCaptor.forClass(CollecteOperationGeneree.class);
        verify(collecteOperationGenereeRepository, atLeastOnce()).save(traceCaptor.capture());
        assertThat(traceCaptor.getAllValues())
            .anyMatch(trace -> "EPARGNE_DEPOT".equals(trace.getTypeOperation())
                && trace.getLigneCollecte() != null
                && Objects.equals(trace.getLigneCollecte().getId(), 11L)
                && Objects.equals(trace.getCreatedBy(), 2L));
    }

    @Test
    void validationCollecteEstRefuseeSiMembreHorsSiteDansGenerationEpargne() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("100"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);
        CollecteMembreLigne epargne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("100"))
            .compteEpargne(compte)
            .build();
        epargne.setId(11L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(epargne));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(operationEpargneService.enregistrer(any())).thenThrow(new RuntimeException("Accès refusé: membre hors site de la recette validée"));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("membre hors site");
    }

    @Test
    void validationCollecteEstRefuseeSiMontantIncoherentAvecLigneSource() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("100"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);
        CollecteMembreLigne epargne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("100"))
            .compteEpargne(compte)
            .build();
        epargne.setId(11L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(epargne));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(operationEpargneService.enregistrer(any())).thenThrow(new RuntimeException("Montant incohérent avec la ligne source de collecte"));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Montant incohérent");
    }

    @Test
    void chefBureauNePeutPasValider() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("chef01", null, List.of(new SimpleGrantedAuthority("ROLE_CHEF_BUREAU")))
        );
        Utilisateur chef = new Utilisateur();
        chef.setUsername("chef01");
        when(utilisateurRepository.findByUsername("chef01")).thenReturn(Optional.of(chef));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("ADMIN/CONTROLEUR");
    }

    @Test
    void collecteRejeteeNestPasValidable() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.REJETEE);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Seule une collecte SOUMISE peut être validée");
    }

    @Test
    void adminPeutValiderCollecteSoumise() {
        Utilisateur admin = new Utilisateur();
        admin.setId(9L);
        admin.setUsername("admin01");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin01", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        when(utilisateurRepository.findByUsername("admin01")).thenReturn(Optional.of(admin));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("10"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("10"))
            .quantite(1)
            .build();
        ligne.setId(1L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(ligne));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(admin)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString())).thenReturn(false);
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(901L).build());

        var response = service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());
        assertThat(response.getStatut()).isEqualTo(RecetteStatut.VALIDEE);
    }

    @Test
    void collecteBrouillonNestPasValidable() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.BROUILLON);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Seule une collecte SOUMISE peut être validée");
    }

    @Test
    void rejetCollecteSoumiseNeDeclenchePasLazyInitialization() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of());

        var response = service.rejeter(500L, ValidateCollecteTerrainRequest.builder().motifRejet("Incohérence terrain").build());
        assertThat(response.getStatut()).isEqualTo(RecetteStatut.REJETEE);
    }

    @Test
    void aucuneOperationOfficielleAvantValidation() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("10"))
            .build()));

        service.soumettre(500L, CreateCollecteTerrainRequest.builder().especesRemises(new BigDecimal("10")).build());

        verify(operationCaisseService, never()).enregistrer(any());
        verify(operationEpargneService, never()).enregistrer(any());
        verify(creditService, never()).enregistrerRemboursement(anyLong(), any());
    }

    @Test
    void validationGenereOperationEpargneEtRemboursementEtFrais() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("165"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);
        Credit credit = new Credit();
        credit.setId(400L);
        credit.setStatut(StatutCredit.EN_COURS);

        CollecteMembreLigne epargne = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.EPARGNE).montant(new BigDecimal("100")).compteEpargne(compte).build();
        epargne.setId(11L);
        CollecteMembreLigne remboursement = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.REMBOURSEMENT_CREDIT).montant(new BigDecimal("50")).credit(credit).build();
        remboursement.setId(12L);
        CollecteMembreLigne carnet = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.CARNET).montant(new BigDecimal("10")).build();
        carnet.setId(13L);
        CollecteMembreLigne frais = CollecteMembreLigne.builder().collecte(collecte).membre(membre).typeLigne(TypeLigneCollecte.FRAIS_ANALYSE).montant(new BigDecimal("5")).build();
        frais.setId(14L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(epargne, remboursement, carnet, frais));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder().caisse(caisse).utilisateur(ctrl).dateOuverture(LocalDateTime.now()).statut(StatutSessionCaisse.OUVERTE).build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString())).thenReturn(false);
        when(operationEpargneService.enregistrer(any())).thenReturn(OperationEpargneResponse.builder().id(1000L).typeOperation(TypeOperationEpargne.EPARGNE).build());
        com.mini.credit.entity.credit.RemboursementCredit remboursementCredit = new com.mini.credit.entity.credit.RemboursementCredit();
        remboursementCredit.setId(1500L);
        when(remboursementCreditRepository.findTopByCreditIdOrderByIdDesc(400L)).thenReturn(Optional.of(remboursementCredit));
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(2000L).build());

        service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());

        ArgumentCaptor<OperationEpargneRequest> epargneCaptor = ArgumentCaptor.forClass(OperationEpargneRequest.class);
        verify(operationEpargneService, times(1)).enregistrer(epargneCaptor.capture());
        assertThat(epargneCaptor.getValue().getSessionCaisseId()).isNull();

        ArgumentCaptor<RemboursementRequest> remboursementCaptor = ArgumentCaptor.forClass(RemboursementRequest.class);
        verify(creditService, times(1)).enregistrerRemboursement(eq(400L), remboursementCaptor.capture());
        assertThat(remboursementCaptor.getValue().getSessionCaisseId()).isNull();

        ArgumentCaptor<OperationCaisseRequest> caisseCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService, times(1)).enregistrer(caisseCaptor.capture());
        assertThat(caisseCaptor.getValue().getMontant()).isEqualByComparingTo("165");
        assertThat(caisseCaptor.getValue().getSource()).isEqualTo(SourceOperationCaisse.RECETTE_JOURNALIERE);
        assertThat(caisseCaptor.getValue().getRecetteId()).isEqualTo(500L);
        verify(collecteOperationGenereeRepository, atLeast(5)).save(any());
    }

    @Test
    void validationMixteEpargneCarnetGenereUneSeuleEntreeCaisseGlobale() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("51000"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);
        CollecteMembreLigne epargne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("50000"))
            .compteEpargne(compte)
            .build();
        epargne.setId(11L);
        CollecteMembreLigne carnet = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("1000"))
            .build();
        carnet.setId(12L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(epargne, carnet));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString())).thenReturn(false);
        when(operationEpargneService.enregistrer(any())).thenReturn(OperationEpargneResponse.builder().id(1000L).typeOperation(TypeOperationEpargne.EPARGNE).build());
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(2000L).build());

        service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());

        verify(operationEpargneService, times(1)).enregistrer(any());
        verify(operationCaisseService, times(1)).enregistrer(any());

        ArgumentCaptor<OperationCaisseRequest> caisseCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService).enregistrer(caisseCaptor.capture());
        assertThat(caisseCaptor.getValue().getMontant()).isEqualByComparingTo("51000");
        assertThat(caisseCaptor.getValue().getSource()).isEqualTo(SourceOperationCaisse.RECETTE_JOURNALIERE);
        assertThat(caisseCaptor.getValue().getRecetteId()).isEqualTo(500L);
    }

    @Test
    void validationEpargneSeuleGenereUneSeuleEntreeCaisseGlobale() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("50000"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);
        CollecteMembreLigne epargne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("50000"))
            .compteEpargne(compte)
            .build();
        epargne.setId(11L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(epargne));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString())).thenReturn(false);
        when(operationEpargneService.enregistrer(any())).thenReturn(OperationEpargneResponse.builder().id(1000L).typeOperation(TypeOperationEpargne.EPARGNE).build());
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(2000L).build());

        service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());

        ArgumentCaptor<OperationCaisseRequest> caisseCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService, times(1)).enregistrer(caisseCaptor.capture());
        assertThat(caisseCaptor.getValue().getMontant()).isEqualByComparingTo("50000");
    }

    @Test
    void validationCarnetSeulGenereUneSeuleEntreeCaisseGlobale() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        markBilletageConfirme(collecte, new BigDecimal("1000"));
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CollecteMembreLigne carnet = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("1000"))
            .build();
        carnet.setId(13L);
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(carnet));

        Caisse caisse = new Caisse();
        caisse.setId(55L);
        SessionCaisse session = SessionCaisse.builder()
            .caisse(caisse)
            .utilisateur(ctrl)
            .dateOuverture(LocalDateTime.now())
            .statut(StatutSessionCaisse.OUVERTE)
            .build();
        session.setId(77L);
        when(sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.findById(77L)).thenReturn(Optional.of(session));
        when(collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(anyLong(), anyLong(), anyString())).thenReturn(false);
        when(collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(anyLong(), anyString())).thenReturn(false);
        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(2000L).build());

        service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build());

        verify(operationEpargneService, never()).enregistrer(any());
        verify(creditService, never()).enregistrerRemboursement(anyLong(), any());

        ArgumentCaptor<OperationCaisseRequest> caisseCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService, times(1)).enregistrer(caisseCaptor.capture());
        assertThat(caisseCaptor.getValue().getMontant()).isEqualByComparingTo("1000");
        assertThat(caisseCaptor.getValue().getSource()).isEqualTo(SourceOperationCaisse.RECETTE_JOURNALIERE);
        assertThat(caisseCaptor.getValue().getRecetteId()).isEqualTo(500L);
    }

    @Test
    void controleurHorsAntenneNePeutPasValider() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence autreAgence = new Agence();
        autreAgence.setId(888L);
        employe.setAgence(autreAgence);
        ctrl.setEmploye(employe);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("hors périmètre antenne");
    }

    @Test
    void controleurNePeutPasValiderSansBilletageConfirme() {
        Utilisateur ctrl = new Utilisateur();
        ctrl.setId(2L);
        ctrl.setUsername("ctrl01");
        Employe employe = new Employe();
        Agence agenceCtrl = new Agence();
        agenceCtrl.setId(99L);
        employe.setAgence(agenceCtrl);
        ctrl.setEmploye(employe);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR")))
        );
        when(utilisateurRepository.findByUsername("ctrl01")).thenReturn(Optional.of(ctrl));

        collecte.setStatut(RecetteStatut.SOUMISE);
        collecte.setBilletageConfirme(false);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("billetage non confirmé");
    }

    @Test
    void caissierConfirmeBilletageDansSonAntenne() {
        Utilisateur cashier = new Utilisateur();
        cashier.setId(3L);
        cashier.setUsername("cash01");
        Role rCash = new Role();
        rCash.setCode(RoleCode.CAISSIER);
        cashier.setRole(rCash);
        Employe emp = new Employe();
        Agence ag = new Agence();
        ag.setId(99L);
        emp.setAgence(ag);
        cashier.setEmploye(emp);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("cash01", null, List.of(new SimpleGrantedAuthority("ROLE_CAISSIER")))
        );
        when(utilisateurRepository.findByUsername("cash01")).thenReturn(Optional.of(cashier));
        collecte.setStatut(RecetteStatut.SOUMISE);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("120"))
            .quantite(1)
            .build();
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(ligne));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.confirmerBilletage(500L, ConfirmerBilletageRequest.builder()
            .especesConfirmeesCaissier(new BigDecimal("125"))
            .observationBilletage("Billets recomptés")
            .build());

        assertThat(response.getBilletageConfirme()).isTrue();
        assertThat(response.getEspecesConfirmeesCaissier()).isEqualByComparingTo("125");
        assertThat(response.getEcartTresorerie()).isEqualByComparingTo("5");
    }

    @Test
    void agentNePeutPasConfirmerBilletage() {
        collecte.setStatut(RecetteStatut.SOUMISE);

        assertThatThrownBy(() -> service.confirmerBilletage(500L, ConfirmerBilletageRequest.builder()
            .especesConfirmeesCaissier(new BigDecimal("100"))
            .build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("CAISSIER");
    }

    @Test
    void membreHorsSiteRefuse() {
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        Site otherSite = new Site();
        otherSite.setId(20L);
        Membre otherMembre = new Membre();
        otherMembre.setId(999L);
        otherMembre.setSite(otherSite);
        when(membreRepository.findById(999L)).thenReturn(Optional.of(otherMembre));

        CreateCollecteMembreLigneRequest request = CreateCollecteMembreLigneRequest.builder()
            .membreId(999L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("5"))
            .build();

        assertThatThrownBy(() -> service.addLigne(500L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Membre hors site");
    }

    @Test
    void caissierNePeutPasValider() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("cash01", null, List.of(new SimpleGrantedAuthority("ROLE_CAISSIER")))
        );
        Utilisateur cashier = new Utilisateur();
        cashier.setUsername("cash01");
        when(utilisateurRepository.findByUsername("cash01")).thenReturn(Optional.of(cashier));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("ADMIN/CONTROLEUR");
    }

    @Test
    void gestionnaireNePeutPasValider() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("gest01", null, List.of(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE")))
        );
        Utilisateur u = new Utilisateur();
        u.setUsername("gest01");
        when(utilisateurRepository.findByUsername("gest01")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("ADMIN/CONTROLEUR");
    }

    @Test
    void agentTerrainNePeutPasValider() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("agent01", null, List.of(new SimpleGrantedAuthority("ROLE_AGENT_TERRAIN")))
        );

        assertThatThrownBy(() -> service.valider(500L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("ADMIN/CONTROLEUR");
    }

    @Test
    void agentPeutLireSaCollecteBrouillonAncienne() {
        collecte.setDateCollecte(LocalDate.now().minusDays(1));
        collecte.setStatut(RecetteStatut.BROUILLON);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of());

        var response = service.getById(500L);

        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getDateCollecte()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(response.getStatut()).isEqualTo(RecetteStatut.BROUILLON);
    }

    @Test
    void agentNePeutPasLireCollecteAutreAgent() {
        AgentTerrain autreAgent = new AgentTerrain();
        autreAgent.setId(999L);
        CollecteJournaliereTerrain autreCollecte = CollecteJournaliereTerrain.builder()
            .agentTerrain(autreAgent)
            .site(site)
            .antenneId(99L)
            .dateCollecte(LocalDate.now().minusDays(1))
            .statut(RecetteStatut.BROUILLON)
            .build();
        autreCollecte.setId(900L);

        when(collecteRepository.findById(900L)).thenReturn(Optional.of(autreCollecte));

        assertThatThrownBy(() -> service.getById(900L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Accès refusé");
    }

    @Test
    void agentPeutModifierCollecteBrouillonAncienne() {
        collecte.setDateCollecte(LocalDate.now().minusDays(1));
        collecte.setStatut(RecetteStatut.BROUILLON);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(membreRepository.findById(200L)).thenReturn(Optional.of(membre));

        CompteEpargne compte = new CompteEpargne();
        compte.setId(300L);
        compte.setStatut(StatutCompte.ACTIF);
        when(compteEpargneRepository.findFirstByMembreIdAndStatut(200L, StatutCompte.ACTIF)).thenReturn(Optional.of(compte));
        when(ligneRepository.save(any())).thenAnswer(inv -> {
            CollecteMembreLigne l = inv.getArgument(0);
            l.setId(777L);
            return l;
        });
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of());
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.addLigne(500L, CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("100"))
            .build());

        assertThat(response.getId()).isEqualTo(777L);
    }

    @Test
    void agentNePeutPasModifierCollecteValidee() {
        collecte.setStatut(RecetteStatut.VALIDEE);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> service.addLigne(500L, CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Interdiction de modifier");
    }

    @Test
    void agentNePeutPasModifierCollecteAutreAgent() {
        AgentTerrain autreAgent = new AgentTerrain();
        autreAgent.setId(999L);
        CollecteJournaliereTerrain autreCollecte = CollecteJournaliereTerrain.builder()
            .agentTerrain(autreAgent)
            .site(site)
            .antenneId(99L)
            .dateCollecte(LocalDate.now().minusDays(1))
            .statut(RecetteStatut.BROUILLON)
            .build();
        autreCollecte.setId(901L);
        when(collecteRepository.findById(901L)).thenReturn(Optional.of(autreCollecte));

        assertThatThrownBy(() -> service.addLigne(901L, CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .build()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Accès refusé");
    }

    @Test
    void soumissionAncienneCollecteBrouillonFonctionneSiComplete() {
        collecte.setDateCollecte(LocalDate.now().minusDays(1));
        collecte.setStatut(RecetteStatut.BROUILLON);
        when(collecteRepository.findById(500L)).thenReturn(Optional.of(collecte));
        when(collecteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(TypeLigneCollecte.EPARGNE)
            .montant(new BigDecimal("100"))
            .build();
        when(ligneRepository.findByCollecteId(500L)).thenReturn(List.of(ligne));

        var submitted = service.soumettre(500L, CreateCollecteTerrainRequest.builder()
            .especesRemises(new BigDecimal("100"))
            .observations("RAS")
            .build());

        assertThat(submitted.getStatut()).isEqualTo(RecetteStatut.SOUMISE);
    }

    @Test
    void listingScopeParRole() {
        CollecteJournaliereTerrain cMine = CollecteJournaliereTerrain.builder().agentTerrain(agent).site(site).antenneId(99L).dateCollecte(LocalDate.now()).statut(RecetteStatut.SOUMISE).build();
        cMine.setId(1L);
        AgentTerrain autreAgent = new AgentTerrain();
        autreAgent.setId(999L);
        CollecteJournaliereTerrain cOther = CollecteJournaliereTerrain.builder().agentTerrain(autreAgent).site(site).antenneId(99L).dateCollecte(LocalDate.now()).statut(RecetteStatut.SOUMISE).build();
        cOther.setId(2L);
        when(collecteRepository.findAll()).thenReturn(List.of(cMine, cOther));
        when(ligneRepository.findByCollecteId(anyLong())).thenReturn(List.of());

        Page<?> pageAgent = service.list(null, null, null, null, null, null, 0, 10);
        assertThat(pageAgent.getTotalElements()).isEqualTo(1);

        Utilisateur cashier = new Utilisateur();
        cashier.setId(3L);
        cashier.setUsername("cash01");
        Role rCash = new Role();
        rCash.setCode(RoleCode.CAISSIER);
        cashier.setRole(rCash);
        Employe emp = new Employe();
        Agence ag = new Agence();
        ag.setId(99L);
        emp.setAgence(ag);
        cashier.setEmploye(emp);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("cash01", null, List.of(new SimpleGrantedAuthority("ROLE_CAISSIER")))
        );
        when(utilisateurRepository.findByUsername("cash01")).thenReturn(Optional.of(cashier));

        Page<?> pageCash = service.list(RecetteStatut.SOUMISE, null, null, null, null, null, 0, 10);
        assertThat(pageCash.getTotalElements()).isEqualTo(2);
    }

    @Test
    void agentTerrainNePeutPasElargirPerimetreAvecFiltres() {
        CollecteJournaliereTerrain cMine = CollecteJournaliereTerrain.builder()
            .agentTerrain(agent)
            .site(site)
            .antenneId(99L)
            .dateCollecte(LocalDate.now())
            .statut(RecetteStatut.SOUMISE)
            .build();
        cMine.setId(11L);

        AgentTerrain autreAgent = new AgentTerrain();
        autreAgent.setId(555L);
        Site autreSite = new Site();
        autreSite.setId(777L);
        CollecteJournaliereTerrain cOther = CollecteJournaliereTerrain.builder()
            .agentTerrain(autreAgent)
            .site(autreSite)
            .antenneId(100L)
            .dateCollecte(LocalDate.now())
            .statut(RecetteStatut.SOUMISE)
            .build();
        cOther.setId(12L);

        when(collecteRepository.findAll()).thenReturn(List.of(cMine, cOther));

        // Sans filtre frauduleux, l'agent voit uniquement sa collecte.
        Page<?> pageSansFiltreFrauduleux = service.list(RecetteStatut.SOUMISE, null, null, null, null, null, 0, 10);
        assertThat(pageSansFiltreFrauduleux.getTotalElements()).isEqualTo(1);

        // Filtre frauduleux (autre agent/site/antenne) ne doit jamais élargir le périmètre.
        Page<?> pageAgent = service.list(RecetteStatut.SOUMISE, null, null, 555L, 777L, 100L, 0, 10);
        assertThat(pageAgent.getTotalElements()).isEqualTo(0);

        verify(collecteRepository, times(2)).findAll();
    }
}
