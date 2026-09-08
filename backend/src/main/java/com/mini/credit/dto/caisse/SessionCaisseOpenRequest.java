package com.mini.credit.dto.caisse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class SessionCaisseOpenRequest {

    @NotNull
    private Long caisseId;

    @NotNull
    private LocalDateTime dateOuverture;

    @DecimalMin(value = "0.00")
    private BigDecimal soldeOuverture;

    private LocalDate dateComptable;

    private String observation;
}