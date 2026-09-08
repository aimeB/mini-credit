package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.ModaliteRemboursementCollecte;
import com.mini.credit.enums.TypeLigneCollecte;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "collecte_membre_ligne",
    indexes = {
        @Index(name = "idx_cml_collecte", columnList = "collecte_id"),
        @Index(name = "idx_cml_membre", columnList = "membre_id"),
        @Index(name = "idx_cml_type", columnList = "type_ligne")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollecteMembreLigne extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collecte_id", nullable = false)
    private CollecteJournaliereTerrain collecte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_epargne_id")
    private CompteEpargne compteEpargne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id")
    private Credit credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_credit_id")
    private DemandeCredit demandeCredit;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_ligne", nullable = false, length = 40)
    private TypeLigneCollecte typeLigne;

    @Column(name = "montant", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montant = BigDecimal.ZERO;

    @Column(name = "quantite")
    @Builder.Default
    private Integer quantite = 0;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "montant_souhaite", precision = 15, scale = 2)
    private BigDecimal montantSouhaite;

    @Column(name = "objet_credit", length = 255)
    private String objetCredit;

    @Column(name = "gage_propose", length = 255)
    private String gagePropose;

    @Column(name = "duree_valeur")
    @Builder.Default
    private Integer dureeValeur = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "duree_unite", length = 20)
    @Builder.Default
    private DureeUnite dureeUnite = DureeUnite.MOIS;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalite_remboursement", length = 20)
    private ModaliteRemboursementCollecte modaliteRemboursement;
}
