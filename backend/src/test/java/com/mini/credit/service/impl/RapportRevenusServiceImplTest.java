package com.mini.credit.service.impl;

import com.mini.credit.dto.rapport.revenus.RapportRevenusResponse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.TransportSiteParametre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DepenseCaisseCategorie;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.RemboursementApportProprietaireRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.TransportSiteParametreRepository;
import com.mini.credit.service.ParametreMetierService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RapportRevenusServiceImplTest {

    @Mock private OperationCaisseRepository operationCaisseRepository;
    @Mock private RecetteJournaliereTerrainRepository recetteJournaliereTerrainRepository;
    @Mock private DepenseCaisseRepository depenseCaisseRepository;
    @Mock private RemboursementCreditRepository remboursementCreditRepository;
    @Mock private CollecteMembreLigneRepository collecteMembreLigneRepository;
    @Mock private CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;
    @Mock private CreditRepository creditRepository;
    @Mock private DemandeCreditRepository demandeCreditRepository;
    @Mock private RemboursementApportProprietaireRepository remboursementApportRepository;
    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;
    @Mock private CompteEpargneRepository compteEpargneRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private AgentTerrainRepository agentTerrainRepository;
    @Mock private TransportSiteParametreRepository transportSiteParametreRepository;
    @Mock private AgenceRepository agenceRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private ParametreMetierService parametreMetierService;

    @InjectMocks private RapportRevenusServiceImpl service;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Agence agence;
    private Site site;
    private Membre membre;
    private Employe agentTerrainEmploye;
    private AgentTerrain agentTerrain;

    @BeforeEach
    void setUp() {
        dateDebut = LocalDate.of(2026, 7, 24);
        dateFin = dateDebut;

        Role adminRole = Role.builder().code(RoleCode.ADMIN).libelle("Admin").build();
        Utilisateur admin = Utilisateur.builder()
            .username("admin")
            .nomComplet("Admin")
            .motDePasseHash("hash")
            .role(adminRole)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        agence = Agence.builder().codeAgence("ANT-01").nomAgence("Antenne 01").actif(true).build();
        agence.setId(10L);
        site = Site.builder().codeSite("SITE-01").nomSite("Site 01").zone("Zone 01").agence(agence).actif(true).build();
        site.setId(20L);
        membre = Membre.builder()
            .codeMembre("MB-01")
            .nom("Membre")
            .nomComplet("Membre Test")
            .site(site)
            .dateAdhesion(dateDebut)
            .build();
        membre.setId(30L);

        agentTerrainEmploye = agentTerrainEmploye(501L, 77L);
        agentTerrain = agentTerrain(agentTerrainEmploye.getUtilisateur(), 601L);

        stubEmptyReportDependencies();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRapportRevenus_shouldOnlyCountValidatedFinancialRevenueLines() {
        CollecteMembreLigne brouillonCarnet = collecteLine(RecetteStatut.BROUILLON, TypeLigneCollecte.CARNET, "BROUILLON-CARNET", "7000");
        CollecteMembreLigne soumiseCarnet = collecteLine(RecetteStatut.SOUMISE, TypeLigneCollecte.CARNET, "SOUMISE-CARNET", "8000");
        CollecteMembreLigne rejeteeCarnet = collecteLine(RecetteStatut.REJETEE, TypeLigneCollecte.CARNET, "REJETEE-CARNET", "9000");
        CollecteMembreLigne valideeCarnet = collecteLine(RecetteStatut.VALIDEE, TypeLigneCollecte.CARNET, "VALIDEE-CARNET", "5000");
        CollecteMembreLigne epargneValidee = collecteLine(RecetteStatut.VALIDEE, TypeLigneCollecte.EPARGNE, "VALIDEE-EPARGNE", "40000");
        CollecteMembreLigne principalValide = collecteLine(RecetteStatut.VALIDEE, TypeLigneCollecte.REMBOURSEMENT_CREDIT, "VALIDEE-PRINCIPAL", "30000");
        RemboursementCredit remboursementAvecInteret = remboursementCredit("RC-INTERET", "20000", "2500", "0");
        RemboursementCredit remboursementPrincipalSeul = remboursementCredit("RC-PRINCIPAL", "30000", "0", "0");

        when(collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(
            eq(TypeLigneCollecte.CARNET), eq(dateDebut), eq(dateFin), eq(null)
        )).thenReturn(List.of(valideeCarnet));
        when(collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(
            eq(TypeLigneCollecte.EPARGNE), eq(dateDebut), eq(dateFin), eq(null)
        )).thenReturn(List.of(epargneValidee));
        when(collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(
            eq(TypeLigneCollecte.FRAIS_ANALYSE), eq(dateDebut), eq(dateFin), eq(null)
        )).thenReturn(List.of());
        when(remboursementCreditRepository.findRevenueRemboursements(
            eq(dateDebut.atStartOfDay()), eq(dateFin.atTime(java.time.LocalTime.MAX)), eq(null)
        )).thenReturn(List.of(remboursementAvecInteret));
        when(remboursementCreditRepository.findPrincipalRemboursements(
            eq(dateDebut.atStartOfDay()), eq(dateFin.atTime(java.time.LocalTime.MAX)), eq(null)
        )).thenReturn(List.of(remboursementPrincipalSeul));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getDetails())
            .extracting(detail -> detail.getReference())
            .containsExactlyInAnyOrder("VALIDEE-CARNET", "RC-INTERET")
            .doesNotContain(
                brouillonCarnet.getReference(),
                soumiseCarnet.getReference(),
                rejeteeCarnet.getReference(),
                epargneValidee.getReference(),
                principalValide.getReference()
            );
        assertThat(response.getKpis().getCarnetsVendus()).isEqualByComparingTo("5000");
        assertThat(response.getKpis().getInteretsCredit()).isEqualByComparingTo("2500");
        assertThat(response.getTotalRevenus()).isEqualByComparingTo("7500");
        assertThat(response.getTotalMouvementsNonRevenus()).isEqualByComparingTo("70000");
        assertThat(response.getDetails())
            .extracting(detail -> detail.getCategorie())
            .containsExactlyInAnyOrder("CARNETS_VENDUS", "INTERETS_CREDIT");
        assertThat(response.getMouvementsNonRevenus())
            .extracting(mouvement -> mouvement.getMontant())
            .contains(new BigDecimal("40000"), new BigDecimal("30000"));
    }

    @Test
    void getRapportRevenus_shouldCalculateAgentTerrainPrimesFromValidatedCollectesOnly() {
        Utilisateur utilisateurAgent = Utilisateur.builder()
            .username("agent.terrain")
            .nomComplet("Agent Terrain")
            .motDePasseHash("hash")
            .build();
        utilisateurAgent.setId(77L);
        Employe employe = Employe.builder()
            .matricule("AT-001")
            .nom("Agent")
            .prenom("Terrain")
            .nomComplet("Agent Terrain")
            .fonction(PosteEmploye.AGENT_TERRAIN)
            .dateEmbauche(dateDebut.minusMonths(1))
            .salaireBase(new BigDecimal("75000"))
            .primeFixe(BigDecimal.ZERO)
            .bonusVariable(BigDecimal.ZERO)
            .actif(true)
            .agence(agence)
            .site(site)
            .utilisateur(utilisateurAgent)
            .build();
        employe.setId(501L);
        AgentTerrain agentTerrain = AgentTerrain.builder()
            .utilisateur(utilisateurAgent)
            .matricule("AT-001")
            .site(site)
            .actif(true)
            .build();
        agentTerrain.setId(601L);

        when(employeRepository.findByActifTrue()).thenReturn(List.of(employe));
        when(agentTerrainRepository.findByUtilisateurId(77L)).thenReturn(Optional.of(agentTerrain));
        when(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
            601L, TypeLigneCollecte.EPARGNE, dateDebut, dateFin
        )).thenReturn(new BigDecimal("240000"));
        when(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
            601L, TypeLigneCollecte.REMBOURSEMENT_CREDIT, dateDebut, dateFin
        )).thenReturn(new BigDecimal("125000"));
        when(collecteMembreLigneRepository.sumValidatedCarnetsByAgentAndPeriod(601L, dateDebut, dateFin)).thenReturn(3L);

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getMasseSalarialeMensuellePrevue()).isEqualByComparingTo("80500.00");
        assertThat(response.getMasseSalariale().getSalairesRestantAPayer()).isEqualByComparingTo("80500.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes()).hasSize(1);
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalEpargneCollecteeValidee()).isEqualByComparingTo("240000.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getPrimeEpargne()).isEqualByComparingTo("2400.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalRemboursementCollecteValide()).isEqualByComparingTo("125000.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getPrimeRemboursement()).isEqualByComparingTo("2500.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getNombreCarnetsVendus()).isEqualTo(3);
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getBonusCarnets()).isEqualByComparingTo("600.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalPrimesAgentTerrain()).isEqualByComparingTo("5500.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getRemunerationAttendueTotale()).isEqualByComparingTo("80500.00");
        verify(collecteMembreLigneRepository, never()).sumByTypeAndPeriod(any(), any(), any(), any());
    }

    @Test
    void primeEpargne1Pourcent() {
        stubAgentTerrainPayroll("240000", "0", 0L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalEpargneCollecteeValidee()).isEqualByComparingTo("240000.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getPrimeEpargne()).isEqualByComparingTo("2400.00");
    }

    @Test
    void primeRemboursement2Pourcent() {
        stubAgentTerrainPayroll("0", "125000", 0L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalRemboursementCollecteValide()).isEqualByComparingTo("125000.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getPrimeRemboursement()).isEqualByComparingTo("2500.00");
    }

    @Test
    void bonusCarnet200ParCarnet() {
        stubAgentTerrainPayroll("0", "0", 3L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getNombreCarnetsVendus()).isEqualTo(3);
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getBonusCarnets()).isEqualByComparingTo("600.00");
    }

    @Test
    void remunerationAttendueAgentTerrainInclutSalaireEtPrimes() {
        stubAgentTerrainPayroll("240000", "125000", 3L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getSalaireBase()).isEqualByComparingTo("75000.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalPrimesAgentTerrain()).isEqualByComparingTo("5500.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getRemunerationAttendueTotale()).isEqualByComparingTo("80500.00");
    }

    @Test
    void collectesNonValideesExclues() {
        stubAgentTerrainPayroll("0", "0", 0L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalPrimesAgentTerrain()).isZero();
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getRemunerationAttendueTotale()).isEqualByComparingTo("75000.00");
        verify(collecteMembreLigneRepository).sumValidatedAmountByAgentAndTypeAndPeriod(601L, TypeLigneCollecte.EPARGNE, dateDebut, dateFin);
        verify(collecteMembreLigneRepository).sumValidatedAmountByAgentAndTypeAndPeriod(601L, TypeLigneCollecte.REMBOURSEMENT_CREDIT, dateDebut, dateFin);
        verify(collecteMembreLigneRepository).sumValidatedCarnetsByAgentAndPeriod(601L, dateDebut, dateFin);
    }

    @Test
    void autreAgentExclu() {
        stubAgentTerrainPayroll("0", "0", 0L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalPrimesAgentTerrain()).isZero();
        verify(collecteMembreLigneRepository).sumValidatedAmountByAgentAndTypeAndPeriod(eq(601L), eq(TypeLigneCollecte.EPARGNE), eq(dateDebut), eq(dateFin));
        verify(collecteMembreLigneRepository, never()).sumValidatedAmountByAgentAndTypeAndPeriod(eq(602L), any(), any(), any());
        verify(collecteMembreLigneRepository, never()).sumValidatedCarnetsByAgentAndPeriod(eq(602L), any(), any());
    }

    @Test
    void horsPeriodeExclu() {
        stubAgentTerrainPayroll("0", "0", 0L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalPrimesAgentTerrain()).isZero();
        verify(collecteMembreLigneRepository).sumValidatedAmountByAgentAndTypeAndPeriod(601L, TypeLigneCollecte.EPARGNE, dateDebut, dateFin);
        verify(collecteMembreLigneRepository).sumValidatedAmountByAgentAndTypeAndPeriod(601L, TypeLigneCollecte.REMBOURSEMENT_CREDIT, dateDebut, dateFin);
        verify(collecteMembreLigneRepository).sumValidatedCarnetsByAgentAndPeriod(601L, dateDebut, dateFin);
    }

    @Test
    void transportExcluDeLaRemuneration() {
        DepenseCaisse transport = depense(DepenseCaisseCategorie.TRANSPORT, "30000");
        stubAgentTerrainPayroll("240000", "125000", 3L, List.of());
        when(depenseCaisseRepository.findTransportPaymentsForPeriod(any(), any(), any(), eq(null))).thenReturn(List.of(transport));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getRemunerationAttendueTotale()).isEqualByComparingTo("80500.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalPrimesAgentTerrain()).isEqualByComparingTo("5500.00");
        assertThat(response.getTransportFixePrevu().getTotalTransportPaye()).isZero();
    }

    @Test
    void transportPrevuCalculeParSiteAgentTerrainEtJoursCalendaires() {
        dateDebut = LocalDate.of(2026, 7, 1);
        dateFin = LocalDate.of(2026, 7, 10);
        site.setNomSite("SACOMBI");
        Employe agent1 = agentTerrainEmploye(501L, 77L);
        Employe agent2 = agentTerrainEmploye(502L, 78L);
        stubTransportTerrain(List.of(agent1, agent2), List.of(transportParametre(site, "3000")), List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTransportFixePrevu().getNombreJoursPeriode()).isEqualTo(10);
        assertThat(response.getTransportFixePrevu().getTotalTransportPrevu()).isEqualByComparingTo("60000.00");
        assertThat(response.getTransportFixePrevu().getDetailsParSite().get(0).getSiteNom()).isEqualTo("SACOMBI");
        assertThat(response.getTransportFixePrevu().getDetailsParSite().get(0).getNombreAgentsTerrainActifs()).isEqualTo(2);
    }

    @Test
    void transportPayeSoustraitDuTransportPrevu() {
        dateDebut = LocalDate.of(2026, 7, 1);
        dateFin = LocalDate.of(2026, 7, 10);
        Employe agent1 = agentTerrainEmploye(501L, 77L);
        Employe agent2 = agentTerrainEmploye(502L, 78L);
        DepenseCaisse transportPaye = transportPaye(agent1, "20000");
        stubTransportTerrain(List.of(agent1, agent2), List.of(transportParametre(site, "3000")), List.of(transportPaye));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTransportFixePrevu().getTotalTransportPrevu()).isEqualByComparingTo("60000.00");
        assertThat(response.getTransportFixePrevu().getTotalTransportPaye()).isEqualByComparingTo("20000.00");
        assertThat(response.getTransportFixePrevu().getTotalTransportRestant()).isEqualByComparingTo("40000.00");
    }

    @Test
    void transportRestantReduitEstimationRecuperable() {
        dateDebut = LocalDate.of(2026, 7, 1);
        dateFin = LocalDate.of(2026, 7, 10);
        Employe agent1 = agentTerrainEmploye(501L, 77L);
        Employe agent2 = agentTerrainEmploye(502L, 78L);
        stubTransportTerrain(List.of(agent1, agent2), List.of(transportParametre(site, "3000")), List.of(transportPaye(agent1, "20000")));
        when(sessionCaisseRepository.sumSoldeTheoriqueByStatutsAndAgence(anyList(), eq(null))).thenReturn(new BigDecimal("500000"));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTresorerieDisponible().getTransportRestantAPayer()).isEqualByComparingTo("40000.00");
        assertThat(response.getTresorerieDisponible().getTotalEngagementsCourtTerme()).isEqualByComparingTo("40000.00");
        assertThat(response.getTresorerieDisponible().getTresorerieRecuperablePrudente()).isEqualByComparingTo("460000.00");
    }

    @Test
    void transportPrevuExcluSalaireEtPrimes() {
        dateDebut = LocalDate.of(2026, 7, 1);
        dateFin = LocalDate.of(2026, 7, 10);
        stubAgentTerrainPayroll("240000", "125000", 3L, List.of());
        stubTransportTerrain(List.of(agentTerrainEmploye), List.of(transportParametre(site, "3000")), List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTransportFixePrevu().getTotalTransportPrevu()).isEqualByComparingTo("30000.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getRemunerationAttendueTotale()).isEqualByComparingTo("80500.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getTotalPrimesAgentTerrain()).isEqualByComparingTo("5500.00");
    }

    @Test
    void siteAvecAgentTerrainSansParametreTransportDeclencheAlerte() {
        Employe agent = agentTerrainEmploye(501L, 77L);
        stubTransportTerrain(List.of(agent), List.of(), List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTransportFixePrevu().getTotalTransportPrevu()).isZero();
        assertThat(response.getTransportFixePrevu().getNombreSitesSansMontant()).isEqualTo(1);
        assertThat(response.getControlesCoherence())
            .extracting(controle -> controle.getMessage())
            .anySatisfy(message -> assertThat(message).contains("Transport Agent Terrain non configuré pour le site"));
    }

    @Test
    void agentTerrainSansSiteDeclencheAlerteEtTransportNonCalcule() {
        Employe agentSansSite = agentTerrainEmploye(501L, 77L);
        agentSansSite.setSite(null);
        stubTransportTerrain(List.of(agentSansSite), List.of(), List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTransportFixePrevu().getTotalTransportPrevu()).isZero();
        assertThat(response.getTransportFixePrevu().getDetails()).isEmpty();
        assertThat(response.getControlesCoherence())
            .extracting(controle -> controle.getMessage())
            .contains("Agent Terrain sans site : transport non calculable.");
    }

    @Test
    void employeNonAgentTerrainSurMemeSiteExcluDuTransportPrevu() {
        dateDebut = LocalDate.of(2026, 7, 1);
        dateFin = LocalDate.of(2026, 7, 10);
        Employe agent = agentTerrainEmploye(501L, 77L);
        Employe gestionnaire = agentTerrainEmploye(601L, 88L);
        gestionnaire.setFonction(PosteEmploye.GESTIONNAIRE);
        stubTransportTerrain(List.of(agent), List.of(transportParametre(site, "3000")), List.of());
        when(employeRepository.findByActifTrue()).thenReturn(List.of(agent, gestionnaire));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTransportFixePrevu().getNombreAgentsTerrain()).isEqualTo(1);
        assertThat(response.getTransportFixePrevu().getTotalTransportPrevu()).isEqualByComparingTo("30000.00");
        verify(employeRepository, org.mockito.Mockito.atLeastOnce()).findByFonctionAndActifTrue(PosteEmploye.AGENT_TERRAIN);
    }

    @Test
    void employesNonTerrainSansSiteNeDeclenchentPasAlerteTransport() {
        Employe gestionnaire = agentTerrainEmploye(601L, 88L);
        gestionnaire.setFonction(PosteEmploye.GESTIONNAIRE);
        gestionnaire.setSite(null);
        Employe caissier = agentTerrainEmploye(602L, 89L);
        caissier.setFonction(PosteEmploye.CAISSIER);
        caissier.setSite(null);
        Employe controleur = agentTerrainEmploye(603L, 90L);
        controleur.setFonction(PosteEmploye.CONTROLEUR);
        controleur.setSite(null);

        stubTransportTerrain(List.of(), List.of(), List.of());
        when(employeRepository.findByActifTrue()).thenReturn(List.of(gestionnaire, caissier, controleur));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTransportFixePrevu().getTotalTransportPrevu()).isZero();
        assertThat(response.getTransportFixePrevu().getNombreAgentsTerrain()).isZero();
        assertThat(response.getControlesCoherence())
            .extracting(controle -> controle.getMessage())
            .doesNotContain("Agent Terrain sans site : transport non calculable.");
    }

    @Test
    void paiementPartielSalaireConserveReste() {
        DepenseCaisse salairePartiel = depense(DepenseCaisseCategorie.SALAIRE, "50000");
        salairePartiel.setEmploye(agentTerrainEmploye);
        salairePartiel.setPeriodePaie("2026-07");
        stubAgentTerrainPayroll("240000", "125000", 3L, List.of(salairePartiel));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getMontantPaye()).isEqualByComparingTo("50000.00");
        assertThat(response.getMasseSalariale().getDetailsEmployes().get(0).getResteAPayer()).isEqualByComparingTo("30500.00");
        assertThat(response.getMasseSalariale().getSalairesRestantAPayer()).isEqualByComparingTo("30500.00");
    }

    @Test
    void primesTerrainSontChargesSalarialesPasRevenus() {
        stubAgentTerrainPayroll("240000", "125000", 3L, List.of());

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTotalRevenus()).isZero();
        assertThat(response.getKpis().getFraisAnalyseCredit()).isZero();
        assertThat(response.getKpis().getFraisRetraitEpargne()).isZero();
        assertThat(response.getKpis().getInteretsCredit()).isZero();
        assertThat(response.getKpis().getPenalitesCredit()).isZero();
        assertThat(response.getMasseSalariale().getMasseSalarialeMensuellePrevue()).isEqualByComparingTo("80500.00");
        assertThat(response.getResultatPrevisionnelApresSalairesAPayer()).isEqualByComparingTo("-80500.00");
    }

    @Test
    void getRapportRevenus_shouldClassifyCaisseFeesAndCreditInterestPenaltiesAsRevenueOnly() {
        OperationCaisse fraisDemande = operationCaisse(CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT, SourceOperationCaisse.MANUEL, "FRAIS-DEM-1", "1500");
        OperationCaisse fraisRetrait = operationCaisse(CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE, SourceOperationCaisse.RETRAIT_EPARGNE, "FRAIS-RET-1", "300");
        RemboursementCredit remboursement = remboursementCredit("RC-REVENU", "20000", "2500", "750");

        when(operationCaisseRepository.findRevenueOperations(any(), any(), eq(null), anyList()))
            .thenReturn(List.of(fraisDemande, fraisRetrait));
        when(remboursementCreditRepository.findRevenueRemboursements(any(), any(), eq(null))).thenReturn(List.of(remboursement));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getKpis().getFraisAnalyseCredit()).isEqualByComparingTo("1500");
        assertThat(response.getKpis().getFraisRetraitEpargne()).isEqualByComparingTo("300");
        assertThat(response.getKpis().getInteretsCredit()).isEqualByComparingTo("2500");
        assertThat(response.getKpis().getPenalitesCredit()).isEqualByComparingTo("750");
        assertThat(response.getTotalRevenus()).isEqualByComparingTo("5050");
        assertThat(response.getDetails())
            .extracting(detail -> detail.getCategorie())
            .containsExactlyInAnyOrder("FRAIS_ANALYSE_CREDIT", "FRAIS_RETRAIT_EPARGNE", "INTERETS_CREDIT", "PENALITES_CREDIT");
        assertThat(response.getDetails())
            .extracting(detail -> detail.getReference())
            .containsExactlyInAnyOrder("FRAIS-DEM-1", "FRAIS-RET-1", "RC-REVENU", "RC-REVENU");
    }

    @Test
    void rapportRevenusInclutCommissionsRetraitEtExclutPrincipalRetrait() {
        OperationCaisse commissionRetrait1 = operationCaisse(CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE, SourceOperationCaisse.RETRAIT_EPARGNE, "RET-001-FRAIS", "900");
        OperationCaisse commissionRetrait2 = operationCaisse(CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE, SourceOperationCaisse.RETRAIT_EPARGNE, "RET-002-FRAIS", "1200");
        OperationCaisse principalRetrait = operationCaisse(CategorieOperationCaisse.RETRAIT_EPARGNE, SourceOperationCaisse.RETRAIT_EPARGNE, "RET-001", "10000");

        when(operationCaisseRepository.findRevenueOperations(any(), any(), eq(null), anyList()))
            .thenReturn(List.of(commissionRetrait1, commissionRetrait2));
        when(operationCaisseRepository.findAccountingMovementOperations(any(), any(), eq(null), argThat(categories ->
            categories.contains(CategorieOperationCaisse.RETRAIT_EPARGNE)
        ))).thenReturn(List.of(principalRetrait));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getKpis().getFraisRetraitEpargne()).isEqualByComparingTo("2100");
        assertThat(response.getTotalRevenus()).isEqualByComparingTo("2100");
        assertThat(response.getDetails())
            .extracting(detail -> detail.getReference())
            .containsExactlyInAnyOrder("RET-001-FRAIS", "RET-002-FRAIS")
            .doesNotContain("RET-001");
        assertThat(response.getMouvementsNonRevenus())
            .anySatisfy(mouvement -> {
                assertThat(mouvement.getSousCategorie()).isEqualTo("Sorties non charges - Retraits épargne");
                assertThat(mouvement.getMontant()).isEqualByComparingTo("10000");
            });
    }

    @Test
    void getRapportRevenus_shouldClassifySavingsPrincipalWithdrawalsAndDisbursementsAsNonRevenueMovements() {
        CollecteMembreLigne epargneValidee = collecteLine(RecetteStatut.VALIDEE, TypeLigneCollecte.EPARGNE, "EPARGNE-VALIDEE", "40000");
        RemboursementCredit principalRembourse = remboursementCredit("RC-PRINCIPAL", "20000", "0", "0");
        OperationCaisse retraitEpargne = operationCaisse(CategorieOperationCaisse.RETRAIT_EPARGNE, SourceOperationCaisse.RETRAIT_EPARGNE, "RET-EP-1", "25000");
        OperationCaisse decaissementCredit = operationCaisse(CategorieOperationCaisse.DECAISSEMENT_CREDIT, SourceOperationCaisse.CREDIT_DECAISSEMENT, "DEC-CR-1", "50000");

        when(collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(
            eq(TypeLigneCollecte.EPARGNE), eq(dateDebut), eq(dateFin), eq(null)
        )).thenReturn(List.of(epargneValidee));
        when(remboursementCreditRepository.findPrincipalRemboursements(any(), any(), eq(null))).thenReturn(List.of(principalRembourse));
        when(operationCaisseRepository.findAccountingMovementOperations(any(), any(), eq(null), argThat(categories ->
            categories.contains(CategorieOperationCaisse.RETRAIT_EPARGNE)
                && categories.contains(CategorieOperationCaisse.DECAISSEMENT_CREDIT)
        ))).thenReturn(List.of(retraitEpargne, decaissementCredit));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTotalRevenus()).isZero();
        assertThat(response.getTotalCharges()).isZero();
        assertThat(response.getTotalMouvementsNonRevenus()).isEqualByComparingTo("135000");
        assertThat(response.getMouvementsNonRevenus())
            .extracting(detail -> detail.getSousCategorie())
            .containsExactlyInAnyOrder(
                "Entrées non revenus - Épargne collectée",
                "Entrées non revenus - Principal crédit remboursé",
                "Sorties non charges - Retraits épargne",
                "Sorties non charges - Décaissements crédit"
            );
        assertThat(response.getDetails()).isEmpty();
    }

    @Test
    void getRapportRevenus_shouldClassifyPaidCashExpensesAsChargesByDomain() {
        DepenseCaisse salaire = depense(DepenseCaisseCategorie.SALAIRE, "75000");
        DepenseCaisse transport = depense(DepenseCaisseCategorie.TRANSPORT, "5000");
        DepenseCaisse fourniture = depense(DepenseCaisseCategorie.FOURNITURE_BUREAU, "12000");

        when(depenseCaisseRepository.sumDepensesPayeesByPeriodAndAgence(any(), any(), eq(null))).thenReturn(new BigDecimal("92000"));
        when(depenseCaisseRepository.findDepensesPayeesWithContext(any(), any(), eq(null))).thenReturn(List.of(salaire, transport, fourniture));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getTotalCharges()).isEqualByComparingTo("92000");
        assertThat(response.getBeneficeNetEstime()).isEqualByComparingTo("-92000");
        assertThat(response.getChargesParCategorie())
            .extracting(detail -> detail.getCategorie())
            .containsExactlyInAnyOrder("Salaire / prime / commission", "Transport", "Fonctionnement");
        assertThat(response.getChargeDetails())
            .extracting(detail -> detail.getCategorieTechnique())
            .containsExactlyInAnyOrder("SALAIRE", "TRANSPORT", "FOURNITURE_BUREAU");
    }

    @Test
    void comparaisonAgencesClasseRevenusDansBonneAgence() {
        OperationCaisse commissionRetrait = operationCaisse(CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE, SourceOperationCaisse.RETRAIT_EPARGNE, "RET-FRAIS", "900");
        RemboursementCredit remboursement = remboursementCredit("RC-REVENU", "20000", "2500", "750");

        when(operationCaisseRepository.findRevenueOperations(any(), any(), eq(null), anyList()))
            .thenReturn(List.of(commissionRetrait));
        when(remboursementCreditRepository.findRevenueRemboursements(any(), any(), eq(null))).thenReturn(List.of(remboursement));
        when(creditRepository.countByStatutInAndAgence(anyList(), eq(agence.getId()))).thenReturn(4L);
        when(membreRepository.countActiveByAgence(agence.getId())).thenReturn(12L);
        when(collecteJournaliereTerrainRepository.countValideesByAgenceAndPeriod(dateDebut, dateFin, agence.getId())).thenReturn(3L);

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getComparaisonAgences().getAgences()).hasSize(1);
        assertThat(response.getComparaisonAgences().getTotalRevenusAgences()).isEqualByComparingTo("4150");
        assertThat(response.getComparaisonAgences().getAgences().get(0).getCommissionsRetrait()).isEqualByComparingTo("900");
        assertThat(response.getComparaisonAgences().getAgences().get(0).getInteretsCredit()).isEqualByComparingTo("2500");
        assertThat(response.getComparaisonAgences().getAgences().get(0).getPenalitesCredit()).isEqualByComparingTo("750");
        assertThat(response.getComparaisonAgences().getAgences().get(0).getMembresActifs()).isEqualTo(12L);
        assertThat(response.getComparaisonAgences().getAgences().get(0).getCreditsActifs()).isEqualTo(4L);
        assertThat(response.getComparaisonAgences().getAgences().get(0).getCollectesValidees()).isEqualTo(3L);
        assertThat(response.getComparaisonAgences().getAgencePlusRevenus().getAgenceId()).isEqualTo(agence.getId());
    }

    @Test
    void salairesCooEtRciSontChargesSiegeEtNeFaussentPasAgence() {
        Employe coo = employeSiege(701L, PosteEmploye.COO, "300000");
        Employe rci = employeSiege(702L, PosteEmploye.RCI, "300000");
        when(employeRepository.findByActifTrue()).thenReturn(List.of(coo, rci));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getComparaisonAgences().getAgences()).hasSize(1);
        assertThat(response.getComparaisonAgences().getAgences().get(0).getChargesConnues()).isZero();
        assertThat(response.getChargesGlobalesSiege()).isEqualByComparingTo("600000");
        assertThat(response.getResultatApresChargesGlobalesSiege()).isEqualByComparingTo("-600000");
    }

    @Test
    void chefBureauNeVoitQueSonAgenceDansComparaison() {
        Role chefRole = Role.builder().code(RoleCode.CHEF_BUREAU).libelle("Chef bureau").build();
        Employe chefEmploye = Employe.builder()
            .matricule("CB-01")
            .nomComplet("Chef Bureau")
            .fonction(PosteEmploye.CHEF_BUREAU)
            .agence(agence)
            .actif(true)
            .build();
        Utilisateur chef = Utilisateur.builder()
            .username("chef")
            .nomComplet("Chef Bureau")
            .motDePasseHash("hash")
            .role(chefRole)
            .employe(chefEmploye)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(chef, null, chef.getAuthorities())
        );
        OperationCaisse commissionRetrait = operationCaisse(CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE, SourceOperationCaisse.RETRAIT_EPARGNE, "RET-FRAIS", "900");
        when(operationCaisseRepository.findRevenueOperations(any(), any(), eq(agence.getId()), anyList()))
            .thenReturn(List.of(commissionRetrait));

        RapportRevenusResponse response = service.getRapportRevenus(dateDebut, dateFin, null, null, null);

        assertThat(response.getComparaisonAgences().getAgences()).hasSize(1);
        assertThat(response.getComparaisonAgences().getAgences().get(0).getAgenceId()).isEqualTo(agence.getId());
        assertThat(response.getComparaisonAgences().getTotalRevenusAgences()).isEqualByComparingTo("900");
        verify(operationCaisseRepository, org.mockito.Mockito.atLeastOnce()).findRevenueOperations(any(), any(), eq(agence.getId()), anyList());
    }

    private void stubEmptyReportDependencies() {
        lenient().when(operationCaisseRepository.findRevenueOperations(any(), any(), any(), anyList())).thenReturn(List.of());
        lenient().when(operationCaisseRepository.findAccountingMovementOperations(any(), any(), any(), anyList())).thenReturn(List.of());
        lenient().when(operationCaisseRepository.sumApprovisionnementsByNatureUntil(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(depenseCaisseRepository.sumDepensesPayeesByPeriodAndAgence(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(depenseCaisseRepository.findDepensesPayeesWithContext(any(), any(), any())).thenReturn(List.of());
        lenient().when(depenseCaisseRepository.sumDepensesValideesNonPayeesByAgence(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(depenseCaisseRepository.findSalaryPaymentsForPayrollPeriod(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(depenseCaisseRepository.findTransportPaymentsForPeriod(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(remboursementCreditRepository.findRevenueRemboursements(any(), any(), any())).thenReturn(List.of());
        lenient().when(remboursementCreditRepository.sumPrincipalRembourseByPeriodAndAgence(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(remboursementCreditRepository.sumInteretsByPeriodAndAgence(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(remboursementCreditRepository.sumPenalitesByPeriodAndAgence(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(remboursementCreditRepository.findPrincipalRemboursements(any(), any(), any())).thenReturn(List.of());
        lenient().when(remboursementCreditRepository.findRemboursementsSansVentilation(any(), any(), any())).thenReturn(List.of());
        lenient().when(remboursementApportRepository.sumRemboursementsPayesByAgence(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(sessionCaisseRepository.sumSoldeTheoriqueByStatutsAndAgence(anyList(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(demandeRetraitEpargneRepository.sumRetraitsValidesNonPayesByAgence(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(compteEpargneRepository.sumSoldeDisponibleActifByAgence(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(compteEpargneRepository.sumSoldeBloqueActifByAgence(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(employeRepository.findByActifTrue()).thenReturn(List.of());
        lenient().when(employeRepository.findByAgenceIdAndActifTrue(any())).thenReturn(List.of());
        lenient().when(agentTerrainRepository.findAllActifsWithSalaryBeneficiaryContext()).thenReturn(List.of());
        lenient().when(transportSiteParametreRepository.findActifsAtDate(any())).thenReturn(List.of());
        lenient().when(agenceRepository.findByActifTrue()).thenReturn(List.of(agence));
        lenient().when(agenceRepository.findById(any())).thenReturn(Optional.of(agence));
        lenient().when(membreRepository.countActiveByAgence(any())).thenReturn(0L);
        lenient().when(collecteJournaliereTerrainRepository.countValideesByAgenceAndPeriod(any(), any(), any())).thenReturn(0L);
        lenient().when(creditRepository.sumCapitalDecaisseByPeriodAndAgence(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(creditRepository.sumCreditsApprouvesNonDecaissesByAgence(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(creditRepository.countByStatutInAndAgence(anyList(), any())).thenReturn(0L);
        lenient().when(creditRepository.countRemboursesByPeriodAndAgence(any(), any(), any(), anyList())).thenReturn(0L);
        lenient().when(creditRepository.findDecaissesWithoutMontantAccorde(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(creditRepository.findActiveCreditsWithInconsistentEncours(anyList(), any())).thenReturn(List.of());
        lenient().when(demandeCreditRepository.findForRevenueControls(any(), any(), any())).thenReturn(List.of());
        lenient().when(collecteJournaliereTerrainRepository.findForCarnetControls(any(), any(), any())).thenReturn(List.of());
        lenient().when(collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(parametreMetierService.getDecimal(any())).thenReturn(BigDecimal.ZERO);
    }

    private CollecteMembreLigne collecteLine(RecetteStatut statut, TypeLigneCollecte type, String reference, String montant) {
        CollecteJournaliereTerrain collecte = CollecteJournaliereTerrain.builder()
            .site(site)
            .antenneId(agence.getId())
            .dateCollecte(dateDebut)
            .statut(statut)
            .build();
        collecte.setId((long) reference.hashCode());

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(type)
            .montant(new BigDecimal(montant))
            .quantite(1)
            .reference(reference)
            .build();
        ligne.setId((long) (reference.hashCode() + 1));
        return ligne;
    }

    private RemboursementCredit remboursementCredit(String numeroRecu, String principal, String interet, String penalite) {
        Credit credit = Credit.builder()
            .numeroCredit("CR-01")
            .membre(membre)
            .site(site)
            .statut(StatutCredit.EN_COURS)
            .build();
        credit.setId(40L);

        RemboursementCredit remboursement = RemboursementCredit.builder()
            .numeroRecu(numeroRecu)
            .credit(credit)
            .membre(membre)
            .datePaiement(LocalDateTime.of(2026, 7, 24, 10, 0))
            .montantPrincipal(new BigDecimal(principal))
            .montantInteret(new BigDecimal(interet))
            .montantPenalite(new BigDecimal(penalite))
            .montantTotal(new BigDecimal(principal).add(new BigDecimal(interet)).add(new BigDecimal(penalite)))
            .build();
        remboursement.setId(50L);
        return remboursement;
    }

    private void stubAgentTerrainPayroll(String epargne, String remboursement, Long carnets, List<DepenseCaisse> paiementsSalaire) {
        when(employeRepository.findByActifTrue()).thenReturn(List.of(agentTerrainEmploye));
        when(agentTerrainRepository.findByUtilisateurId(77L)).thenReturn(Optional.of(agentTerrain));
        when(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
            601L, TypeLigneCollecte.EPARGNE, dateDebut, dateFin
        )).thenReturn(new BigDecimal(epargne));
        when(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
            601L, TypeLigneCollecte.REMBOURSEMENT_CREDIT, dateDebut, dateFin
        )).thenReturn(new BigDecimal(remboursement));
        when(collecteMembreLigneRepository.sumValidatedCarnetsByAgentAndPeriod(601L, dateDebut, dateFin)).thenReturn(carnets);
        when(depenseCaisseRepository.findSalaryPaymentsForPayrollPeriod(any(), any(), any(), eq(null))).thenReturn(paiementsSalaire);
    }

    private void stubTransportTerrain(List<Employe> agentsTerrain, List<TransportSiteParametre> parametres, List<DepenseCaisse> transportsPayes) {
        when(employeRepository.findByFonctionAndActifTrue(PosteEmploye.AGENT_TERRAIN)).thenReturn(agentsTerrain);
        when(transportSiteParametreRepository.findActifsAtDate(any())).thenReturn(parametres);
        when(depenseCaisseRepository.findTransportPaymentsForPeriod(any(), any(), any(), eq(null))).thenReturn(transportsPayes);
    }

    private TransportSiteParametre transportParametre(Site site, String montantJournalier) {
        TransportSiteParametre parametre = TransportSiteParametre.builder()
            .site(site)
            .montantTransportJournalierParAgent(new BigDecimal(montantJournalier))
            .actif(true)
            .dateDebutValidite(LocalDate.of(2026, 1, 1))
            .build();
        parametre.setId(900L + site.getId());
        return parametre;
    }

    private DepenseCaisse transportPaye(Employe employe, String montant) {
        DepenseCaisse depense = depense(DepenseCaisseCategorie.TRANSPORT, montant);
        depense.setEmploye(employe);
        depense.setPeriodeCharge("2026-07");
        depense.setSiteCharge(employe.getSite());
        return depense;
    }

    private Employe agentTerrainEmploye(Long employeId, Long utilisateurId) {
        Utilisateur utilisateurAgent = Utilisateur.builder()
            .username("agent.terrain")
            .nomComplet("Agent Terrain")
            .motDePasseHash("hash")
            .build();
        utilisateurAgent.setId(utilisateurId);
        Employe employe = Employe.builder()
            .matricule("AT-001")
            .nom("Agent")
            .prenom("Terrain")
            .nomComplet("Agent Terrain")
            .fonction(PosteEmploye.AGENT_TERRAIN)
            .dateEmbauche(dateDebut.minusMonths(1))
            .salaireBase(new BigDecimal("75000"))
            .primeFixe(BigDecimal.ZERO)
            .bonusVariable(BigDecimal.ZERO)
            .actif(true)
            .agence(agence)
            .site(site)
            .utilisateur(utilisateurAgent)
            .build();
        employe.setId(employeId);
        utilisateurAgent.setEmploye(employe);
        return employe;
    }

    private AgentTerrain agentTerrain(Utilisateur utilisateur, Long agentTerrainId) {
        AgentTerrain agent = AgentTerrain.builder()
            .utilisateur(utilisateur)
            .matricule("AT-001")
            .site(site)
            .actif(true)
            .build();
        agent.setId(agentTerrainId);
        return agent;
    }

    private Employe employeSiege(Long employeId, PosteEmploye poste, String salaireBase) {
        Employe employe = Employe.builder()
            .matricule(poste.name() + "-001")
            .nom(poste.name())
            .prenom("Siege")
            .nomComplet(poste.name() + " Siege")
            .fonction(poste)
            .dateEmbauche(dateDebut.minusMonths(1))
            .salaireBase(new BigDecimal(salaireBase))
            .primeFixe(BigDecimal.ZERO)
            .bonusVariable(BigDecimal.ZERO)
            .actif(true)
            .agence(agence)
            .site(null)
            .build();
        employe.setId(employeId);
        return employe;
    }

    private OperationCaisse operationCaisse(CategorieOperationCaisse categorie, SourceOperationCaisse source, String reference, String montant) {
        Caisse caisse = Caisse.builder()
            .codeCaisse("CAISSE-01")
            .libelle("Caisse 01")
            .agence(agence)
            .site(site)
            .build();
        caisse.setId(70L);

        OperationCaisse operation = OperationCaisse.builder()
            .numeroPiece(reference)
            .caisse(caisse)
            .dateOperation(dateDebut.atTime(10, 0))
            .typeOperation(TypeOperationCaisse.ENTREE)
            .categorieOperation(categorie)
            .source(source)
            .referenceMetier(reference)
            .montant(new BigDecimal(montant))
            .membre(membre)
            .build();
        operation.setId((long) reference.hashCode());
        return operation;
    }

    private DepenseCaisse depense(DepenseCaisseCategorie categorie, String montant) {
        Caisse caisse = Caisse.builder()
            .codeCaisse("CAISSE-01")
            .libelle("Caisse 01")
            .agence(agence)
            .site(site)
            .build();
        caisse.setId(70L);

        DepenseCaisse depense = DepenseCaisse.builder()
            .caisse(caisse)
            .site(site)
            .categorie(categorie)
            .montant(new BigDecimal(montant))
            .motif("Charge test")
            .statut(DepenseCaisseStatus.PAYEE)
            .dateDemande(dateDebut.atStartOfDay())
            .datePaiement(dateDebut.atTime(11, 0))
            .build();
        depense.setId((long) (categorie.name().hashCode() + montant.hashCode()));
        return depense;
    }
}