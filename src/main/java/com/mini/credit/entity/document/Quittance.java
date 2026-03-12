package com.mini.credit.entity.document;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.TypeQuittance;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quittance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quittance extends BaseEntity {



    @Column(name = "numero_quittance", nullable = false, unique = true, length = 50)
    private String numeroQuittance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id")
    private Membre membre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_quittance", nullable = false, length = 20)
    private TypeQuittance typeQuittance;

    @Column(name = "reference_operation", length = 100)
    private String referenceOperation;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @Column(name = "date_emission", nullable = false)
    private LocalDateTime dateEmission;

    @Column(name = "fichier_url")
    private String fichierUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
}
