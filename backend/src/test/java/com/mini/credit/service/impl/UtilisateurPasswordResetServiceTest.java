package com.mini.credit.service.impl;

import com.mini.credit.dto.utilisateur.AdminPasswordResetRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordResponse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.service.audit.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UtilisateurServiceImpl - reset password sécurisé")
class UtilisateurPasswordResetServiceTest {

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
    private UtilisateurServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void admin_canResetAnyStandardUser() {
        Utilisateur admin = user(1L, RoleCode.ADMIN, site(10L, "SITE-A"));
        Utilisateur target = user(2L, RoleCode.GESTIONNAIRE, site(20L, "SITE-B"));
        authenticate(admin);

        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(target));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("HASH_TEMP");

        ResetPasswordResponse response = service.resetPasswordByAdmin(2L, AdminPasswordResetRequest.builder().motif("Incident sécurité").build());

        assertThat(response.getUsername()).isEqualTo("user2");
        assertThat(response.getTemporaryPassword()).isNotBlank();
        assertThat(response.getPasswordChangeRequired()).isTrue();

        assertThat(target.getMotDePasseHash()).isEqualTo("HASH_TEMP");
        assertThat(target.getPasswordChangeRequired()).isTrue();
        assertThat(target.getCredentialsVersion()).isEqualTo(1);
        verify(auditService).logSuccess(any(AuditAction.class), anyString(), any(), anyString());
    }

    @Test
    void chefBureau_canResetUserInSameSite() {
        Site site = site(11L, "SITE-11");
        Utilisateur chef = user(10L, RoleCode.CHEF_BUREAU, site);
        Utilisateur target = user(20L, RoleCode.CAISSIER, site);
        authenticate(chef);

        when(utilisateurRepository.findById(20L)).thenReturn(Optional.of(target));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("HASH_TEMP");

        ResetPasswordResponse response = service.resetPasswordByAdmin(20L, AdminPasswordResetRequest.builder().motif("Rotation mot de passe").build());

        assertThat(response.getPasswordChangeRequired()).isTrue();
        assertThat(target.getLastPasswordResetBy()).isEqualTo(chef);
    }

    @Test
    void chefBureau_cannotResetUserOutsideSite() {
        Utilisateur chef = user(10L, RoleCode.CHEF_BUREAU, site(11L, "SITE-11"));
        Utilisateur target = user(20L, RoleCode.CAISSIER, site(12L, "SITE-12"));
        authenticate(chef);

        when(utilisateurRepository.findById(20L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(20L, AdminPasswordResetRequest.builder().motif("Test hors site").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hors périmètre");
    }

    @Test
    void chefBureau_cannotResetProtectedRoles() {
        Utilisateur chef = user(10L, RoleCode.CHEF_BUREAU, site(11L, "SITE-11"));
        Utilisateur adminTarget = user(21L, RoleCode.ADMIN, site(11L, "SITE-11"));
        adminTarget.setUsername("admin");
        authenticate(chef);

        when(utilisateurRepository.findById(21L)).thenReturn(Optional.of(adminTarget));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(21L, AdminPasswordResetRequest.builder().motif("Test role protégé").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La réinitialisation de ce compte sensible est interdite par l’endpoint standard.");
    }

    @Test
    void resetByAdmin_mustRejectSensitiveRoleRci() {
        Utilisateur actor = user(1L, RoleCode.ADMIN, site(1L, "SITE-1"));
        Utilisateur target = user(2L, RoleCode.RCI, site(2L, "SITE-2"));
        authenticate(actor);

        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(2L, AdminPasswordResetRequest.builder().motif("Test RCI").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La réinitialisation de ce compte sensible est interdite par l’endpoint standard.");
    }

    @Test
    void resetByAdmin_mustRejectSensitiveRoleCoo() {
        Utilisateur actor = user(1L, RoleCode.ADMIN, site(1L, "SITE-1"));
        Role cooRole = Role.builder().build();
        cooRole.setCode(null);

        Utilisateur target = user(3L, RoleCode.GESTIONNAIRE, site(3L, "SITE-3"));
        target.setRole(cooRole);
        target.setUsername("coo");
        authenticate(actor);

        when(utilisateurRepository.findById(3L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(3L, AdminPasswordResetRequest.builder().motif("Test COO").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La réinitialisation de ce compte sensible est interdite par l’endpoint standard.");
    }

    @Test
    void resetByAdmin_mustRejectSensitiveRoleGerantGeneral() {
        Utilisateur actor = user(1L, RoleCode.ADMIN, site(1L, "SITE-1"));
        Utilisateur target = user(4L, RoleCode.GESTIONNAIRE, site(4L, "SITE-4"));
        target.setUsername("gerant_general");
        authenticate(actor);

        when(utilisateurRepository.findById(4L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(4L, AdminPasswordResetRequest.builder().motif("Test GERANT_GENERAL").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La réinitialisation de ce compte sensible est interdite par l’endpoint standard.");
    }

    @Test
    void resetByAdmin_mustRejectSensitiveAdminAccountEvenForAdminActor() {
        Utilisateur actor = user(1L, RoleCode.ADMIN, site(1L, "SITE-1"));
        actor.setUsername("super.admin");
        Utilisateur target = user(5L, RoleCode.ADMIN, site(5L, "SITE-5"));
        target.setUsername("admin");
        authenticate(actor);

        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(5L, AdminPasswordResetRequest.builder().motif("Test ADMIN").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La réinitialisation de ce compte sensible est interdite par l’endpoint standard.");
    }

    @Test
    void nonPrivilegedRoles_cannotResetPasswords() {
        Utilisateur controleur = user(30L, RoleCode.CONTROLEUR, site(30L, "SITE-30"));
        Utilisateur target = user(40L, RoleCode.AGENT_TERRAIN, site(30L, "SITE-30"));
        authenticate(controleur);

        when(utilisateurRepository.findById(40L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(40L, AdminPasswordResetRequest.builder().motif("Pas autorisé").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("n'êtes pas autorisé");
    }

    @Test
    void changeOwnPassword_clearsMandatoryFlag_andIncrementsCredentialsVersion() {
        Utilisateur user = user(50L, RoleCode.GESTIONNAIRE, site(50L, "SITE-50"));
        user.setMotDePasseHash("OLD_HASH");
        user.setPasswordChangeRequired(true);
        user.setPasswordResetRequired(true);
        user.setCredentialsVersion(3);
        authenticate(user);

        when(passwordEncoder.matches("old-secret", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.encode("new-secret-123")).thenReturn("NEW_HASH");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .oldPassword("old-secret")
                .newPassword("new-secret-123")
                .confirmPassword("new-secret-123")
                .build();

        service.changeOwnPassword(req);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        Utilisateur saved = captor.getValue();
        assertThat(saved.getPasswordChangeRequired()).isFalse();
        assertThat(saved.getPasswordResetRequired()).isFalse();
        assertThat(saved.getCredentialsVersion()).isEqualTo(4);
        assertThat(saved.getMotDePasseHash()).isEqualTo("NEW_HASH");
    }

    @Test
    void admin_canChangeOwnPasswordWithCurrentPassword() {
        Utilisateur admin = user(60L, RoleCode.ADMIN, site(60L, "SITE-60"));
        admin.setUsername("admin");
        admin.setMotDePasseHash("OLD_ADMIN_HASH");
        admin.setPasswordChangeRequired(true);
        admin.setPasswordResetRequired(true);
        admin.setCredentialsVersion(7);
        authenticate(admin);

        when(passwordEncoder.matches("old-admin-secret", "OLD_ADMIN_HASH")).thenReturn(true);
        when(passwordEncoder.encode("new-admin-secret-123")).thenReturn("NEW_ADMIN_HASH");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .oldPassword("old-admin-secret")
                .newPassword("new-admin-secret-123")
                .confirmPassword("new-admin-secret-123")
                .build();

        service.changeOwnPassword(req);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        Utilisateur saved = captor.getValue();
        assertThat(saved.getPasswordChangeRequired()).isFalse();
        assertThat(saved.getPasswordResetRequired()).isFalse();
        assertThat(saved.getCredentialsVersion()).isEqualTo(8);
        assertThat(saved.getMotDePasseHash()).isEqualTo("NEW_ADMIN_HASH");
    }

    @Test
    void resetSelf_mustRejectSensitiveAdmin() {
        Utilisateur admin = user(100L, RoleCode.ADMIN, site(100L, "SITE-100"));
        admin.setUsername("admin");
        authenticate(admin);

        when(utilisateurRepository.findById(100L)).thenReturn(Optional.of(admin));

        com.mini.credit.dto.utilisateur.ResetPasswordRequest req = com.mini.credit.dto.utilisateur.ResetPasswordRequest.builder()
                .newPassword("AdminSecure2026!")
                .confirmPassword("AdminSecure2026!")
                .build();

        assertThatThrownBy(() -> service.resetPasswordSelf(100L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ce compte doit utiliser le changement de mot de passe avec ancien mot de passe.");
    }

    private Utilisateur user(Long id, RoleCode roleCode, Site site) {
        Role role = Role.builder().code(roleCode).build();
        Utilisateur user = Utilisateur.builder()
                .username("user" + id)
                .nomComplet("User " + id)
                .motDePasseHash("HASH")
                .role(role)
                .site(site)
                .passwordChangeRequired(false)
                .passwordResetRequired(false)
                .credentialsVersion(0)
                .build();
        user.setId(id);
        return user;
    }

    private Site site(Long id, String name) {
        Site site = Site.builder().nomSite(name).build();
        site.setId(id);
        return site;
    }

    private void authenticate(Utilisateur utilisateur) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(utilisateur, null, List.copyOf(utilisateur.getAuthorities()))
        );
    }
}
