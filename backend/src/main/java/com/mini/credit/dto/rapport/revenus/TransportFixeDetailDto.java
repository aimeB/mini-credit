package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportFixeDetailDto {
    private Long employeId;
    private String matricule;
    private String nomComplet;
    private Long siteId;
    private String siteNom;
    private Long agenceId;
    private String agenceNom;
    private BigDecimal montantJournalierParAgent;
    private Integer nombreJoursPeriode;
    private BigDecimal montantPrevu;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private String statut;
    private String referencesPaiement;
}
