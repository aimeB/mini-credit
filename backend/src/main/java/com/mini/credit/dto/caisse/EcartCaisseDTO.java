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
 * PHASE 7: DTO pour EcartCaisse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcartCaisseDTO implements Serializable {

    @JsonProperty
    private Long id;

    @JsonProperty
    private Long sessionCaisseId;

    @JsonProperty
    private Long recetteId;

    @JsonProperty
    private LocalDate dateJour;

    @JsonProperty
    private String typeEcart;

    @JsonProperty
    private BigDecimal montantEcart;

    @JsonProperty
    private String description;

    @JsonProperty
    private String statut;

    @JsonProperty
    private String notesInvestigation;

    @JsonProperty
    private String raisonResolution;

    @JsonProperty
    private Long enqueteParId;

    @JsonProperty
    private LocalDateTime dateEnquete;

    @JsonProperty
    private Long valideParId;

    @JsonProperty
    private LocalDateTime dateValidation;

    @JsonProperty
    private Boolean seuilDepassé;
}
