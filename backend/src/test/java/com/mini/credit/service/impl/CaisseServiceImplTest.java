package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.CaisseCreateRequest;
import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.ReferenceGeneratorService;
import com.mini.credit.service.audit.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaisseServiceImplTest {

    @Mock
    private CaisseRepository caisseRepository;

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private CashMapper cashMapper;

    @Mock
    private ReferenceGeneratorService referenceGeneratorService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CaisseServiceImpl service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void caissier_canInitializeCaisse_onOwnSite() {
        Agence agence = agence(1L, "Agence A");
        Site site = site(10L, "Site A", agence);
        Utilisateur caissier = user(101L, "cash1", RoleCode.CAISSIER, site);
        authenticate(caissier);

        CaisseCreateRequest request = request(1L, 10L, null);

        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agence));
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(caisseRepository.existsByActifTrueAndAgenceId(1L)).thenReturn(false);
        when(referenceGeneratorService.genererReference("CAI")).thenReturn("CAI-001");
        when(caisseRepository.save(any(Caisse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMapper.toResponse(any(Caisse.class))).thenReturn(CaisseResponse.builder().id(1L).codeCaisse("CAI-001").build());

        CaisseResponse response = service.create(request);

        ArgumentCaptor<Caisse> captor = ArgumentCaptor.forClass(Caisse.class);
        verify(caisseRepository).save(captor.capture());
        assertThat(captor.getValue().getAgence().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getSite().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getCaissierResponsable().getId()).isEqualTo(101L);
        assertThat(response.getCodeCaisse()).isEqualTo("CAI-001");
    }

    @Test
    void caissierPeutInitialiserSaCaisseSiAucuneCaisse() {
        Agence agence = agence(1L, "Agence A");
        Site site = site(10L, "Site A", agence);
        Utilisateur caissier = user(101L, "cash1", RoleCode.CAISSIER, null);
        caissier.setEmploye(employeActif(501L, agence, site));
        authenticate(caissier);

        when(caisseRepository.existsByActifTrueAndAgenceId(1L)).thenReturn(false);
        when(referenceGeneratorService.genererReference("CAI")).thenReturn("CAI-INIT-001");
        when(caisseRepository.save(any(Caisse.class))).thenAnswer(invocation -> {
            Caisse saved = invocation.getArgument(0);
            saved.setId(900L);
            return saved;
        });
        when(cashMapper.toResponse(any(Caisse.class))).thenReturn(CaisseResponse.builder()
                .id(900L)
                .codeCaisse("CAI-INIT-001")
                .soldeDisponibleActuel(java.math.BigDecimal.ZERO)
                .build());

        CaisseResponse response = service.initialiserMaCaisse();

        ArgumentCaptor<Caisse> captor = ArgumentCaptor.forClass(Caisse.class);
        verify(caisseRepository).save(captor.capture());
        Caisse caisse = captor.getValue();
        assertThat(caisse.getAgence().getId()).isEqualTo(1L);
        assertThat(caisse.getCaissierResponsable().getId()).isEqualTo(101L);
        assertThat(caisse.getDevise()).isEqualTo("CDF");
        assertThat(caisse.getActif()).isTrue();
        assertThat(response.getCodeCaisse()).isEqualTo("CAI-INIT-001");
    }

    @Test
    void caisseInitialiseeLieeAgenceDuCaissierEtSoldeZero() {
        Agence agence = agence(2L, "Agence B");
        Utilisateur caissier = user(102L, "cash2", RoleCode.CAISSIER, null);
        caissier.setEmploye(employeActif(502L, agence, null));
        authenticate(caissier);

        when(caisseRepository.existsByActifTrueAndAgenceId(2L)).thenReturn(false);
        when(referenceGeneratorService.genererReference("CAI")).thenReturn("CAI-INIT-002");
        when(caisseRepository.save(any(Caisse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMapper.toResponse(any(Caisse.class))).thenReturn(CaisseResponse.builder()
                .id(902L)
                .agenceId(2L)
                .soldeDisponibleActuel(java.math.BigDecimal.ZERO)
                .build());

        CaisseResponse response = service.initialiserMaCaisse();

        ArgumentCaptor<Caisse> captor = ArgumentCaptor.forClass(Caisse.class);
        verify(caisseRepository).save(captor.capture());
        assertThat(captor.getValue().getAgence().getId()).isEqualTo(2L);
        assertThat(response.getSoldeDisponibleActuel()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
    }

    @Test
    void initialisationNeCreePasOperationCaisseNiSession() {
        Agence agence = agence(3L, "Agence C");
        Utilisateur caissier = user(103L, "cash3", RoleCode.CAISSIER, null);
        caissier.setEmploye(employeActif(503L, agence, null));
        authenticate(caissier);

        when(caisseRepository.existsByActifTrueAndAgenceId(3L)).thenReturn(false);
        when(referenceGeneratorService.genererReference("CAI")).thenReturn("CAI-INIT-003");
        when(caisseRepository.save(any(Caisse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMapper.toResponse(any(Caisse.class))).thenReturn(CaisseResponse.builder().id(903L).build());

        service.initialiserMaCaisse();

        verify(caisseRepository).save(any(Caisse.class));
        verifyNoInteractions(sessionCaisseRepository);
    }

    @Test
    void caissierNePeutPasInitialiserDeuxFois() {
        Agence agence = agence(4L, "Agence D");
        Utilisateur caissier = user(104L, "cash4", RoleCode.CAISSIER, null);
        caissier.setEmploye(employeActif(504L, agence, null));
        authenticate(caissier);

        when(caisseRepository.existsByActifTrueAndAgenceId(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.initialiserMaCaisse())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Une caisse active existe déjà pour cette agence");

        verify(caisseRepository, never()).save(any(Caisse.class));
    }

    @Test
    void caissierSansAgenceRefuseMessageClair() {
        Utilisateur caissier = user(105L, "cash5", RoleCode.CAISSIER, null);
        caissier.setEmploye(employeActif(505L, null, null));
        authenticate(caissier);

        assertThatThrownBy(() -> service.initialiserMaCaisse())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'est rattaché à aucune agence")
                .hasMessageContaining("Contactez l'administrateur");
    }

    @Test
    void rciNePeutPasInitialiserCaisseOperationnelle() {
        Utilisateur rci = user(106L, "rci", RoleCode.RCI, null);
        authenticate(rci);

        assertThatThrownBy(() -> service.initialiserMaCaisse())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Seul un Caissier");
    }

    @Test
    void gestionnaireNePeutPasInitialiserCaisse() {
        Utilisateur gestionnaire = user(107L, "gest", RoleCode.GESTIONNAIRE, null);
        authenticate(gestionnaire);

        assertThatThrownBy(() -> service.initialiserMaCaisse())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Seul un Caissier");
    }

    @Test
        void caissier_cannotInitializeCaisse_outsideOwnSite() {
        Agence ownAgence = agence(1L, "Agence A");
        Site ownSite = site(10L, "Site A", ownAgence);
        Utilisateur caissier = user(101L, "cash1", RoleCode.CAISSIER, ownSite);
        authenticate(caissier);

        Agence targetAgence = agence(2L, "Agence B");
        Site targetSite = site(20L, "Site B", targetAgence);
        when(agenceRepository.findById(2L)).thenReturn(Optional.of(targetAgence));
        when(siteRepository.findById(20L)).thenReturn(Optional.of(targetSite));

        CaisseCreateRequest request = request(2L, 20L, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ne peut initialiser que la caisse de son agence");
    }

    @Test
    void caissier_cannotCreateSecondActiveCaisse_onSameSite() {
        Agence agence = agence(1L, "Agence A");
        Site site = site(10L, "Site A", agence);
        Utilisateur caissier = user(101L, "cash1", RoleCode.CAISSIER, site);
        authenticate(caissier);

        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agence));
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(caisseRepository.existsByActifTrueAndAgenceId(1L)).thenReturn(true);

        CaisseCreateRequest request = request(1L, 10L, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Une caisse active existe déjà pour cette agence");

        verify(caisseRepository, never()).save(any(Caisse.class));
    }

    @Test
    void caissier_withEmployeSite_canInitializeCaisse_whenUserSiteIsNull() {
        Agence agence = agence(1L, "Agence A");
        Site site = site(10L, "Site A", agence);
        Utilisateur caissier = user(101L, "cash1", RoleCode.CAISSIER, null);
        caissier.setEmploye(employe(500L, site));
        authenticate(caissier);

        CaisseCreateRequest request = request(1L, 10L, null);

        when(agenceRepository.findById(1L)).thenReturn(Optional.of(agence));
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(caisseRepository.existsByActifTrueAndAgenceId(1L)).thenReturn(false);
        when(referenceGeneratorService.genererReference("CAI")).thenReturn("CAI-010");
        when(caisseRepository.save(any(Caisse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMapper.toResponse(any(Caisse.class))).thenReturn(CaisseResponse.builder().id(10L).codeCaisse("CAI-010").build());

        CaisseResponse response = service.create(request);

        assertThat(response.getCodeCaisse()).isEqualTo("CAI-010");
        verify(caisseRepository).save(any(Caisse.class));
    }

    @Test
    void caissier_withoutAnySite_getsClearMessage() {
        Utilisateur caissier = user(101L, "cash1", RoleCode.CAISSIER, null);
        caissier.setEmploye(null);
        authenticate(caissier);

        Agence targetAgence = agence(2L, "Agence B");
        Site targetSite = site(20L, "Site B", targetAgence);
        when(agenceRepository.findById(2L)).thenReturn(Optional.of(targetAgence));
        when(siteRepository.findById(20L)).thenReturn(Optional.of(targetSite));

        CaisseCreateRequest request = request(2L, 20L, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
            .hasMessageContaining("n'est rattaché à aucune agence")
                .hasMessageContaining("Contactez l'administrateur");
    }

    @Test
    void chefBureau_canCreateAndAssignCaissier() {
        Agence agence = agence(2L, "Agence B");
        Site site = site(20L, "Site B", agence);
        Utilisateur chefBureau = user(201L, "chef", RoleCode.CHEF_BUREAU, null);
        Utilisateur caissierAffecte = user(101L, "cash1", RoleCode.CAISSIER, site);
        authenticate(chefBureau);

        when(agenceRepository.findById(2L)).thenReturn(Optional.of(agence));
        when(siteRepository.findById(20L)).thenReturn(Optional.of(site));
        when(utilisateurRepository.findById(101L)).thenReturn(Optional.of(caissierAffecte));
        when(referenceGeneratorService.genererReference("CAI")).thenReturn("CAI-002");
        when(caisseRepository.save(any(Caisse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMapper.toResponse(any(Caisse.class))).thenReturn(CaisseResponse.builder().id(2L).build());

        CaisseCreateRequest request = request(2L, 20L, 101L);
        service.create(request);

        ArgumentCaptor<Caisse> captor = ArgumentCaptor.forClass(Caisse.class);
        verify(caisseRepository).save(captor.capture());
        assertThat(captor.getValue().getCaissierResponsable()).isNotNull();
        assertThat(captor.getValue().getCaissierResponsable().getId()).isEqualTo(101L);
    }

    @Test
    void chefBureau_getsClearMessage_whenAssignedUserIsNotCaissier() {
        Agence agence = agence(2L, "Agence B");
        Site site = site(20L, "Site B", agence);
        Utilisateur chefBureau = user(201L, "chef", RoleCode.CHEF_BUREAU, null);
        Utilisateur notCaissier = user(301L, "member1", RoleCode.MEMBER, site);
        authenticate(chefBureau);

        when(agenceRepository.findById(2L)).thenReturn(Optional.of(agence));
        when(siteRepository.findById(20L)).thenReturn(Optional.of(site));
        when(utilisateurRepository.findById(301L)).thenReturn(Optional.of(notCaissier));

        CaisseCreateRequest request = request(2L, 20L, 301L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'a pas le rôle CAISSIER");
    }

    @Test
    void chefBureauCanCreateCaisseForCurrentUser() {
        Agence agence = agence(3L, "Agence C");
        Site site = site(30L, "Site C", agence);
        Utilisateur chefBureau = user(301L, "chef", RoleCode.CHEF_BUREAU, null);
        authenticate(chefBureau);

        when(agenceRepository.findById(3L)).thenReturn(Optional.of(agence));
        when(siteRepository.findById(30L)).thenReturn(Optional.of(site));
        when(referenceGeneratorService.genererReference("CAI")).thenReturn("CAI-003");
        when(caisseRepository.save(any(Caisse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMapper.toResponse(any(Caisse.class))).thenReturn(CaisseResponse.builder().id(3L).build());

        CaisseCreateRequest request = request(3L, 30L, null);
        CaisseResponse response = service.create(request);

        assertThat(response).isNotNull();
        verify(caisseRepository).save(any(Caisse.class));
    }

    private CaisseCreateRequest request(Long agenceId, Long siteId, Long caissierAffecteId) {
        CaisseCreateRequest request = new CaisseCreateRequest();
        request.setLibelle("Caisse principale");
        request.setAgenceId(agenceId);
        request.setSiteId(siteId);
        request.setCaissierAffecteId(caissierAffecteId);
        request.setDevise("CDF");
        return request;
    }

    private Agence agence(Long id, String nom) {
        Agence agence = Agence.builder().nomAgence(nom).codeAgence("AG" + id).actif(true).build();
        agence.setId(id);
        return agence;
    }

    private Site site(Long id, String nom, Agence agence) {
        Site site = Site.builder().nomSite(nom).build();
        site.setId(id);
        site.setAgence(agence);
        return site;
    }

    private Utilisateur user(Long id, String username, RoleCode roleCode, Site site) {
        Utilisateur user = Utilisateur.builder()
                .username(username)
                .nomComplet(username)
                .motDePasseHash("hash")
                .role(Role.builder().code(roleCode).libelle(roleCode.name()).build())
                .site(site)
                .build();
        user.setId(id);
        return user;
    }

    private Employe employe(Long id, Site site) {
        Employe employe = Employe.builder().site(site).build();
        employe.setId(id);
        return employe;
    }

    private Employe employeActif(Long id, Agence agence, Site site) {
        Employe employe = Employe.builder()
                .agence(agence)
                .site(site)
                .actif(true)
                .build();
        employe.setId(id);
        return employe;
    }

    private void authenticate(Utilisateur utilisateur) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities())
        );
    }
}
