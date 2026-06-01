package com.mini.credit.dto.employe;

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
    private String telephone;
    private String adresse;
    private LocalDate dateEmbauche;
    private BigDecimal salaireBase;
    private BigDecimal primeFixe;
    private BigDecimal bonusVariable;
    private BigDecimal totalRemuneration;
    private Boolean actif;
    private Long utilisateurId;
    private String roleUtilisateur;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
