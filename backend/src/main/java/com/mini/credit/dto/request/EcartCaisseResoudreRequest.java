package com.mini.credit.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Q6: Request pour résoudre un écart caisse.
 * 
 * Raison: Courte description de la résolution.
 * 
 * Q8: Peut être appelé seulement si écart JUSTIFIE.
 * Après résolution → RESOLU → peut être accepté par RCI si variance tolérée.
 */
@Data
public class EcartCaisseResoudreRequest {

    @NotNull(message = "Raison obligatoire")
    @NotBlank(message = "Raison ne peut pas être vide")
    private String raison;
}
