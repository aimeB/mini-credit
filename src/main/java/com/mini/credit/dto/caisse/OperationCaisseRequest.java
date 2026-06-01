package com.mini.credit.dto.caisse;

import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.TypeOperationCaisse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OperationCaisseRequest {

    @NotNull
    private Long sessionCaisseId;

    @NotNull
    private Long caisseId;

    @NotNull
    private LocalDateTime dateOperation;

    @NotNull
    private TypeOperationCaisse typeOperation;

    @NotNull
    private CategorieOperationCaisse categorieOperation;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal montant;

    private String devise = "CDF";
    private Long membreId;
    private Long creditId;
    private Long remboursementId;
    private Long operationEpargneId;
    private Long agentId;
    private String description;
    private Long createdBy;
    private ModePaiement modePaiement;
    private String observation;
}