package com.mini.credit.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Q7: Request pour justifier un écart caisse.
 * 
 * Justification:
 * - Obligatoire
 * - Minimum 10 caractères (évite "ABC", "OK", "Non", etc.)
 * - Texte libre (textarea)
 * 
 * Phase 2: Pièces jointes (optionnelles)
 */
@Data
public class EcartCaisseJustifierRequest {

    @NotNull(message = "Justification obligatoire")
    @NotBlank(message = "Justification ne peut pas être vide")
    @Size(min = 10, max = 1000, message = "Justification entre 10 et 1000 caractères")
    private String justification;
}
