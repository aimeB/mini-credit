package com.mini.credit.dto.caisse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaisseCreateRequest {

    @NotBlank
    private String libelle;

    @NotNull
    private Long agenceId;

    private Long siteId;

    private Long caissierResponsableId;

    private Long caissierAffecteId;

    @NotBlank
    private String devise = "CDF";
}