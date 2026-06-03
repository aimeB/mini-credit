package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.TypeEcartCaisse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 7: Écart Caisse - Investigation et résolution.
 *
 * Représente une variance détectée lors de:
 * - Réconciliation recettes journalières (variance)
 * - Clôture session caisse (solde physique != théorique)
 */
@Entity
@Table(name = "ecart_caisse", indexes = {
        @Index(name = "idx_date_jour", columnList = "dateJour"),
        @Index(name = "idx_statut", columnList = "statut"),
        @Index(name = "idx_type_ecart", columnList = "typeEcart"),
        @Index(name = "idx_montant_ecart", columnList = "montantEcart")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcartCaisse extends BaseEntity {

    /**
     * Session caisse associée (si variance session)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_caisse_id", foreignKey = @ForeignKey(name = "fk_ecart_session"))
    private SessionCaisse sessionCaisse;

    /**
     * Recette journalière associée (si variance recette)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recette_id", foreignKey = @ForeignKey(name = "fk_ecart_recette"))
    private RecetteJournaliereTerrain recette;

    /**
     * Date de détection de l'écart
     */
    @Column(name = "dateJour", nullable = false)
    private LocalDate dateJour;

    /**
     * Type d'écart (DEFICIT, EXCEDENT, VARIANCE_RECETTE, VARIANCE_SESSION)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEcartCaisse typeEcart;

    /**
     * Montant de l'écart (valeur absolue)
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montantEcart;

    /**
     * Description: valeur attendue vs valeur observée
     * Ex: "Attendu 100000, observé 95000"
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /**
     * Statut de l'écart
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEcartCaisse statut = StatutEcartCaisse.DETECTE;

    /**
     * PHASE 7: Investigation - notes du CONTROLEUR
     */
    @Column(columnDefinition = "TEXT")
    private String notesInvestigation;

    /**
     * PHASE 7: Raison de la résolution
     */
    @Column(columnDefinition = "TEXT")
    private String raisonResolution;

    /**
     * Contrôleur qui a enquêté
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquete_par_id", foreignKey = @ForeignKey(name = "fk_ecart_enquete_par"))
    private Utilisateur enquetePar;

    /**
     * Date investigation CONTROLEUR
     */
    @Column(name = "dateEnquete")
    private LocalDateTime dateEnquete;

    /**
     * R.C.I. qui valide (si > seuil)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id", foreignKey = @ForeignKey(name = "fk_ecart_valide_par"))
    private Utilisateur validePar;

    /**
     * Date validation R.C.I.
     */
    @Column(name = "dateValidation")
    private LocalDateTime dateValidation;

    /**
     * Seuil dépassé? (SEUIL_ECART_INVESTIGATION = 10000)
     * Determines si R.C.I. doit valider
     */
    @Column(nullable = false)
    private Boolean seuilDepassé = false;

    // ============ Helpers ============

    @Transient
    public boolean isDeficit() {
        return this.typeEcart == TypeEcartCaisse.DEFICIT;
    }

    @Transient
    public boolean requiresRCIValidation() {
        return this.seuilDepassé || this.montantEcart.compareTo(new java.math.BigDecimal("10000")) > 0;
    }

    @Transient
    public boolean canBeResolved() {
        return this.statut == StatutEcartCaisse.DETECTE ||
               this.statut == StatutEcartCaisse.EN_INVESTIGATION;
    }
}
