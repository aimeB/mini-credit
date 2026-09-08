package com.mini.credit.dto.epargne;

import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.TypeOperationEpargne;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OperationEpargneResponse {
    private Long id;
    private Long compteEpargneId;
    private String numeroCompte;
    private Long membreId;
    private String membreNomComplet;
    private Long demandeCreditId;
    private Long creditId;
    private LocalDateTime dateOperation;
    private TypeOperationEpargne typeOperation;
    private BigDecimal montant;
    private SensOperation sens;
    private ModePaiement modePaiement;
    private String referenceExterne;
    private Long agentId;
    private Long sessionCaisseId;
    private String observation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
