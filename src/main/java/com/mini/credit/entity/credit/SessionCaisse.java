package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutSessionCaisse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_caisse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCaisse extends BaseEntity {



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDateTime dateOuverture;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "solde_ouverture", nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeOuverture = BigDecimal.ZERO;

    @Column(name = "total_entrees", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEntrees = BigDecimal.ZERO;

    @Column(name = "total_sorties", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalSorties = BigDecimal.ZERO;

    @Column(name = "solde_theorique", nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeTheorique = BigDecimal.ZERO;

    @Column(name = "solde_physique", precision = 18, scale = 2)
    private BigDecimal soldePhysique;

    @Column(name = "ecart_caisse", precision = 18, scale = 2)
    private BigDecimal ecartCaisse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSessionCaisse statut = StatutSessionCaisse.OUVERTE;

    private String observation;
}
