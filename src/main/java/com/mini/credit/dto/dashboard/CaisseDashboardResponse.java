package com.mini.credit.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CaisseDashboardResponse {
    private Long sessionCaisseId;
    private Long caisseId;
    private String caisseCode;
    private BigDecimal soldeOuverture;
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal soldeTheorique;
    private BigDecimal soldePhysique;
    private BigDecimal ecartCaisse;
    private List<CaisseCategorieItemResponse> repartitionEntrees;
    private List<CaisseCategorieItemResponse> repartitionSorties;
}