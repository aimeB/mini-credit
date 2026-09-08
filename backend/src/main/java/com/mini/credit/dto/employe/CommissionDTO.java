package com.mini.credit.dto.employe;

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
 * PHASE 8: DTO pour Commission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionDTO implements Serializable {

    @JsonProperty
    private Long id;

    @JsonProperty
    private Long agentId;

    @JsonProperty
    private LocalDate datePeriodeDebut;

    @JsonProperty
    private LocalDate datePeriodeFin;

    @JsonProperty
    private BigDecimal totalRecettes;

    @JsonProperty
    private BigDecimal tauxCommission;

    @JsonProperty
    private BigDecimal montantCommission;

    @JsonProperty
    private Long nbRecettes;

    @JsonProperty
    private String statut;

    @JsonProperty
    private Long valideParId;

    @JsonProperty
    private LocalDateTime dateValidation;

    @JsonProperty
    private Long payeeAId;

    @JsonProperty
    private LocalDateTime datePaiement;

    @JsonProperty
    private String observation;
}
