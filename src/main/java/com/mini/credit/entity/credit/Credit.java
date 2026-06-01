package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.StatutCredit;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credit extends BaseEntity {

    @Version
    private Long version;

    @Column(name = "numero_credit", nullable = false, unique = true, length = 50)
    private String numeroCredit;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_credit_id", nullable = false, unique = true)
    private DemandeCredit demandeCredit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "date_approbation", nullable = false)
    private LocalDate dateApprobation;

    @Column(name = "date_decaissement")
    private LocalDate dateDecaissement;

    @Column(name = "montant_octroye", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantOctroye;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @Column(name = "taux_interet", nullable = false, precision = 8, scale = 4)
    private BigDecimal tauxInteret;

    @Column(name = "duree_valeur", nullable = false)
    private Integer dureeValeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "duree_unite", nullable = false, length = 20)
    private DureeUnite dureeUnite = DureeUnite.MOIS;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicite_remboursement", nullable = false, length = 20)
    private PeriodiciteRemboursement periodiciteRemboursement = PeriodiciteRemboursement.MENSUEL;

    @Column(name = "nombre_echeances", nullable = false)
    private Integer nombreEcheances;

    @Column(name = "principal_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalTotal;

    @Column(name = "interet_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal interetTotal;

    @Column(name = "penalite_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaliteTotal = BigDecimal.ZERO;

    @Column(name = "total_a_rembourser", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalARembourser;

    @Column(name = "encours_principal", nullable = false, precision = 18, scale = 2)
    private BigDecimal encoursPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCredit statut = StatutCredit.APPROUVE;

    @Column(name = "motif_contentieux", columnDefinition = "TEXT")
    private String motifContentieux;

    @Column(name = "cloture_at")
    private LocalDateTime clotureAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @OneToOne(mappedBy = "credit", cascade = CascadeType.ALL, orphanRemoval = true)
    private ContratCredit contratCredit;

    @OneToMany(mappedBy = "credit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcheanceCredit> echeances = new ArrayList<>();


    @OneToMany(mappedBy = "credit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RemboursementCredit> remboursements = new ArrayList<>();

    @OneToMany(mappedBy = "credit")
    @Builder.Default
    private List<Garantie> garanties = new ArrayList<>();
}
