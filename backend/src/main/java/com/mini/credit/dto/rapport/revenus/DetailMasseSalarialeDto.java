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
public class DetailMasseSalarialeDto {
    private Long employeId;
    private String matricule;
    private String nomComplet;
    private String poste;
    private Long agenceId;
    private String agenceNom;
    @Builder.Default
    private Boolean chargeSiege = false;
    @Builder.Default
    private BigDecimal salaireBase = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal primeFixe = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal bonusVariable = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalEpargneCollecteeValidee = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal primeEpargne = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalRemboursementCollecteValide = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal primeRemboursement = BigDecimal.ZERO;
    @Builder.Default
    private Integer nombreCarnetsVendus = 0;
    @Builder.Default
    private BigDecimal bonusCarnets = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalPrimesAgentTerrain = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal primesBonusJustifies = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal remunerationAttendueTotale = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ecartRemuneration = BigDecimal.ZERO;
    private String motifRemuneration;
    @Builder.Default
    private BigDecimal salairePrevu = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal montantPaye = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salairesPartielsPayes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal avancesPayees = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal retenues = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal primes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal commissions = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal regularisations = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resteAPayer = BigDecimal.ZERO;
    private String statutPaie;
    private String referenceDepenseCaisse;
}