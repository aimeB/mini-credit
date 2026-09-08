package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TransportSiteParametreRequest {
    @NotNull
    private Long siteId;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal montantTransportJournalierParAgent;
    private Boolean actif = true;
    private LocalDate dateDebutValidite;
    private LocalDate dateFinValidite;
    @NotBlank
    private String commentaire;
}
