package com.mini.credit.dto.credit;

import com.mini.credit.enums.StatutDemandeCredit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class FraisCreditAEncaisserResponse {
    private Long demandeCreditId;
    private String numeroDemande;
    private String membreNomComplet;
    private String siteNom;
    private String antenneNom;
    private LocalDate dateDemande;
    private BigDecimal montantDemande;
    private BigDecimal fraisDemande;
    private BigDecimal fraisDemandePayes;
    private BigDecimal resteFraisAPayer;
    private StatutDemandeCredit statutDemande;
    private String objetCredit;
    private boolean sessionCaisseRequise;
}