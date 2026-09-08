package com.mini.credit.dto.referentiel;

import com.mini.credit.enums.RecetteStatut;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteTerrainResponse {
    private Long id;
    private Long agentTerrainId;
    private String agentTerrainNom;
    private Long siteId;
    private String siteNom;
    private Long antenneId;
    private LocalDate dateCollecte;
    private RecetteStatut statut;
    private BigDecimal especesRemises;
    private BigDecimal especesDeclareesAgent;
    private BigDecimal especesConfirmeesCaissier;
    private java.time.LocalDateTime dateConfirmationBilletage;
    private Long confirmeParCaissierId;
    private String confirmeParCaissierNom;
    private Long billetagePar;
    private String billetageParNom;
    private LocalDateTime dateBilletage;
    private String observationBilletage;
    private Boolean billetageConfirme;
    private BigDecimal totalEpargneCalcule;
    private BigDecimal totalRemboursementsCalcule;
    private BigDecimal totalFraisCalcule;
    private Integer totalCarnetsCalcule;
    private BigDecimal totalGeneralCalcule;
    private BigDecimal ecartTresorerie;
    private String observations;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private Long validatedBy;
    private String validatedByNom;
    private LocalDateTime validatedAt;
    private LocalDateTime operationsGeneratedAt;
    private Integer operationsGeneratedCount;
    private String generationSummary;
    private List<CollecteMembreLigneResponse> lignes;
}