package com.mini.credit.dto.utilisateur;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUtilisateurRequest {
    private String email;
    private String nomComplet;

    @Pattern(regexp = "^\\+243\\d{9}$", message = "Le numéro doit être au format +243 suivi de 9 chiffres.")
    private String telephone;

    private Boolean active;
    private List<String> roles;

    // Employé lié (optionnel — permet de lier un compte à une fiche employé existante)
    private Long employeId;
}
