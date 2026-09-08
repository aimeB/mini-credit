package com.mini.credit.dto.caisse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemboursementApportPaiementRequest {
    @NotNull
    private Long sessionCaisseId;

    @NotNull
    private Long caisseId;

    @NotBlank
    private String commentaire;
}
