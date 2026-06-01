package com.mini.credit.dto.caisse;

import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.TypeOperationCaisse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OperationCaisseResponse {
    private Long id;
    private String numeroPiece;
    private Long sessionCaisseId;
    private Long caisseId;
    private LocalDateTime dateOperation;
    private TypeOperationCaisse typeOperation;
    private CategorieOperationCaisse categorieOperation;
    private BigDecimal montant;
    private String devise;
    private Long membreId;
    private Long creditId;
    private Long remboursementId;
    private Long operationEpargneId;
    private Long paiementCreditId; // ✅ AJOUT
    private Long agentId;
    private Long createdById; // ✅ AJOUT
    private ModePaiement modePaiement; // ✅ AJOUT
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}