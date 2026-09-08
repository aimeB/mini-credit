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
public class RevenuDetailDto {
    private LocalDateTime date;
    private Long antenneId;
    private String antenne;
    private String categorie;
    private String nature;
    private String sousCategorie;
    private String source;
    private String reference;
    private String membre;
    private BigDecimal montant;
    private String utilisateur;
    private String observation;
}
