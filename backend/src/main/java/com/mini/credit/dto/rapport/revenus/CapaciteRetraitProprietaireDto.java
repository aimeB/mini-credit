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
public class CapaciteRetraitProprietaireDto {
    @Builder.Default
    private BigDecimal capitalInjecteCumule = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal remboursementsApportPayes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal capitalInjecteRestantARecuperer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal soldeCaisseTheoriqueActif = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fondsMembresAProteger = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal tresorerieApresProtectionMembres = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal engagementsCourtTerme = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salairesRestantAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal transportRestantAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fondsMinimumSecurite = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margePrudence = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal tresorerieRecuperablePrudente = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal tresoreriePotentiellementRecuperable = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal montantRecuperableConseille = BigDecimal.ZERO;
    @Builder.Default
    private Boolean retraitDeconseille = false;
    private String alerte;
    private String alerteTresorerie;
    private String alerteCredit;
    private String commentairePedagogique;
}
