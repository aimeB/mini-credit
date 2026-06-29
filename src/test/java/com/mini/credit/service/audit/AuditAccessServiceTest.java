package com.mini.credit.service.audit;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAccessServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private OperationCaisseRepository operationCaisseRepository;

    @InjectMocks
    private AuditAccessService auditAccessService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getEntityAuditTrailScoped_shouldReturnEmptyTimelineForOwnedSession() {
        authenticateAs(utilisateur(50L, "cash.timeline", RoleCode.CAISSIER, 7L));
        SessionCaisse session = sessionAvecContexte(12L, 50L, 7L, "Sakombi");
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());

        when(sessionCaisseRepository.findByIdWithAuditContext(12L)).thenReturn(Optional.of(session));
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("SessionCaisse"), eq(12L), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditAccessService.getEntityAuditTrailScoped("SessionCaisse", 12L, 0, 50);

        assertThat(result).isEmpty();
        assertThat(session.getCaisse().getSite().getNomSite()).isEqualTo("Site Test");
        assertThat(session.getCaisse().getSite().getAgence().getNomAgence()).isEqualTo("Sakombi");
        verify(sessionCaisseRepository).findByIdWithAuditContext(12L);
        verify(auditLogRepository).findByEntityTypeAndEntityId(eq("SessionCaisse"), eq(12L), any(Pageable.class));
    }

    @Test
    void getEntityAuditTrailScoped_shouldReturnOpeningAuditEvent_whenPresent() {
        authenticateAs(utilisateur(50L, "cash.timeline", RoleCode.CAISSIER, 7L));
        SessionCaisse session = sessionAvecContexte(12L, 50L, 7L, "Sakombi");
        AuditLog log = AuditLog.builder()
                .entityType("SessionCaisse")
                .entityId(12L)
                .username("cash.timeline")
                .success(true)
                .build();
        Page<AuditLog> page = new PageImpl<>(List.of(log));

        when(sessionCaisseRepository.findByIdWithAuditContext(12L)).thenReturn(Optional.of(session));
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("SessionCaisse"), eq(12L), any(Pageable.class)))
                .thenReturn(page);

        Page<AuditLog> result = auditAccessService.getEntityAuditTrailScoped("SessionCaisse", 12L, 0, 50);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("cash.timeline");
    }

    @Test
    void getEntityAuditTrailScoped_shouldAllowCaissier_whenSessionBelongsToCurrentCaissier() {
        authenticateAs(utilisateur(50L, "cash.timeline", RoleCode.CAISSIER, 7L));
        SessionCaisse session = sessionAvecContexte(12L, 50L, 7L, "Autre antenne");
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());

        when(sessionCaisseRepository.findByIdWithAuditContext(12L)).thenReturn(Optional.of(session));
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("SessionCaisse"), eq(12L), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditAccessService.getEntityAuditTrailScoped("SessionCaisse", 12L, 0, 50);

        assertThat(result).isEmpty();
        verify(auditLogRepository).findByEntityTypeAndEntityId(eq("SessionCaisse"), eq(12L), any(Pageable.class));
    }

    @Test
    void getEntityAuditTrailScoped_shouldNotUsePlainFindById_forSessionTimeline() {
        authenticateAs(utilisateur(50L, "cash.timeline", RoleCode.CAISSIER, 7L));
        SessionCaisse session = sessionAvecContexte(12L, 50L, 7L, "Sakombi");

        when(sessionCaisseRepository.findByIdWithAuditContext(12L)).thenReturn(Optional.of(session));
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("SessionCaisse"), eq(12L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        auditAccessService.getEntityAuditTrailScoped("SessionCaisse", 12L, 0, 50);

                verify(sessionCaisseRepository, never()).findById(12L);
    }

    @Test
    void getEntityAuditTrailScoped_shouldFailClearly_whenSessionDoesNotExist() {
        authenticateAs(utilisateur(50L, "cash.timeline", RoleCode.CAISSIER, 7L));
        when(sessionCaisseRepository.findByIdWithAuditContext(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditAccessService.getEntityAuditTrailScoped("SessionCaisse", 12L, 0, 50))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session caisse introuvable");
    }

    private void authenticateAs(Utilisateur utilisateur) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities())
        );
    }

    private SessionCaisse sessionAvecContexte(Long sessionId, Long utilisateurId, Long siteId, String agenceNom) {
        Agence agence = agenceAvecIdEtNom(15L, agenceNom);

        Site site = Site.builder()
                .codeSite("SITE-001")
                .nomSite("Site Test")
                .zone("Zone Test")
                .agence(agence)
                .actif(true)
                .build();
        site.setId(siteId);

        Caisse caisse = Caisse.builder()
                .codeCaisse("CAI-001")
                .libelle("Caisse Test")
                .site(site)
                .devise("CDF")
                .actif(true)
                .build();
        caisse.setId(1L);

        Utilisateur utilisateur = utilisateur(utilisateurId, "cash.timeline", RoleCode.CAISSIER, siteId);

        SessionCaisse session = SessionCaisse.builder()
                .caisse(caisse)
                .utilisateur(utilisateur)
                .build();
        session.setId(sessionId);
        return session;
    }

    private Utilisateur utilisateur(Long id, String username, RoleCode roleCode, Long siteId) {
        Site site = siteAvecAgence(siteId, "Sakombi");

        Utilisateur utilisateur = Utilisateur.builder()
                .username(username)
                .motDePasseHash("x")
                .nomComplet("Caissier Timeline")
                .role(Role.builder().code(roleCode).build())
                .site(site)
                .build();
        utilisateur.setId(id);
        return utilisateur;
    }

        private Site siteAvecAgence(Long siteId, String agenceNom) {
                Agence agence = agenceAvecIdEtNom(15L, agenceNom);

                Site site = Site.builder()
                                .codeSite("SITE-001")
                                .nomSite("Site Test")
                                .zone("Zone Test")
                                .agence(agence)
                                .actif(true)
                                .build();
                site.setId(siteId);
                return site;
        }

        private Agence agenceAvecIdEtNom(Long id, String nomAgence) {
                Agence agence = Agence.builder()
                                .codeAgence("ANT-001")
                                .nomAgence(nomAgence)
                                .build();
                agence.setId(id);
                return agence;
        }
}