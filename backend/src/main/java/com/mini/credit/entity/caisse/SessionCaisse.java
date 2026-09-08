package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutSessionCaisse;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_caisse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCaisse extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    /**
     * Q4: Who closed/clôturé this session (audit trail).
     * CAISSIER ouvre session, CONTROLEUR clôture session.
     * Important for traceability: which user performed closure action.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ferme_par_id", nullable = true)
    private Utilisateur fermePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controle_valide_par_id")
    private Utilisateur controleValidePar;

    @Column(name = "date_controle")
    private LocalDateTime dateControle;

    @Column(name = "date_comptable", nullable = false)
    private LocalDate dateComptable;

    private LocalDateTime dateOuverture;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "motif_annulation", length = 1000)
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annulee_par_id")
    private Utilisateur annuleePar;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "statut_correction", length = 80)
    private String statutCorrection;

    @Column(name = "commentaire_correction", length = 1000)
    private String commentaireCorrection;

    @Column(name = "solde_ouverture", nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeOuverture = BigDecimal.ZERO;

    @Column(name = "total_entrees", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEntrees = BigDecimal.ZERO;

    @Column(name = "total_sorties", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalSorties = BigDecimal.ZERO;

    @Column(name = "solde_theorique", nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeTheorique = BigDecimal.ZERO;

    @Column(name = "solde_physique", precision = 18, scale = 2)
    private BigDecimal soldePhysique;

    @Column(name = "ecart_caisse", precision = 18, scale = 2)
    private BigDecimal ecartCaisse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSessionCaisse statut = StatutSessionCaisse.OUVERTE;

    /**
     * Q4: Audit comment/observation during closure.
     * Saisie CONTROLEUR lors clôture: raison variance, ajustements, explications.
     * Complète la traçabilité: createdBy (who opened), fermePar (who closed), observation (why/how).
     */
    private String observation;
}
