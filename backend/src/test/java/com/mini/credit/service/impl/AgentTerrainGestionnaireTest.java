package com.mini.credit.service.impl;

import com.mini.credit.dto.referentiel.AgentTerrainResponse;
import com.mini.credit.dto.referentiel.CreateAgentTerrainRequest;
import com.mini.credit.dto.referentiel.SiteResponse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.mapper.AgentTerrainMapper;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour la fonctionnalité Gestionnaire de AgentTerrainServiceImpl.
 * Vérifie la validation du gestionnaire à la création et les méthodes de portefeuille.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentTerrainServiceImpl — Tests Gestionnaire")
class AgentTerrainGestionnaireTest {

    @Mock private AgentTerrainRepository agentTerrainRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private EmployeRepository employeRepository;
    @Mock private AgentTerrainMapper agentTerrainMapper;

    @InjectMocks private AgentTerrainServiceImpl agentTerrainService;

    private Utilisateur utilisateur;
    private Site site;
    private Employe gestionnaire;
    private Employe employe;
    /** Agence commune à l'agent et au site — utilisée dans les tests de cohérence */
    private com.mini.credit.entity.agence.Agence agencePrincipale;

    @BeforeEach
    void setUp() {
        // Agence commune : le site ET l'employé agent terrain doivent appartenir à la même agence
        agencePrincipale = new com.mini.credit.entity.agence.Agence();
        agencePrincipale.setId(100L);
        agencePrincipale.setNomAgence("Agence Delvaux");

        // Employé lié à l'utilisateur agent terrain (requis par la validation cohérence agence)
        Employe agentEmploye = new Employe();
        agentEmploye.setId(5L);
        agentEmploye.setNomComplet("Agent Terrain Employé");
        agentEmploye.setMatricule("DEL1-AGT-26-001");
        agentEmploye.setFonction(PosteEmploye.AGENT_TERRAIN);
        agentEmploye.setActif(true);
        agentEmploye.setAgence(agencePrincipale); // MÊME agence que le site

        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setUsername("agent01");
        utilisateur.setEmploye(agentEmploye);

        site = new Site();
        site.setId(10L);
        site.setNomSite("Site Principal");
        site.setActif(true);
        site.setAgence(agencePrincipale); // MÊME agence que l'agent

        gestionnaire = new Employe();
        gestionnaire.setId(50L);
        gestionnaire.setNomComplet("Jean Gestionnaire");
        gestionnaire.setMatricule("DEL1-GES-26-001");
        gestionnaire.setFonction(PosteEmploye.GESTIONNAIRE);
        gestionnaire.setActif(true);
        gestionnaire.setAgence(agencePrincipale); // MÊME agence que le site

        employe = new Employe();
        employe.setId(60L);
        employe.setNomComplet("Paul Controleur");
        employe.setFonction(PosteEmploye.CONTROLEUR);
        employe.setActif(true);
    }

    // =========================================================
    // Tests Création avec gestionnaire
    // =========================================================

    @Test
    @DisplayName("create : gestionnaire valide → agent créé avec gestionnaire assigné")
    void create_withValidGestionnaire_shouldSetGestionnaire() {
        // Arrange
        CreateAgentTerrainRequest request = CreateAgentTerrainRequest.builder()
                .utilisateurId(1L)
                .matricule("AT001")
                .siteId(10L)
                .gestionnaireId(50L)
                .dateAffectation(LocalDate.now())
                .build();

        AgentTerrain savedAgent = new AgentTerrain();
        savedAgent.setId(1L);
        savedAgent.setGestionnaire(gestionnaire);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(agentTerrainRepository.findByUtilisateurId(1L)).thenReturn(Optional.empty());
        when(agentTerrainRepository.findAll()).thenReturn(List.of());
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(employeRepository.findById(50L)).thenReturn(Optional.of(gestionnaire));
        when(agentTerrainMapper.toEntity(any())).thenReturn(new AgentTerrain());
        when(agentTerrainRepository.save(any())).thenReturn(savedAgent);
        when(agentTerrainMapper.toDTO(savedAgent)).thenReturn(
                AgentTerrainResponse.builder().id(1L).gestionnaireId(50L)
                        .gestionnaireNomComplet("Jean Gestionnaire").build());

        // Act
        AgentTerrainResponse result = agentTerrainService.create(request);

        // Assert
        assertThat(result.getGestionnaireId()).isEqualTo(50L);
        assertThat(result.getGestionnaireNomComplet()).isEqualTo("Jean Gestionnaire");
        verify(agentTerrainRepository).save(any());
    }

    @Test
    @DisplayName("create : employe non Gestionnaire → exception")
    void create_withNonGestionnaireEmploye_shouldThrowException() {
        // Arrange
        CreateAgentTerrainRequest request = CreateAgentTerrainRequest.builder()
                .utilisateurId(1L)
                .matricule("AT001")
                .siteId(10L)
                .gestionnaireId(60L) // employe CONTROLEUR
                .build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(agentTerrainRepository.findByUtilisateurId(1L)).thenReturn(Optional.empty());
        when(agentTerrainRepository.findAll()).thenReturn(List.of());
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(employeRepository.findById(60L)).thenReturn(Optional.of(employe)); // CONTROLEUR
        when(agentTerrainMapper.toEntity(any())).thenReturn(new AgentTerrain());

        // Act & Assert
        assertThatThrownBy(() -> agentTerrainService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("n'est pas un Gestionnaire");
    }

    @Test
    @DisplayName("create : gestionnaire inactif → exception")
    void create_withInactiveGestionnaire_shouldThrowException() {
        // Arrange
        gestionnaire.setActif(false); // gestionnaire inactif

        CreateAgentTerrainRequest request = CreateAgentTerrainRequest.builder()
                .utilisateurId(1L)
                .matricule("AT001")
                .siteId(10L)
                .gestionnaireId(50L)
                .build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(agentTerrainRepository.findByUtilisateurId(1L)).thenReturn(Optional.empty());
        when(agentTerrainRepository.findAll()).thenReturn(List.of());
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(employeRepository.findById(50L)).thenReturn(Optional.of(gestionnaire));
        when(agentTerrainMapper.toEntity(any())).thenReturn(new AgentTerrain());

        // Act & Assert
        assertThatThrownBy(() -> agentTerrainService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("gestionnaire doit être actif");
    }

    // =========================================================
    // Tests getAgentsByGestionnaire
    // =========================================================

    @Test
    @DisplayName("getAgentsByGestionnaire : retourne uniquement les agents actifs du gestionnaire")
    void getAgentsByGestionnaire_shouldReturnOnlyHisActiveAgents() {
        // Arrange
        AgentTerrain agent1 = new AgentTerrain();
        agent1.setId(1L);
        agent1.setActif(true);
        agent1.setGestionnaire(gestionnaire);

        AgentTerrain agent2 = new AgentTerrain();
        agent2.setId(2L);
        agent2.setActif(true);
        agent2.setGestionnaire(gestionnaire);

        when(employeRepository.findById(50L)).thenReturn(Optional.of(gestionnaire));
        when(agentTerrainRepository.findByGestionnaireIdAndActifTrue(50L))
                .thenReturn(List.of(agent1, agent2));
        when(agentTerrainMapper.toDTO(agent1)).thenReturn(
                AgentTerrainResponse.builder().id(1L).gestionnaireId(50L).build());
        when(agentTerrainMapper.toDTO(agent2)).thenReturn(
                AgentTerrainResponse.builder().id(2L).gestionnaireId(50L).build());

        // Act
        List<AgentTerrainResponse> result = agentTerrainService.getAgentsByGestionnaire(50L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> Long.valueOf(50L).equals(a.getGestionnaireId()));
    }

    @Test
    @DisplayName("getAgentsByGestionnaire : employe non Gestionnaire → exception")
    void getAgentsByGestionnaire_withNonGestionnaire_shouldThrowException() {
        when(employeRepository.findById(60L)).thenReturn(Optional.of(employe)); // CONTROLEUR

        assertThatThrownBy(() -> agentTerrainService.getAgentsByGestionnaire(60L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("n'est pas un Gestionnaire");
    }

    // =========================================================
    // Tests getSitesByGestionnaire
    // =========================================================

    @Test
    @DisplayName("getSitesByGestionnaire : retourne les sites distincts actifs")
    void getSitesByGestionnaire_shouldReturnDistinctActiveSites() {
        // Arrange
        Site site2 = new Site();
        site2.setId(20L);
        site2.setNomSite("Site 2");
        site2.setActif(true);
        site2.setAgence(site.getAgence());

        when(employeRepository.findById(50L)).thenReturn(Optional.of(gestionnaire));
        when(agentTerrainRepository.findSitesByGestionnaireId(50L))
                .thenReturn(List.of(site, site2));

        // Act
        List<SiteResponse> result = agentTerrainService.getSitesByGestionnaire(50L);

        // Assert
        assertThat(result).hasSize(2);
    }

    // =========================================================
    // Tests getAgentsBySite — correction bug site principal
    // =========================================================

    @Test
    @DisplayName("getAgentsBySite : agent avec site principal → retourné même si sitesAffectes vide")
    void getAgentsBySite_shouldReturnAgentWithSitePrincipalOnly() {
        // Arrange : l'agent a site.id = 10L mais sitesAffectes vide
        AgentTerrain agent = new AgentTerrain();
        agent.setId(1L);
        agent.setActif(true);
        agent.setSite(site);
        agent.setSitesAffectes(new java.util.HashSet<>());

        AgentTerrainResponse expectedResponse = AgentTerrainResponse.builder()
                .id(1L).siteId(10L).sitePrincipalNom("Site Principal").build();

        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(utilisateurRepository.findSelectableAgentsTerrainByEmployeSiteId(10L)).thenReturn(List.of());
        when(agentTerrainRepository.findActifsBySiteId(10L)).thenReturn(List.of(agent));
        when(agentTerrainMapper.toDTO(agent)).thenReturn(expectedResponse);

        // Act
        List<AgentTerrainResponse> result = agentTerrainService.getAgentsBySite(10L);

        // Assert — la requête JPQL couvre désormais le site principal
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSiteId()).isEqualTo(10L);
        verify(agentTerrainRepository).findActifsBySiteId(10L);
    }

    @Test
    @DisplayName("getAgentsBySite : agent avec siteAffecte correspondant → retourné")
    void getAgentsBySite_shouldReturnAgentWithMatchingSiteAffecte() {
        // Arrange : l'agent a un autre site principal, mais site 10 est dans sitesAffectes
        Site autreSite = new Site();
        autreSite.setId(99L);
        autreSite.setNomSite("Autre Site");
        autreSite.setActif(true);

        AgentTerrain agent = new AgentTerrain();
        agent.setId(2L);
        agent.setActif(true);
        agent.setSite(autreSite);
        agent.setSitesAffectes(new java.util.HashSet<>(List.of(site)));

        AgentTerrainResponse expectedResponse = AgentTerrainResponse.builder()
                .id(2L).siteId(99L).siteIds(java.util.Set.of(10L)).build();

        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(utilisateurRepository.findSelectableAgentsTerrainByEmployeSiteId(10L)).thenReturn(List.of());
        when(agentTerrainRepository.findActifsBySiteId(10L)).thenReturn(List.of(agent));
        when(agentTerrainMapper.toDTO(agent)).thenReturn(expectedResponse);

        // Act
        List<AgentTerrainResponse> result = agentTerrainService.getAgentsBySite(10L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSiteIds()).contains(10L);
    }

        @Test
        @DisplayName("getAgentsBySite : utilisateur Agent Terrain synchronisé sans ligne agent_terrain préalable")
        void getAgentsBySite_shouldSynchronizeUtilisateurAgentTerrainBeforeReturningList() {
                Utilisateur nouvelUtilisateur = new Utilisateur();
                nouvelUtilisateur.setId(77L);
                nouvelUtilisateur.setUsername("agent.site10");
                utilisateur.getEmploye().setSite(site);
                nouvelUtilisateur.setEmploye(utilisateur.getEmploye());

                AgentTerrain agentSynchronise = new AgentTerrain();
                agentSynchronise.setId(88L);
                agentSynchronise.setUtilisateur(nouvelUtilisateur);
                agentSynchronise.setSite(site);
                agentSynchronise.setMatricule("DEL1-AGT-26-001");
                agentSynchronise.setActif(true);

                AgentTerrainResponse expectedResponse = AgentTerrainResponse.builder()
                                .id(88L)
                                .utilisateurId(77L)
                                .siteId(10L)
                                .matricule("DEL1-AGT-26-001")
                                .build();

                when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
                when(utilisateurRepository.findSelectableAgentsTerrainByEmployeSiteId(10L)).thenReturn(List.of(nouvelUtilisateur));
                when(agentTerrainRepository.findByUtilisateurId(77L)).thenReturn(Optional.empty());
                when(agentTerrainRepository.findActifsBySiteId(10L)).thenReturn(List.of(agentSynchronise));
                when(agentTerrainMapper.toDTO(agentSynchronise)).thenReturn(expectedResponse);

                List<AgentTerrainResponse> result = agentTerrainService.getAgentsBySite(10L);

                verify(utilisateurRepository).findSelectableAgentsTerrainByEmployeSiteId(10L);
                ArgumentCaptor<AgentTerrain> captor = ArgumentCaptor.forClass(AgentTerrain.class);
                verify(agentTerrainRepository).save(captor.capture());
                assertThat(captor.getValue().getUtilisateur().getId()).isEqualTo(77L);
                assertThat(captor.getValue().getSite().getId()).isEqualTo(10L);
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getUtilisateurId()).isEqualTo(77L);
        }

    @Test
    @DisplayName("getAgentsBySite : site inactif → exception")
    void getAgentsBySite_withInactiveSite_shouldThrowException() {
        site.setActif(false);
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));

        assertThatThrownBy(() -> agentTerrainService.getAgentsBySite(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("actif");
    }

    @Test
    @DisplayName("getAgentsBySite : site inexistant → exception")
    void getAgentsBySite_withUnknownSite_shouldThrowException() {
        when(siteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentTerrainService.getAgentsBySite(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non trouvé");
    }

    // =========================================================
    // Tests cohérence Agence/Site/AgentTerrain/Gestionnaire
    // =========================================================

    @Test
    @DisplayName("createAgentTerrain_shouldRejectAgentEmployeFromDifferentAgenceThanSite")
    void createAgentTerrain_shouldRejectAgentEmployeFromDifferentAgenceThanSite() {
        // L'employé de l'agent appartient à une agence DIFFÉRENTE du site
        com.mini.credit.entity.agence.Agence autreAgence = new com.mini.credit.entity.agence.Agence();
        autreAgence.setId(999L);
        autreAgence.setNomAgence("Autre Agence");
        utilisateur.getEmploye().setAgence(autreAgence); // agence 999 ≠ site.agence 100

        CreateAgentTerrainRequest request = CreateAgentTerrainRequest.builder()
                .utilisateurId(1L)
                .matricule("AT-DIFF-001")
                .siteId(10L)
                .gestionnaireId(50L)
                .dateAffectation(java.time.LocalDate.now())
                .build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(agentTerrainRepository.findByUtilisateurId(1L)).thenReturn(Optional.empty());
        when(agentTerrainRepository.findAll()).thenReturn(List.of());
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        // Pas de stub pour toEntity : l'exception est levée avant d'atteindre le mapper

        assertThatThrownBy(() -> agentTerrainService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("même agence");
    }

    @Test
    @DisplayName("createAgentTerrain_shouldRejectGestionnaireFromDifferentAgenceThanSite")
    void createAgentTerrain_shouldRejectGestionnaireFromDifferentAgenceThanSite() {
        // L'agent est dans la bonne agence, mais le gestionnaire est dans une agence différente
        com.mini.credit.entity.agence.Agence autreAgence = new com.mini.credit.entity.agence.Agence();
        autreAgence.setId(888L);
        autreAgence.setNomAgence("Agence Tierce");
        gestionnaire.setAgence(autreAgence); // agence 888 ≠ site.agence 100

        CreateAgentTerrainRequest request = CreateAgentTerrainRequest.builder()
                .utilisateurId(1L)
                .matricule("AT-GESDIFF-001")
                .siteId(10L)
                .gestionnaireId(50L)
                .dateAffectation(java.time.LocalDate.now())
                .build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(agentTerrainRepository.findByUtilisateurId(1L)).thenReturn(Optional.empty());
        when(agentTerrainRepository.findAll()).thenReturn(List.of());
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(employeRepository.findById(50L)).thenReturn(Optional.of(gestionnaire));
        when(agentTerrainMapper.toEntity(any())).thenReturn(new AgentTerrain());

        assertThatThrownBy(() -> agentTerrainService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("même agence");
    }

    @Test
    @DisplayName("createAgentTerrain_shouldAcceptAgentAndGestionnaireFromSameAgenceAsSite")
    void createAgentTerrain_shouldAcceptAgentAndGestionnaireFromSameAgenceAsSite() {
        // L'agent et le gestionnaire sont dans la MÊME agence que le site → succès
        CreateAgentTerrainRequest request = CreateAgentTerrainRequest.builder()
                .utilisateurId(1L)
                .matricule("AT-OK-001")
                .siteId(10L)
                .gestionnaireId(50L)
                .dateAffectation(java.time.LocalDate.now())
                .build();

        AgentTerrain savedAgent = new AgentTerrain();
        savedAgent.setId(99L);
        savedAgent.setGestionnaire(gestionnaire);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(agentTerrainRepository.findByUtilisateurId(1L)).thenReturn(Optional.empty());
        when(agentTerrainRepository.findAll()).thenReturn(List.of());
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(employeRepository.findById(50L)).thenReturn(Optional.of(gestionnaire));
        when(agentTerrainMapper.toEntity(any())).thenReturn(new AgentTerrain());
        when(agentTerrainRepository.save(any())).thenReturn(savedAgent);
        when(agentTerrainMapper.toDTO(savedAgent)).thenReturn(
                AgentTerrainResponse.builder().id(99L).gestionnaireId(50L).build());

        AgentTerrainResponse result = agentTerrainService.create(request);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getGestionnaireId()).isEqualTo(50L);
        verify(agentTerrainRepository).save(any());
    }
}
