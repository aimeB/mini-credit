package com.mini.credit.dto.credit;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class PenaliteEcheanceResponse {
    private Long echeanceId;
    private Integer numeroEcheance;
    private LocalDate dateEcheance;
    private long joursRetard;
    private BigDecimal penaliteCumulee;
    private BigDecimal resteAPayer;
}
