package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transport_site_parametre", indexes = {
        @Index(name = "idx_transport_site_param_site", columnList = "site_id"),
        @Index(name = "idx_transport_site_param_actif", columnList = "actif")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportSiteParametre extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "montant_transport_journalier_par_agent", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantTransportJournalierParAgent;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "date_debut_validite", nullable = false)
    private LocalDate dateDebutValidite;

    @Column(name = "date_fin_validite")
    private LocalDate dateFinValidite;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
