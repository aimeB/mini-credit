package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCollecteTerrainRequest {

    @PastOrPresent(message = "Date collecte ne peut pas être future")
    private LocalDate dateCollecte;

    @DecimalMin(value = "0.0", inclusive = true, message = "Espèces remises >= 0")
    private BigDecimal especesRemises;

    @DecimalMin(value = "0.0", inclusive = true, message = "Espèces déclarées agent >= 0")
    private BigDecimal especesDeclareesAgent;

    private String observations;

    // Champs potentiellement frauduleux ignorés par le backend
    private BigDecimal totalGeneralCalcule;
    private BigDecimal totalEpargneCalcule;
    private BigDecimal totalRemboursementsCalcule;
    private BigDecimal totalFraisCalcule;
}