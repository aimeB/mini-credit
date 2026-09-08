package com.mini.credit.dto.credit;

import com.mini.credit.enums.StatutCredit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CreditEnCoursResponse {
    private Long id;
    private String numeroCredit;
    private String membreNomComplet;
    private BigDecimal montantAccorde;
    private BigDecimal totalPaye;
    private BigDecimal resteAPayer;
    private LocalDate dateDecaissement;
    private Integer dureeValeur;
    private String dureeUnite;
    private StatutCredit statut;
    private String antenneNom;
    private String agentTerrainNom;
    private String gestionnaireNom;
    private String controleurNom;
    private String chefBureauNom;
    private String caissierNom;
    private String devise;
}
