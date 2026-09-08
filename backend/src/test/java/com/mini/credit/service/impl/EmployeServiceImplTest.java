package com.mini.credit.service.impl;

import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.UpdateEmployeRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.mapper.EmployeMapper;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.MatriculeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour EmployeServiceImpl.
 *
 * Depuis la Phase 3 : le matricule est généré automatiquement par MatriculeGeneratorService.
 * Il n'est plus fourni dans la requête de création.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeServiceImpl — Tests de création employé")
class EmployeServiceImplTest {

    @Mock private EmployeRepository employeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AgenceRepository agenceRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private EmployeMapper employeMapper;
    @Mock private MatriculeGeneratorService matriculeGeneratorService;

    @InjectMocks
    private EmployeServiceImpl employeService;

    private Agence agenceTest;
    private Site siteTest;

    @BeforeEach
    void setUp() {
        agenceTest = new Agence();
        agenceTest.setId(1L);
        agenceTest.setNomAgence("Agence Delvaux");
        agenceTest.setCodeAgence("DEL1");

        siteTest = new Site();
        siteTest.setId(1L);
        siteTest.setNomSite("Site Gombe");
        siteTest.setAgence(agenceTest);
    }

    /** Construit une requête de création SANS matricule (auto-généré) */
    private CreateEmployeRequest buildRequest(Long utilisateurId) {
        return CreateEmployeRequest.builder()
                .nom("Falck")
                .prenom("Jean")
                .telephone("+243812345678")
                .adresse("Gombe, Kinshasa")
                .commune("Gombe")
                .fonction(PosteEmploye.GESTIONNAIRE)
                .dateEmbauche(LocalDate.of(2026, 1, 15))
                .salaireBase(new BigDecimal("250000"))
                .primeFixe(BigDecimal.ZERO)
                .bonusVariable(BigDecimal.ZERO)
                .agenceId(1L)
                .siteId(1L)
                .utilisateurId(utilisateurId)
                .build();
    }

    private void setupCommonMocks() {
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
        when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn("DEL1-GES-26-001");
        Employe employe = new Employe();
        employe.setId(1L);
        employe.setMatricule("DEL1-GES-26-001");
        when(employeMapper.toEntity(any())).thenReturn(employe);
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-GES-26-001").build());
    }

    private Employe buildExistingEmploye() {
        Employe employe = new Employe();
        employe.setId(1L);
        employe.setNom("Falck");
        employe.setPrenom("Jean");
        employe.setTelephone("+243812345678");
        employe.setFonction(PosteEmploye.GESTIONNAIRE);
        employe.setAgence(agenceTest);
        employe.setSite(siteTest);
        return employe;
    }

    private void assertCreateNonAgentWithSiteForcesNull(PosteEmploye fonction, String matricule) {
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
        when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn(matricule);
        when(employeMapper.toEntity(any())).thenReturn(new Employe());
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule(matricule).fonction(fonction).build());

        CreateEmployeRequest request = buildRequest(null);
        request.setFonction(fonction);
        request.setSiteId(1L);

        assertThat(employeService.create(request)).isNotNull();

        ArgumentCaptor<Employe> captor = ArgumentCaptor.forClass(Employe.class);
        verify(employeRepository).save(captor.capture());
        assertThat(captor.getValue().getSite()).isNull();
        verify(siteRepository, never()).findById(anyLong());
    }

            private void setupCreateWithoutSiteMocks(PosteEmploye fonction, String matricule) {
            when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
            when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn(matricule);
            when(employeMapper.toEntity(any())).thenReturn(new Employe());
            when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule(matricule).fonction(fonction).build());
            }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests : création de base
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createEmploye_withoutUtilisateur_shouldWork — un employé peut être créé sans compte utilisateur")
    void createEmploye_withoutUtilisateur_shouldWork() {
        setupCommonMocks();

        EmployeDTO result = employeService.create(buildRequest(null));

        assertThat(result).isNotNull();
        verify(utilisateurRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("createEmploye_withUtilisateur_shouldLink — un employé peut être créé avec un compte existant")
    void createEmploye_withUtilisateur_shouldLink() {
        setupCommonMocks();
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(10L);
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        EmployeDTO result = employeService.create(buildRequest(10L));

        assertThat(result).isNotNull();
        verify(utilisateurRepository).findById(10L);
        verify(utilisateurRepository).save(any(Utilisateur.class));
    }

    @Test
    @DisplayName("createEmploye_shouldRequireAgence — exception si agence absente")
    void createEmploye_shouldRequireAgence() {
        when(agenceRepository.findById(99L)).thenReturn(Optional.empty());

        CreateEmployeRequest request = buildRequest(null);
        request.setAgenceId(99L);

        assertThatThrownBy(() -> employeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agence non trouvée");
    }

    @Test
    @DisplayName("createEmploye_siteNotInAgence_shouldThrow — site doit appartenir à l'agence")
    void createEmploye_siteNotInAgence_shouldThrow() {
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
        Agence autreAgence = new Agence();
        autreAgence.setId(99L);
        Site siteAutreAgence = new Site();
        siteAutreAgence.setId(1L);
        siteAutreAgence.setAgence(autreAgence);
        when(siteRepository.findById(1L)).thenReturn(Optional.of(siteAutreAgence));

        CreateEmployeRequest request = buildRequest(null);
        request.setFonction(PosteEmploye.AGENT_TERRAIN);

        assertThatThrownBy(() -> employeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("même agence");
    }

            @Test
            @DisplayName("createEmploye_globalRoleWithoutSite_shouldWork — COO sans site est accepté")
            void createEmploye_globalRoleWithoutSite_shouldWork() {
            when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
            when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn("DEL1-COO-26-001");
            Employe capturedEmploye = new Employe();
            when(employeMapper.toEntity(any())).thenReturn(capturedEmploye);
            when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-COO-26-001").fonction(PosteEmploye.COO).build());

            CreateEmployeRequest request = buildRequest(null);
            request.setFonction(PosteEmploye.COO);
            request.setSiteId(null);

            EmployeDTO result = employeService.create(request);

            assertThat(result).isNotNull();
            verify(siteRepository, never()).findById(anyLong());
            ArgumentCaptor<Employe> captor = ArgumentCaptor.forClass(Employe.class);
            verify(employeRepository).save(captor.capture());
            assertThat(captor.getValue().getSite()).isNull();
            }

            @Test
            @DisplayName("createEmploye_rciWithoutSite_shouldWork — RCI sans site est accepté")
            void createEmploye_rciWithoutSite_shouldWork() {
            when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
            when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn("DEL1-RCI-26-001");
            when(employeMapper.toEntity(any())).thenReturn(new Employe());
            when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-RCI-26-001").fonction(PosteEmploye.RCI).build());

            CreateEmployeRequest request = buildRequest(null);
            request.setFonction(PosteEmploye.RCI);
            request.setSiteId(null);

            assertThat(employeService.create(request)).isNotNull();
            }

            @Test
            @DisplayName("createEmploye_gerantGeneralWithoutSite_shouldWork — GERANT_GENERAL sans site est accepté")
            void createEmploye_gerantGeneralWithoutSite_shouldWork() {
            when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
            when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn("DEL1-GG-26-001");
            when(employeMapper.toEntity(any())).thenReturn(new Employe());
            when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-GG-26-001").fonction(PosteEmploye.GERANT_GENERAL).build());

            CreateEmployeRequest request = buildRequest(null);
            request.setFonction(PosteEmploye.GERANT_GENERAL);
            request.setSiteId(null);

            assertThat(employeService.create(request)).isNotNull();
            }

            @Test
            @DisplayName("creerAgentTerrainSansSite_refuse — AGENT_TERRAIN garde le site obligatoire")
            void createEmploye_agentTerrainWithoutSite_shouldThrow() {
            when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));

            CreateEmployeRequest request = buildRequest(null);
            request.setFonction(PosteEmploye.AGENT_TERRAIN);
            request.setSiteId(null);

            assertThatThrownBy(() -> employeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Le site est obligatoire pour un Agent Terrain.");
            verify(matriculeGeneratorService, never()).generer(any(), any());
            }

            @Test
                @DisplayName("creerAgentTerrainAvecSite_OK — AGENT_TERRAIN avec site est accepté")
                void createEmploye_agentTerrainWithSite_shouldWork() {
                setupCommonMocks();
                when(siteRepository.findById(1L)).thenReturn(Optional.of(siteTest));

                CreateEmployeRequest request = buildRequest(null);
                request.setFonction(PosteEmploye.AGENT_TERRAIN);
                request.setSiteId(1L);

                assertThat(employeService.create(request)).isNotNull();
                verify(siteRepository).findById(1L);
                }

                @Test
                @DisplayName("creerGestionnaireSansSite_OK — GESTIONNAIRE sans site est accepté")
                void createEmploye_gestionnaireWithoutSite_shouldWork() {
                setupCreateWithoutSiteMocks(PosteEmploye.GESTIONNAIRE, "DEL1-GES-26-001");

                CreateEmployeRequest request = buildRequest(null);
                request.setFonction(PosteEmploye.GESTIONNAIRE);
                request.setSiteId(null);

                assertThat(employeService.create(request)).isNotNull();
                verify(siteRepository, never()).findById(anyLong());
                }

                @Test
                @DisplayName("creerControleurSansSite_OK — CONTROLEUR sans site est accepté")
                void createEmploye_controleurWithoutSite_shouldWork() {
                setupCreateWithoutSiteMocks(PosteEmploye.CONTROLEUR, "DEL1-CTR-26-001");

                CreateEmployeRequest request = buildRequest(null);
                request.setFonction(PosteEmploye.CONTROLEUR);
                request.setSiteId(null);

                assertThat(employeService.create(request)).isNotNull();
                verify(siteRepository, never()).findById(anyLong());
                }

                @Test
                @DisplayName("creerCaissierSansSite_OK — CAISSIER sans site est accepté")
                void createEmploye_caissierWithoutSite_shouldWork() {
                setupCreateWithoutSiteMocks(PosteEmploye.CAISSIER, "DEL1-CAI-26-001");

            CreateEmployeRequest request = buildRequest(null);
            request.setFonction(PosteEmploye.CAISSIER);
            request.setSiteId(null);

                assertThat(employeService.create(request)).isNotNull();
                verify(siteRepository, never()).findById(anyLong());
                }

                @Test
                @DisplayName("creerChefBureauSansSite_OK — CHEF_BUREAU sans site est accepté")
                void createEmploye_chefBureauWithoutSite_shouldWork() {
                setupCreateWithoutSiteMocks(PosteEmploye.CHEF_BUREAU, "DEL1-CB-26-001");

                CreateEmployeRequest request = buildRequest(null);
                request.setFonction(PosteEmploye.CHEF_BUREAU);
                request.setSiteId(null);

                assertThat(employeService.create(request)).isNotNull();
                verify(siteRepository, never()).findById(anyLong());
            }

            @Test
            @DisplayName("creerGestionnaireAvecSite_forceSiteNull — un site envoyé pour GESTIONNAIRE est ignoré")
            void createEmploye_gestionnaireWithSite_shouldForceSiteNull() {
            assertCreateNonAgentWithSiteForcesNull(PosteEmploye.GESTIONNAIRE, "DEL1-GES-26-001");
            }

            @Test
            @DisplayName("creerCaissierAvecSite_forceSiteNull — un site envoyé pour CAISSIER est ignoré")
            void createEmploye_caissierWithSite_shouldForceSiteNull() {
            assertCreateNonAgentWithSiteForcesNull(PosteEmploye.CAISSIER, "DEL1-CAI-26-001");
            }

            @Test
            @DisplayName("creerControleurAvecSite_forceSiteNull — un site envoyé pour CONTROLEUR est ignoré")
            void createEmploye_controleurWithSite_shouldForceSiteNull() {
            assertCreateNonAgentWithSiteForcesNull(PosteEmploye.CONTROLEUR, "DEL1-CTR-26-001");
            }

            @Test
            @DisplayName("creerChefBureauAvecSite_forceSiteNull — un site envoyé pour CHEF_BUREAU est ignoré")
            void createEmploye_chefBureauWithSite_shouldForceSiteNull() {
            assertCreateNonAgentWithSiteForcesNull(PosteEmploye.CHEF_BUREAU, "DEL1-CB-26-001");
            }

            @Test
            @DisplayName("modifierAgentTerrainVersGestionnaire_videSite — changement vers non terrain supprime le site")
            void updateEmploye_agentTerrainToGestionnaire_shouldClearSite() {
            Employe employe = buildExistingEmploye();
            employe.setFonction(PosteEmploye.AGENT_TERRAIN);
            employe.setSite(siteTest);
            when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
            doAnswer(inv -> {
                UpdateEmployeRequest request = inv.getArgument(0);
                Employe entity = inv.getArgument(1);
                if (request.getFonction() != null) {
                    entity.setFonction(request.getFonction());
                }
                return null;
            }).when(employeMapper).updateEntityFromDTO(any(UpdateEmployeRequest.class), any(Employe.class));
            when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder().id(1L).fonction(PosteEmploye.GESTIONNAIRE).build());

            UpdateEmployeRequest request = UpdateEmployeRequest.builder()
                .fonction(PosteEmploye.GESTIONNAIRE)
                .siteId(null)
                .build();

            assertThat(employeService.update(1L, request)).isNotNull();
            ArgumentCaptor<Employe> captor = ArgumentCaptor.forClass(Employe.class);
            verify(employeRepository).save(captor.capture());
            assertThat(captor.getValue().getFonction()).isEqualTo(PosteEmploye.GESTIONNAIRE);
            assertThat(captor.getValue().getSite()).isNull();
            verify(siteRepository, never()).findById(anyLong());
            }

            @Test
            @DisplayName("modifierGestionnaireAvecSiteDansPayload_forceSiteNull — payload site ignoré pour non terrain")
            void updateEmploye_gestionnaireWithSitePayload_shouldForceSiteNull() {
            Employe employe = buildExistingEmploye();
            employe.setFonction(PosteEmploye.GESTIONNAIRE);
            employe.setSite(siteTest);
            when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
            when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder().id(1L).fonction(PosteEmploye.GESTIONNAIRE).build());

            UpdateEmployeRequest request = UpdateEmployeRequest.builder()
                .fonction(PosteEmploye.GESTIONNAIRE)
                .siteId(1L)
                .build();

            assertThat(employeService.update(1L, request)).isNotNull();
            ArgumentCaptor<Employe> captor = ArgumentCaptor.forClass(Employe.class);
            verify(employeRepository).save(captor.capture());
            assertThat(captor.getValue().getSite()).isNull();
            verify(siteRepository, never()).findById(anyLong());
            }

            @Test
            @DisplayName("modifierEmployeNonTerrainSansSite_OK — update non terrain sans site est accepté")
            void updateEmploye_nonTerrainWithoutSite_shouldWork() {
            Employe employe = new Employe();
            employe.setId(1L);
            employe.setFonction(PosteEmploye.GESTIONNAIRE);
            employe.setAgence(agenceTest);
            employe.setSite(null);
            when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
            when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
            when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).fonction(PosteEmploye.GESTIONNAIRE).build());

            UpdateEmployeRequest request = UpdateEmployeRequest.builder()
                .fonction(PosteEmploye.GESTIONNAIRE)
                .siteId(null)
                .build();

            assertThat(employeService.update(1L, request)).isNotNull();
            }

            @Test
            @DisplayName("modifierAgentTerrainSansSite_refuse — update Agent Terrain sans site est refusé")
            void updateEmploye_agentTerrainWithoutSite_shouldThrow() {
            Employe employe = new Employe();
            employe.setId(1L);
            employe.setFonction(PosteEmploye.AGENT_TERRAIN);
            employe.setAgence(agenceTest);
            employe.setSite(null);
            when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));

            UpdateEmployeRequest request = UpdateEmployeRequest.builder()
                .fonction(PosteEmploye.AGENT_TERRAIN)
                .siteId(null)
                .build();

            assertThatThrownBy(() -> employeService.update(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Le site est obligatoire pour un Agent Terrain.");
            verify(employeRepository, never()).save(any(Employe.class));
            }

    @Test
    @DisplayName("createEmploye_utilisateurAlreadyLinked_shouldThrow — un utilisateur ne peut être employé deux fois")
    void createEmploye_utilisateurAlreadyLinked_shouldThrow() {
        Employe autreEmploye = new Employe();
        autreEmploye.setId(99L);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(10L);
        utilisateur.setEmploye(autreEmploye);
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> employeService.create(buildRequest(10L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà associé");
    }

    @Test
    @DisplayName("creerEmployeAvecTelephoneDejaUtilise_refuse — téléphone unique dans Employe")
    void createEmploye_withDuplicatePhone_shouldThrow() {
        when(employeRepository.existsByNormalizedTelephoneExcludingId("+243812345678", null)).thenReturn(true);

        assertThatThrownBy(() -> employeService.create(buildRequest(null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ce numéro de téléphone est déjà utilisé.");
        verify(employeRepository, never()).save(any(Employe.class));
    }

    @Test
    @DisplayName("creerEmployeAvecMemePrenomEtNom_refuse — prénom + nom unique dans Employe")
    void createEmploye_withDuplicatePrenomAndNom_shouldThrow() {
        when(employeRepository.existsByNormalizedPrenomAndNomExcludingId("JEAN", "FALCK", null)).thenReturn(true);

        assertThatThrownBy(() -> employeService.create(buildRequest(null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Une personne avec le même prénom et le même nom existe déjà.");
        verify(employeRepository, never()).save(any(Employe.class));
    }

    @Test
    @DisplayName("creerEmployeAvecMemePrenomMaisNomDifferent_OK — prénom seul ne bloque pas")
    void createEmploye_withSamePrenomOnly_shouldWork() {
        setupCommonMocks();
        CreateEmployeRequest request = buildRequest(null);
        request.setNom("Mbala");

        EmployeDTO result = employeService.create(request);

        assertThat(result).isNotNull();
        verify(employeRepository).existsByNormalizedPrenomAndNomExcludingId("JEAN", "MBALA", null);
    }

    @Test
    @DisplayName("creerEmployeAvecMemeNomMaisPrenomDifferent_OK — nom seul ne bloque pas")
    void createEmploye_withSameNomOnly_shouldWork() {
        setupCommonMocks();
        CreateEmployeRequest request = buildRequest(null);
        request.setPrenom("Paul");

        EmployeDTO result = employeService.create(request);

        assertThat(result).isNotNull();
        verify(employeRepository).existsByNormalizedPrenomAndNomExcludingId("PAUL", "FALCK", null);
    }

    @Test
    @DisplayName("comparaisonPrenomNomInsensibleCasseEtEspaces — nom/prénom normalisés")
    void createEmploye_withDifferentCaseAndSpaces_shouldNormalizeBeforeChecking() {
        CreateEmployeRequest request = buildRequest(null);
        request.setPrenom("  jean  ");
        request.setNom("  falck  ");
        request.setTelephone(" +243 812 345 678 ");
        when(employeRepository.existsByNormalizedPrenomAndNomExcludingId("JEAN", "FALCK", null)).thenReturn(true);

        assertThatThrownBy(() -> employeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Une personne avec le même prénom et le même nom existe déjà.");
        verify(employeRepository).existsByNormalizedTelephoneExcludingId("+243812345678", null);
    }

    @Test
    @DisplayName("modifierEmployeSansChangerSonTelephone_OK — son propre téléphone est exclu")
    void updateEmploye_withoutChangingPhone_shouldWork() {
        Employe employe = buildExistingEmploye();
        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder().id(1L).build());

        EmployeDTO result = employeService.update(1L, UpdateEmployeRequest.builder().build());

        assertThat(result).isNotNull();
        verify(employeRepository).existsByNormalizedTelephoneExcludingId("+243812345678", 1L);
    }

    @Test
    @DisplayName("modifierEmployeAvecTelephoneAutreEmploye_refuse — téléphone d'un autre employé bloqué")
    void updateEmploye_withOtherEmployeePhone_shouldThrow() {
        Employe employe = buildExistingEmploye();
        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(employeRepository.existsByNormalizedTelephoneExcludingId("+243899999999", 1L)).thenReturn(true);

        UpdateEmployeRequest request = UpdateEmployeRequest.builder()
                .telephone("+243899999999")
                .build();

        assertThatThrownBy(() -> employeService.update(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ce numéro de téléphone est déjà utilisé.");
        verify(employeRepository, never()).save(any(Employe.class));
    }

    @Test
    @DisplayName("modifierEmployeSansChangerSonPrenomNom_OK — son propre prénom/nom est exclu")
    void updateEmploye_withoutChangingPrenomNom_shouldWork() {
        Employe employe = buildExistingEmploye();
        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder().id(1L).build());

        EmployeDTO result = employeService.update(1L, UpdateEmployeRequest.builder().build());

        assertThat(result).isNotNull();
        verify(employeRepository).existsByNormalizedPrenomAndNomExcludingId("JEAN", "FALCK", 1L);
    }

    @Test
    @DisplayName("modifierEmployeAvecPrenomNomAutreEmploye_refuse — prénom + nom d'un autre employé bloqué")
    void updateEmploye_withOtherEmployeePrenomNom_shouldThrow() {
        Employe employe = buildExistingEmploye();
        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(employeRepository.existsByNormalizedPrenomAndNomExcludingId("MARIE", "MBALA", 1L)).thenReturn(true);

        UpdateEmployeRequest request = UpdateEmployeRequest.builder()
                .prenom("Marie")
                .nom("Mbala")
                .build();

        assertThatThrownBy(() -> employeService.update(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Une personne avec le même prénom et le même nom existe déjà.");
        verify(employeRepository, never()).save(any(Employe.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests : génération automatique du matricule (Phase 3)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createEmploye_shouldCallMatriculeGenerator — le service générateur est invoqué à chaque création")
    void createEmploye_shouldCallMatriculeGenerator() {
        setupCommonMocks();

        employeService.create(buildRequest(null));

        // Vérifier que le générateur a été appelé avec l'agence et la fonction du request
        verify(matriculeGeneratorService).generer(agenceTest, PosteEmploye.GESTIONNAIRE);
    }

    @Test
    @DisplayName("createEmploye_shouldApplyGeneratedMatriculeToEntity — le matricule généré est posé sur l'entité")
    void createEmploye_shouldApplyGeneratedMatriculeToEntity() {
        // Mocks complets sans passer par setupCommonMocks (évite les stubs redondants)
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
        when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn("DEL1-GES-26-042");
        Employe capturedEmploye = new Employe();
        when(employeMapper.toEntity(any())).thenReturn(capturedEmploye);
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-GES-26-042").build());

        employeService.create(buildRequest(null));

        // L'entité sauvegardée doit porter le matricule généré
        ArgumentCaptor<Employe> captor = ArgumentCaptor.forClass(Employe.class);
        verify(employeRepository).save(captor.capture());
        assertThat(captor.getValue().getMatricule()).isEqualTo("DEL1-GES-26-042");
    }

    @Test
    @DisplayName("createEmploye_shouldReturnGeneratedMatriculeInDTO — le DTO retourné contient le matricule auto-généré")
    void createEmploye_shouldReturnGeneratedMatriculeInDTO() {
        // Mocks complets sans passer par setupCommonMocks
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
        when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn("DEL1-GES-26-001");
        when(employeMapper.toEntity(any())).thenReturn(new Employe());
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any())).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-GES-26-001").build());

        EmployeDTO result = employeService.create(buildRequest(null));

        assertThat(result.getMatricule()).isEqualTo("DEL1-GES-26-001");
    }

    @Test
    @DisplayName("createEmploye_shouldNeverCheckExistsByMatricule — la vérification manuelle du matricule est supprimée")
    void createEmploye_shouldNeverCheckExistsByMatricule() {
        setupCommonMocks();

        employeService.create(buildRequest(null));

        // existsByMatricule ne doit plus jamais être appelé
        verify(employeRepository, never()).existsByMatricule(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests : compatibilité code_employe (Option B — alias = matricule)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createEmploye_shouldSetCodeEmployeEqualToMatricule — code_employe reçoit la valeur du matricule généré")
    void createEmploye_shouldSetCodeEmployeEqualToMatricule() {
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
        when(matriculeGeneratorService.generer(any(Agence.class), any(PosteEmploye.class)))
                .thenReturn("DEL1-GES-26-001");
        Employe capturedEmploye = new Employe();
        when(employeMapper.toEntity(any())).thenReturn(capturedEmploye);
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-GES-26-001").build());

        employeService.create(buildRequest(null));

        ArgumentCaptor<Employe> captor = ArgumentCaptor.forClass(Employe.class);
        verify(employeRepository).save(captor.capture());
        // Compatibilité Option B : code_employe doit être égal au matricule
        assertThat(captor.getValue().getCode_employe()).isEqualTo("DEL1-GES-26-001");
        assertThat(captor.getValue().getMatricule()).isEqualTo("DEL1-GES-26-001");
    }

    @Test
    @DisplayName("createEmploye_shouldWorkWithoutCodeEmployeInRequest — CreateEmployeRequest n'a pas de champ code_employe")
    void createEmploye_shouldWorkWithoutCodeEmployeInRequest() {
        setupCommonMocks();

        // La requête ne contient pas de champ code_employe — aucune exception attendue
        EmployeDTO result = employeService.create(buildRequest(null));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("employe_shouldUseMatriculeAsMainReference — matricule est la référence unique principale affichée")
    void employe_shouldUseMatriculeAsMainReference() {
        setupCommonMocks();

        EmployeDTO result = employeService.create(buildRequest(null));

        // Le DTO ne renvoie que matricule (pas de code_employe dans EmployeDTO)
        assertThat(result.getMatricule()).isEqualTo("DEL1-GES-26-001");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests : affichage poste métier — non-confusion fonction / rôle applicatif
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("employeResponse_shouldExposeFonctionGestionnaire — EmployeDTO.fonction doit être GESTIONNAIRE")
    void employeResponse_shouldExposeFonctionGestionnaire() {
        // Le mapper retourne bien la fonction métier stockée dans l'entité
        com.mini.credit.mapper.EmployeMapper realMapper = new com.mini.credit.mapper.EmployeMapper();
        Employe employe = new Employe();
        employe.setId(1L);
        employe.setMatricule("DEL1-GES-26-001");
        employe.setNom("Kalumbo");
        employe.setPrenom("aim\u00e9");
        employe.setNomComplet("aim\u00e9 Kalumbo Nyamabo");
        employe.setFonction(PosteEmploye.GESTIONNAIRE);
        employe.setActif(true);

        EmployeDTO dto = realMapper.toDTO(employe);

        assertThat(dto.getFonction()).isEqualTo(PosteEmploye.GESTIONNAIRE);
        assertThat(dto.getFonction()).as("La fonction doit \u00eatre GESTIONNAIRE")
                .isNotEqualTo(PosteEmploye.AGENT_TERRAIN);
    }

    @Test
    @DisplayName("mapperNonAgentTerrainNeRetournePasSite — site masqué pour non terrain")
    void mapperNonAgentTerrain_shouldNotReturnSite() {
        com.mini.credit.mapper.EmployeMapper realMapper = new com.mini.credit.mapper.EmployeMapper();
        Employe employe = buildExistingEmploye();
        employe.setFonction(PosteEmploye.GESTIONNAIRE);
        employe.setSite(siteTest);

        EmployeDTO dto = realMapper.toDTO(employe);

        assertThat(dto.getSiteId()).isNull();
        assertThat(dto.getNomSite()).isNull();
    }

    @Test
    @DisplayName("mapperAgentTerrainRetourneSite — site visible pour Agent Terrain")
    void mapperAgentTerrain_shouldReturnSite() {
        com.mini.credit.mapper.EmployeMapper realMapper = new com.mini.credit.mapper.EmployeMapper();
        Employe employe = buildExistingEmploye();
        employe.setFonction(PosteEmploye.AGENT_TERRAIN);
        employe.setSite(siteTest);

        EmployeDTO dto = realMapper.toDTO(employe);

        assertThat(dto.getSiteId()).isEqualTo(1L);
        assertThat(dto.getNomSite()).isEqualTo("Site Gombe");
    }

    @Test
    @DisplayName("employeList_shouldNotExposeRoleUtilisateurAsPoste — roleUtilisateur et fonction sont deux champs distincts")
    void employeList_shouldNotExposeRoleUtilisateurAsPoste() {
        // Arrange : un employé GESTIONNAIRE lié à un utilisateur avec rôle GESTIONNAIRE
        com.mini.credit.mapper.EmployeMapper realMapper = new com.mini.credit.mapper.EmployeMapper();
        Role role = new Role();
        role.setCode(RoleCode.GESTIONNAIRE);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(10L);
        utilisateur.setUsername("gest_01");
        utilisateur.setRole(role);

        Employe employe = new Employe();
        employe.setId(1L);
        employe.setMatricule("DEL1-GES-26-001");
        employe.setFonction(PosteEmploye.GESTIONNAIRE);
        employe.setUtilisateur(utilisateur);
        employe.setActif(true);

        // Act
        EmployeDTO dto = realMapper.toDTO(employe);

        // Assert : fonction métier = GESTIONNAIRE, rôle applicatif = GESTIONNAIRE
        assertThat(dto.getFonction())
                .as("La colonne Poste doit afficher la fonction m\u00e9tier (GESTIONNAIRE)")
                .isEqualTo(PosteEmploye.GESTIONNAIRE);
        assertThat(dto.getRoleUtilisateur())
                .as("Le champ roleUtilisateur porte le r\u00f4le applicatif technique")
                .isEqualTo("GESTIONNAIRE");
    }

    @Test
    @DisplayName("matriculeGestionnaire_shouldMatchFonctionGestionnaire — DEL1-GES-26-001 confirme la fonction GESTIONNAIRE")
    void matriculeGestionnaire_shouldMatchFonctionGestionnaire() {
        // Le segment "GES" dans DEL1-GES-26-001 correspond à PosteEmploye.GESTIONNAIRE
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agenceTest));
        when(matriculeGeneratorService.generer(agenceTest, PosteEmploye.GESTIONNAIRE))
                .thenReturn("DEL1-GES-26-001");
        Employe employe = new Employe();
        employe.setId(1L);
        employe.setFonction(PosteEmploye.GESTIONNAIRE);
        when(employeMapper.toEntity(any())).thenReturn(employe);
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(
                EmployeDTO.builder().id(1L).matricule("DEL1-GES-26-001")
                        .fonction(PosteEmploye.GESTIONNAIRE).build());

        EmployeDTO result = employeService.create(buildRequest(null));

        assertThat(result.getMatricule()).contains("GES");
        assertThat(result.getFonction()).isEqualTo(PosteEmploye.GESTIONNAIRE);
    }

    // =========================================================
    // Tests getEmployesByAgence
    // =========================================================

    @Test
    @DisplayName("getEmployesByAgence : retourne les employés actifs de l'agence")
    void getEmployesByAgence_shouldReturnEmployesOfAgence() {
        Employe e1 = new Employe();
        e1.setId(1L); e1.setNomComplet("Aimé Kalumbo");
        e1.setFonction(PosteEmploye.GESTIONNAIRE); e1.setAgence(agenceTest); e1.setSite(siteTest);
        Employe e2 = new Employe();
        e2.setId(2L); e2.setNomComplet("Paul Mbuyi");
        e2.setFonction(PosteEmploye.AGENT_TERRAIN); e2.setAgence(agenceTest); e2.setSite(siteTest);

        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(employeRepository.findByAgenceIdAndActifTrue(1L)).thenReturn(java.util.List.of(e1, e2));
        when(employeMapper.toDTO(e1)).thenReturn(EmployeDTO.builder().id(1L).nomComplet("Aimé Kalumbo").agenceId(1L).fonction(PosteEmploye.GESTIONNAIRE).build());
        when(employeMapper.toDTO(e2)).thenReturn(EmployeDTO.builder().id(2L).nomComplet("Paul Mbuyi").agenceId(1L).fonction(PosteEmploye.AGENT_TERRAIN).build());

        java.util.List<EmployeDTO> result = employeService.getEmployesByAgence(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> Long.valueOf(1L).equals(e.getAgenceId()));
    }

    @Test
    @DisplayName("getEmployesByAgence : agence inexistante → exception")
    void getEmployesByAgence_withUnknownAgence_shouldThrowException() {
        when(agenceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> employeService.getEmployesByAgence(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agence non trouvée");
    }

    @Test
    @DisplayName("getEmployesByAgence : ne retourne pas les employés d'une autre agence")
    void getEmployesByAgence_shouldExcludeOtherAgences() {
        // Seul l'agence 1 est requêtée — on mocke une liste avec 1 élément
        Employe e1 = new Employe();
        e1.setId(1L); e1.setAgence(agenceTest); e1.setSite(siteTest);

        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(employeRepository.findByAgenceIdAndActifTrue(1L)).thenReturn(java.util.List.of(e1));
        when(employeMapper.toDTO(e1)).thenReturn(EmployeDTO.builder().id(1L).agenceId(1L).build());

        java.util.List<EmployeDTO> result = employeService.getEmployesByAgence(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAgenceId()).isEqualTo(1L);
        // Le repo est appelé uniquement avec l'agenceId cible (par déf. Spring Data isole)
        verify(employeRepository).findByAgenceIdAndActifTrue(1L);
    }

    @Test
    @DisplayName("getEmployesByAgence : expose bien le champ fonction et matricule")
    void getEmployesByAgence_shouldReturnFonctionAndMatricule() {
        Employe e1 = new Employe();
        e1.setId(1L); e1.setMatricule("DEL1-CON-26-001");
        e1.setFonction(PosteEmploye.CONTROLEUR); e1.setAgence(agenceTest); e1.setSite(siteTest);

        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(employeRepository.findByAgenceIdAndActifTrue(1L)).thenReturn(java.util.List.of(e1));
        when(employeMapper.toDTO(e1)).thenReturn(EmployeDTO.builder()
                .id(1L).matricule("DEL1-CON-26-001").fonction(PosteEmploye.CONTROLEUR).agenceId(1L).build());

        java.util.List<EmployeDTO> result = employeService.getEmployesByAgence(1L);

        assertThat(result.get(0).getMatricule()).isEqualTo("DEL1-CON-26-001");
        assertThat(result.get(0).getFonction()).isEqualTo(PosteEmploye.CONTROLEUR);
    }

    @Test
    @DisplayName("getEmployesByAgence : agence sans employé → liste vide")
    void getEmployesByAgence_withNoEmployes_shouldReturnEmptyList() {
        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(employeRepository.findByAgenceIdAndActifTrue(1L)).thenReturn(java.util.List.of());

        java.util.List<EmployeDTO> result = employeService.getEmployesByAgence(1L);

        assertThat(result).isEmpty();
    }

    // =========================================================
    // Tests changerAgence
    // =========================================================

    @Test
    @DisplayName("changerAgence_shouldSetAgenceAndSite — l'employé est transféré vers la nouvelle agence")
    void changerAgence_shouldSetAgenceAndSite() {
        Agence nouvelleAgence = new Agence();
        nouvelleAgence.setId(2L);
        nouvelleAgence.setNomAgence("Agence Matete");

        Site nouveauSite = new Site();
        nouveauSite.setId(20L);
        nouveauSite.setNomSite("Site Matete");
        nouveauSite.setAgence(nouvelleAgence); // site appartient à la nouvelle agence

        Employe employe = new Employe();
        employe.setId(10L);
        employe.setAgence(agenceTest);
        employe.setSite(siteTest);

        when(employeRepository.findById(10L)).thenReturn(Optional.of(employe));
        when(agenceRepository.findById(2L)).thenReturn(Optional.of(nouvelleAgence));
        when(siteRepository.findById(20L)).thenReturn(Optional.of(nouveauSite));
        when(employeRepository.save(any(Employe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeMapper.toDTO(any(Employe.class))).thenReturn(
                EmployeDTO.builder().id(10L).agenceId(2L).siteId(20L).build());

        EmployeDTO result = employeService.changerAgence(10L, 2L, 20L);

        assertThat(result.getAgenceId()).isEqualTo(2L);
        assertThat(result.getSiteId()).isEqualTo(20L);
        ArgumentCaptor<Employe> captor = ArgumentCaptor.forClass(Employe.class);
        verify(employeRepository).save(captor.capture());
        assertThat(captor.getValue().getAgence().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getSite().getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("changerAgence_shouldRejectIfEmployeNotFound — exception si employé inexistant")
    void changerAgence_shouldRejectIfEmployeNotFound() {
        when(employeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeService.changerAgence(999L, 2L, 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employé non trouvé");
    }

    @Test
    @DisplayName("changerAgence_shouldRejectIfAgenceNotFound — exception si nouvelle agence inexistante")
    void changerAgence_shouldRejectIfAgenceNotFound() {
        Employe employe = new Employe();
        employe.setId(10L);
        when(employeRepository.findById(10L)).thenReturn(Optional.of(employe));
        when(agenceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeService.changerAgence(10L, 999L, 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agence non trouvée");
    }

    @Test
    @DisplayName("changerAgence_shouldRejectIfSiteNotInTargetAgence — le site doit appartenir à la nouvelle agence")
    void changerAgence_shouldRejectIfSiteNotInTargetAgence() {
        Agence nouvelleAgence = new Agence();
        nouvelleAgence.setId(2L);

        // Le site appartient à l'agence 1, pas à l'agence 2
        Site siteAutreAgence = new Site();
        siteAutreAgence.setId(20L);
        siteAutreAgence.setAgence(agenceTest); // agence 1, pas 2

        Employe employe = new Employe();
        employe.setId(10L);
        when(employeRepository.findById(10L)).thenReturn(Optional.of(employe));
        when(agenceRepository.findById(2L)).thenReturn(Optional.of(nouvelleAgence));
        when(siteRepository.findById(20L)).thenReturn(Optional.of(siteAutreAgence));

        assertThatThrownBy(() -> employeService.changerAgence(10L, 2L, 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("doit appartenir");
    }

    // =========================================================
    // Tests getEmployesAAffecterAgence
    // =========================================================

    @Test
    @DisplayName("getEmployesAAffecterAgence_shouldExcludeEmployesAlreadyInAgence — seuls les employés hors agence sont retournés")
    void getEmployesAAffecterAgence_shouldExcludeEmployesAlreadyInAgence() {
        Agence autreAgence = new Agence();
        autreAgence.setId(2L);
        autreAgence.setNomAgence("Agence Matete");

        Employe e1 = new Employe();
        e1.setId(10L); e1.setNomComplet("Marie Luzia"); e1.setAgence(autreAgence);
        Employe e2 = new Employe();
        e2.setId(11L); e2.setNomComplet("Paul Mbuyi"); e2.setAgence(autreAgence);

        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(employeRepository.findByAgenceIdNotAndActifTrue(1L)).thenReturn(java.util.List.of(e1, e2));
        when(employeMapper.toDTO(e1)).thenReturn(EmployeDTO.builder().id(10L).agenceId(2L).nomComplet("Marie Luzia").build());
        when(employeMapper.toDTO(e2)).thenReturn(EmployeDTO.builder().id(11L).agenceId(2L).nomComplet("Paul Mbuyi").build());

        java.util.List<EmployeDTO> result = employeService.getEmployesAAffecterAgence(1L);

        assertThat(result).hasSize(2);
        // Aucun des résultats ne doit appartenir à l'agence 1
        assertThat(result).noneMatch(e -> Long.valueOf(1L).equals(e.getAgenceId()));
        verify(employeRepository).findByAgenceIdNotAndActifTrue(1L);
    }

    @Test
    @DisplayName("getEmployesAAffecterAgence_shouldReturnEmployesFromOtherAgences — regroupe les employés de toutes les autres agences")
    void getEmployesAAffecterAgence_shouldReturnEmployesFromOtherAgences() {
        Agence agence2 = new Agence(); agence2.setId(2L);
        Agence agence3 = new Agence(); agence3.setId(3L);

        Employe empAgence2 = new Employe(); empAgence2.setId(20L); empAgence2.setAgence(agence2);
        Employe empAgence3 = new Employe(); empAgence3.setId(30L); empAgence3.setAgence(agence3);

        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(employeRepository.findByAgenceIdNotAndActifTrue(1L)).thenReturn(java.util.List.of(empAgence2, empAgence3));
        when(employeMapper.toDTO(empAgence2)).thenReturn(EmployeDTO.builder().id(20L).agenceId(2L).build());
        when(employeMapper.toDTO(empAgence3)).thenReturn(EmployeDTO.builder().id(30L).agenceId(3L).build());

        java.util.List<EmployeDTO> result = employeService.getEmployesAAffecterAgence(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EmployeDTO::getAgenceId)
                .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("getEmployesAAffecterAgence_shouldRejectUnknownAgence — agence inexistante lève une exception")
    void getEmployesAAffecterAgence_shouldRejectUnknownAgence() {
        when(agenceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> employeService.getEmployesAAffecterAgence(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agence non trouvée");
    }

    @Test
    @DisplayName("getEmployesAAffecterAgence_shouldReturnEmptyWhenAllEmployesInAgence — liste vide si tous sont déjà dans l'agence")
    void getEmployesAAffecterAgence_shouldReturnEmptyWhenAllEmployesInAgence() {
        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(employeRepository.findByAgenceIdNotAndActifTrue(1L)).thenReturn(java.util.List.of());

        java.util.List<EmployeDTO> result = employeService.getEmployesAAffecterAgence(1L);

        assertThat(result).isEmpty();
    }
}

