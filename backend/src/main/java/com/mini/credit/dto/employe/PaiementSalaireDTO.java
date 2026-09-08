package com.mini.credit.dto.employe;

import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.StatutPaiement;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementSalaireDTO {
    private Long id;
    private Long employeId;
    private String employeNom;
    private String employeMatricule;
    private LocalDate datePaiement;
    private BigDecimal montant;
    private ModePaiement modePaiement;
    private StatutPaiement statut;
    private String notes;
    private Long numeroOperationCaisse;
    private String referenceExterne;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
