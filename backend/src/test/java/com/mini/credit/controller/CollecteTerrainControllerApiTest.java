package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.CollecteMembreLigneResponse;
import com.mini.credit.dto.referentiel.CollecteRecapResponse;
import com.mini.credit.dto.referentiel.CollecteTerrainResponse;
import com.mini.credit.dto.referentiel.ConfirmerBilletageRequest;
import com.mini.credit.dto.referentiel.CreateCollecteMembreLigneRequest;
import com.mini.credit.dto.referentiel.CreateCollecteTerrainRequest;
import com.mini.credit.dto.referentiel.UpdateCollecteMembreLigneRequest;
import com.mini.credit.dto.referentiel.ValidateCollecteTerrainRequest;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.service.CollecteTerrainService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollecteTerrainController API")
class CollecteTerrainControllerApiTest {

    @Mock
    private CollecteTerrainService collecteTerrainService;

    @InjectMocks
    private CollecteTerrainController controller;

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void list_shouldReturn200_andForwardFilters() {
        when(collecteTerrainService.list(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageImpl<>(List.of(CollecteTerrainResponse.builder().id(1L).statut(RecetteStatut.SOUMISE).build())));

        var response = controller.list("SOUMISE", LocalDate.now().minusDays(1), LocalDate.now(), 100L, 10L, 99L, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);

        ArgumentCaptor<RecetteStatut> statutCaptor = ArgumentCaptor.forClass(RecetteStatut.class);
        verify(collecteTerrainService).list(statutCaptor.capture(), any(), any(), eq(100L), eq(10L), eq(99L), eq(0), eq(10));
        assertThat(statutCaptor.getValue()).isEqualTo(RecetteStatut.SOUMISE);
    }

    @Test
    void getToday_shouldReturn200() {
        when(collecteTerrainService.getToday()).thenReturn(CollecteTerrainResponse.builder().id(2L).build());

        var response = controller.getToday();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(2L);
    }

    @Test
    void getById_shouldReturn200() {
        when(collecteTerrainService.getById(15L)).thenReturn(CollecteTerrainResponse.builder().id(15L).build());

        var response = controller.getById(15L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(15L);
    }

    @Test
    void addUpdateDeleteSoumettre_shouldReturnExpectedStatusCodes() {
        when(collecteTerrainService.addLigne(eq(1L), any(CreateCollecteMembreLigneRequest.class)))
            .thenReturn(CollecteMembreLigneResponse.builder().id(10L).build());
        when(collecteTerrainService.updateLigne(eq(1L), eq(10L), any(UpdateCollecteMembreLigneRequest.class)))
            .thenReturn(CollecteMembreLigneResponse.builder().id(10L).build());
        when(collecteTerrainService.soumettre(eq(1L), any(CreateCollecteTerrainRequest.class)))
            .thenReturn(CollecteTerrainResponse.builder().id(1L).statut(RecetteStatut.SOUMISE).build());

        CreateCollecteMembreLigneRequest add = CreateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("10"))
            .quantite(1)
            .build();

        UpdateCollecteMembreLigneRequest update = UpdateCollecteMembreLigneRequest.builder()
            .membreId(200L)
            .typeLigne(TypeLigneCollecte.FRAIS_ANALYSE)
            .montant(new BigDecimal("5"))
            .build();

        assertThat(controller.addLigne(1L, add).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateLigne(1L, 10L, update).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteLigne(1L, 10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.soumettre(1L, CreateCollecteTerrainRequest.builder().especesRemises(new BigDecimal("100")).build()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void validerRejeterRecap_shouldReturnExpectedStatusCodes() {
        when(collecteTerrainService.valider(eq(1L), any(ValidateCollecteTerrainRequest.class)))
            .thenReturn(CollecteTerrainResponse.builder().id(1L).statut(RecetteStatut.VALIDEE).build());
        when(collecteTerrainService.rejeter(eq(1L), any(ValidateCollecteTerrainRequest.class)))
            .thenReturn(CollecteTerrainResponse.builder().id(1L).statut(RecetteStatut.REJETEE).build());
        when(collecteTerrainService.recap(1L))
            .thenReturn(CollecteRecapResponse.builder().collecteId(1L).build());

        assertThat(controller.valider(1L, ValidateCollecteTerrainRequest.builder().decision("VALIDEE").build()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.rejeter(1L, ValidateCollecteTerrainRequest.builder().decision("REJETEE").motifRejet("Erreur").build()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.recap(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void confirmerBilletage_shouldReturnExpectedStatusCode() {
        when(collecteTerrainService.confirmerBilletage(eq(1L), any(ConfirmerBilletageRequest.class)))
            .thenReturn(CollecteTerrainResponse.builder().id(1L).billetageConfirme(true).build());

        var response = controller.confirmerBilletage(1L, ConfirmerBilletageRequest.builder()
            .especesConfirmeesCaissier(new BigDecimal("100"))
            .observationBilletage("RAS")
            .build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBilletageConfirme()).isTrue();
    }

    @Test
    void dtoValidation_shouldRejectInvalidPayloads() {
        CreateCollecteMembreLigneRequest invalidLigne = CreateCollecteMembreLigneRequest.builder()
            .typeLigne(TypeLigneCollecte.CARNET)
            .montant(new BigDecimal("-1"))
            .build();

        ValidateCollecteTerrainRequest invalidValidate = ValidateCollecteTerrainRequest.builder()
            .decision(" ")
            .build();

        assertThat(validator.validate(invalidLigne)).isNotEmpty();
        assertThat(validator.validate(invalidValidate)).isNotEmpty();
    }
}
