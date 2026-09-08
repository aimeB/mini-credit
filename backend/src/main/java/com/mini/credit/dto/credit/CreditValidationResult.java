package com.mini.credit.dto.credit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Résultat détaillé de la validation stricte d'une demande crédit (PHASE 4).
 * Contient l'état de chaque critère de validation et un message global.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Indique si toutes les validations sont passées
     */
    @JsonProperty("is_valid")
    private boolean isValid;

    /**
     * Message global de validation (description du problème si invalid)
     */
    @JsonProperty("message")
    private String message;

    // ============ Validation Frais Demande ============

    /**
     * True si les frais de demande > 0
     */
    @JsonProperty("frais_demande_payes_ok")
    private boolean fraisDemandePayesOk;

    @JsonProperty("frais_demande_montant")
    private BigDecimal fraisDemandeMontant;

    @JsonProperty("frais_demande_message")
    private String fraisDemandeMessage;

    // ============ Validation Garantie ============

    /**
     * True si garantie >= 20% montant demandé
     */
    @JsonProperty("garantie_ok")
    private boolean garantieOk;

    @JsonProperty("garantie_montant_bloque")
    private BigDecimal garantieMontantBloque;

    @JsonProperty("garantie_montant_requis")
    private BigDecimal garantieMontantRequis;

    @JsonProperty("garantie_pourcentage_actuel")
    private BigDecimal garantiePourcentageActuel;

    @JsonProperty("garantie_message")
    private String garantieMessage;

    // ============ Validation Analyse Terrain ============

    /**
     * True si analyse terrain est complète et validée
     */
    @JsonProperty("analyse_terrain_ok")
    private boolean analyseTerrainOk;

    @JsonProperty("analyse_terrain_complete")
    private Boolean analyseTerrainComplete;

    @JsonProperty("adresse_validee")
    private Boolean adresseValidee;

    @JsonProperty("emploi_verifie")
    private Boolean emploiVerifie;

    @JsonProperty("analyse_terrain_message")
    private String analyseTerrainMessage;

    // ============ Résumé ============

    /**
     * Compteur des critères validés (0-3)
     */
    @JsonProperty("validation_score")
    private int validationScore;

    /**
     * Motif de rejet si invalid
     */
    @JsonProperty("motif_rejet")
    private String motifRejet;
}
