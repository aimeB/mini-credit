package com.mini.credit.dto.caisse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 6: DTO pour RecetteJournaliereTerrain.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetteJournaliereTerrainDTO implements Serializable {

    @JsonProperty
    private Long id;

    @JsonProperty
    private Long agentId;

    @JsonProperty
    private Long membreId;

    @JsonProperty
    private LocalDate dateJour;

    @JsonProperty
    private String typeRecette;

    @JsonProperty
    private BigDecimal montant;

    @JsonProperty
    private String statut;

    @JsonProperty
    private String observation;

    @JsonProperty
    private String referencePapier;

    @JsonProperty
    private Long valideParId;

    @JsonProperty
    private LocalDateTime dateValidation;

    @JsonProperty
    private String motifRejet;

    @JsonProperty
    private BigDecimal cashRemis;

    @JsonProperty
    private BigDecimal variance;
}
