package com.mini.credit.service.impl;

import com.mini.credit.dto.membre.MembreCreateRequest;
import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.Sexe;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.mapper.MembreMapper;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.ActivationService;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.ReferenceGeneratorService;
import com.mini.credit.service.UtilisateurService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembreServiceImplAccountCreationTest {

    @Mock private MembreRepository membreRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private AgentTerrainRepository agentTerrainRepository;
    @Mock private MembreMapper membreMapper;
    @Mock private UtilisateurService utilisateurService;
    @Mock private ActivationService activationService;
    @Mock private CompteEpargneRepository compteEpargneRepository;
    @Mock private ReferenceGeneratorService referenceGeneratorService;
    @Mock private CollecteJournaliereTerrainRepository collecteRepository;
    @Mock private CollecteMembreLigneRepository collecteLigneRepository;
    @Mock private ParametreMetierService parametreMetierService;

    @InjectMocks private MembreServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldAutoCreateActiveSavingsAccountWithZeroBalances() {
        Site site = new Site();
        site.setId(10L);

        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(membreRepository.existsByCodeMembre(any())).thenReturn(false);
        when(membreRepository.findAllExistingUsernames()).thenReturn(List.of());
        when(referenceGeneratorService.genererReference("CEP")).thenReturn("CEP202606150001");
        when(compteEpargneRepository.existsByMembreIdAndStatut(101L, StatutCompte.ACTIF)).thenReturn(false);
        when(membreMapper.toResponse(any())).thenReturn(MembreResponse.builder().id(101L).build());

        Utilisateur memberUser = new Utilisateur();
        memberUser.setId(50L);
        memberUser.setUsername("doe.jane");
        Role role = new Role();
        role.setCode(RoleCode.MEMBER);
        memberUser.setRole(role);
        when(utilisateurService.createRawEntity(any(CreateUtilisateurRequest.class))).thenReturn(memberUser);

        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> {
            Membre membre = invocation.getArgument(0);
            if (membre.getId() == null) {
                membre.setId(101L);
            }
            return membre;
        });

        when(compteEpargneRepository.save(any(CompteEpargne.class))).thenAnswer(invocation -> {
            CompteEpargne compte = invocation.getArgument(0);
            compte.setId(201L);
            return compte;
        });

        MembreCreateRequest request = new MembreCreateRequest();
        request.setNom("Doe");
        request.setPostnom("Alpha");
        request.setPrenom("Jane");
        request.setSexe(Sexe.F);
        request.setDateNaissance(LocalDate.of(1998, 1, 1));
        request.setTelephonePrincipal("099000111");
        request.setAdresse("Adresse 1");
        request.setVille("Kinshasa");
        request.setCommune("Gombe");
        request.setQuartier("Q1");
        request.setProfessionActivite("Commerce");
        request.setLieuActivite("Marche");
        request.setSiteId(10L);
        request.setDateAdhesion(LocalDate.now());
        request.setEmail("jane@example.com");

        service.create(request);

        ArgumentCaptor<CompteEpargne> captor = ArgumentCaptor.forClass(CompteEpargne.class);
        verify(compteEpargneRepository).save(captor.capture());
        verify(compteEpargneRepository, times(1)).existsByMembreIdAndStatut(101L, StatutCompte.ACTIF);

        CompteEpargne created = captor.getValue();
        assertThat(created.getMembre().getId()).isEqualTo(101L);
        assertThat(created.getStatut()).isEqualTo(StatutCompte.ACTIF);
        assertThat(created.getSoldeDisponible()).isNotNull();
        assertThat(created.getSoldeDisponible().signum()).isZero();
        assertThat(created.getSoldeBloque()).isNotNull();
        assertThat(created.getSoldeBloque().signum()).isZero();
    }

    @Test
    void createByAgentTerrain_shouldAutoCreateCarnetFeeLineWithoutSavingsBalanceIncrease() {
        Role agentRole = new Role();
        agentRole.setCode(RoleCode.AGENT_TERRAIN);

        Utilisateur agentUser = new Utilisateur();
        agentUser.setId(70L);
        agentUser.setUsername("agent.terrain");
        agentUser.setRole(agentRole);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(agentUser, null)
        );

        Agence antenne = new Agence();
        antenne.setId(99L);

        Site site = new Site();
        site.setId(10L);
        site.setAgence(antenne);

        AgentTerrain agent = new AgentTerrain();
        agent.setId(80L);
        agent.setUtilisateur(agentUser);
        agent.setSite(site);

        when(agentTerrainRepository.findByUtilisateurId(70L)).thenReturn(Optional.of(agent));
        when(membreRepository.existsByCodeMembre(any())).thenReturn(false);
        when(membreRepository.findAllExistingUsernames()).thenReturn(List.of());
        when(referenceGeneratorService.genererReference("CEP")).thenReturn("CEP202606150002");
        when(compteEpargneRepository.existsByMembreIdAndStatut(101L, StatutCompte.ACTIF)).thenReturn(false);
        when(parametreMetierService.getDecimal("FRAIS_CARNET_EPARGNE")).thenReturn(new BigDecimal("1000"));
        when(membreMapper.toResponse(any())).thenReturn(MembreResponse.builder().id(101L).build());

        Utilisateur memberUser = new Utilisateur();
        memberUser.setId(50L);
        memberUser.setUsername("doe.jane");
        Role memberRole = new Role();
        memberRole.setCode(RoleCode.MEMBER);
        memberUser.setRole(memberRole);
        when(utilisateurService.createRawEntity(any(CreateUtilisateurRequest.class))).thenReturn(memberUser);

        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> {
            Membre membre = invocation.getArgument(0);
            if (membre.getId() == null) {
                membre.setId(101L);
            }
            return membre;
        });

        when(compteEpargneRepository.save(any(CompteEpargne.class))).thenAnswer(invocation -> {
            CompteEpargne compte = invocation.getArgument(0);
            compte.setId(201L);
            return compte;
        });

        LocalDate adhesionDate = LocalDate.now();
        when(collecteRepository.findByAgentTerrainIdAndDateCollecte(80L, adhesionDate)).thenReturn(Optional.empty());
        when(collecteRepository.save(any(CollecteJournaliereTerrain.class))).thenAnswer(invocation -> {
            CollecteJournaliereTerrain collecte = invocation.getArgument(0);
            if (collecte.getId() == null) {
                collecte.setId(500L);
            }
            return collecte;
        });
        when(collecteLigneRepository.existsByCollecteIdAndMembreIdAndTypeLigne(500L, 101L, TypeLigneCollecte.CARNET)).thenReturn(false);

        MembreCreateRequest request = new MembreCreateRequest();
        request.setNom("Doe");
        request.setPostnom("Alpha");
        request.setPrenom("Jane");
        request.setSexe(Sexe.F);
        request.setDateNaissance(LocalDate.of(1998, 1, 1));
        request.setTelephonePrincipal("099000111");
        request.setAdresse("Adresse 1");
        request.setVille("Kinshasa");
        request.setCommune("Gombe");
        request.setQuartier("Q1");
        request.setProfessionActivite("Commerce");
        request.setLieuActivite("Marche");
        request.setSiteId(10L);
        request.setDateAdhesion(adhesionDate);
        request.setEmail("jane@example.com");

        service.create(request);

        ArgumentCaptor<CollecteMembreLigne> ligneCaptor = ArgumentCaptor.forClass(CollecteMembreLigne.class);
        verify(collecteLigneRepository).save(ligneCaptor.capture());

        CollecteMembreLigne ligne = ligneCaptor.getValue();
        assertThat(ligne.getCollecte().getId()).isEqualTo(500L);
        assertThat(ligne.getMembre().getId()).isEqualTo(101L);
        assertThat(ligne.getTypeLigne()).isEqualTo(TypeLigneCollecte.CARNET);
        assertThat(ligne.getMontant()).isEqualByComparingTo("1000");
        assertThat(ligne.getQuantite()).isEqualTo(1);
        assertThat(ligne.getReference()).isEqualTo("ADHESION-MEMBRE-101");

        ArgumentCaptor<CollecteJournaliereTerrain> collecteCaptor = ArgumentCaptor.forClass(CollecteJournaliereTerrain.class);
        verify(collecteRepository, atLeastOnce()).save(collecteCaptor.capture());
        CollecteJournaliereTerrain collecte = collecteCaptor.getAllValues().get(collecteCaptor.getAllValues().size() - 1);
        assertThat(collecte.getStatut()).isEqualTo(RecetteStatut.BROUILLON);
        assertThat(collecte.getAgentTerrain().getId()).isEqualTo(80L);
        assertThat(collecte.getSite().getId()).isEqualTo(10L);
        assertThat(collecte.getAntenneId()).isEqualTo(99L);
        assertThat(collecte.getTotalCarnetsCalcule()).isEqualTo(1);
        assertThat(collecte.getTotalFraisCalcule()).isEqualByComparingTo("1000");
        assertThat(collecte.getEspecesDeclareesAgent()).isEqualByComparingTo("1000");
    }
}
