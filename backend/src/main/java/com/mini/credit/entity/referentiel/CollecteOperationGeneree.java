package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "collecte_operation_generee",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cog_collecte_ligne_type", columnNames = {"collecte_id", "ligne_collecte_id", "type_operation"}),
        @UniqueConstraint(name = "uk_cog_type_operation_id", columnNames = {"type_operation", "operation_id"})
    },
    indexes = {
        @Index(name = "idx_cog_collecte", columnList = "collecte_id"),
        @Index(name = "idx_cog_ligne", columnList = "ligne_collecte_id"),
        @Index(name = "idx_cog_type", columnList = "type_operation")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollecteOperationGeneree extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collecte_id", nullable = false)
    private CollecteJournaliereTerrain collecte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_collecte_id")
    private CollecteMembreLigne ligneCollecte;

    @Column(name = "type_operation", nullable = false, length = 60)
    private String typeOperation;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    @Column(name = "created_by")
    private Long createdBy;
}
