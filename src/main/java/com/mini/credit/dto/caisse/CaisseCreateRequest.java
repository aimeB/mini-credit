package com.mini.credit.dto.caisse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaisseCreateRequest {

    @NotBlank
    private String codeCaisse;

    @NotBlank
    private String libelle;

    @NotNull
    private Long siteId;

    @NotBlank
    private String devise = "CDF";
}