package com.mini.credit.dto.document;

import com.mini.credit.enums.TypeTicketRecu;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TicketRecuGenerationRequest {
    private TypeTicketRecu typeTicket;
    private Long operationEpargneId;
    private Long operationCaisseId;
    private Long operationCaisseCommissionId;
    private Long demandeRetraitEpargneId;
    private Long collecteJournaliereId;
    private Long collecteMembreLigneId;
    private Long sessionCaisseId;
    private Long caisseId;
    private Long membreId;
    private Long compteEpargneId;
    private Long utilisateurCreateurId;
    private String devise;
    private BigDecimal montantPrincipal;
    private BigDecimal tauxCommission;
    private BigDecimal montantCommission;
    private BigDecimal montantTotalDebite;
    private BigDecimal montantRemisMembre;
    private BigDecimal ancienSolde;
    private BigDecimal nouveauSolde;
    private String commentaire;
}
