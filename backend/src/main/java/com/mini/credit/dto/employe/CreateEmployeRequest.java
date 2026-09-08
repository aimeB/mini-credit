package com.mini.credit.dto.employe;

import com.mini.credit.enums.PosteEmploye;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de création d'un employé.
 * Le matricule n'est PAS fourni : il est généré automatiquement par MatriculeGeneratorService
 * au format AGENCE-FONCTION-AA-SEQ (ex. DEL1-GES-26-001).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeRequest {

    @NotBlank(message = "Nom est obligatoire")
    @Size(min = 1, max = 100, message = "Nom doit avoir entre 1 et 100 caractères")
    private String nom;

    @NotBlank(message = "Prénom est obligatoire")
    @Size(min = 1, max = 100, message = "Prénom doit avoir entre 1 et 100 caractères")
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+243\\d{9}$", message = "Le numéro doit être au format +243 suivi de 9 chiffres.")
    private String telephone;

    private String photoUrl;

    @Size(max = 255, message = "Adresse doit avoir maximum 255 caractères")
    private String adresse;

    @Size(max = 100, message = "Commune doit avoir maximum 100 caractères")
    private String commune;

    @NotNull(message = "La fonction est obligatoire")
    private PosteEmploye fonction;

    @NotNull(message = "Date d'embauche est obligatoire")
    private LocalDate dateEmbauche;

    @NotNull(message = "Salaire de base est obligatoire")
    @DecimalMin(value = "0.0", message = "Salaire de base doit être positif")
    private BigDecimal salaireBase;

    @DecimalMin(value = "0.0", message = "Prime fixe doit être positive")
    private BigDecimal primeFixe;

    @DecimalMin(value = "0.0", message = "Bonus variable doit être positif")
    private BigDecimal bonusVariable;

    @NotNull(message = "Agence est obligatoire")
    private Long agenceId;

    private Long siteId;

    /** OPTIONNEL : l'employé peut exister avant d'avoir un compte utilisateur */
    private Long utilisateurId;
}
