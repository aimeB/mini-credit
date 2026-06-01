package com.mini.credit.dto.credit;

import com.mini.credit.enums.ModePaiement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class RemboursementRequest {

    private Long echeanceId;

    @NotNull
    private Long membreId;

    @NotNull
    private LocalDateTime datePaiement;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal montantTotal;


    @NotNull
    private ModePaiement modePaiement;

    private Long sessionCaisseId;
    private Long agentId;
    private Long createdBy;
    private String observation;
}