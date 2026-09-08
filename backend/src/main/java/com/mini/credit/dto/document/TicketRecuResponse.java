package com.mini.credit.dto.document;

import com.mini.credit.enums.StatutTicketRecu;
import com.mini.credit.enums.TypeTicketRecu;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TicketRecuResponse {
    private Long id;
    private String numeroTicket;
    private TypeTicketRecu typeTicket;
    private StatutTicketRecu statut;
    private Boolean duplicata;
    private String numeroOriginal;
    private Integer nombreImpressions;
    private Integer nombreDuplicatas;
    private LocalDateTime dateGeneration;
    private LocalDateTime dateDerniereImpression;
    private LocalDateTime dateDernierDuplicata;
    private Long operationEpargneId;
    private Long operationCaisseId;
    private Long operationCaisseCommissionId;
    private Long demandeRetraitEpargneId;
    private Long collecteJournaliereId;
    private Long collecteMembreLigneId;
    private String membreNomComplet;
    private String numeroMembre;
    private String numeroCompte;
    private String typeOperation;
    private String devise;
    private BigDecimal montantPrincipal;
    private BigDecimal tauxCommission;
    private BigDecimal montantCommission;
    private BigDecimal montantTotalDebite;
    private BigDecimal montantRemisMembre;
    private BigDecimal ancienSolde;
    private BigDecimal nouveauSolde;
    private String agenceNom;
    private String siteNom;
    private String caisseCode;
    private Long sessionCaisseId;
    private String utilisateurNom;
    private String controleurNom;
    private String caissierNom;
    private String codeVerification;
    private String qrPayload;
    private String commentaire;
    private String motifDuplicata;
}
