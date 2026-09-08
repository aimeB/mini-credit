package com.mini.credit.service;

import com.mini.credit.dto.epargne.DemandeRetraitEpargneDTO;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.enums.StatutDemandeRetrait;

import java.util.List;

/**
 * Service pour la gestion des demandes de retrait épargne (PHASE 5).
 *
 * Responsabilités:
 * - Créer demandes de retrait
 * - Valider demandes (vérif solde CompteEpargne)
 * - Rejeter demandes
 * - Décaisser retraits (crée OperationEpargne)
 */
public interface DemandeRetraitEpargneService {

    /**
     * Crée une nouvelle demande de retrait épargne
     *
     * @param compteEpargneId ID du compte
     * @param montant montant à retirer
    * @param fraisRetrait frais de retrait à encaisser séparément
        * @param observation observation facultative
     * @return demande créée (statut CREEE)
     */
    DemandeRetraitEpargneDTO creerDemande(Long compteEpargneId, java.math.BigDecimal montant, java.math.BigDecimal fraisRetrait, String observation);

    /**
     * Récupère une demande par ID
     */
    DemandeRetraitEpargneDTO getById(Long id);

    /**
     * Récupère les demandes, éventuellement filtrées par statut.
     */
    List<DemandeRetraitEpargneDTO> getAll(StatutDemandeRetrait statut);

    /**
     * Récupère les demandes d'un compte épargne
     */
    List<DemandeRetraitEpargneDTO> getByCompteEpargne(Long compteEpargneId);

    /**
     * Récupère les demandes en attente de validation CONTROLEUR
     */
    List<DemandeRetraitEpargneDTO> getEnAttenteValidation();

    /**
     * Valide une demande de retrait (vérif solde)
     * Passe en VALIDEE si solde suffisant, sinon REJETEE
     *
     * @param demandeId ID de la demande
     * @return demande validée/rejetée
     */
    DemandeRetraitEpargneDTO validerDemande(Long demandeId);

    /**
     * Rejette une demande de retrait avec motif
     *
     * @param demandeId ID de la demande
     * @param motif raison du rejet
     * @return demande rejetée
     */
    DemandeRetraitEpargneDTO rejeterDemande(Long demandeId, String motif);

    /**
     * Décaisse un retrait (crée OperationEpargne de type RETRAIT)
     * Passe en DECAISSEE
     *
     * @param demandeId ID de la demande
     * @return demande décaissée
     */
    DemandeRetraitEpargneDTO decaisserRetrait(Long demandeId);

    /**
     * Annule une demande (avant décaissement)
     *
     * @param demandeId ID de la demande
     * @return demande annulée
     */
    DemandeRetraitEpargneDTO annulerDemande(Long demandeId);
}
