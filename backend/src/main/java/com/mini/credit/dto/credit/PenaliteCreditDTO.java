package com.mini.credit.dto.credit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mini.credit.enums.StatutPenalite;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 10: DTO pour PenaliteCredit
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaliteCreditDTO implements Serializable {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("creditId")
    private Long creditId;

    @JsonProperty("demandeCreditId")
    private Long demandeCreditId;

    @JsonProperty("dateEchéance")
    private LocalDate dateEchéance;

    @JsonProperty("nombreJoursRetard")
    private Long nombreJoursRetard;

    @JsonProperty("montantPenalite")
    private BigDecimal montantPenalite;

    @JsonProperty("tauxApplique")
    private BigDecimal tauxApplique;

    @JsonProperty("statut")
    private StatutPenalite statut;

    @JsonProperty("dateCreationPenalite")
    private LocalDateTime dateCreationPenalite;

    @JsonProperty("dateAcquittement")
    private LocalDateTime dateAcquittement;

    @JsonProperty("acquitteParId")
    private Long acquitteParId;

    @JsonProperty("dateEffacement")
    private LocalDateTime dateEffacement;

    @JsonProperty("effaceeParId")
    private Long effaceeParId;

    @JsonProperty("motifEffacement")
    private String motifEffacement;

    @JsonProperty("observation")
    private String observation;

    @JsonProperty("dateCreation")
    private LocalDateTime dateCreation;

    @JsonProperty("dateModification")
    private LocalDateTime dateModification;
}
