package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApportFinancementDetailDto {
    private Long operationId;
    private LocalDateTime date;
    private Long antenneId;
    private String antenne;
    private String categorie;
    private String natureFinancement;
    private String reference;
    private String utilisateur;
    @Builder.Default
    private BigDecimal montant = BigDecimal.ZERO;
    private String observation;
}
