package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.document.TicketRecuGenerationRequest;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.SavingMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.QuittanceService;
import com.mini.credit.service.TicketRecuService;
import com.mini.credit.service.security.ScopeService;
import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationEpargneServiceImplTest {

    @Mock
    private OperationEpargneRepository operationEpargneRepository;

    @Mock
    private CompteEpargneRepository compteEpargneRepository;

    @Mock
    private MembreRepository membreRepository;

    @Mock
    private AgentTerrainRepository agentTerrainRepository;

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;

    @Mock
    private CollecteMembreLigneRepository collecteMembreLigneRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private SavingMapper savingMapper;

    @Mock
    private OperationCaisseService operationCaisseService;

    @Mock
    private QuittanceService quittanceService;

    @Mock
    private TicketRecuService ticketRecuService;

    @Mock
    private ScopeService scopeService;

    @InjectMocks
    private OperationEpargneServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enregistrer_shouldKeepManualScopeCheck_whenRequestIsNotAutomaticCollecte() {
        OperationEpargneRequest request = baseRequest();
        request.setReferenceExterne(null);

        when(scopeService.canRecordEpargneOperation(request.getMembreId())).thenReturn(false);

        assertThatThrownBy(() -> service.enregistrer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Accès refusé: vous n'êtes pas autorisé");

        verify(scopeService).canRecordEpargneOperation(request.getMembreId());
    }

    @Test
    void enregistrer_shouldBypassManualScope_whenAutomaticCollecteContextIsValid() {
        OperationEpargneRequest request = automaticCollecteRequest();
        Utilisateur controleur = controleur(10L, 5L);
        Utilisateur principalDetache = utilisateur(10L, RoleCode.CONTROLEUR, null);
        authenticate(principalDetache);

        CollecteJournaliereTerrain collecte = collecteValidee(10L, 3L, 11L, 5L);
        CollecteMembreLigne ligne = ligneEpargne(22L, collecte, 7L, new BigDecimal("1000"));
        Membre membre = membre(7L, 11L, 5L);

        when(utilisateurRepository.findByIdWithValidationContext(10L)).thenReturn(Optional.of(controleur));
        when(collecteJournaliereTerrainRepository.findById(10L)).thenReturn(Optional.of(collecte));
        when(collecteMembreLigneRepository.findById(22L)).thenReturn(Optional.of(ligne));
        when(membreRepository.findById(7L)).thenReturn(Optional.of(membre));
        when(compteEpargneRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enregistrer(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compte épargne introuvable");

        verify(scopeService, never()).canRecordEpargneOperation(anyLong());
    }

    @Test
    void enregistrer_shouldRejectAutomaticCollecte_whenMemberIsOutsideCollecteSite() {
        OperationEpargneRequest request = automaticCollecteRequest();
        Utilisateur controleur = controleur(10L, 5L);
        authenticate(controleur);

        CollecteJournaliereTerrain collecte = collecteValidee(10L, 3L, 11L, 5L);
        CollecteMembreLigne ligne = ligneEpargne(22L, collecte, 7L, new BigDecimal("1000"));
        Membre membreHorsSite = membre(7L, 99L, 5L);

        when(utilisateurRepository.findByIdWithValidationContext(10L)).thenReturn(Optional.of(controleur));
        when(collecteJournaliereTerrainRepository.findById(10L)).thenReturn(Optional.of(collecte));
        when(collecteMembreLigneRepository.findById(22L)).thenReturn(Optional.of(ligne));
        when(membreRepository.findById(7L)).thenReturn(Optional.of(membreHorsSite));

        assertThatThrownBy(() -> service.enregistrer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("membre hors site");
    }

    @Test
    void enregistrer_shouldRejectAutomaticCollecte_whenUserRoleIsNotAllowed() {
        OperationEpargneRequest request = automaticCollecteRequest();
        Utilisateur agent = utilisateur(77L, RoleCode.AGENT_TERRAIN, null);
        authenticate(agent);

        when(utilisateurRepository.findByIdWithValidationContext(77L)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.enregistrer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("génération automatique réservée");
    }

            @Test
            void enregistrer_shouldAllowBlocageGarantie_onEpargneVolontaireAccount() {
            OperationEpargneRequest request = baseRequest();
            request.setTypeOperation(TypeOperationEpargne.BLOCAGE_GARANTIE);
            request.setMontant(new BigDecimal("250"));
            request.setAgentId(null);
            request.setCreatedBy(null);

            Membre membre = Membre.builder()
                .statut(StatutMembre.ACTIF)
                .build();
            membre.setId(request.getMembreId());

            CompteEpargne compte = CompteEpargne.builder()
                .membre(membre)
                .typeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE)
                .statut(StatutCompte.ACTIF)
                .soldeDisponible(new BigDecimal("1000"))
                .soldeBloque(BigDecimal.ZERO)
                .build();
            compte.setId(request.getCompteEpargneId());

            OperationEpargneResponse response = OperationEpargneResponse.builder()
                .typeOperation(TypeOperationEpargne.BLOCAGE_GARANTIE)
                .build();

            when(scopeService.canRecordEpargneOperation(request.getMembreId())).thenReturn(true);
            when(compteEpargneRepository.findById(request.getCompteEpargneId())).thenReturn(Optional.of(compte));
            when(membreRepository.findById(request.getMembreId())).thenReturn(Optional.of(membre));
            when(operationEpargneRepository.save(any(OperationEpargne.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(savingMapper.toResponse(any(OperationEpargne.class))).thenReturn(response);

            OperationEpargneResponse result = service.enregistrer(request);

            assertThat(result.getTypeOperation()).isEqualTo(TypeOperationEpargne.BLOCAGE_GARANTIE);
            assertThat(compte.getSoldeDisponible()).isEqualByComparingTo("750");
            assertThat(compte.getSoldeBloque()).isEqualByComparingTo("250");
            verify(compteEpargneRepository).save(compte);
            }

    @Test
    void depotDirectRenseigneCreatedByOperationEpargne() {
        OperationEpargneRequest request = depotDirectRequestWithoutCreatedBy();
        Utilisateur admin = utilisateur(1L, RoleCode.ADMIN, 5L);
        authenticate(admin);
        OperationEpargneResponse response = OperationEpargneResponse.builder()
                .typeOperation(TypeOperationEpargne.EPARGNE)
                .build();

        prepareDepotDirectSuccess(request, admin, response);

        service.enregistrer(request);

        ArgumentCaptor<OperationEpargne> operationCaptor = ArgumentCaptor.forClass(OperationEpargne.class);
        verify(operationEpargneRepository).save(operationCaptor.capture());
        assertThat(operationCaptor.getValue().getCreatedBy()).isNotNull();
        assertThat(operationCaptor.getValue().getCreatedBy().getId()).isEqualTo(1L);
    }

    @Test
    void operationCaisseEtOperationEpargneOntTraceUtilisateurCoherente() {
        OperationEpargneRequest request = depotDirectRequestWithoutCreatedBy();
        Utilisateur admin = utilisateur(1L, RoleCode.ADMIN, 5L);
        authenticate(admin);
        OperationEpargneResponse response = OperationEpargneResponse.builder()
                .typeOperation(TypeOperationEpargne.EPARGNE)
                .build();

        prepareDepotDirectSuccess(request, admin, response);

        service.enregistrer(request);

        ArgumentCaptor<OperationEpargne> operationCaptor = ArgumentCaptor.forClass(OperationEpargne.class);
        ArgumentCaptor<OperationCaisseRequest> caisseRequestCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationEpargneRepository).save(operationCaptor.capture());
        verify(operationCaisseService).enregistrer(caisseRequestCaptor.capture());

        assertThat(operationCaptor.getValue().getCreatedBy().getId()).isEqualTo(1L);
        assertThat(caisseRequestCaptor.getValue().getCreatedBy()).isEqualTo(1L);
        assertThat(caisseRequestCaptor.getValue().getOperationEpargneId()).isEqualTo(operationCaptor.getValue().getId());
    }

    @Test
    void collecteTerrainRenseigneCreatedByOperationEpargneOuUtilisateurFlux() {
        OperationEpargneRequest request = automaticCollecteRequest();
        Utilisateur controleur = controleur(10L, 5L);
        authenticate(controleur);
        OperationEpargneResponse response = OperationEpargneResponse.builder()
                .typeOperation(TypeOperationEpargne.EPARGNE)
                .build();

        CollecteJournaliereTerrain collecte = collecteValidee(10L, 3L, 11L, 5L);
        CollecteMembreLigne ligne = ligneEpargne(22L, collecte, 7L, new BigDecimal("1000"));
        Membre membre = membreActif(7L, 11L, 5L);
        CompteEpargne compte = compteActif(99L, membre, new BigDecimal("1000"));
        AgentTerrain agent = AgentTerrain.builder().build();
        agent.setId(3L);

        when(utilisateurRepository.findByIdWithValidationContext(10L)).thenReturn(Optional.of(controleur));
        when(collecteJournaliereTerrainRepository.findById(10L)).thenReturn(Optional.of(collecte));
        when(collecteMembreLigneRepository.findById(22L)).thenReturn(Optional.of(ligne));
        when(membreRepository.findById(7L)).thenReturn(Optional.of(membre));
        when(compteEpargneRepository.findById(99L)).thenReturn(Optional.of(compte));
        when(agentTerrainRepository.findById(3L)).thenReturn(Optional.of(agent));
        when(operationEpargneRepository.save(any(OperationEpargne.class))).thenAnswer(invocation -> {
            OperationEpargne operation = invocation.getArgument(0);
            operation.setId(700L);
            return operation;
        });
        when(savingMapper.toResponse(any(OperationEpargne.class))).thenReturn(response);

        service.enregistrer(request);

        ArgumentCaptor<OperationEpargne> operationCaptor = ArgumentCaptor.forClass(OperationEpargne.class);
        ArgumentCaptor<TicketRecuGenerationRequest> ticketCaptor = ArgumentCaptor.forClass(TicketRecuGenerationRequest.class);
        verify(operationEpargneRepository).save(operationCaptor.capture());
        verify(ticketRecuService).genererDepuisOperation(ticketCaptor.capture());

        assertThat(operationCaptor.getValue().getCreatedBy().getId()).isEqualTo(10L);
        assertThat(ticketCaptor.getValue().getUtilisateurCreateurId()).isEqualTo(10L);
    }

    private OperationEpargneRequest baseRequest() {
        OperationEpargneRequest request = new OperationEpargneRequest();
        request.setCompteEpargneId(99L);
        request.setMembreId(7L);
        request.setDateOperation(LocalDateTime.now());
        request.setTypeOperation(TypeOperationEpargne.EPARGNE);
        request.setMontant(new BigDecimal("1000"));
        request.setModePaiement(ModePaiement.ESPECES);
        request.setAgentId(3L);
        request.setCreatedBy(10L);
        return request;
    }

    private OperationEpargneRequest depotDirectRequestWithoutCreatedBy() {
        OperationEpargneRequest request = baseRequest();
        request.setSessionCaisseId(55L);
        request.setCreatedBy(null);
        request.setReferenceExterne("VALID-TRACE-FUTURE");
        request.setObservation("Validation trace future");
        return request;
    }

    private OperationEpargneRequest automaticCollecteRequest() {
        OperationEpargneRequest request = baseRequest();
        request.setReferenceExterne("COLLECTE-10-LIGNE-22");
        return request;
    }

    private void authenticate(Utilisateur utilisateur) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities())
        );
    }

    private void prepareDepotDirectSuccess(
            OperationEpargneRequest request,
            Utilisateur currentUser,
            OperationEpargneResponse response
    ) {
        Membre membre = membreActif(request.getMembreId(), 11L, 5L);
        CompteEpargne compte = compteActif(request.getCompteEpargneId(), membre, new BigDecimal("1000"));
        SessionCaisse session = sessionOuverte(request.getSessionCaisseId(), 5L);
        AgentTerrain agent = AgentTerrain.builder().build();
        agent.setId(request.getAgentId());

        when(scopeService.canRecordEpargneOperation(request.getMembreId())).thenReturn(true);
        when(utilisateurRepository.findByIdWithValidationContext(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(compteEpargneRepository.findById(request.getCompteEpargneId())).thenReturn(Optional.of(compte));
        when(membreRepository.findById(request.getMembreId())).thenReturn(Optional.of(membre));
        when(agentTerrainRepository.findById(request.getAgentId())).thenReturn(Optional.of(agent));
        when(sessionCaisseRepository.findById(request.getSessionCaisseId())).thenReturn(Optional.of(session));
        when(operationEpargneRepository.save(any(OperationEpargne.class))).thenAnswer(invocation -> {
            OperationEpargne operation = invocation.getArgument(0);
            operation.setId(600L);
            return operation;
        });
        when(operationCaisseService.enregistrer(any(OperationCaisseRequest.class))).thenReturn(OperationCaisseResponse.builder()
                .id(800L)
                .build());
        when(savingMapper.toResponse(any(OperationEpargne.class))).thenReturn(response);
    }

    private Utilisateur controleur(Long userId, Long antenneId) {
        return utilisateur(userId, RoleCode.CONTROLEUR, antenneId);
    }

    private Utilisateur utilisateur(Long userId, RoleCode roleCode, Long antenneId) {
        Utilisateur user = Utilisateur.builder()
                .username("user")
                .motDePasseHash("x")
                .nomComplet("Utilisateur Test")
                .role(Role.builder().code(roleCode).build())
                .build();
        user.setId(userId);

        if (antenneId != null) {
            Agence agence = Agence.builder()
                    .codeAgence("ANT-01")
                    .nomAgence("Antenne Test")
                    .build();
            agence.setId(antenneId);

            Employe employe = Employe.builder()
                    .nomComplet("Controleur Test")
                    .dateEmbauche(LocalDate.now())
                    .agence(agence)
                    .build();
            employe.setId(500L);
            user.setEmploye(employe);
        }

        return user;
    }

    private CollecteJournaliereTerrain collecteValidee(Long collecteId, Long agentId, Long siteId, Long antenneId) {
        Site site = Site.builder()
                .codeSite("SITE-01")
                .nomSite("Site Test")
                .zone("ZONE")
                .actif(true)
                .build();
        site.setId(siteId);

        AgentTerrain agent = AgentTerrain.builder().build();
        agent.setId(agentId);

        CollecteJournaliereTerrain collecte = CollecteJournaliereTerrain.builder()
                .agentTerrain(agent)
                .site(site)
                .antenneId(antenneId)
                .dateCollecte(LocalDate.now())
                .statut(RecetteStatut.VALIDEE)
                .build();
        collecte.setId(collecteId);
        return collecte;
    }

    private CollecteMembreLigne ligneEpargne(Long ligneId, CollecteJournaliereTerrain collecte, Long membreId, BigDecimal montant) {
        Membre membre = Membre.builder().build();
        membre.setId(membreId);

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
                .collecte(collecte)
                .membre(membre)
                .typeLigne(TypeLigneCollecte.EPARGNE)
                .montant(montant)
                .build();
        ligne.setId(ligneId);
        return ligne;
    }

    private Membre membre(Long membreId, Long siteId, Long antenneId) {
        Agence agence = Agence.builder()
                .codeAgence("ANT-01")
                .nomAgence("Antenne Test")
                .build();
        agence.setId(antenneId);

        Site site = Site.builder()
                .codeSite("SITE-01")
                .nomSite("Site Membre")
                .zone("ZONE")
                .agence(agence)
                .actif(true)
                .build();
        site.setId(siteId);

        Membre membre = Membre.builder().build();
        membre.setId(membreId);
        membre.setSite(site);
        return membre;
    }

    private Membre membreActif(Long membreId, Long siteId, Long antenneId) {
        Membre membre = membre(membreId, siteId, antenneId);
        membre.setStatut(StatutMembre.ACTIF);
        return membre;
    }

    private CompteEpargne compteActif(Long compteId, Membre membre, BigDecimal soldeDisponible) {
        CompteEpargne compte = CompteEpargne.builder()
                .membre(membre)
                .typeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE)
                .statut(StatutCompte.ACTIF)
                .soldeDisponible(soldeDisponible)
                .soldeBloque(BigDecimal.ZERO)
                .build();
        compte.setId(compteId);
        return compte;
    }

    private SessionCaisse sessionOuverte(Long sessionId, Long antenneId) {
        Agence agence = Agence.builder()
                .codeAgence("ANT-01")
                .nomAgence("Antenne Test")
                .build();
        agence.setId(antenneId);
        Caisse caisse = Caisse.builder()
                .agence(agence)
                .actif(true)
                .build();
        caisse.setId(44L);
        SessionCaisse session = SessionCaisse.builder()
                .caisse(caisse)
                .statut(StatutSessionCaisse.OUVERTE)
                .build();
        session.setId(sessionId);
        return session;
    }
}
