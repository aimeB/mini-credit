package com.mini.credit.dto.caisse;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DepenseCaisseBeneficiaireSalaireResponse {
    private Long id;
    private Long employeId;
    private String matricule;
    private String nom;
    private String role;
    private Long agenceId;
    private String agenceNom;
    private BigDecimal salaireBase;
    private BigDecimal primeFixe;
    private BigDecimal bonusVariable;
    private BigDecimal totalRemuneration;
    private String affichage;
}
