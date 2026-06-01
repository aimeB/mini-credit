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
    private String password;  // Optionnel - sera généré si null
    private String email;
    private String nomComplet;
    private String telephone;
    private List<String> roles;
}
