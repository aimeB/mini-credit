package com.mini.credit.dto.credit;

import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.PeriodiciteRemboursement;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DemandeCreditCreateRequest {

    @NotNull
    private Long membreId;

    @NotNull
    private Long siteId;

    private Long agentId;

    @NotNull
    @DecimalMin(value = "1")
    @DecimalMax(value = "100000000")
    private BigDecimal montantDemande;

    @NotBlank
    private String devise = "CDF";

    @NotNull
    @Min(1)
    @Max(60)
    private Integer dureeValeur;

    @NotNull
    private DureeUnite dureeUnite = DureeUnite.MOIS;

    @NotNull
    private PeriodiciteRemboursement periodiciteRemboursement = PeriodiciteRemboursement.MENSUEL;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "20")
    private BigDecimal tauxInteret;

    @NotBlank
    private String objetCredit;

    private String gagePropose;

    private String activiteFinancee;

    @DecimalMin(value = "0.00")
    private BigDecimal revenusEstimes = BigDecimal.ZERO;

    @DecimalMin(value = "0.00")
    private BigDecimal chargesEstimees = BigDecimal.ZERO;

    @DecimalMin(value = "0.00")
    private BigDecimal fraisDemande;
}
