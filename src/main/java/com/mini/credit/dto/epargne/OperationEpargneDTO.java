package com.mini.credit.dto.epargne;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.TypeOperationEpargne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PHASE 9: DTO pour OperationEpargne (Intérêts)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationEpargneDTO implements Serializable {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("compteEpargneId")
    private Long compteEpargneId;

    @JsonProperty("membreId")
    private Long membreId;

    @JsonProperty("dateOperation")
    private LocalDateTime dateOperation;

    @JsonProperty("typeOperation")
    private TypeOperationEpargne typeOperation;

    @JsonProperty("montant")
    private BigDecimal montant;

    @JsonProperty("sens")
    private SensOperation sens;

    @JsonProperty("modePaiement")
    private ModePaiement modePaiement;

    @JsonProperty("observation")
    private String observation;

    @JsonProperty("dateCreation")
    private LocalDateTime dateCreation;

    @JsonProperty("dateModification")
    private LocalDateTime dateModification;
}
