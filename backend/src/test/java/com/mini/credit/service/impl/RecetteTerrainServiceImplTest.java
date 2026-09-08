package com.mini.credit.service.impl;

import com.mini.credit.dto.referentiel.CreateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.RecetteTerrainResponse;
import com.mini.credit.dto.referentiel.UpdateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.ValidateRecetteTerrainRequest;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.RecetteTerrainJournaliere;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.mapper.RecetteTerrainMapper;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.recetteTerrain.RecetteTerrainRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.WorkflowTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecetteTerrainServiceImpl - règles métier AGENT_TERRAIN")
class RecetteTerrainServiceImplTest {

    @Mock private RecetteTerrainRepository recetteRepository;
    @Mock private AgentTerrainRepository agentTerrainRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RecetteTerrainMapper recetteMapper;
    @Mock private WorkflowTaskService workflowTaskService;

    @InjectMocks private RecetteTerrainServiceImpl service;

    private Utilisateur userAgent;
    private Utilisateur userControleur;
    private AgentTerrain myAgent;
    private AgentTerrain otherAgent;
    private Site site;

    @BeforeEach
    void setUp() {
        Role roleAgent = new Role();
        roleAgent.setCode(RoleCode.AGENT_TERRAIN);
        userAgent = new Utilisateur();
        userAgent.setId(10L);
        userAgent.setUsername("agent01");
        userAgent.setRole(roleAgent);

        Role roleCtrl = new Role();
        roleCtrl.setCode(RoleCode.CONTROLEUR);
        userControleur = new Utilisateur();
        userControleur.setId(20L);
        userControleur.setUsername("ctrl01");
        userControleur.setRole(roleCtrl);

        myAgent = new AgentTerrain();
        myAgent.setId(100L);
        myAgent.setActif(true);
        myAgent.setUtilisateur(userAgent);

        otherAgent = new AgentTerrain();
        otherAgent.setId(200L);
        otherAgent.setActif(true);

        site = new Site();
        site.setId(300L);
        site.setActif(true);
        com.mini.credit.entity.agence.Agence agence = new com.mini.credit.entity.agence.Agence();
        agence.setId(1L);
        site.setAgence(agence);
        myAgent.setSite(site);
        myAgent.setSitesAffectes(new java.util.HashSet<>(List.of(site)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void asAgentConnected() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "agent01",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_AGENT_TERRAIN"))
                )
        );
        when(utilisateurRepository.findByUsername("agent01")).thenReturn(Optional.of(userAgent));
    }

    private void stubConnectedAgentProfile() {
        when(agentTerrainRepository.findByUtilisateurId(10L)).thenReturn(Optional.of(myAgent));
    }

    private CreateRecetteTerrainRequest createRequest(Long agentId, Long siteId, BigDecimal remises, BigDecimal epargne, BigDecimal remb, BigDecimal frais, String obs) {
        return CreateRecetteTerrainRequest.builder()
                .agentTerrainId(agentId)
                .siteId(siteId)
                .dateRecette(LocalDate.now())
                .membresVisites(1)
                .nouveauxMembres(0)
                .carnetDistribues(0)
                .epargneCollectee(epargne)
                .epargneSourceType("VOLONTAIRE")
                .remboursementsCreditCollectes(remb)
                .fraisCollectes(frais)
                .demandesCreditRecueillies(0)
                .especesRemises(remises)
                .especesEmises(BigDecimal.ZERO)
                .observations(obs)
                .build();
    }

    @Test
    void agentConnecteCreate_shouldUseConnectedAgent() {
        asAgentConnected();
        stubConnectedAgentProfile();
        CreateRecetteTerrainRequest request = createRequest(null, null, new BigDecimal("100"), new BigDecimal("60"), new BigDecimal("30"), new BigDecimal("10"), "OK");

        RecetteTerrainJournaliere entity = new RecetteTerrainJournaliere();
        when(siteRepository.findById(300L)).thenReturn(Optional.of(site));
        when(agentTerrainRepository.findById(100L)).thenReturn(Optional.of(myAgent));
        when(recetteRepository.existsByAgentTerrainIdAndDateRecetteAndActifTrue(100L, request.getDateRecette())).thenReturn(false);
        when(recetteMapper.toEntity(request)).thenReturn(entity);
        when(recetteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recetteMapper.toDTO(any())).thenAnswer(inv -> {
            RecetteTerrainJournaliere saved = inv.getArgument(0);
            return RecetteTerrainResponse.builder()
                    .agentTerrainId(saved.getAgentTerrain().getId())
                    .siteId(saved.getSite().getId())
                    .build();
        });

        RecetteTerrainResponse response = service.create(request);
        assertThat(response.getAgentTerrainId()).isEqualTo(100L);
        assertThat(response.getSiteId()).isEqualTo(300L);
    }

    @Test
    void agentCannotCreateForOtherAgent() {
        asAgentConnected();
        stubConnectedAgentProfile();
        CreateRecetteTerrainRequest request = createRequest(200L, 300L, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, "OK");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ne peut pas créer une recette pour un autre agent");
    }

    @Test
    void agentCannotCreateForUnauthorizedSite() {
        asAgentConnected();
        stubConnectedAgentProfile();
        CreateRecetteTerrainRequest request = createRequest(null, 999L, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, "OK");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("site non affecté");
    }

    @Test
    void agentWithoutAssignedSite_shouldBeRejected() {
        asAgentConnected();
        myAgent.setSite(null);
        myAgent.setSitesAffectes(new java.util.HashSet<>());
        stubConnectedAgentProfile();

        CreateRecetteTerrainRequest request = createRequest(null, null, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, "OK");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Aucun site n'est affecté");
    }

    @Test
    void totalCollecte_shouldBeComputedCorrectly() {
        RecetteTerrainResponse dto = RecetteTerrainResponse.builder()
                .epargneCollectee(new BigDecimal("100"))
                .remboursementsCreditCollectes(new BigDecimal("50"))
                .fraisCollectes(new BigDecimal("25"))
                .build();

        assertThat(dto.getTotalCollecte()).isEqualByComparingTo("175");
    }

    @Test
    void ecart_shouldBeComputedCorrectly() {
        RecetteTerrainResponse dto = RecetteTerrainResponse.builder()
                .excedent(new BigDecimal("30"))
                .manquant(null)
                .build();

        assertThat(dto.getEcart()).isEqualByComparingTo("30");
    }

    @Test
    void observationShouldBeRequiredWhenEcartNotZero() {
        asAgentConnected();
        stubConnectedAgentProfile();
        CreateRecetteTerrainRequest request = createRequest(null, 300L, new BigDecimal("120"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, "");

        when(agentTerrainRepository.findById(100L)).thenReturn(Optional.of(myAgent));
        when(siteRepository.findById(300L)).thenReturn(Optional.of(site));
        when(recetteRepository.existsByAgentTerrainIdAndDateRecetteAndActifTrue(100L, request.getDateRecette())).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Observation obligatoire");
    }

    @Test
    void agentCanModifyOnlyOwnBrouillon() {
        asAgentConnected();
        RecetteTerrainJournaliere recette = new RecetteTerrainJournaliere();
        recette.setId(1L);
        recette.setStatut(RecetteStatut.BROUILLON);
        recette.setAgentTerrain(otherAgent);
        when(recetteRepository.findById(1L)).thenReturn(Optional.of(recette));

        UpdateRecetteTerrainRequest request = UpdateRecetteTerrainRequest.builder()
                .membresVisites(1).nouveauxMembres(0).carnetDistribues(0)
                .epargneCollectee(BigDecimal.ZERO).epargneSourceType("VOLONTAIRE")
                .remboursementsCreditCollectes(BigDecimal.ZERO).fraisCollectes(BigDecimal.ZERO)
                .demandesCreditRecueillies(0)
                .especesRemises(BigDecimal.ZERO).especesEmises(BigDecimal.ZERO)
                .observations("ok")
                .build();

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Accès refusé");
    }

    @Test
    void controleurCanValidateOrReject() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR"))
                )
        );

        RecetteTerrainJournaliere recette = new RecetteTerrainJournaliere();
        recette.setId(5L);
        recette.setStatut(RecetteStatut.SOUMISE);
        when(recetteRepository.findById(5L)).thenReturn(Optional.of(recette));
        when(utilisateurRepository.findById(20L)).thenReturn(Optional.of(userControleur));
        when(recetteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recetteMapper.toDTO(any())).thenReturn(RecetteTerrainResponse.builder().statut("VALIDEE").build());

        ValidateRecetteTerrainRequest request = ValidateRecetteTerrainRequest.builder()
                .decision("VALIDEE")
                .validePar(20L)
                .build();

        RecetteTerrainResponse response = service.valider(5L, request);
        assertThat(response.getStatut()).isEqualTo("VALIDEE");
    }

    @Test
    void afterValidation_agentCannotModify() {
        asAgentConnected();
        RecetteTerrainJournaliere recette = new RecetteTerrainJournaliere();
        recette.setId(9L);
        recette.setStatut(RecetteStatut.VALIDEE);
        recette.setAgentTerrain(myAgent);
        when(recetteRepository.findById(9L)).thenReturn(Optional.of(recette));

        UpdateRecetteTerrainRequest request = UpdateRecetteTerrainRequest.builder()
                .membresVisites(1).nouveauxMembres(0).carnetDistribues(0)
                .epargneCollectee(BigDecimal.ZERO).epargneSourceType("VOLONTAIRE")
                .remboursementsCreditCollectes(BigDecimal.ZERO).fraisCollectes(BigDecimal.ZERO)
                .demandesCreditRecueillies(0)
                .especesRemises(BigDecimal.ZERO).especesEmises(BigDecimal.ZERO)
                .observations("ok")
                .build();

        assertThatThrownBy(() -> service.update(9L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot modify recette with status");
    }

            @Test
            void soumettre_shouldCreateControleurWorkflowTask() {
            asAgentConnected();

            RecetteTerrainJournaliere recette = new RecetteTerrainJournaliere();
            recette.setId(101L);
            recette.setStatut(RecetteStatut.BROUILLON);
            recette.setAgentTerrain(myAgent);
            recette.setSite(site);
            recette.setDateRecette(LocalDate.now());
            recette.setMembresVisites(1);
            recette.setEpargneCollectee(BigDecimal.TEN);
            recette.setRemboursementsCreditCollectes(BigDecimal.ZERO);
            recette.setFraisCollectes(BigDecimal.ZERO);
            recette.setEspecesRemises(BigDecimal.TEN);
            recette.setEspecesEmises(BigDecimal.TEN);

            when(recetteRepository.findById(101L)).thenReturn(Optional.of(recette));
            when(recetteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recetteMapper.toDTO(any())).thenReturn(RecetteTerrainResponse.builder().id(101L).statut("SOUMISE").build());

            service.soumettre(101L);

            verify(workflowTaskService).onRecetteSoumise(101L, "RECETTE-101", 1L, 300L);
            }

            @Test
            void valider_shouldCloseControleurTaskWhenDecisionValidee() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR"))
                )
            );

            RecetteTerrainJournaliere recette = new RecetteTerrainJournaliere();
            recette.setId(202L);
            recette.setStatut(RecetteStatut.SOUMISE);
            recette.setSite(site);
            when(recetteRepository.findById(202L)).thenReturn(Optional.of(recette));
            when(utilisateurRepository.findById(20L)).thenReturn(Optional.of(userControleur));
            when(recetteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recetteMapper.toDTO(any())).thenReturn(RecetteTerrainResponse.builder().id(202L).statut("VALIDEE").build());

            ValidateRecetteTerrainRequest request = ValidateRecetteTerrainRequest.builder()
                .decision("VALIDEE")
                .validePar(20L)
                .build();

            service.valider(202L, request);

            verify(workflowTaskService).onRecetteValidee(202L, "RECETTE-202", 1L, 300L);
            }

            @Test
            void valider_shouldCloseControleurTaskWhenDecisionRejetee() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "ctrl01", null, List.of(new SimpleGrantedAuthority("ROLE_CONTROLEUR"))
                )
            );

            RecetteTerrainJournaliere recette = new RecetteTerrainJournaliere();
            recette.setId(203L);
            recette.setStatut(RecetteStatut.SOUMISE);
            recette.setSite(site);
            when(recetteRepository.findById(203L)).thenReturn(Optional.of(recette));
            when(utilisateurRepository.findById(20L)).thenReturn(Optional.of(userControleur));
            when(recetteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recetteMapper.toDTO(any())).thenReturn(RecetteTerrainResponse.builder().id(203L).statut("REJETEE").build());

            ValidateRecetteTerrainRequest request = ValidateRecetteTerrainRequest.builder()
                .decision("REJETEE")
                .motifRejet("Montants incohérents")
                .validePar(20L)
                .build();

            service.valider(203L, request);

            verify(workflowTaskService).onRecetteRejetee(203L, "RECETTE-203", 1L, 300L);
            }
}
