package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenuParCategorieDto {
    private String categorie;
    private String sousCategorie;
    @Builder.Default
    private BigDecimal montant = BigDecimal.ZERO;
}
