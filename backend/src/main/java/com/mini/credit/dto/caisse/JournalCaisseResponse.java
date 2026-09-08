package com.mini.credit.dto.caisse;

import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.NatureFinancementApprovisionnement;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.RoleCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class JournalCaisseResponse {
    private Long operationId;
    private Long sessionCaisseId;
    private Long caisseId;
    private String caisseLibelle;
    private Long siteId;
    private String siteLibelle;
    private LocalDateTime dateOperation;
    private LocalDate dateOperationJour;
    private LocalTime heureOperation;
    private Long utilisateurId;
    private String utilisateurNom;
    private RoleCode roleUtilisateur;
    private TypeOperationCaisse typeOperation;
    private CategorieOperationCaisse categorie;
    private NatureFinancementApprovisionnement natureFinancement;
    private SourceOperationCaisse source;
    private BigDecimal montant;
    private String devise;
    private BigDecimal soldeApresOperation;
    private String commentaire;
    private String referenceMetier;
    private Long recetteId;
    private Long depenseCaisseId;
    private Long creditId;
    private Long retraitEpargneId;
    private Long operationEpargneId;
    private String statutSession;
}