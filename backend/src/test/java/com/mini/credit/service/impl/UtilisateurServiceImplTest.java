package com.mini.credit.service.impl;

import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.UtilisateurDTO;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.service.audit.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UtilisateurServiceImpl.
 *
 * Couvre les scénarios critiques pour les tests E2E manuels :
 * - mot de passe encodé en BCrypt avant sauvegarde
 * - utilisateur actif/enabled par défaut
 * - passwordResetRequired = false (connexion immédiate possible)
 * - rôle correctement assigné
 * - email optionnel (pas de blocage si absent)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UtilisateurServiceImpl — Tests de création utilisateur")
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmployeRepository employeRepository;

    @Mock
    private AgentTerrainRepository agentTerrainRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

        @Mock
        private AuditService auditService;

    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;

    // ── Helpers ────────────────────────────────────────────────────────────

    private Role buildRole(RoleCode code) {
        Role role = new Role();
        role.setCode(code);
        return role;
    }

    private Utilisateur captureUtilisateurSaved() {
        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        return captor.getValue();
    }

    private void setupMocksForCreate(RoleCode roleCode) {
        when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED_PASSWORD");
        when(roleRepository.findByCode(roleCode)).thenReturn(Optional.of(buildRole(roleCode)));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Employe actif sans compte utilisateur -- satisfait les validations métier */
    private Employe buildActiveEmploye(Long id, com.mini.credit.enums.PosteEmploye fonction) {
        Employe e = new Employe();
        e.setId(id);
        e.setNomComplet("Employe Test " + id);
        e.setTelephone("+243810000000");
        e.setFonction(fonction);
        e.setActif(true);
        // pas d'utilisateur lie (disponible)
        return e;
    }

        private Site buildSite(Long id) {
                Site site = new Site();
                site.setId(id);
                site.setNomSite("SITE " + id);
                return site;
        }

        private void assertCreateUserWithEmployeWithoutSiteWorks(RoleCode roleCode, com.mini.credit.enums.PosteEmploye fonction) {
                setupMocksForCreate(roleCode);
                Employe employe = buildActiveEmploye(100L + roleCode.ordinal(), fonction);
                employe.setSite(null);
                when(employeRepository.findById(employe.getId())).thenReturn(Optional.of(employe));

                CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                                .username("user." + roleCode.name().toLowerCase())
                                .password("Pass2026!")
                                .roles(List.of(roleCode.name()))
                                .employeId(employe.getId())
                                .build();

                UtilisateurDTO result = utilisateurService.create(request);

                assertThat(result).isNotNull();
                assertThat(captureUtilisateurSaved().getEmploye()).isSameAs(employe);
        }

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser_shouldEncodePassword — le mot de passe est encodé avec PasswordEncoder avant sauvegarde")
    void createUser_shouldEncodePassword() {
        // ADMIN : pas besoin d'employé lié (vérifie le comportement générique d'encodage)
        setupMocksForCreate(RoleCode.ADMIN);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("falck")
                .password("MonMotDePasse123")
                .nomComplet("Admin Technique")
                .roles(List.of("ADMIN"))
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        // Vérifie que PasswordEncoder.encode() a été appelé avec le mot de passe brut
        verify(passwordEncoder).encode("MonMotDePasse123");
        // Vérifie que le hash est stocké (pas le mot de passe brut)
        assertThat(saved.getMotDePasseHash()).isEqualTo("$2a$10$HASHED_PASSWORD");
        assertThat(saved.getMotDePasseHash()).isNotEqualTo("MonMotDePasse123");
    }

    @Test
    @DisplayName("createUser_shouldBeEnabledByDefault — l'utilisateur créé est actif, enabled et non verrouillé")
    void createUser_shouldBeEnabledByDefault() {
        // ADMIN : test du comportement générique (actif/enabled/non-verrouillé)
        setupMocksForCreate(RoleCode.ADMIN);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("caissier01")
                .password("pass123")
                .nomComplet("Admin Test")
                .roles(List.of("ADMIN"))
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        assertThat(saved.getActif()).as("actif doit être true").isTrue();
        assertThat(saved.getIsEnabled()).as("isEnabled doit être true").isTrue();
        assertThat(saved.getIsLocked()).as("isLocked doit être false").isFalse();
    }

    @Test
    @DisplayName("createUser_passwordResetRequired_false — connexion immédiate possible sans workflow email")
    void createUser_passwordResetRequired_shouldBeFalse() {
        // ADMIN : test du comportement générique (passwordResetRequired = false)
        setupMocksForCreate(RoleCode.ADMIN);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("chefbureau")
                .password("ChefBureau2026!")
                .nomComplet("Admin Test")
                .roles(List.of("ADMIN"))
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        // CORRECTIF du bug : passwordResetRequired doit être false
        // Sinon AuthService.login() refuse la connexion avec "Compte non activé"
        assertThat(saved.getPasswordResetRequired())
                .as("passwordResetRequired doit être false pour permettre la connexion immédiate")
                .isFalse();
    }

    @Test
    @DisplayName("createUser_shouldAssignRole — le rôle envoyé par le frontend est correctement assigné")
    void createUser_shouldAssignRole() {
        setupMocksForCreate(RoleCode.CONTROLEUR);
        when(employeRepository.findById(1L)).thenReturn(Optional.of(
                buildActiveEmploye(1L, com.mini.credit.enums.PosteEmploye.CONTROLEUR)));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("controleur01")
                .password("ctrl2026!")
                .nomComplet("Contrôleur Test")
                .roles(List.of("CONTROLEUR"))
                .employeId(1L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        assertThat(saved.getRole()).isNotNull();
        assertThat(saved.getRole().getCode()).isEqualTo(RoleCode.CONTROLEUR);
    }

    @Test
    @DisplayName("createUser_withoutEmail_shouldWork — la création sans email ne doit pas échouer")
    void createUser_withoutEmail_shouldWork() {
        // ADMIN : test de création sans email (comportement générique, pas lié au rôle)
        setupMocksForCreate(RoleCode.ADMIN);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("agenterrain01")
                .password("terrain2026")
                .nomComplet("Admin Test")
                .roles(List.of("ADMIN"))
                // email intentionnellement absent
                .build();

        // Ne doit pas lever d'exception
        UtilisateurDTO result = utilisateurService.create(request);
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("agenterrain01");
    }

        @Test
        @DisplayName("createUser_agentTerrain_shouldSynchronizeAgentTerrain — visible sans redémarrage backend")
        void createUser_agentTerrain_shouldSynchronizeAgentTerrain() {
                when(utilisateurRepository.existsByUsername("agent.site10")).thenReturn(false);
                when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED_PASSWORD");
                when(roleRepository.findByCode(RoleCode.AGENT_TERRAIN)).thenReturn(Optional.of(buildRole(RoleCode.AGENT_TERRAIN)));

                Site site = buildSite(10L);
                Employe employe = buildActiveEmploye(1L, com.mini.credit.enums.PosteEmploye.AGENT_TERRAIN);
                employe.setMatricule("MAT-AT-001");
                employe.setSite(site);
                when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
                when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> {
                        Utilisateur utilisateur = inv.getArgument(0);
                        utilisateur.setId(2L);
                        return utilisateur;
                });
                when(agentTerrainRepository.findByUtilisateurId(2L)).thenReturn(Optional.empty());

                CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                                .username("agent.site10")
                                .password("terrain2026")
                                .roles(List.of("AGENT_TERRAIN"))
                                .employeId(1L)
                                .build();

                utilisateurService.create(request);

                ArgumentCaptor<AgentTerrain> captor = ArgumentCaptor.forClass(AgentTerrain.class);
                verify(agentTerrainRepository).save(captor.capture());
                AgentTerrain agent = captor.getValue();
                assertThat(agent.getUtilisateur().getId()).isEqualTo(2L);
                assertThat(agent.getSite().getId()).isEqualTo(10L);
                assertThat(agent.getMatricule()).isEqualTo("MAT-AT-001");
                assertThat(agent.getActif()).isTrue();
        }

    @Test
    @DisplayName("createUser_usernameAlreadyExists_shouldThrow — doublon username refusé")
    void createUser_usernameAlreadyExists_shouldThrow() {
        when(utilisateurRepository.existsByUsername("admin")).thenReturn(true);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("admin")
                .password("admin123")
                .nomComplet("Admin Test")
                .roles(List.of("ADMIN"))
                .build();

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("createUser_passwordEncoding_bcryptRealEncoder — vérification BCrypt réel")
    void createUser_passwordEncoding_bcryptRealEncoder() {
        // Test d'intégration léger : vérifier que BCrypt peut matcher le mot de passe encodé
        PasswordEncoder realEncoder = new BCryptPasswordEncoder();
        String rawPassword = "MonMotDePasseTest2026!";
        String encoded = realEncoder.encode(rawPassword);

        assertThat(realEncoder.matches(rawPassword, encoded))
                .as("BCrypt doit valider le mot de passe brut contre le hash")
                .isTrue();
        assertThat(realEncoder.matches("mauvaisMotDePasse", encoded))
                .as("BCrypt doit rejeter un mot de passe incorrect")
                .isFalse();
    }

    @Test
    @DisplayName("createUser_rciRole_shouldWork — création d'un utilisateur RCI")
    void createUser_rciRole_shouldWork() {
        setupMocksForCreate(RoleCode.RCI);
        when(employeRepository.findById(2L)).thenReturn(Optional.of(
                buildActiveEmploye(2L, com.mini.credit.enums.PosteEmploye.RCI)));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("rci01")
                .password("rci2026!")
                .nomComplet("RCI Test")
                .roles(List.of("RCI"))
                .employeId(2L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        assertThat(saved.getRole().getCode()).isEqualTo(RoleCode.RCI);
        assertThat(saved.getPasswordResetRequired()).isFalse();
        assertThat(saved.getActif()).isTrue();
    }

        @Test
        @DisplayName("createUser_cooWithoutEmploye_shouldWork — COO global sans employé lié")
        void createUser_cooWithoutEmploye_shouldWork() {
                setupMocksForCreate(RoleCode.COO);

                CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                                .username("coo01")
                                .password("Coo2026!")
                                .nomComplet("COO Test")
                                .roles(List.of("COO"))
                                .build();

                UtilisateurDTO result = utilisateurService.create(request);

                assertThat(result).isNotNull();
                assertThat(result.getEmployeId()).isNull();
                verify(employeRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("createUser_rciWithoutEmploye_shouldWork — RCI global sans employé lié")
        void createUser_rciWithoutEmploye_shouldWork() {
                setupMocksForCreate(RoleCode.RCI);

                CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                                .username("rci.global")
                                .password("Rci2026!")
                                .nomComplet("RCI Global")
                                .roles(List.of("RCI"))
                                .build();

                UtilisateurDTO result = utilisateurService.create(request);

                assertThat(result).isNotNull();
                assertThat(result.getEmployeId()).isNull();
                verify(employeRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("createUser_gerantGeneralWithoutEmploye_shouldWork — GERANT_GENERAL global sans employé lié")
        void createUser_gerantGeneralWithoutEmploye_shouldWork() {
                setupMocksForCreate(RoleCode.GERANT_GENERAL);

                CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                                .username("gerant.general")
                                .password("Gerant2026!")
                                .nomComplet("Gérant Général")
                                .roles(List.of("GERANT_GENERAL"))
                                .build();

                UtilisateurDTO result = utilisateurService.create(request);

                assertThat(result).isNotNull();
                assertThat(result.getEmployeId()).isNull();
                verify(employeRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("createUser_caissierWithoutEmploye_shouldThrow — CAISSIER garde l'employé obligatoire")
        void createUser_caissierWithoutEmploye_shouldThrow() {
                when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);
                when(roleRepository.findByCode(RoleCode.CAISSIER)).thenReturn(Optional.of(buildRole(RoleCode.CAISSIER)));

                CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                                .username("caissier.sans.employe")
                                .password("Caisse2026!")
                                .roles(List.of("CAISSIER"))
                                .build();

                assertThatThrownBy(() -> utilisateurService.create(request))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("employé lié est obligatoire");
        }

    @Test
    @DisplayName("createUser_withEmployeId_shouldLinkEmploye — utilisateur lié à un employé existant")
    void createUser_withEmployeId_shouldLinkEmploye() {
        setupMocksForCreate(RoleCode.AGENT_TERRAIN);

        Employe employe = buildActiveEmploye(5L, com.mini.credit.enums.PosteEmploye.AGENT_TERRAIN);
                employe.setSite(buildSite(20L));
        when(employeRepository.findById(5L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("falck.jean")
                .password("Terrain2026!")
                .roles(List.of("AGENT_TERRAIN"))
                .employeId(5L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        assertThat(saved.getEmploye()).isNotNull();
        assertThat(saved.getEmploye().getId()).isEqualTo(5L);
    }

        @Test
        @DisplayName("creerUtilisateurAgentTerrainEmployeSansSite_refuse — AGENT_TERRAIN lié à un employé sans site est refusé")
        void createUser_agentTerrainEmployeWithoutSite_shouldThrow() {
                when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);
                when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED_PASSWORD");
                when(roleRepository.findByCode(RoleCode.AGENT_TERRAIN)).thenReturn(Optional.of(buildRole(RoleCode.AGENT_TERRAIN)));
                Employe employe = buildActiveEmploye(6L, com.mini.credit.enums.PosteEmploye.AGENT_TERRAIN);
                employe.setSite(null);
                when(employeRepository.findById(6L)).thenReturn(Optional.of(employe));

                CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                                .username("terrain.sans.site")
                                .password("Terrain2026!")
                                .roles(List.of("AGENT_TERRAIN"))
                                .employeId(6L)
                                .build();

                assertThatThrownBy(() -> utilisateurService.create(request))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Le site est obligatoire pour un Agent Terrain.");
                verify(utilisateurRepository, never()).save(any(Utilisateur.class));
        }

        @Test
        @DisplayName("creerGestionnaireSansSite_OK — GESTIONNAIRE lié à un employé sans site est accepté")
        void createUser_gestionnaireEmployeWithoutSite_shouldWork() {
                assertCreateUserWithEmployeWithoutSiteWorks(RoleCode.GESTIONNAIRE, com.mini.credit.enums.PosteEmploye.GESTIONNAIRE);
        }

        @Test
        @DisplayName("creerControleurSansSite_OK — CONTROLEUR lié à un employé sans site est accepté")
        void createUser_controleurEmployeWithoutSite_shouldWork() {
                assertCreateUserWithEmployeWithoutSiteWorks(RoleCode.CONTROLEUR, com.mini.credit.enums.PosteEmploye.CONTROLEUR);
        }

        @Test
        @DisplayName("creerCaissierSansSite_OK — CAISSIER lié à un employé sans site est accepté")
        void createUser_caissierEmployeWithoutSite_shouldWork() {
                assertCreateUserWithEmployeWithoutSiteWorks(RoleCode.CAISSIER, com.mini.credit.enums.PosteEmploye.CAISSIER);
        }

        @Test
        @DisplayName("creerChefBureauSansSite_OK — CHEF_BUREAU lié à un employé sans site est accepté")
        void createUser_chefBureauEmployeWithoutSite_shouldWork() {
                assertCreateUserWithEmployeWithoutSiteWorks(RoleCode.CHEF_BUREAU, com.mini.credit.enums.PosteEmploye.CHEF_BUREAU);
        }

        @Test
        @DisplayName("creerCOOSansSite_OK — COO lié à un employé sans site est accepté")
        void createUser_cooEmployeWithoutSite_shouldWork() {
                assertCreateUserWithEmployeWithoutSiteWorks(RoleCode.COO, com.mini.credit.enums.PosteEmploye.COO);
        }

        @Test
        @DisplayName("creerRCISansSite_OK — RCI lié à un employé sans site est accepté")
        void createUser_rciEmployeWithoutSite_shouldWork() {
                assertCreateUserWithEmployeWithoutSiteWorks(RoleCode.RCI, com.mini.credit.enums.PosteEmploye.RCI);
        }

        @Test
        @DisplayName("creerGerantGeneralSansSite_OK — GERANT_GENERAL lié à un employé sans site est accepté")
        void createUser_gerantGeneralEmployeWithoutSite_shouldWork() {
                assertCreateUserWithEmployeWithoutSiteWorks(RoleCode.GERANT_GENERAL, com.mini.credit.enums.PosteEmploye.GERANT_GENERAL);
        }

    @Test
    @DisplayName("createUser_withInvalidEmployeId_shouldThrow — employeId inexistant provoque une exception")
    void createUser_withInvalidEmployeId_shouldThrow() {
        // N'utilise pas setupMocksForCreate car save() n'est jamais atteint (exception avant)
        when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED");
        when(roleRepository.findByCode(RoleCode.CAISSIER)).thenReturn(Optional.of(buildRole(RoleCode.CAISSIER)));
        when(employeRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("caissier02")
                .password("Caisse2026!")
                .nomComplet("Caissier Test")
                .roles(List.of("CAISSIER"))
                .employeId(999L)
                .build();

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employé non trouvé");
    }

    @Test
    @DisplayName("utilisateurResponse_shouldExposeEmployeInfo — le DTO expose employeId, nomComplet, fonction, téléphone")
    void utilisateurResponse_shouldExposeEmployeInfo() {
        setupMocksForCreate(RoleCode.CAISSIER);

        Employe employe = new Employe();
        employe.setId(12L);
        employe.setNomComplet("Jean Caissier");
        employe.setFonction(com.mini.credit.enums.PosteEmploye.CAISSIER);
        employe.setTelephone("+243812345678");
        employe.setActif(true); // actif obligatoire
        when(employeRepository.findById(12L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("jean.caissier")
                .password("Caisse2026!")
                // nomComplet et telephone non envoyés : repris depuis Employe
                .roles(List.of("CAISSIER"))
                .employeId(12L)
                .build();

        UtilisateurDTO result = utilisateurService.create(request);

        assertThat(result.getEmployeId()).isEqualTo(12L);
        assertThat(result.getEmployeNomComplet()).isEqualTo("Jean Caissier");
        assertThat(result.getEmployeFonction()).isEqualTo("CAISSIER");
        assertThat(result.getEmployeTelephone()).isEqualTo("+243812345678");
    }

    @Test
    @DisplayName("createUser_shouldRejectEmployeAlreadyLinked — un employé déjà lié à un utilisateur est refusé")
    void createUser_shouldRejectEmployeAlreadyLinked() {
        when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED");
        when(roleRepository.findByCode(RoleCode.CAISSIER)).thenReturn(Optional.of(buildRole(RoleCode.CAISSIER)));

        // Employé déjà lié à un utilisateur existant
        Employe employe = new Employe();
        employe.setId(12L);
        employe.setNomComplet("Jean Caissier");
        employe.setActif(true); // actif (pass la première validation)
        // Simule que l'employé est déjà lié via utilisateur.employe_id
        Utilisateur autreUtilisateur = new Utilisateur();
        autreUtilisateur.setId(99L);
        autreUtilisateur.setEmploye(employe);
        employe.setUtilisateur(autreUtilisateur); // côté inverse reflété
        when(employeRepository.findById(12L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("doublon.caissier")
                .password("Caisse2026!")
                .roles(List.of("CAISSIER"))
                .employeId(12L)
                .build();

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà associé à un compte utilisateur");
    }

    // ── Nouveaux tests : validation employ\u00e9 obligatoire pour r\u00f4les op\u00e9rationnels ─────

    @Test
    @DisplayName("createUser_operationalRole_shouldRequireEmploye \u2014 AGENT_TERRAIN sans employ\u00e9 est refus\u00e9")
    void createUser_operationalRole_shouldRequireEmploye() {
        when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.AGENT_TERRAIN)).thenReturn(Optional.of(buildRole(RoleCode.AGENT_TERRAIN)));
        // Note : passwordEncoder.encode n'est PAS appel\u00e9 (exception lev\u00e9e avant)

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("terrain01")
                .password("Terrain2026!")
                .roles(List.of("AGENT_TERRAIN"))
                // employeId intentionnellement absent
                .build();

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("employ\u00e9 li\u00e9 est obligatoire");
    }

    @Test
    @DisplayName("createUser_admin_shouldAllowNoEmploye \u2014 ADMIN peut \u00eatre cr\u00e9\u00e9 sans employ\u00e9")
    void createUser_admin_shouldAllowNoEmploye() {
        setupMocksForCreate(RoleCode.ADMIN);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("admin.sys")
                .password("Admin2026!")
                .nomComplet("Admin Syst\u00e8me")
                .roles(List.of("ADMIN"))
                // pas d'employeId
                .build();

        // Ne doit pas lancer d'exception
        UtilisateurDTO result = utilisateurService.create(request);
        assertThat(result).isNotNull();
        assertThat(result.getEmployeId()).isNull();
    }

    @Test
    @DisplayName("createUser_withEmploye_shouldCopyIdentityFromEmploye \u2014 nomComplet et t\u00e9l\u00e9phone repris depuis Employ\u00e9")
    void createUser_withEmploye_shouldCopyIdentityFromEmploye() {
        setupMocksForCreate(RoleCode.CAISSIER);
        Employe employe = buildActiveEmploye(20L, com.mini.credit.enums.PosteEmploye.CAISSIER);
        employe.setNomComplet("Marie Caissier");
        employe.setTelephone("+243897654321");
        when(employeRepository.findById(20L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("marie.caissier")
                .password("Caisse2026!")
                // nomComplet et telephone NON envoy\u00e9s : seront repris depuis Employ\u00e9
                .roles(List.of("CAISSIER"))
                .employeId(20L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        assertThat(saved.getNomComplet()).isEqualTo("Marie Caissier");
        assertThat(saved.getTelephone()).isEqualTo("+243897654321");
    }

    @Test
    @DisplayName("createUser_shouldIgnoreNomTelephoneFromRequestWhenEmployeLinked \u2014 source de v\u00e9rit\u00e9 = Employ\u00e9")
    void createUser_shouldIgnoreNomTelephoneFromRequestWhenEmployeLinked() {
        setupMocksForCreate(RoleCode.CAISSIER);
        Employe employe = buildActiveEmploye(21L, com.mini.credit.enums.PosteEmploye.CAISSIER);
        employe.setNomComplet("Vrai Nom Employe");
        employe.setTelephone("+243811111111");
        when(employeRepository.findById(21L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("test.user")
                .password("Test2026!")
                .nomComplet("Faux Nom Envoye Par Frontend") // doit \u00eatre ignor\u00e9
                .telephone("+243899999999")                 // doit \u00eatre ignor\u00e9
                .roles(List.of("CAISSIER"))
                .employeId(21L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
        // L'identit\u00e9 vient de l'employ\u00e9, pas du request
        assertThat(saved.getNomComplet()).isEqualTo("Vrai Nom Employe");
        assertThat(saved.getTelephone()).isEqualTo("+243811111111");
        assertThat(saved.getNomComplet()).isNotEqualTo("Faux Nom Envoye Par Frontend");
    }

    @Test
    @DisplayName("createUser_shouldRejectInactiveEmploye \u2014 employ\u00e9 inactif refus\u00e9")
    void createUser_shouldRejectInactiveEmploye() {
        when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED");
        when(roleRepository.findByCode(RoleCode.CAISSIER)).thenReturn(Optional.of(buildRole(RoleCode.CAISSIER)));

        Employe employe = new Employe();
        employe.setId(30L);
        employe.setNomComplet("Ancien Caissier");
        employe.setActif(false); // inactif
        when(employeRepository.findById(30L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("ancien.caissier")
                .password("Test2026!")
                .roles(List.of("CAISSIER"))
                .employeId(30L)
                .build();

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactif");
    }

    @Test
        @DisplayName("createUser_gestionnaire_shouldMatchGestionnaireFunction")
        void createUser_gestionnaire_shouldMatchGestionnaireFunction() {
                setupMocksForCreate(RoleCode.GESTIONNAIRE);
        Employe employe = buildActiveEmploye(40L, com.mini.credit.enums.PosteEmploye.GESTIONNAIRE);
        employe.setNomComplet("Jean Gestionnaire");
        when(employeRepository.findById(40L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("jean.gestionnaire")
                .password("Gest2026!")
                                .roles(List.of("GESTIONNAIRE"))
                .employeId(40L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
                assertThat(saved.getRole().getCode()).isEqualTo(RoleCode.GESTIONNAIRE);
        assertThat(saved.getNomComplet()).isEqualTo("Jean Gestionnaire");
    }

    @Test
        @DisplayName("createUser_chefBureau_shouldMatchChefDeBureauFunction")
        void createUser_chefBureau_shouldMatchChefDeBureauFunction() {
                setupMocksForCreate(RoleCode.CHEF_BUREAU);
        Employe employe = buildActiveEmploye(50L, com.mini.credit.enums.PosteEmploye.CHEF_BUREAU);
        employe.setNomComplet("Chef de Bureau Test");
        when(employeRepository.findById(50L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("chef.bureau")
                .password("Chef2026!")
                .roles(List.of("CHEF_BUREAU"))
                .employeId(50L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
                assertThat(saved.getRole().getCode()).isEqualTo(RoleCode.CHEF_BUREAU);
        assertThat(saved.getNomComplet()).isEqualTo("Chef de Bureau Test");
    }

    // ── Tests : validation BLOQUANTE r\u00f4le / fonction ──────────────────────────

    @Test
    @DisplayName("createUser_shouldRejectRoleFunctionMismatch \u2014 CAISSIER + employ\u00e9 CONTROLEUR refus\u00e9")
    void createUser_shouldRejectRoleFunctionMismatch() {
        when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED");        when(roleRepository.findByCode(RoleCode.CAISSIER)).thenReturn(Optional.of(buildRole(RoleCode.CAISSIER)));

        // Employ\u00e9 CONTROLEUR mais on essaie le r\u00f4le CAISSIER
        Employe employe = buildActiveEmploye(60L, com.mini.credit.enums.PosteEmploye.CONTROLEUR);
        when(employeRepository.findById(60L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("mismatch.user")
                .password("Test2026!")
                .roles(List.of("CAISSIER"))
                .employeId(60L)
                .build();

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ne correspond pas \u00e0 la fonction de l'employ\u00e9");
    }

    @Test
        @DisplayName("createUser_gestionnaire_shouldAcceptGestionnaireEmploye")
        void createUser_gestionnaire_shouldAcceptGestionnaireEmploye() {
                setupMocksForCreate(RoleCode.GESTIONNAIRE);
        Employe employe = buildActiveEmploye(70L, com.mini.credit.enums.PosteEmploye.GESTIONNAIRE);
        employe.setNomComplet("Jean Gestionnaire");
        when(employeRepository.findById(70L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("jean.gestionnaire")
                .password("Gest2026!")
                .roles(List.of("GESTIONNAIRE"))
                .employeId(70L)
                .build();

        // Combinaison valide \u2014 ne doit pas lever d'exception
        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
                assertThat(saved.getRole().getCode()).isEqualTo(RoleCode.GESTIONNAIRE);
        assertThat(saved.getEmploye().getFonction()).isEqualTo(com.mini.credit.enums.PosteEmploye.GESTIONNAIRE);
    }

    @Test
        @DisplayName("createUser_chefBureau_shouldAcceptChefDeBureauEmploye")
        void createUser_chefBureau_shouldAcceptChefDeBureauEmploye() {
                setupMocksForCreate(RoleCode.CHEF_BUREAU);
        Employe employe = buildActiveEmploye(80L, com.mini.credit.enums.PosteEmploye.CHEF_BUREAU);
        employe.setNomComplet("Chef Bureau Test");
        when(employeRepository.findById(80L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("chef.bureau.test")
                .password("Chef2026!")
                .roles(List.of("CHEF_BUREAU"))
                .employeId(80L)
                .build();

        utilisateurService.create(request);

        Utilisateur saved = captureUtilisateurSaved();
                assertThat(saved.getRole().getCode()).isEqualTo(RoleCode.CHEF_BUREAU);
        assertThat(saved.getEmploye().getFonction()).isEqualTo(com.mini.credit.enums.PosteEmploye.CHEF_BUREAU);
    }

    @Test
    @DisplayName("createUser_caissier_shouldRejectControleurRole \u2014 CONTROLEUR + employ\u00e9 CAISSIER refus\u00e9")
    void createUser_caissier_shouldRejectControleurRole() {
        when(utilisateurRepository.existsByUsername(anyString())).thenReturn(false);        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$HASHED");        when(roleRepository.findByCode(RoleCode.CONTROLEUR)).thenReturn(Optional.of(buildRole(RoleCode.CONTROLEUR)));

        // Employ\u00e9 CAISSIER mais r\u00f4le CONTROLEUR
        Employe employe = buildActiveEmploye(90L, com.mini.credit.enums.PosteEmploye.CAISSIER);
        when(employeRepository.findById(90L)).thenReturn(Optional.of(employe));

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("caissier.mauvaisrole")
                .password("Test2026!")
                .roles(List.of("CONTROLEUR"))
                .employeId(90L)
                .build();

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ne correspond pas \u00e0 la fonction de l'employ\u00e9")
                .hasMessageContaining("CONTROLEUR")
                .hasMessageContaining("CAISSIER");
    }

    @Test
    @DisplayName("createUser_adminWithoutEmploye_shouldWork \u2014 ADMIN sans employ\u00e9 : exon\u00e9r\u00e9 du mapping")
    void createUser_adminWithoutEmploye_shouldWork() {
        setupMocksForCreate(RoleCode.ADMIN);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("admin.test")
                .password("Admin2026!")
                .nomComplet("Admin Syst\u00e8me")
                .roles(List.of("ADMIN"))
                .build();

        UtilisateurDTO result = utilisateurService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmployeId()).isNull();
    }

    @Test
    @DisplayName("createUser_memberWithoutEmploye_shouldWorkForNow \u2014 MEMBER sans employ\u00e9 : exon\u00e9r\u00e9 pour l'instant")
    void createUser_memberWithoutEmploye_shouldWorkForNow() {
        setupMocksForCreate(RoleCode.MEMBER);

        CreateUtilisateurRequest request = CreateUtilisateurRequest.builder()
                .username("membre.test")
                .password("Membre2026!")
                .nomComplet("Membre Test")
                .roles(List.of("MEMBER"))
                .build();

        UtilisateurDTO result = utilisateurService.create(request);

        assertThat(result).isNotNull();
    }
}
