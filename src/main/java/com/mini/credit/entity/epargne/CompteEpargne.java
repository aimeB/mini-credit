package com.mini.credit.entity.epargne;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.TypeCompteEpargne;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compte_epargne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteEpargne extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "numero_compte", nullable = false, unique = true, length = 50)
    private String numeroCompte;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_compte", nullable = false, length = 30)
    private TypeCompteEpargne typeCompte;

    @Column(name = "solde_disponible", nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeDisponible = BigDecimal.ZERO;

    @Column(name = "solde_bloque", nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeBloque = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCompte statut = StatutCompte.ACTIF;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDate dateOuverture;

    @Column(name = "date_fermeture")
    private LocalDate dateFermeture;

    @OneToMany(mappedBy = "compteEpargne", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OperationEpargne> operations = new ArrayList<>();
}
