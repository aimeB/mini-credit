package com.mini.credit.dto.caisse;

import com.mini.credit.enums.NatureFinancementApprovisionnement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequalificationNatureFinancementRequest {
    @NotNull
    private NatureFinancementApprovisionnement natureFinancement;

    @NotBlank
    private String commentaireCorrection;

    private String referenceCorrection;
}
