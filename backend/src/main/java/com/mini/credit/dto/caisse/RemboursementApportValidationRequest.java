package com.mini.credit.dto.caisse;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemboursementApportValidationRequest {
    @NotBlank
    private String commentaire;
}
