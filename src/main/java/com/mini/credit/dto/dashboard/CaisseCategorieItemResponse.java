package com.mini.credit.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CaisseCategorieItemResponse {
    private String categorie;
    private BigDecimal montant;
}