package com.mini.credit.dto.caisse;

import com.mini.credit.enums.TypePaiementPersonnel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepenseCaisseRattachementPaieRequest {

    @NotNull
    private Long employeId;

    @NotBlank
    private String periodePaie;

    @NotNull
    private TypePaiementPersonnel typePaiementPersonnel;

    private String motif;
    private String motifRetenue;
    private String motifPaiementPartiel;
    private Boolean retenueDefinitive;

    @NotBlank
    private String commentaireCorrection;
}