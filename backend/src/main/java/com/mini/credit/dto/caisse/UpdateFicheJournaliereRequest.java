package com.mini.credit.dto.caisse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * PHASE 6B.1: Update request for FicheJournaliereAgentTerrain
 * 
 * Only BROUILLON status fiches can be updated.
 * Allows modification of observations_agent.
 * 
 * Phase 6B.2: consolidation will update financial fields
 * Phase 6B.3: validation will add controleur fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFicheJournaliereRequest implements Serializable {

    @JsonProperty("observationsAgent")
    private String observationsAgent;
}
