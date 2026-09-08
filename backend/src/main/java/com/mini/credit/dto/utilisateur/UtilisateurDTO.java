package com.mini.credit.dto.utilisateur;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurDTO {
    private Long id;
    private String username;
    private String email;
    private String nomComplet;
    private String telephone;
    private Boolean active;
    private List<String> roles;
    private Boolean passwordResetRequired;
    private Boolean passwordChangeRequired;
    // Employe lie (null si ADMIN technique ou compte sans fiche employe)
    private Long employeId;
    private String employeMatricule;
    private String employeNomComplet;
    /** Fonction metier de l employe lie, ex : CAISSIER, AGENT_TERRAIN */
    private String employeFonction;
    /** Telephone de l employe lie (pour affichage consolide) */
    private String employeTelephone;
    private String employeAgenceNom;
    private String employeSiteNom;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}