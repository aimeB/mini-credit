package com.mini.credit.service;

import com.mini.credit.dto.utilisateur.UtilisateurDTO;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.CreateUtilisateurWithCredentialsResponse;
import com.mini.credit.dto.utilisateur.UpdateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.dto.utilisateur.ChangeUsernameRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordRequest;
import com.mini.credit.dto.utilisateur.AdminPasswordResetRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordResponse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;

import java.util.List;

public interface UtilisateurService {
    UtilisateurDTO create(CreateUtilisateurRequest request);
    Utilisateur createRawEntity(CreateUtilisateurRequest request);
    CreateUtilisateurWithCredentialsResponse createRawEntityWithCredentials(CreateUtilisateurRequest request);
    UtilisateurDTO update(Long id, UpdateUtilisateurRequest request);
    UtilisateurDTO getById(Long id);
    List<UtilisateurDTO> getAll();
    List<UtilisateurDTO> getByRole(RoleCode roleCode);
    void delete(Long id);
    void changePassword(Long id, ChangePasswordRequest request);
    void changeOwnPassword(ChangePasswordRequest request);
    void resetPasswordSelf(Long id, ResetPasswordRequest request);
    ResetPasswordResponse resetPasswordByAdmin(Long targetUserId, AdminPasswordResetRequest request);
    UtilisateurDTO changeUsername(Long id, ChangeUsernameRequest request);
    boolean usernameExists(String username);

    /**
     * Utilisateurs éligibles pour la création d'un profil Agent Terrain.
     * Critères : actif + rôle AGENT_TERRAIN + employé avec fonction AGENT_TERRAIN + pas encore agent.
     */
    List<UtilisateurDTO> getDisponiblesAgentTerrain();
}
