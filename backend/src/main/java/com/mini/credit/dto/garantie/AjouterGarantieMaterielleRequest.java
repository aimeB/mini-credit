package com.mini.credit.dto.garantie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjouterGarantieMaterielleRequest {
    @NotBlank(message = "Le type de bien est requis")
    private String typeBien;

    @NotBlank(message = "La description est requise")
    private String description;

    @NotNull(message = "La valeur estimée est requise")
    @Positive(message = "La valeur estimée doit être positive")
    private BigDecimal valeurEstimee;

    @NotBlank(message = "La devise est requise")
    private String devise;

    private String proprietaireDeclare;
    private String localisation;
    private String referenceDocument;
    private String commentaire;
}