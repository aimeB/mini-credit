package com.mini.credit.dto.caisse;

import com.mini.credit.enums.DepenseCaisseCategorie;
import com.mini.credit.enums.TypePaiementPersonnel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepenseCaisseCreateRequest {

    @NotNull
    private Long caisseId;

    @NotNull
    private DepenseCaisseCategorie categorie;

    @DecimalMin(value = "0.01")
    private BigDecimal montant;

    private String devise = "CDF";
    private String motif;
    private String beneficiaire;
    private Long beneficiaireId;
    private String beneficiaireNom;
    private String beneficiaireRole;
    private String beneficiaireAgence;
    private Long employeId;
    private String periodePaie;
    private TypePaiementPersonnel typePaiementPersonnel;
    private BigDecimal montantRemunerationReference;
    private BigDecimal montantEcartRemuneration;
    private String motifEcartRemuneration;
    private String naturePaiementPaie;
    private BigDecimal montantSalaireDu;
    private BigDecimal montantDejaPaye;
    private BigDecimal montantRestantApresPaiement;
    private BigDecimal montantRetenue;
    private String motifRetenue;
    private String motifPaiementPartiel;
    private String commentairePaie;
    private Boolean retenueDefinitive;
    private BigDecimal primeMotivationManuelle;
    private String motifPrimeMotivationManuelle;
    private String justificatifUrl;
}