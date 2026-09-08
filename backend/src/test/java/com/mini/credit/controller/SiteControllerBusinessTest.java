package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.CreateSiteRequest;
import com.mini.credit.dto.referentiel.UpdateSiteRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests métier pour SiteController.
 *
 * Couvre :
 * - createSite_shouldRequireZone
 * - createSite_shouldNotRequireReference
 * - updateSite_shouldUpdateZone
 * - deactivateSite_shouldSetActifFalse
 * - deleteSite_shouldNotBreakLinkedData (soft delete)
 */
@DisplayName("SiteController — Tests métier")
@ExtendWith(MockitoExtension.class)
class SiteControllerBusinessTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @InjectMocks
    private SiteController siteController;

    private Agence testAgence;
    private Site testSite;

    @BeforeEach
    void setUp() {
        testAgence = Agence.builder()
                .codeAgence("AGE01")
                .nomAgence("Agence Test")
                .ville("Kinshasa")
                .commune("Gombe")
                .actif(true)
                .build();
        ReflectionTestUtils.setField(testAgence, "id", 1L);

        testSite = Site.builder()
                .codeSite("SITE01")
                .nomSite("Site Test")
                .zone("Zone Place Météo jusqu'à l'avenue X")
                .actif(true)
                .agence(testAgence)
                .build();
        ReflectionTestUtils.setField(testSite, "id", 10L);
    }

    // ──────────────────────────────────────────────────────────────────────
    // createSite_shouldRequireZone
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSite_shouldRequireZone — zone non nulle dans l'entité créée")
    void createSite_shouldRequireZone() {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .agenceId(1L)
                .codeSite("ZONE01")
                .nomSite("Site Zone Test")
                .zone("De la Place Météo jusqu'à l'avenue X")
                .build();

        when(siteRepository.findByCodeSite("ZONE01")).thenReturn(Optional.empty());
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(testAgence));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> {
            Site s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 20L);
            return s;
        });

        var response = siteController.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(siteRepository).save(argThat(s -> s.getZone() != null && !s.getZone().isBlank()));
    }

    @Test
    @DisplayName("createSite_shouldUppercaseNomSite — POST normalise le nom en majuscules")
    void createSite_shouldUppercaseNomSite() {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .agenceId(1L)
                .codeSite("site01")
                .nomSite("  Bunia centre  ")
                .zone("Zone test")
                .build();

        when(siteRepository.findByCodeSite("site01")).thenReturn(Optional.empty());
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(testAgence));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        siteController.create(request);

        verify(siteRepository).save(argThat(s ->
                "SITE01".equals(s.getCodeSite())
                        && "BUNIA CENTRE".equals(s.getNomSite())));
    }

    @Test
    @DisplayName("updateSite_shouldUppercaseNomSite — PUT trim et majuscules")
    void updateSite_shouldUppercaseNomSite() {
        UpdateSiteRequest request = UpdateSiteRequest.builder()
                .nomSite("  sacombi  ")
                .build();

        when(siteRepository.findById(10L)).thenReturn(Optional.of(testSite));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        siteController.update(10L, request);

        assertThat(testSite.getNomSite()).isEqualTo("SACOMBI");
    }

    // ──────────────────────────────────────────────────────────────────────
    // createSite_shouldNotRequireReference
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSite_shouldNotRequireReference — CreateSiteRequest sans champ reference")
    void createSite_shouldNotRequireReference() {
        boolean hasReferenceField = false;
        for (java.lang.reflect.Field f : CreateSiteRequest.class.getDeclaredFields()) {
            if (f.getName().equals("reference")) {
                hasReferenceField = true;
                break;
            }
        }
        assertThat(hasReferenceField)
                .as("CreateSiteRequest ne doit pas avoir le champ 'reference'")
                .isFalse();
    }

    // ──────────────────────────────────────────────────────────────────────
    // updateSite_shouldUpdateZone
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateSite_shouldUpdateZone — zone mise à jour via PUT")
    void updateSite_shouldUpdateZone() {
        UpdateSiteRequest request = UpdateSiteRequest.builder()
                .zone("Nouvelle zone terrain opérationnelle")
                .build();

        when(siteRepository.findById(10L)).thenReturn(Optional.of(testSite));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = siteController.update(10L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(testSite.getZone()).isEqualTo("Nouvelle zone terrain opérationnelle");
    }

    // ──────────────────────────────────────────────────────────────────────
    // deactivateSite_shouldSetActifFalse
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateSite_shouldSetActifFalse — DELETE met actif=false sans supprimer")
    void deactivateSite_shouldSetActifFalse() {
        when(siteRepository.findById(10L)).thenReturn(Optional.of(testSite));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = siteController.deactivate(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(testSite.getActif()).isFalse();
        verify(siteRepository).save(testSite);
        verify(siteRepository, never()).delete(any());
        verify(siteRepository, never()).deleteById(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // deleteSite_shouldNotBreakLinkedData
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSite_shouldNotBreakLinkedData — soft delete: aucune suppression physique")
    void deleteSite_shouldNotBreakLinkedData() {
        // Le soft delete ne doit jamais appeler repository.delete() ou deleteById()
        when(siteRepository.findById(10L)).thenReturn(Optional.of(testSite));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        siteController.deactivate(10L);

        verify(siteRepository, never()).delete(any(Site.class));
        verify(siteRepository, never()).deleteById(anyLong());
        verify(siteRepository, times(1)).save(testSite);
    }

    // ──────────────────────────────────────────────────────────────────────
    // deactivateSite_shouldThrow404IfNotFound
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateSite_shouldThrow404IfNotFound — 404 si site inexistant")
    void deactivateSite_shouldThrow404IfNotFound() {
        when(siteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> siteController.deactivate(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Site introuvable");
    }

    // ──────────────────────────────────────────────────────────────────────
    // createSite_shouldWorkWithoutVilleCommuneAdresseReferenceObservation
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSite_shouldWorkWithoutVilleCommuneAdresseReferenceObservation — site créé sans ces champs")
    void createSite_shouldWorkWithoutVilleCommuneAdresseReferenceObservation() {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .agenceId(1L)
                .codeSite("METEO01")
                .nomSite("Site Météo")
                .zone("De la Place Météo jusqu'à l'avenue X")
                .build();

        when(siteRepository.findByCodeSite("METEO01")).thenReturn(Optional.empty());
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(testAgence));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> {
            Site s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 30L);
            return s;
        });

        var response = siteController.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(siteRepository).save(argThat(s ->
            s.getVille() == null &&
            s.getCommune() == null &&
            s.getAdresse() == null &&
            s.getZone() != null && !s.getZone().isBlank()
        ));
    }

    // ──────────────────────────────────────────────────────────────────────
    // createSite_shouldNotRequireVille/Commune/Adresse/Observation
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSite_shouldNotRequireVille — pas de champ ville dans CreateSiteRequest")
    void createSite_shouldNotRequireVille() {
        boolean hasVille = false;
        for (java.lang.reflect.Field f : CreateSiteRequest.class.getDeclaredFields()) {
            if (f.getName().equals("ville")) { hasVille = true; break; }
        }
        assertThat(hasVille).as("CreateSiteRequest ne doit pas avoir le champ 'ville'").isFalse();
    }

    @Test
    @DisplayName("createSite_shouldNotRequireCommune — pas de champ commune dans CreateSiteRequest")
    void createSite_shouldNotRequireCommune() {
        boolean hasCommune = false;
        for (java.lang.reflect.Field f : CreateSiteRequest.class.getDeclaredFields()) {
            if (f.getName().equals("commune")) { hasCommune = true; break; }
        }
        assertThat(hasCommune).as("CreateSiteRequest ne doit pas avoir le champ 'commune'").isFalse();
    }

    @Test
    @DisplayName("createSite_shouldNotRequireAdresse — pas de champ adresse dans CreateSiteRequest")
    void createSite_shouldNotRequireAdresse() {
        boolean hasAdresse = false;
        for (java.lang.reflect.Field f : CreateSiteRequest.class.getDeclaredFields()) {
            if (f.getName().equals("adresse")) { hasAdresse = true; break; }
        }
        assertThat(hasAdresse).as("CreateSiteRequest ne doit pas avoir le champ 'adresse'").isFalse();
    }

    @Test
    @DisplayName("createSite_shouldNotRequireObservation — pas de champ observation dans CreateSiteRequest")
    void createSite_shouldNotRequireObservation() {
        boolean hasObservation = false;
        for (java.lang.reflect.Field f : CreateSiteRequest.class.getDeclaredFields()) {
            if (f.getName().equals("observation")) { hasObservation = true; break; }
        }
        assertThat(hasObservation).as("CreateSiteRequest ne doit pas avoir le champ 'observation'").isFalse();
    }

    @Test
    @DisplayName("createSite_villeNullInEntity — ville est null dans le Site créé (colonne nullable en DB)")
    void createSite_villeNullInEntity() {
        // Vérifier que l'entité Site peut être construite avec ville=null
        Site siteWithoutVille = Site.builder()
                .codeSite("TEST01")
                .nomSite("Site Test Ville Null")
                .zone("Zone test")
                .actif(true)
                .agence(testAgence)
                .build();

        assertThat(siteWithoutVille.getVille()).isNull();
        assertThat(siteWithoutVille.getCommune()).isNull();
        assertThat(siteWithoutVille.getAdresse()).isNull();
        assertThat(siteWithoutVille.getZone()).isEqualTo("Zone test");
    }

    // ──────────────────────────────────────────────────────────────────────
    // getSitesByAgence_shouldReturnOnlySitesOfAgence
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSitesByAgence_shouldReturnOnlySitesOfAgence — filtre correct par agenceId")
    void getSitesByAgence_shouldReturnOnlySitesOfAgence() {
        Site autresite = Site.builder()
                .codeSite("SITE02")
                .nomSite("Site Autre Agence")
                .zone("Zone autre")
                .actif(true)
                .agence(testAgence)
                .build();
        ReflectionTestUtils.setField(autresite, "id", 11L);

        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(siteRepository.findByAgenceId(1L)).thenReturn(java.util.List.of(testSite, autresite));

        var result = siteController.getByAgence(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(s ->
                assertThat(s.getAgenceId()).isEqualTo(1L));
        verify(siteRepository).findByAgenceId(1L);
    }

    @Test
    @DisplayName("getSitesByAgence_shouldThrow404IfAgenceNotFound — agence inexistante → 404")
    void getSitesByAgence_shouldThrow404IfAgenceNotFound() {
        when(agenceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> siteController.getByAgence(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Agence introuvable");
    }

    @Test
    @DisplayName("getSitesByAgence_shouldReturnEmptyListIfNoSites — liste vide si aucun site")
    void getSitesByAgence_shouldReturnEmptyListIfNoSites() {
        when(agenceRepository.existsById(1L)).thenReturn(true);
        when(siteRepository.findByAgenceId(1L)).thenReturn(java.util.List.of());

        var result = siteController.getByAgence(1L);

        assertThat(result).isEmpty();
    }
}
