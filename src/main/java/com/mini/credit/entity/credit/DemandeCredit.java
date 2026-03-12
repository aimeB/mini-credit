package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.StatutDemandeCredit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demande_credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeCredit extends BaseEntity {



    @Column(name = "numero_demande", nullable = false, unique = true, length = 50)
    private String numeroDemande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentTerrain agent;

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

    @Column(name = "montant_demande", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantDemande;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @Column(name = "duree_valeur", nullable = false)
    private Integer dureeValeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "duree_unite", nullable = false, length = 20)
    private DureeUnite dureeUnite = DureeUnite.MOIS;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicite_remboursement", nullable = false, length = 20)
    private PeriodiciteRemboursement periodiciteRemboursement = PeriodiciteRemboursement.MENSUEL;

    @Column(name = "taux_interet", nullable = false, precision = 8, scale = 4)
    private BigDecimal tauxInteret;

    @Column(name = "objet_credit", nullable = false, columnDefinition = "TEXT")
    private String objetCredit;

    @Column(name = "activite_financee", columnDefinition = "TEXT")
    private String activiteFinancee;

    @Column(name = "revenus_estimes", precision = 18, scale = 2)
    private BigDecimal revenusEstimes;

    @Column(name = "charges_estimees", precision = 18, scale = 2)
    private BigDecimal chargesEstimees;

    @Column(name = "frais_demande", nullable = false, precision = 18, scale = 2)
    private BigDecimal fraisDemande = BigDecimal.ZERO;

    @Column(name = "depot_garantie_requis", nullable = false, precision = 18, scale = 2)
    private BigDecimal depotGarantieRequis = BigDecimal.ZERO;

    @Column(name = "depot_garantie_paye", nullable = false, precision = 18, scale = 2)
    private BigDecimal depotGarantiePaye = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDemandeCredit statut = StatutDemandeCredit.BROUILLON;

    @Column(name = "commentaire_decision", columnDefinition = "TEXT")
    private String commentaireDecision;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private Utilisateur decidedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @OneToOne(mappedBy = "demandeCredit", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnalyseRisque analyseRisque;

    @OneToMany(mappedBy = "demandeCredit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Garantie> garanties = new ArrayList<>();

    @OneToOne(mappedBy = "demandeCredit")
    private Credit credit;
}
