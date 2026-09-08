package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutGarantieMaterielle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "garantie_materielle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarantieMaterielle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "garantie_credit_id", nullable = false)
    private GarantieCredit garantieCredit;

    @Column(name = "type_bien", nullable = false, length = 100)
    private String typeBien;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "valeur_estimee", nullable = false, precision = 18, scale = 2)
    private BigDecimal valeurEstimee;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @Column(name = "proprietaire_declare", length = 200)
    private String proprietaireDeclare;

    @Column(name = "localisation", columnDefinition = "TEXT")
    private String localisation;

    @Column(name = "reference_document", length = 255)
    private String referenceDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutGarantieMaterielle statut = StatutGarantieMaterielle.DECLAREE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controle_par")
    private Utilisateur controlePar;

    @Column(name = "date_controle")
    private LocalDateTime dateControle;

    @Column(columnDefinition = "TEXT")
    private String commentaire;
}