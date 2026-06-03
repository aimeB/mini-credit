package com.mini.credit.dto.caisse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mini.credit.enums.StatutReconciliation;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PHASE 11: DTO Réconciliation Caisse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationCaisseDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("sessionCaisseId")
    private Long sessionCaisseId;

    @JsonProperty("ecartCaisseId")
    private Long ecartCaisseId;

    @JsonProperty("montantAttendu")
    private BigDecimal montantAttendu;

    @JsonProperty("montantObserve")
    private BigDecimal montantObserve;

    @JsonProperty("montantEcart")
    private BigDecimal montantEcart;

    @JsonProperty("statut")
    private StatutReconciliation statut;

    @JsonProperty("dateCreationReconciliation")
    private LocalDateTime dateCreationReconciliation;

    @JsonProperty("dateRapprochement")
    private LocalDateTime dateRapprochement;

    @JsonProperty("rapprochePar")
    private String rapprochePar; // Nom de l'utilisateur

    @JsonProperty("motifRapprochement")
    private String motifRapprochement;

    @JsonProperty("raisonRejet")
    private String raisonRejet;

    @JsonProperty("observation")
    private String observation;

    @JsonProperty("dateCreation")
    private LocalDateTime dateCreation;

    @JsonProperty("dateModification")
    private LocalDateTime dateModification;

    @JsonProperty("isDeficit")
    private Boolean isDeficit;

    @JsonProperty("isSurplus")
    private Boolean isSurplus;
}
