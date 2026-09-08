package com.mini.credit.dto.garantie;

import com.mini.credit.enums.StatutGarantieMaterielle;
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
public class GarantieMaterielleResponse {
    private Long id;
    private Long garantieCreditId;
    private String typeBien;
    private String description;
    private BigDecimal valeurEstimee;
    private String devise;
    private String proprietaireDeclare;
    private String localisation;
    private String referenceDocument;
    private StatutGarantieMaterielle statut;
    private Long controleParId;
    private String controleParNom;
    private LocalDateTime dateControle;
    private String commentaire;
}