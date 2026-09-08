package com.mini.credit.dto.utilisateur;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUtilisateurRequest {
    private String username;
    private String password;  // Optionnel - sera genere si null

    // Email : OPTIONNEL -- ne pas demander dans le flux principal
    private String email;

    /**
     * OPTIONNEL -- si employeId est fourni, nomComplet est recupere depuis Employe.
     * Ne pas faire confiance a ce champ si employeId est present.
     */
    private String nomComplet;

    /**
     * OPTIONNEL -- si employeId est fourni, telephone est recupere depuis Employe.
     * Ne pas faire confiance a ce champ si employeId est present.
     */
    private String telephone;

    private List<String> roles;

    /**
     * Employe lie -- OBLIGATOIRE pour les roles operationnels (sauf ADMIN et MEMBER).
     */
    private Long employeId;
}