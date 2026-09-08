package com.mini.credit.dto.credit;

import com.mini.credit.enums.StatutEcheance;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class EcheanceCreditResponse {

    private Long id;
    private Integer numeroEcheance;
    private LocalDate dateEcheance;

    private BigDecimal principalPrevu;
    private BigDecimal interetPrevu;
    private BigDecimal penaliteCumulee;
    private BigDecimal totalPrevu;

    private BigDecimal principalPaye;
    private BigDecimal interetPaye;
    private BigDecimal penalitePayee;
    private BigDecimal totalPaye;

    private BigDecimal resteAPayer;

    private BigDecimal principalRestant;
    private BigDecimal interetRestant;
    private BigDecimal penaliteRestante;

    private LocalDate dateDernierPaiement;
    private StatutEcheance statut;
}