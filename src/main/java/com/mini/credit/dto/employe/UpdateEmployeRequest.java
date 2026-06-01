package com.mini.credit.dto.employe;

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
    private String telephone;
    private String adresse;
    private LocalDate dateEmbauche;
    private BigDecimal salaireBase;
    private BigDecimal primeFixe;
    private BigDecimal bonusVariable;
    private Boolean actif;
}
