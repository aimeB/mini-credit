package com.mini.credit.dto.employe;

import com.mini.credit.enums.PosteEmploye;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeDTO {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String telephone;
    private String photoUrl;
    private String adresse;
    private String commune;
    private PosteEmploye fonction;
    private LocalDate dateEmbauche;
    private BigDecimal salaireBase;
    private BigDecimal primeFixe;
    private BigDecimal bonusVariable;
    private BigDecimal totalRemuneration;
    private Boolean actif;
    private Long agenceId;
    private String nomAgence;
    private Long siteId;
    private String nomSite;
    private Long utilisateurId;
    private String roleUtilisateur;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
