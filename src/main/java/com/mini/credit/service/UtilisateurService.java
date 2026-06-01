package com.mini.credit.service;

import com.mini.credit.dto.utilisateur.UtilisateurDTO;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.CreateUtilisateurWithCredentialsResponse;
import com.mini.credit.dto.utilisateur.UpdateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.dto.utilisateur.ChangeUsernameRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordRequest;
import com.mini.credit.entity.referentiel.Utilisateur;

import java.util.List;

public interface UtilisateurService {
    UtilisateurDTO create(CreateUtilisateurRequest request);
    Utilisateur createRawEntity(CreateUtilisateurRequest request);
    CreateUtilisateurWithCredentialsResponse createRawEntityWithCredentials(CreateUtilisateurRequest request);
    UtilisateurDTO update(Long id, UpdateUtilisateurRequest request);
    UtilisateurDTO getById(Long id);
    List<UtilisateurDTO> getAll();
    void delete(Long id);
    void changePassword(Long id, ChangePasswordRequest request);
    void resetPasswordSelf(Long id, ResetPasswordRequest request);
    UtilisateurDTO changeUsername(Long id, ChangeUsernameRequest request);
    boolean usernameExists(String username);
}
