package com.mini.credit.dto.caisse;

import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.NatureFinancementApprovisionnement;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.RoleCode;
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
    private String caisseLibelle;
    private Long siteId;
    private String siteLibelle;
    private LocalDateTime dateOperation;
    private TypeOperationCaisse typeOperation;
    private CategorieOperationCaisse categorieOperation;
    private NatureFinancementApprovisionnement natureFinancement;
    private BigDecimal montant;
    private BigDecimal soldeApresOperation;
    private String devise;
    private Long membreId;
    private Long creditId;
    private Long retraitEpargneId;
    private Long remboursementId;
    private Long operationEpargneId;
    private Long depenseCaisseId;
    private Long paiementCreditId; // ✅ AJOUT
    private Long agentId;
    private Long createdById; // ✅ AJOUT
    private Long utilisateurId;
    private String utilisateurNom;
    private RoleCode roleUtilisateur;
    private ModePaiement modePaiement; // ✅ AJOUT
    private SourceOperationCaisse source;
    private String referenceExterne;
    private String referenceMetier;
    private String description;
    private String observation;
    private String commentaire;
    private String statutSession;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}