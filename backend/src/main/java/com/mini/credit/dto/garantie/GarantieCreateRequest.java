package com.mini.credit.dto.garantie;

import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarantieCreateRequest {
    
    @NotNull(message = "Type de garantie est requis")
    private TypeGarantie typeGarantie;

    @NotBlank(message = "Description est requise")
    private String description;

    @NotNull(message = "Valeur estimée est requise")
    @Positive(message = "Valeur estimée doit être positive")
    private BigDecimal valeurEstimee;

    @Positive(message = "Taux doit être positif")
    private BigDecimal taux;

    private String localisation;
    
    private String notes;

    private Long creditId;
    
    private Long membreId;
}
