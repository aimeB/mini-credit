package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.NiveauRisque;
import com.mini.credit.enums.RecommandationRisque;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "analyse_risque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyseRisque extends BaseEntity {



    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_credit_id", nullable = false, unique = true)
    private DemandeCredit demandeCredit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyste_id", nullable = false)
    private Utilisateur analyste;

    @Column(name = "date_visite")
    private LocalDate dateVisite;

    @Column(name = "lieu_visite", columnDefinition = "TEXT")
    private String lieuVisite;

    @Column(name = "activite_verifiee")
    private Boolean activiteVerifiee;

    @Column(name = "description_activite", columnDefinition = "TEXT")
    private String descriptionActivite;

    @Column(name = "anciennete_activite", length = 100)
    private String ancienneteActivite;

    @Column(name = "chiffre_affaires_estime", precision = 18, scale = 2)
    private BigDecimal chiffreAffairesEstime;

    @Column(name = "revenu_net_estime", precision = 18, scale = 2)
    private BigDecimal revenuNetEstime;

    @Column(name = "charges_mensuelles", precision = 18, scale = 2)
    private BigDecimal chargesMensuelles;

    @Column(name = "capacite_remboursement", precision = 18, scale = 2)
    private BigDecimal capaciteRemboursement;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_niveau", length = 20)
    private NiveauRisque risqueNiveau;

    @Column(name = "score_risque", precision = 5, scale = 2)
    private BigDecimal scoreRisque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommandationRisque recommandation;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // ============ PHASE 4: Validation stricte crédit ============

    /**
     * Indique si l'analyse terrain est complète (visite effectuée, observations validées)
     */
    @Column(name = "analyse_terrain_complete", nullable = false)
    private Boolean analyseTerrainComplete = false;

    /**
     * Indique si l'adresse du demandeur a été vérifiée sur le terrain
     */
    @Column(name = "adresse_validee", nullable = false)
    private Boolean adresseValidee = false;

    /**
     * Indique si l'emploi/activité a été vérifié sur le terrain
     */
    @Column(name = "emploi_verifie", nullable = false)
    private Boolean emploiVerifie = false;

    /**
     * Date de validation de l'analyse terrain
     */
    @Column(name = "date_validation_terrain")
    private LocalDate dateValidationTerrain;

    /**
     * Utilisateur qui a validé cette analyse terrain
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    /**
     * Vérifie si l'analyse terrain est complètement validée (tous les champs de vérification = true)
     */
    @Transient
    public boolean isAnalyseTerrainCompleteAndValidated() {
        return Boolean.TRUE.equals(analyseTerrainComplete) &&
               Boolean.TRUE.equals(adresseValidee) &&
               Boolean.TRUE.equals(emploiVerifie);
    }
}
