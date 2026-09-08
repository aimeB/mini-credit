package com.mini.credit.dto.credit;

import com.mini.credit.enums.StatutCredit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CreditRembourseResponse {
    private Long id;
    private String numeroCredit;
    private String membreNomComplet;
    private BigDecimal montantAccorde;
    private BigDecimal totalInteret;
    private BigDecimal totalPaye;
    private BigDecimal resteAPayer;
    private LocalDate dateDecaissement;
    private LocalDateTime dateDernierPaiement;
    private LocalDateTime dateCloture;
    private StatutCredit statut;
    private String antenneNom;
    private String devise;
}