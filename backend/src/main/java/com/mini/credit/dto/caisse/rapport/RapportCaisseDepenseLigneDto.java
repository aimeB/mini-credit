package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RapportCaisseDepenseLigneDto {
    private Long depenseId;
    private Long sessionId;
    private Long caisseId;
    private String caisseLibelle;
    private Long siteId;
    private String siteNom;
    private String categorie;
    private BigDecimal montant;
    private String devise;
    private String statut;
    private String demandePar;
    private String validePar;
    private String payePar;
    private LocalDateTime dateDemande;
    private LocalDateTime dateValidation;
    private LocalDateTime datePaiement;
    private String motif;
}
