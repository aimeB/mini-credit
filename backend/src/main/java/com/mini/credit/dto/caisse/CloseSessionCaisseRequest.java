package com.mini.credit.dto.caisse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CloseSessionCaisseRequest {

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal soldePhysique;

    private String observation;
}
