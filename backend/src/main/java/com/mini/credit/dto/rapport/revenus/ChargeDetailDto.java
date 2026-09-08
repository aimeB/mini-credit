package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeDetailDto {
    private Long depenseId;
    private Long caisseId;
    private LocalDateTime date;
    private Long antenneId;
    private String antenne;
    private String categorie;
    private String categorieTechnique;
    private String reference;
    private String beneficiaire;
    private BigDecimal montant;
    private Long employeId;
    private String employeMatricule;
    private String employeNomComplet;
    private String employePoste;
    private String periodePaie;
    private String typePaiementPersonnel;
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
    private String periodeCharge;
    private String typeChargeFixe;
    private Long siteChargeId;
    private String siteChargeNom;
    private BigDecimal montantChargeFixeReference;
    private BigDecimal montantEcartChargeFixe;
    private String commentaireRapprochement;
    private BigDecimal salaireBase;
    private BigDecimal epargneCollecteeReference;
    private BigDecimal remboursementCollecteReference;
    private Integer nombreCarnetsVendus;
    private BigDecimal primeMobilisationEpargne;
    private BigDecimal primeMobilisationRemboursement;
    private BigDecimal bonusCarnets;
    private BigDecimal primeMotivationManuelle;
    private String modeCalculPaie;
    private String statut;
    private Boolean canRattacherPaie;
    private Boolean canRattacherTransport;
    private String observation;
}
