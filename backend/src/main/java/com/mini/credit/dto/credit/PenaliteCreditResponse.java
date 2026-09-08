package com.mini.credit.dto.credit;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PenaliteCreditResponse {
    private Long creditId;
    private String numeroCredit;
    private BigDecimal penaliteTotaleCalculee;
    private List<PenaliteEcheanceResponse> echeancesEnRetard;
}