package com.mini.credit.dto.garantie;

import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarantieResponse {
    private Long id;
    private Long creditId;
    private Long demandeCreditId;
    private Long membreId;
    private TypeGarantie typeGarantie;
    private String description;
    private BigDecimal valeurEstimee;
    private BigDecimal taux;
    private BigDecimal montantBloque;
    private String localisation;
    private StatutGarantie statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private String notes;
}
