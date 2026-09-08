package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmerBilletageRequest {

    @NotNull(message = "Espèces confirmées obligatoires")
    @DecimalMin(value = "0.0", inclusive = true, message = "Espèces confirmées >= 0")
    private BigDecimal especesConfirmeesCaissier;

    private String observationBilletage;
}
