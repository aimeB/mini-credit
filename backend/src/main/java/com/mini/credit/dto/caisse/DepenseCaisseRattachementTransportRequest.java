package com.mini.credit.dto.caisse;

import com.mini.credit.enums.TypeChargeFixe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DepenseCaisseRattachementTransportRequest {
    @NotNull
    private Long employeId;
    @NotBlank
    private String periodeCharge;
    private LocalDate dateDebutPeriode;
    private LocalDate dateFinPeriode;
    @NotNull
    private Long siteId;
    @NotNull
    private TypeChargeFixe typeChargeFixe;
    @NotBlank
    private String commentaireCorrection;
}
