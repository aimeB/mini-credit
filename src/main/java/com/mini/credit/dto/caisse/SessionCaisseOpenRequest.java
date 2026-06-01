package com.mini.credit.dto.caisse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SessionCaisseOpenRequest {

    @NotNull
    private Long caisseId;

    @NotNull
    private Long utilisateurId;

    @NotNull
    private LocalDateTime dateOuverture;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal soldeOuverture;

    private String observation;
}