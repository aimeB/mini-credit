package com.mini.credit.service;

import com.mini.credit.dto.employe.CommissionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 8: Service pour commissions agents.
 */
public interface CommissionService {

    /**
     * Crée une commission pour un agent et une période
     * Calcule montantCommission = totalRecettes * tauxCommission
     */
    CommissionDTO creerCommission(
            Long agentId, LocalDate dateDebut, LocalDate dateFin,
            BigDecimal totalRecettes, Long nbRecettes);

    /**
     * Récupère une commission par ID
     */
    CommissionDTO getById(Long id);

    /**
     * Récupère les commissions d'un agent
     */
    List<CommissionDTO> getByAgent(Long agentId);

    /**
     * Récupère les commissions d'une période
     */
    List<CommissionDTO> getByPeriode(LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupère les commissions en attente de validation
     */
    List<CommissionDTO> getEnAttenteValidation();

    /**
     * Récupère les commissions validées en attente de paiement
     */
    List<CommissionDTO> getEnAttentePaiement();

    /**
     * PHASE 8: Valide une commission (gestionnaire/chef)
     */
    CommissionDTO validerCommission(Long commissionId);

    /**
     * PHASE 8: Paie une commission
     * Passe en PAYEE
     */
    CommissionDTO payerCommission(Long commissionId);

    /**
     * Annule une commission
     */
    CommissionDTO annulerCommission(Long commissionId, String raison);

    /**
     * Total commissions payées par agent (historique)
     */
    BigDecimal getTotalPayeByAgent(Long agentId);

    /**
     * Recalcule les commissions pour une période (suite à modification recettes)
     */
    void recalculerCommissions(LocalDate dateDebut, LocalDate dateFin);
}
