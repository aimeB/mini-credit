package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutGarantieCredit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "garantie_credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarantieCredit extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_credit_id", nullable = false, unique = true)
    private DemandeCredit demandeCredit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id")
    private Credit credit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_epargne_id")
    private CompteEpargne compteEpargne;

    @Column(name = "montant_credit", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantCredit;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @Column(name = "montant_garantie_requis", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantGarantieRequis = BigDecimal.ZERO;

    @Column(name = "montant_garantie_bloque", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantGarantieBloque = BigDecimal.ZERO;

    @Column(name = "montant_garantie_manquant", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantGarantieManquant = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_garantie_epargne", nullable = false, length = 30)
    private StatutGarantieCredit statutGarantieEpargne = StatutGarantieCredit.NON_VERIFIEE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controle_par")
    private Utilisateur controlePar;

    @Column(name = "date_controle")
    private LocalDateTime dateControle;

    @Column(name = "date_blocage")
    private LocalDateTime dateBlocage;

    @Column(name = "date_liberation")
    private LocalDateTime dateLiberation;

    @Column(name = "commentaire_controle", columnDefinition = "TEXT")
    private String commentaireControle;

    @OneToMany(mappedBy = "garantieCredit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GarantieMaterielle> garantiesMaterielles = new ArrayList<>();
}