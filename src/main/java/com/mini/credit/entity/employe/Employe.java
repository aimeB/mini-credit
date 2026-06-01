package com.mini.credit.entity.employe;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employe", uniqueConstraints = {
        @UniqueConstraint(columnNames = "matricule"),
        @UniqueConstraint(columnNames = "utilisateur_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(length = 20)
    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Column(name = "salaire_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal salaireBase;

    @Column(name = "prime_fixe", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal primeFixe = BigDecimal.ZERO;

    @Column(name = "bonus_variable", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal bonusVariable = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employe_utilisateur"))
    private Utilisateur utilisateur;

    public BigDecimal getTotalRemuneration() {
        return salaireBase.add(primeFixe).add(bonusVariable);
    }
}
