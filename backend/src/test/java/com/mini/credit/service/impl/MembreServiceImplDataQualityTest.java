package com.mini.credit.service.impl;

import com.mini.credit.dto.membre.MembreCreateRequest;
import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.dto.membre.MembreUpdateRequest;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.enums.Sexe;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.MembreMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.ActivationService;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.ReferenceGeneratorService;
import com.mini.credit.service.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembreServiceImplDataQualityTest {

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

    @Test
    void create_withDuplicatePhone_shouldThrow() {
        when(membreRepository.existsByNormalizedTelephonePrincipalExcludingId("099000111", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ce numéro de téléphone est déjà utilisé.");
        verify(membreRepository, never()).save(any(Membre.class));
    }

    @Test
    void create_withDuplicatePrenomAndNom_shouldThrow() {
        when(membreRepository.existsByNormalizedPrenomAndNomExcludingId("JANE", "DOE", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Une personne avec le même prénom et le même nom existe déjà.");
        verify(membreRepository, never()).save(any(Membre.class));
    }

    @Test
    void create_withSamePrenomOnly_shouldNotBeBlockedByDataQualityRule() {
        MembreCreateRequest request = buildCreateRequest();
        request.setNom("Mbala");
        setupSuccessfulCreate();

        MembreResponse result = service.create(request);

        assertThat(result).isNotNull();
        verify(membreRepository).existsByNormalizedPrenomAndNomExcludingId("JANE", "MBALA", null);
    }

    @Test
    void create_withSameNomOnly_shouldNotBeBlockedByDataQualityRule() {
        MembreCreateRequest request = buildCreateRequest();
        request.setPrenom("Marie");
        setupSuccessfulCreate();

        MembreResponse result = service.create(request);

        assertThat(result).isNotNull();
        verify(membreRepository).existsByNormalizedPrenomAndNomExcludingId("MARIE", "DOE", null);
    }

    @Test
    void create_withDifferentCaseAndSpaces_shouldNormalizeBeforeChecking() {
        MembreCreateRequest request = buildCreateRequest();
        request.setPrenom("  jane  ");
        request.setNom("  doe  ");
        request.setTelephonePrincipal(" 099 000 111 ");
        when(membreRepository.existsByNormalizedPrenomAndNomExcludingId("JANE", "DOE", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Une personne avec le même prénom et le même nom existe déjà.");
        verify(membreRepository).existsByNormalizedTelephonePrincipalExcludingId("099000111", null);
    }

    @Test
    void update_withoutChangingPhone_shouldWork() {
        Membre membre = buildExistingMembre();
        when(membreRepository.findByIdActiveWithEagerLoad(1L)).thenReturn(Optional.of(membre));
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(membreMapper.toResponse(any(Membre.class))).thenReturn(MembreResponse.builder().id(1L).build());

        MembreResponse result = service.update(1L, new MembreUpdateRequest());

        assertThat(result).isNotNull();
        verify(membreRepository).existsByNormalizedTelephonePrincipalExcludingId("099000111", 1L);
    }

    @Test
    void update_withOtherMemberPhone_shouldThrow() {
        Membre membre = buildExistingMembre();
        when(membreRepository.findByIdActiveWithEagerLoad(1L)).thenReturn(Optional.of(membre));
        when(membreRepository.existsByNormalizedTelephonePrincipalExcludingId("099999999", 1L)).thenReturn(true);

        MembreUpdateRequest request = new MembreUpdateRequest();
        request.setTelephonePrincipal("099999999");

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ce numéro de téléphone est déjà utilisé.");
        verify(membreRepository, never()).save(any(Membre.class));
    }

    @Test
    void update_withoutChangingPrenomNom_shouldWork() {
        Membre membre = buildExistingMembre();
        when(membreRepository.findByIdActiveWithEagerLoad(1L)).thenReturn(Optional.of(membre));
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(membreMapper.toResponse(any(Membre.class))).thenReturn(MembreResponse.builder().id(1L).build());

        MembreResponse result = service.update(1L, new MembreUpdateRequest());

        assertThat(result).isNotNull();
        verify(membreRepository).existsByNormalizedPrenomAndNomExcludingId("JANE", "DOE", 1L);
    }

    @Test
    void update_withOtherMemberPrenomNom_shouldThrow() {
        Membre membre = buildExistingMembre();
        when(membreRepository.findByIdActiveWithEagerLoad(1L)).thenReturn(Optional.of(membre));
        when(membreRepository.existsByNormalizedPrenomAndNomExcludingId("MARIE", "MBALA", 1L)).thenReturn(true);

        MembreUpdateRequest request = new MembreUpdateRequest();
        request.setPrenom("Marie");
        request.setNom("Mbala");

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Une personne avec le même prénom et le même nom existe déjà.");
        verify(membreRepository, never()).save(any(Membre.class));
    }

    private MembreCreateRequest buildCreateRequest() {
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
        return request;
    }

    private Membre buildExistingMembre() {
        Site site = new Site();
        site.setId(10L);

        Membre membre = new Membre();
        membre.setId(1L);
        membre.setNom("Doe");
        membre.setPostnom("Alpha");
        membre.setPrenom("Jane");
        membre.setTelephonePrincipal("099000111");
        membre.setSite(site);
        return membre;
    }

    private void setupSuccessfulCreate() {
        Site site = new Site();
        site.setId(10L);
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(membreRepository.existsByCodeMembre(any())).thenReturn(false);
        when(membreRepository.findAllExistingUsernames()).thenReturn(List.of());
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> {
            Membre membre = invocation.getArgument(0);
            if (membre.getId() == null) {
                membre.setId(1L);
            }
            return membre;
        });
        when(membreMapper.toResponse(any(Membre.class))).thenReturn(MembreResponse.builder().id(1L).build());
    }
}