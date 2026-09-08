package com.mini.credit.dto.caisse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PHASE 6B.1: Create request for FicheJournaliereAgentTerrain
 * 
 * Minimal DTO for creation - only required fields:
 * - agentTerrainId (FK to Utilisateur)
 * - dateFiche (LocalDate)
 * 
 * Other fields auto-set to defaults or calculated later.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFicheJournaliereRequest implements Serializable {

    @JsonProperty("agentTerrainId")
    private Long agentTerrainId;

    @JsonProperty("dateFiche")
    private LocalDate dateFiche;

    @JsonProperty("observationsAgent")
    private String observationsAgent;
}
