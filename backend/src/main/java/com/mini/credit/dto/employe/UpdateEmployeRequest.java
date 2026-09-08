package com.mini.credit.dto.employe;

import com.mini.credit.enums.PosteEmploye;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmployeRequest {
    private String nom;
    private String prenom;

    @Pattern(regexp = "^\\+243\\d{9}$", message = "Le numéro doit être au format +243 suivi de 9 chiffres.")
    private String telephone;

    private String photoUrl;
    @JsonIgnore
    private boolean photoUrlProvided;

    private String adresse;
    private String commune;
    private PosteEmploye fonction;
    private LocalDate dateEmbauche;
    private BigDecimal salaireBase;
    private BigDecimal primeFixe;
    private BigDecimal bonusVariable;
    private Boolean actif;
    private Long agenceId;
    private Long siteId;
    // Permet de lier un compte utilisateur après la création de l'employé
    private Long utilisateurId;

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
        this.photoUrlProvided = true;
    }

    @JsonIgnore
    public boolean isPhotoUrlProvided() {
        return photoUrlProvided;
    }
}
