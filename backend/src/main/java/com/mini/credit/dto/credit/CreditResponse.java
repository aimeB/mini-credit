package com.mini.credit.dto.credit;

import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.StatutCredit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CreditResponse {
    private Long id;
    private String numeroCredit;
    private Long demandeCreditId;
    private Long membreId;
    private String membreNomComplet;
    private Long agentTerrainId;
    private String agentTerrainNom;
    private Long siteId;
    private String siteNom;
    private LocalDate dateApprobation;
    private LocalDate dateDecaissement;
    private BigDecimal montantOctroye;
    private String devise;
    private BigDecimal tauxInteret;
    private Integer dureeValeur;
    private DureeUnite dureeUnite;
    private PeriodiciteRemboursement periodiciteRemboursement;
    private Integer nombreEcheances;
    private BigDecimal principalTotal;
    private BigDecimal interetTotal;
    private BigDecimal penaliteTotal;
    private BigDecimal totalARembourser;
    private BigDecimal encoursPrincipal;
    private StatutCredit statut;
    private String motifContentieux;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}