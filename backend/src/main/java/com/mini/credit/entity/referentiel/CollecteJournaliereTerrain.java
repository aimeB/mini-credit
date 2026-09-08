package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.RecetteStatut;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collecte_journaliere_terrain",
    uniqueConstraints = @UniqueConstraint(name = "uk_collecte_agent_date", columnNames = {"agent_terrain_id", "date_collecte"}),
    indexes = {
        @Index(name = "idx_cjt_agent", columnList = "agent_terrain_id"),
        @Index(name = "idx_cjt_site", columnList = "site_id"),
        @Index(name = "idx_cjt_antenne", columnList = "antenne_id"),
        @Index(name = "idx_cjt_date", columnList = "date_collecte"),
        @Index(name = "idx_cjt_statut", columnList = "statut")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollecteJournaliereTerrain extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_terrain_id", nullable = false)
    private AgentTerrain agentTerrain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "antenne_id", nullable = false)
    private Long antenneId;

    @Column(name = "date_collecte", nullable = false)
    private LocalDate dateCollecte;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private RecetteStatut statut = RecetteStatut.BROUILLON;

    @Column(name = "especes_remises", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal especesRemises = BigDecimal.ZERO;

    @Column(name = "especes_declarees_agent", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal especesDeclareesAgent = BigDecimal.ZERO;

    @Column(name = "especes_confirmees_caissier", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal especesConfirmeesCaissier = BigDecimal.ZERO;

    @Column(name = "date_confirmation_billetage")
    private LocalDateTime dateConfirmationBilletage;

    @Column(name = "confirme_par_caissier_id")
    private Long confirmeParCaissierId;

    @Column(name = "observation_billetage", columnDefinition = "LONGTEXT")
    private String observationBilletage;

    @Column(name = "billetage_confirme", nullable = false)
    @Builder.Default
    private Boolean billetageConfirme = false;

    @Column(name = "total_epargne_calcule", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalEpargneCalcule = BigDecimal.ZERO;

    @Column(name = "total_remboursements_calcule", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRemboursementsCalcule = BigDecimal.ZERO;

    @Column(name = "total_frais_calcule", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalFraisCalcule = BigDecimal.ZERO;

    @Column(name = "total_carnets_calcule", nullable = false)
    @Builder.Default
    private Integer totalCarnetsCalcule = 0;

    @Column(name = "total_general_calcule", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalGeneralCalcule = BigDecimal.ZERO;

    @Column(name = "ecart_tresorerie", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ecartTresorerie = BigDecimal.ZERO;

    @Column(name = "observations", columnDefinition = "LONGTEXT")
    private String observations;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private Utilisateur validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "operations_generated_at")
    private LocalDateTime operationsGeneratedAt;

    @OneToMany(mappedBy = "collecte", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CollecteMembreLigne> lignes = new ArrayList<>();
}
