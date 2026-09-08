package com.mini.credit.dto.epargne;

import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.TypeOperationEpargne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OperationEpargneRequest {

    @NotNull
    private Long compteEpargneId;

    @NotNull
    private Long membreId;

    @NotNull
    private LocalDateTime dateOperation;

    @NotNull
    private TypeOperationEpargne typeOperation;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal montant;

    private SensOperation sens;
    private ModePaiement modePaiement;
    private String referenceExterne;
    private Long agentId;
    private Long sessionCaisseId;
    private Long createdBy;
    private String observation;
    private Long demandeCreditId;
    private Long creditId;
}