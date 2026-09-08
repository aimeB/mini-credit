package com.mini.credit.dto.garantie;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValiderGarantieRequest {
    @NotBlank(message = "Le commentaire est obligatoire")
    private String commentaire;
}