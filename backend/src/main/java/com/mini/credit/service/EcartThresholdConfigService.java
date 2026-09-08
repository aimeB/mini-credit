package com.mini.credit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Q6: Service de configuration des seuils d'écarts caisse.
 * 
 * Tous les seuils sont paramétrables (ne pas coder en dur).
 * À terme: charger depuis DB (table ecart_threshold_config) ou fichier properties.
 * 
 * Seuils supportés:
 * - seuilAutoenquete: Si écart > seuil → lancer enquête auto (EN_INVESTIGATION)
 * - seuilValidationDirecteur: Si écart > seuil → requiert validation directeur
 * - seuilValidationRCI: Si écart > seuil → requiert validation RCI
 */
@Service
@RequiredArgsConstructor
public class EcartThresholdConfigService {

    /**
     * Seuil pour lancer auto-enquête.
     * Défaut: 50.00 USD
     * Configurable via: application.properties ou DB
     */
    @Value("${caisse.ecart.seuil-auto-enquete:50.00}")
    private String seuilAutoenqueteStr;

    /**
     * Seuil pour validation directeur.
     * Défaut: 100.00 USD
     */
    @Value("${caisse.ecart.seuil-validation-directeur:100.00}")
    private String seuilValidationDirecteurStr;

    /**
     * Seuil pour validation RCI.
     * Défaut: 500.00 USD
     */
    @Value("${caisse.ecart.seuil-validation-rci:500.00}")
    private String seuilValidationRCIStr;

    /**
     * Retourner seuil auto-enquête.
     * Q6: Si écart > seuil → lancer EN_INVESTIGATION automatiquement
     */
    public BigDecimal getSeuilAutoenquete() {
        try {
            return new BigDecimal(seuilAutoenqueteStr);
        } catch (Exception e) {
            // Fallback si config manquante
            return new BigDecimal("50.00");
        }
    }

    /**
     * Retourner seuil validation directeur.
     * Optionnel pour phase 1, utilisé pour workflow hiérarchique futur.
     */
    public BigDecimal getSeuilValidationDirecteur() {
        try {
            return new BigDecimal(seuilValidationDirecteurStr);
        } catch (Exception e) {
            return new BigDecimal("100.00");
        }
    }

    /**
     * Retourner seuil validation RCI.
     * Optionnel pour phase 1, utilisé pour workflow hiérarchique futur.
     */
    public BigDecimal getSeuilValidationRCI() {
        try {
            return new BigDecimal(seuilValidationRCIStr);
        } catch (Exception e) {
            return new BigDecimal("500.00");
        }
    }

    /**
     * Vérifier si écart nécessite auto-enquête.
     * Q6: Si montant > seuil, lancer enquête automatiquement.
     */
    public boolean necessiteAutoenquete(BigDecimal montantEcart) {
        return montantEcart != null && 
               montantEcart.compareTo(getSeuilAutoenquete()) > 0;
    }

    /**
     * Vérifier si écart nécessite validation directeur.
     * (Pour phase 2/futur)
     */
    public boolean necessiteValidationDirecteur(BigDecimal montantEcart) {
        return montantEcart != null && 
               montantEcart.compareTo(getSeuilValidationDirecteur()) > 0;
    }

    /**
     * Vérifier si écart nécessite validation RCI.
     * (Pour phase 2/futur)
     */
    public boolean necessiteValidationRCI(BigDecimal montantEcart) {
        return montantEcart != null && 
               montantEcart.compareTo(getSeuilValidationRCI()) > 0;
    }
}
