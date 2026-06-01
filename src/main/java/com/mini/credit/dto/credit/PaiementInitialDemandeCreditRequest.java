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
public class PaiementInitialDemandeCreditRequest {

    @NotNull
    private Long sessionCaisseId;

    @NotNull
    private Long caisseId;

    private Long agentId;

    private Long createdById;

    @NotNull(message = "La date du paiement est obligatoire")
    private LocalDateTime datePaiement;

    @NotNull
    private ModePaiement modePaiement;

    @DecimalMin(value = "0.00")
    private BigDecimal fraisPayes = BigDecimal.ZERO;

    @DecimalMin(value = "0.00")
    private BigDecimal depotGarantiePaye = BigDecimal.ZERO;

    private String observation;
}