package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.CreateFicheJournaliereRequest;
import com.mini.credit.dto.caisse.FicheJournaliereResponse;
import com.mini.credit.dto.caisse.UpdateFicheJournaliereRequest;
import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutFicheJournaliere;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 6B.1: Unit tests for FicheJournaliereMapper
 */
@DisplayName("FicheJournaliereMapper Tests")
class FicheJournaliereMapperTest {

    private FicheJournaliereMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FicheJournaliereMapper();
    }

    @Test
    @DisplayName("Should convert entity to response DTO")
    void testToResponse_WithValidEntity_ReturnsResponse() {
        // Arrange
        Utilisateur agent = Utilisateur.builder()
                .nomComplet("Test Agent")
                .build();
        agent.setId(1L);

        Site site = Site.builder()
                .nomSite("Site Test")
                .build();
        site.setId(1L);

        FicheJournaliereAgentTerrain entity = FicheJournaliereAgentTerrain.builder()
                .agentTerrain(agent)
                .site(site)
                .dateFiche(LocalDate.of(2024, 1, 15))
                .statut(StatutFicheJournaliere.BROUILLON)
                .epargneCollecteeTotal(new BigDecimal("1000.00"))
                .remboursementCollectes(new BigDecimal("500.00"))
                .fraisCollectes(new BigDecimal("50.00"))
                .autresRecettes(BigDecimal.ZERO)
                .nombreMembresVisites(5)
                .nombreNouveauxMembres(1)
                .nombreCarnetsDistribues(0)
                .observationsAgent("Test observations")
                .build();
        entity.setId(1L);

        // Act
        FicheJournaliereResponse response = mapper.toResponse(entity);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getAgentTerrainId());
        assertEquals("Test Agent", response.getAgentTerrainNom());
        assertEquals("BROUILLON", response.getStatut());
        assertEquals(new BigDecimal("1000.00"), response.getEpargneCollecteeTotal());
        assertEquals(5, response.getNombreMembresVisites());
    }

    @Test
    @DisplayName("Should handle null entity")
    void testToResponse_WithNullEntity_ReturnsNull() {
        // Act
        FicheJournaliereResponse response = mapper.toResponse(null);

        // Assert
        assertNull(response);
    }

    @Test
    @DisplayName("Should create entity from create request")
    void testToEntityFromCreateRequest_WithValidRequest_ReturnsEntity() {
        // Arrange
        CreateFicheJournaliereRequest request = CreateFicheJournaliereRequest.builder()
                .agentTerrainId(1L)
                .dateFiche(LocalDate.of(2024, 1, 15))
                .observationsAgent("Test obs")
                .build();

        Utilisateur agent = Utilisateur.builder()
                .nomComplet("Test Agent")
                .build();
        agent.setId(1L);
        
        Site agentSite = Site.builder()
                .nomSite("Site")
                .build();
        agentSite.setId(1L);
        agent.setSite(agentSite);

        // Act
        FicheJournaliereAgentTerrain entity = mapper.toEntityFromCreateRequest(request, agent);

        // Assert
        assertNotNull(entity);
        assertEquals(agent, entity.getAgentTerrain());
        assertEquals(LocalDate.of(2024, 1, 15), entity.getDateFiche());
        assertEquals("Test obs", entity.getObservationsAgent());
        assertEquals(StatutFicheJournaliere.BROUILLON, entity.getStatut());
        assertEquals(BigDecimal.ZERO, entity.getEpargneCollecteeTotal());
        assertEquals(0, entity.getNombreMembresVisites());
    }

    @Test
    @DisplayName("Should handle null request in create")
    void testToEntityFromCreateRequest_WithNullRequest_ReturnsNull() {
        // Act
        FicheJournaliereAgentTerrain entity = mapper.toEntityFromCreateRequest(null, null);

        // Assert
        assertNull(entity);
    }

    @Test
    @DisplayName("Should update entity from update request")
    void testUpdateFromRequest_WithValidRequest_UpdatesEntity() {
        // Arrange
        FicheJournaliereAgentTerrain entity = FicheJournaliereAgentTerrain.builder()
                .build();
        entity.setId(1L);
        entity.setObservationsAgent("Original");

        UpdateFicheJournaliereRequest request = UpdateFicheJournaliereRequest.builder()
                .observationsAgent("Updated")
                .build();

        // Act
        mapper.updateFromRequest(request, entity);

        // Assert
        assertEquals("Updated", entity.getObservationsAgent());
    }

    @Test
    @DisplayName("Should handle null request in update")
    void testUpdateFromRequest_WithNullRequest_DoesNotThrow() {
        // Arrange
        FicheJournaliereAgentTerrain entity = FicheJournaliereAgentTerrain.builder()
                .build();
        entity.setId(1L);

        // Act & Assert
        assertDoesNotThrow(() -> mapper.updateFromRequest(null, entity));
    }
}
