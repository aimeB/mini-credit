package com.mini.credit.controller;

import com.mini.credit.dto.UpdateAgenceRequest;
import com.mini.credit.dto.CreateAgenceRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.repository.AgenceRepository;
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
 * Tests métier pour AgenceController.
 *
 * Couvre :
 * - updateAgence_shouldUpdateVilleCommuneQuartierAdresseReference
 * - deactivateAgence_shouldSetActifFalse
 * - deleteAgence_shouldBeAdminOnly (via SiteControllerSecurityTest — réflexion)
 */
@DisplayName("AgenceController — Tests métier")
@ExtendWith(MockitoExtension.class)
class AgenceControllerBusinessTest {

    @Mock
    private AgenceRepository agenceRepository;

    @InjectMocks
    private AgenceController agenceController;

    private Agence testAgence;

    @BeforeEach
    void setUp() {
        testAgence = Agence.builder()
                .codeAgence("AGE01")
                .nomAgence("Agence Principale")
                .ville("Kinshasa")
                .commune("Gombe")
                .quartier("Centre")
                .adresse("Avenue de l'Équateur, n°1")
                .reference("Bâtiment SONAS")
                .telephone("+243 81 000 0001")
                .actif(true)
                .build();
        ReflectionTestUtils.setField(testAgence, "id", 1L);
    }

    // ──────────────────────────────────────────────────────────────────────
    // updateAgence_shouldUpdateVilleCommuneQuartierAdresseReference
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAgence_shouldUpdateVilleCommuneQuartierAdresseReference — PUT met à jour tous les champs")
    void updateAgence_shouldUpdateVilleCommuneQuartierAdresseReference() {
        UpdateAgenceRequest request = new UpdateAgenceRequest();
        request.setNomAgence("Agence Gombe Centrale");
        request.setVille("Kinshasa");
        request.setCommune("Gombe");
        request.setQuartier("Nouveau Quartier");
        request.setAdresse("Avenue des Aviateurs, n°12");
        request.setReference("En face de la pharmacie");
        request.setTelephone("+243 99 888 7777");
        request.setActif(true);

        when(agenceRepository.findById(1L)).thenReturn(Optional.of(testAgence));
        when(agenceRepository.save(any(Agence.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = agenceController.update(1L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(testAgence.getNomAgence()).isEqualTo("AGENCE GOMBE CENTRALE");
        assertThat(testAgence.getVille()).isEqualTo("Kinshasa");
        assertThat(testAgence.getCommune()).isEqualTo("Gombe");
        assertThat(testAgence.getQuartier()).isEqualTo("Nouveau Quartier");
        assertThat(testAgence.getAdresse()).isEqualTo("Avenue des Aviateurs, n°12");
        assertThat(testAgence.getReference()).isEqualTo("En face de la pharmacie");
        assertThat(testAgence.getTelephone()).isEqualTo("+243 99 888 7777");
    }

    @Test
    @DisplayName("createAgence_shouldUppercaseNomAgence — POST normalise le nom en majuscules")
    void createAgence_shouldUppercaseNomAgence() {
        CreateAgenceRequest request = CreateAgenceRequest.builder()
                .codeAgence("kin01")
                .nomAgence("  Agence principale  ")
                .ville("Kinshasa")
                .commune("Gombe")
                .quartier("Centre")
                .adresse("Adresse test")
                .actif(true)
                .build();

        when(agenceRepository.existsByCodeAgenceIgnoreCase("kin01")).thenReturn(false);
        when(agenceRepository.save(any(Agence.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = agenceController.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(agenceRepository).save(argThat(a ->
                "KIN01".equals(a.getCodeAgence())
                        && "AGENCE PRINCIPALE".equals(a.getNomAgence())));
    }

    @Test
    @DisplayName("updateAgence_shouldUppercaseNomAgence — PUT trim et majuscules")
    void updateAgence_shouldUppercaseNomAgence() {
        UpdateAgenceRequest request = new UpdateAgenceRequest();
        request.setNomAgence("  sacombi  ");
        request.setVille("Kinshasa");
        request.setCommune("Gombe");
        request.setQuartier("Centre");
        request.setAdresse("Adresse test");
        request.setActif(true);

        when(agenceRepository.findById(1L)).thenReturn(Optional.of(testAgence));
        when(agenceRepository.save(any(Agence.class))).thenAnswer(inv -> inv.getArgument(0));

        agenceController.update(1L, request);

        assertThat(testAgence.getNomAgence()).isEqualTo("SACOMBI");
    }

    // ──────────────────────────────────────────────────────────────────────
    // deactivateAgence_shouldSetActifFalse
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateAgence_shouldSetActifFalse — DELETE met actif=false sans supprimer")
    void deactivateAgence_shouldSetActifFalse() {
        when(agenceRepository.findById(1L)).thenReturn(Optional.of(testAgence));
        when(agenceRepository.save(any(Agence.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = agenceController.deactivate(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(testAgence.getActif()).isFalse();
        verify(agenceRepository).save(testAgence);
        verify(agenceRepository, never()).delete(any());
        verify(agenceRepository, never()).deleteById(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // deleteAgence_shouldBeAdminOnly
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAgence_shouldBeAdminOnly — DELETE /api/agences/{id} exige ADMIN")
    void deleteAgence_shouldBeAdminOnly() throws NoSuchMethodException {
        var method = AgenceController.class.getDeclaredMethod("deactivate", Long.class);
        var annotation = method.getAnnotation(
                org.springframework.security.access.prepost.PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
    }

    // ──────────────────────────────────────────────────────────────────────
    // deactivateAgence_shouldThrow404IfNotFound
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateAgence_shouldThrow404IfNotFound — 404 si agence inexistante")
    void deactivateAgence_shouldThrow404IfNotFound() {
        when(agenceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agenceController.deactivate(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Agence introuvable");
    }

    // ──────────────────────────────────────────────────────────────────────
    // updateAgence_shouldThrow404IfNotFound
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAgence_shouldThrow404IfNotFound — 404 si agence inexistante")
    void updateAgence_shouldThrow404IfNotFound() {
        when(agenceRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateAgenceRequest request = new UpdateAgenceRequest();
        request.setNomAgence("Test");
        request.setVille("Kinshasa");
        request.setCommune("Gombe");
        request.setQuartier("Centre");
        request.setAdresse("Adresse test");
        request.setActif(true);

        assertThatThrownBy(() -> agenceController.update(999L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Agence introuvable");
    }
}
