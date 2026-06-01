package com.mini.credit.dto.employe;

import com.mini.credit.enums.ModePaiement;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaiementSalaireRequest {
    private Long employeId;
    private LocalDate datePaiement;
    private BigDecimal montant;
    private ModePaiement modePaiement;
    private String notes;
    private String referenceExterne;
}
