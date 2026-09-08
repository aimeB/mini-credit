package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class RapportCaisseSessionDto {
    private Long sessionId;
    private Long caisseId;
    private String caisseCode;
    private String caisseLibelle;
    private Long siteId;
    private String siteNom;
    private LocalDate dateComptable;
    private String statut;
    private String utilisateur;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateCloture;
    private BigDecimal soldeOuverture;
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal soldeTheorique;
    private BigDecimal soldePhysique;
    private BigDecimal ecartCaisse;
    private long nombreOperations;
    private BigDecimal totalDepensesPayees;
}
