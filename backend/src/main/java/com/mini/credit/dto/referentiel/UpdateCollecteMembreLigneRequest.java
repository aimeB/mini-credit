package com.mini.credit.dto.referentiel;

import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.ModaliteRemboursementCollecte;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCollecteMembreLigneRequest {

    @NotNull(message = "membreId obligatoire")
    private Long membreId;

    private Long compteEpargneId;
    private Long creditId;
    private Long demandeCreditId;

    @NotNull(message = "typeLigne obligatoire")
    private TypeLigneCollecte typeLigne;

    @PositiveOrZero(message = "montant >= 0")
    private BigDecimal montant;

    @PositiveOrZero(message = "quantite >= 0")
    private Integer quantite;

    private String reference;
    private String commentaire;

    @PositiveOrZero(message = "montantSouhaite >= 0")
    private BigDecimal montantSouhaite;

    private String objetCredit;
    private String gagePropose;
    @Min(value = 1, message = "dureeValeur >= 1")
    @Max(value = 60, message = "dureeValeur <= 60")
    private Integer dureeValeur;
    private DureeUnite dureeUnite;
    private ModaliteRemboursementCollecte modaliteRemboursement;
}