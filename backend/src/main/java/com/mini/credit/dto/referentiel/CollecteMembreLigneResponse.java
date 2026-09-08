package com.mini.credit.dto.referentiel;

import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.ModaliteRemboursementCollecte;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteMembreLigneResponse {
    private Long id;
    private Long membreId;
    private String membreCode;
    private String membreNom;
    private Long compteEpargneId;
    private Long creditId;
    private Long demandeCreditId;
    private TypeLigneCollecte typeLigne;
    private BigDecimal montant;
    private Integer quantite;
    private String reference;
    private String commentaire;
    private BigDecimal totalLigne;

    private BigDecimal montantSouhaite;
    private String objetCredit;
    private String gagePropose;
    private Integer dureeValeur;
    private DureeUnite dureeUnite;
    private ModaliteRemboursementCollecte modaliteRemboursement;
}