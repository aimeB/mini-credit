package com.mini.credit.service;

import com.mini.credit.dto.rapport.RapportFinancierDTO;
import com.mini.credit.enums.TypeRapport;

import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 12: Service Rapports Financiers
 *
 * Logique:
 * - Batch job quotidien génère rapport bilan du jour
 * - Admin peut générer rapport manuel pour période/type spécifique
 * - Agrégations: crédits, épargnes, pénalités, commissions
 * - KPIs: taux remboursement, ROA, ROE, ratios
 */
public interface RapportFinancierService {

    /**
     * Batch job: génère rapport financier quotidien (bilan du jour)
     *
     * @return RapportFinancierDTO généré
     */
    RapportFinancierDTO genererRapportQuotidien();

    /**
     * Génère rapport manuel pour type et période spécifique
     *
     * @param typeRapport Type de rapport (BILAN, COMPTE_RESULTAT, KPI, etc.)
     * @param dateDebut Date début
     * @param dateFin Date fin
     * @return RapportFinancierDTO généré
     */
    RapportFinancierDTO genererRapportPersonnalise(TypeRapport typeRapport, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupère rapports par type
     *
     * @param typeRapport Type de rapport
     * @return liste
     */
    List<RapportFinancierDTO> getByTypeRapport(TypeRapport typeRapport);

    /**
     * Récupère rapports en attente de validation
     *
     * @return liste
     */
    List<RapportFinancierDTO> getEnAttenteValidation();

    /**
     * Valide un rapport
     *
     * @param rapportId ID du rapport
     * @return RapportFinancierDTO validé
     */
    RapportFinancierDTO validerRapport(Long rapportId);

    /**
     * Archive un rapport
     *
     * @param rapportId ID du rapport
     * @return RapportFinancierDTO archivé
     */
    RapportFinancierDTO archiverRapport(Long rapportId);

    /**
     * Récupère rapport spécifique
     *
     * @param rapportId ID
     * @return RapportFinancierDTO
     */
    RapportFinancierDTO getById(Long rapportId);

    /**
     * Derniers rapports (non archivés)
     *
     * @return liste
     */
    List<RapportFinancierDTO> getRapportRecents();
}
