package com.mini.credit.service.security;

import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service de contrôle de scope pour l'accès aux données.
 * 
 * Règles :
 * - ADMIN : Accès complet à toutes les données
 * - RESPONSABLE : Accès complet (décisions stratégiques)
 * - CAISSIER : Accès complet aux données métier (pas les données personnelles)
 * - AGENT_TERRAIN : Accès aux données de son site_id
 * - AGENT_BUREAU : Accès complet (par défaut)
 * - MEMBER : Accès seulement à ses propres données (membreId = currentUser.membreId)
 * 
 * Étape 9 : Scope implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeService {

    private final MembreRepository membreRepository;
    private final DemandeCreditRepository demandeCreditRepository;
    private final CreditRepository creditRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final CaisseRepository caisseRepository;

    /**
     * Vérifie si l'utilisateur courant peut lire les données d'un membre.
     * 
     * @param membreId ID du membre à accéder
     * @return true si l'accès est autorisé, false sinon
     */
    public boolean canReadMembre(Long membreId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canReadMembre: Utilisateur non authentifié");
            return false;
        }

        // ADMIN et RESPONSABLE : accès complet
        if (isAdmin(currentUser) || isResponsable(currentUser)) {
            return true;
        }

        // AGENT_BUREAU : accès complet
        if (isAgentBureau(currentUser)) {
            return true;
        }

        // CAISSIER : accès complet
        if (isCaissier(currentUser)) {
            return true;
        }

        // AGENT_TERRAIN : filtre par site
        if (isAgentTerrain(currentUser)) {
            return canAccessByAgentSite(currentUser, membreId);
        }

        // MEMBER : accès seulement à ses propres données
        if (isMember(currentUser)) {
            return currentUser.getMembre() != null && currentUser.getMembre().getId().equals(membreId);
        }

        log.warn("canReadMembre: Rôle {} non géré pour membreId {}", currentUser.getRole().getCode(), membreId);
        return false;
    }

    /**
     * Vérifie si l'utilisateur courant peut lire une demande de crédit.
     * 
     * @param demandeId ID de la demande
     * @return true si l'accès est autorisé, false sinon
     */
    public boolean canReadDemandeCredit(Long demandeId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canReadDemandeCredit: Utilisateur non authentifié");
            return false;
        }

        // ADMIN et RESPONSABLE : accès complet
        if (isAdmin(currentUser) || isResponsable(currentUser)) {
            return true;
        }

        // AGENT_BUREAU : accès complet
        if (isAgentBureau(currentUser)) {
            return true;
        }

        // CAISSIER : accès complet
        if (isCaissier(currentUser)) {
            return true;
        }

        DemandeCredit demande = demandeCreditRepository.findById(demandeId).orElse(null);
        if (demande == null) {
            log.warn("canReadDemandeCredit: Demande {} introuvable", demandeId);
            return false;
        }

        // AGENT_TERRAIN : filtre par site
        if (isAgentTerrain(currentUser)) {
            return demande.getSite() != null && canAccessByAgentSite(currentUser, demande.getMembre().getId());
        }

        // MEMBER : accès seulement à ses propres demandes
        if (isMember(currentUser)) {
            return currentUser.getMembre() != null && 
                   currentUser.getMembre().getId().equals(demande.getMembre().getId());
        }

        log.warn("canReadDemandeCredit: Rôle {} non géré pour demandeId {}", currentUser.getRole().getCode(), demandeId);
        return false;
    }

    /**
     * Vérifie si l'utilisateur courant peut lire un crédit.
     * 
     * @param creditId ID du crédit
     * @return true si l'accès est autorisé, false sinon
     */
    public boolean canReadCredit(Long creditId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canReadCredit: Utilisateur non authentifié");
            return false;
        }

        // ADMIN et RESPONSABLE : accès complet
        if (isAdmin(currentUser) || isResponsable(currentUser)) {
            return true;
        }

        // AGENT_BUREAU : accès complet
        if (isAgentBureau(currentUser)) {
            return true;
        }

        // CAISSIER : accès complet
        if (isCaissier(currentUser)) {
            return true;
        }

        Credit credit = creditRepository.findById(creditId).orElse(null);
        if (credit == null) {
            log.warn("canReadCredit: Crédit {} introuvable", creditId);
            return false;
        }

        // AGENT_TERRAIN : filtre par site
        if (isAgentTerrain(currentUser)) {
            return canAccessByAgentSite(currentUser, credit.getMembre().getId());
        }

        // MEMBER : accès seulement à ses propres crédits
        if (isMember(currentUser)) {
            return currentUser.getMembre() != null && 
                   currentUser.getMembre().getId().equals(credit.getMembre().getId());
        }

        log.warn("canReadCredit: Rôle {} non géré pour creditId {}", currentUser.getRole().getCode(), creditId);
        return false;
    }

    /**
     * Vérifie si l'utilisateur courant peut lire un compte épargne.
     * 
     * @param compteId ID du compte épargne
     * @return true si l'accès est autorisé, false sinon
     */
    public boolean canReadCompteEpargne(Long compteId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canReadCompteEpargne: Utilisateur non authentifié");
            return false;
        }

        // ADMIN et RESPONSABLE : accès complet
        if (isAdmin(currentUser) || isResponsable(currentUser)) {
            return true;
        }

        // AGENT_BUREAU et CAISSIER : accès complet
        if (isAgentBureau(currentUser) || isCaissier(currentUser)) {
            return true;
        }

        CompteEpargne compte = compteEpargneRepository.findById(compteId).orElse(null);
        if (compte == null) {
            log.warn("canReadCompteEpargne: Compte {} introuvable", compteId);
            return false;
        }

        // AGENT_TERRAIN : filtre par site
        if (isAgentTerrain(currentUser)) {
            return canAccessByAgentSite(currentUser, compte.getMembre().getId());
        }

        // MEMBER : accès seulement à ses propres comptes
        if (isMember(currentUser)) {
            return currentUser.getMembre() != null && 
                   currentUser.getMembre().getId().equals(compte.getMembre().getId());
        }

        log.warn("canReadCompteEpargne: Rôle {} non géré pour compteId {}", currentUser.getRole().getCode(), compteId);
        return false;
    }

    /**
     * Vérifie si un AGENT_TERRAIN peut accéder aux données d'un membre.
     * L'agent peut accéder si :
     * 1. Le membre appartient au même site que l'agent
     * 2. OU l'agent a suivi le membre
     * 
     * @param agentUser Utilisateur avec rôle AGENT_TERRAIN
     * @param membreId ID du membre
     * @return true si l'accès est autorisé
     */
    private boolean canAccessByAgentSite(Utilisateur agentUser, Long membreId) {
        try {
            AgentTerrain agent = agentTerrainRepository.findByUtilisateurId(agentUser.getId())
                    .orElse(null);

            if (agent == null) {
                log.warn("canAccessByAgentSite: Agent {} sans AgentTerrain assigné", agentUser.getId());
                return false;
            }

            if (agent.getSite() == null) {
                log.warn("canAccessByAgentSite: Agent {} sans site assigné", agentUser.getId());
                return false;
            }

            Membre membre = membreRepository.findById(membreId).orElse(null);
            if (membre == null) {
                log.warn("canAccessByAgentSite: Membre {} introuvable", membreId);
                return false;
            }

            // Le membre appartient au même site
            if (membre.getSite() != null && membre.getSite().getId().equals(agent.getSite().getId())) {
                return true;
            }

            // Optionnel : vérifier si l'agent a suivi le membre
            // if (agent.getMembresSuivis().contains(membre)) {
            //     return true;
            // }

            log.debug("canAccessByAgentSite: Agent {} (site {}) ne peut pas accéder au membre {} (site {})", 
                    agentUser.getId(), agent.getSite().getId(), membreId, 
                    membre.getSite() != null ? membre.getSite().getId() : "NONE");
            return false;

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du scope agent", e);
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur courant peut accéder à un autre utilisateur
     * Un utilisateur ne peut accéder qu'à son propre compte, sauf s'il est ADMIN
     * 
     * @param userId L'ID de l'utilisateur à accéder
     * @return true si l'accès est autorisé, false sinon
     */
    public boolean canAccessCurrentUser(Long userId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canAccessCurrentUser: Utilisateur non authentifié");
            return false;
        }

        // Un administrateur peut accéder à n'importe quel utilisateur
        if (isAdmin(currentUser)) {
            return true;
        }

        // Un utilisateur normal ne peut accéder que son propre compte
        return currentUser.getId().equals(userId);
    }

    // ============ Helpers pour vérifier le rôle ============

    private boolean isAdmin(Utilisateur user) {
        return user.getRole() != null && user.getRole().getCode() == RoleCode.ADMIN;
    }

    private boolean isResponsable(Utilisateur user) {
        return user.getRole() != null && user.getRole().getCode() == RoleCode.RESPONSABLE;
    }

    private boolean isAgentBureau(Utilisateur user) {
        return user.getRole() != null && user.getRole().getCode() == RoleCode.AGENT_BUREAU;
    }

    private boolean isAgentTerrain(Utilisateur user) {
        return user.getRole() != null && user.getRole().getCode() == RoleCode.AGENT_TERRAIN;
    }

    private boolean isCaissier(Utilisateur user) {
        return user.getRole() != null && user.getRole().getCode() == RoleCode.CAISSIER;
    }

    private boolean isMember(Utilisateur user) {
        return user.getRole() != null && user.getRole().getCode() == RoleCode.MEMBER;
    }

    /**
     * Vérifie si l'utilisateur courant peut enregistrer une opération d'épargne pour un membre.
     * 
     * Règles:
     * - ADMIN, RESPONSABLE: accès complet
     * - CAISSIER: accès complet (gère la caisse pour tous)
     * - AGENT_BUREAU: accès uniquement si le membre est dans son site
     * 
     * @param membreId ID du membre
     * @return true si accès autorisé
     */
    public boolean canRecordEpargneOperation(Long membreId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canRecordEpargneOperation: Utilisateur non authentifié");
            return false;
        }

        // ADMIN et RESPONSABLE : accès complet
        if (isAdmin(currentUser) || isResponsable(currentUser)) {
            return true;
        }

        // CAISSIER : accès complet
        if (isCaissier(currentUser)) {
            return true;
        }

        // AGENT_BUREAU : accès uniquement si même site
        if (isAgentBureau(currentUser)) {
            Membre membre = membreRepository.findById(membreId).orElse(null);
            if (membre == null) {
                log.warn("canRecordEpargneOperation: Membre {} introuvable", membreId);
                return false;
            }
            
            // Utiliser le site du Utilisateur directement (pas AgentTerrain)
            if (currentUser.getSite() == null) {
                log.warn("canRecordEpargneOperation: AgentBureau {} n'a pas de site assigné", currentUser.getId());
                return false;
            }
            
            if (membre.getSite() == null) {
                log.warn("canRecordEpargneOperation: Membre {} sans site", membreId);
                return false;
            }
            
            return membre.getSite().getId().equals(currentUser.getSite().getId());
        }

        log.warn("canRecordEpargneOperation: Rôle {} non autorisé pour membreId {}", 
                currentUser.getRole().getCode(), membreId);
        return false;
    }

    /**
     * Vérifie si l'utilisateur courant peut enregistrer un remboursement pour un crédit.
     * 
     * Règles:
     * - ADMIN, RESPONSABLE, CAISSIER: accès complet
     * - MEMBER: accès uniquement si le crédit lui appartient
     * 
     * @param creditId ID du crédit
     * @return true si accès autorisé
     */
    public boolean canRecordRemboursement(Long creditId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canRecordRemboursement: Utilisateur non authentifié");
            return false;
        }

        // ADMIN, RESPONSABLE, CAISSIER : accès complet
        if (isAdmin(currentUser) || isResponsable(currentUser) || isCaissier(currentUser)) {
            return true;
        }

        Credit credit = creditRepository.findById(creditId).orElse(null);
        if (credit == null) {
            log.warn("canRecordRemboursement: Crédit {} introuvable", creditId);
            return false;
        }

        // MEMBER : accès seulement à ses propres crédits
        if (isMember(currentUser)) {
            return currentUser.getMembre() != null && 
                   currentUser.getMembre().getId().equals(credit.getMembre().getId());
        }

        log.warn("canRecordRemboursement: Rôle {} non autorisé pour creditId {}", 
                currentUser.getRole().getCode(), creditId);
        return false;
    }

    /**
     * Vérifie si l'utilisateur courant peut créer une demande de crédit pour un membre.
     * 
     * Règles:
     * - ADMIN: accès complet
     * - AGENT_BUREAU: accès uniquement si le membre est dans son site
     * - MEMBER: accès uniquement si création pour lui-même
     * 
     * @param membreId ID du membre
     * @return true si accès autorisé
     */
    public boolean canCreateDemandeCredit(Long membreId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            log.warn("canCreateDemandeCredit: Utilisateur non authentifié");
            return false;
        }

        // ADMIN : accès complet
        if (isAdmin(currentUser)) {
            return true;
        }

        // AGENT_BUREAU : accès uniquement si même site
        if (isAgentBureau(currentUser)) {
            Membre membre = membreRepository.findById(membreId).orElse(null);
            if (membre == null) {
                log.warn("canCreateDemandeCredit: Membre {} introuvable", membreId);
                return false;
            }
            
            // Utiliser le site du Utilisateur directement (pas AgentTerrain)
            if (currentUser.getSite() == null) {
                log.warn("canCreateDemandeCredit: AgentBureau {} n'a pas de site assigné", currentUser.getId());
                return false;
            }
            
            if (membre.getSite() == null) {
                log.warn("canCreateDemandeCredit: Membre {} sans site", membreId);
                return false;
            }
            
            return membre.getSite().getId().equals(currentUser.getSite().getId());
        }

        // MEMBER : création seulement pour lui-même
        if (isMember(currentUser)) {
            return currentUser.getMembre() != null && 
                   currentUser.getMembre().getId().equals(membreId);
        }

        log.warn("canCreateDemandeCredit: Rôle {} non autorisé pour membreId {}", 
                currentUser.getRole().getCode(), membreId);
        return false;
    }
}
