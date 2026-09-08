package com.mini.credit.dto.credit;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CritereAnalyseRisqueDto {
    private String codeCritere;
    private String libelle;
    private BigDecimal points;
    private String niveau;
    private String commentaire;
}