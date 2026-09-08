package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class RapportCaisseEcartLigneDto {
    private Long ecartId;
    private Long sessionId;
    private Long caisseId;
    private String caisseLibelle;
    private Long siteId;
    private String siteNom;
    private LocalDate dateJour;
    private String typeEcart;
    private String statut;
    private BigDecimal montantEcart;
    private Boolean seuilDepasse;
    private String enquetePar;
    private String validePar;
    private LocalDateTime dateEnquete;
    private LocalDateTime dateValidation;
    private String description;
}
