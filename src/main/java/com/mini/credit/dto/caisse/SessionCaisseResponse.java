package com.mini.credit.dto.caisse;

import com.mini.credit.enums.StatutSessionCaisse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SessionCaisseResponse {
    private Long id;
    private Long caisseId;
    private String caisseCode;
    private Long utilisateurId;
    private String utilisateurNom;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateCloture;
    private BigDecimal soldeOuverture;
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal soldeTheorique;
    private BigDecimal soldePhysique;
    private BigDecimal ecartCaisse;
    private StatutSessionCaisse statut;
    private String observation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}