package com.mini.credit.dto.epargne;

import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.TypeCompteEpargne;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CompteEpargneResponse {
    private Long id;
    private Long membreId;
    private String membreNomComplet;
    private String numeroCompte;
    private TypeCompteEpargne typeCompte;
    private BigDecimal soldeDisponible;
    private BigDecimal soldeBloque;
    private StatutCompte statut;
    private LocalDate dateOuverture;
    private LocalDate dateFermeture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}