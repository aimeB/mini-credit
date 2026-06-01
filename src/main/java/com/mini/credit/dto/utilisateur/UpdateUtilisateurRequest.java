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
public class UpdateUtilisateurRequest {
    private String email;
    private String nomComplet;
    private String telephone;
    private Boolean active;
    private List<String> roles;
}
