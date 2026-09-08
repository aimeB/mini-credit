package com.mini.credit.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RetardEcheanceItemResponse {
    private Long creditId;
    private String numeroCredit;
    private Long membreId;
    private String membreNomComplet;
    private Long echeanceId;
    private Integer numeroEcheance;
    private LocalDate dateEcheance;
    private BigDecimal resteAPayer;
    private BigDecimal penaliteCumulee;
    private long joursRetard;
}