package com.mini.credit.entity.employe;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.enums.PosteEmploye;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employe", uniqueConstraints = {
        @UniqueConstraint(columnNames = "matricule")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    /**
     * Alias technique — égal au matricule au moment de la création (compatibilité).
     * Nullable depuis Phase 3 : le matricule est la référence principale.
     */
    @Column(nullable = true, length = 50)
    private String code_employe;

    @Column(name = "nom_complet", nullable = false, length = 150)
    private String nomComplet;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(length = 20)
    private String telephone;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "commune", length = 100)
    private String commune;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonction", length = 50)
    private PosteEmploye fonction;

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

    /**
     * Compte utilisateur associé. OPTIONNEL.
     * Source de vérité unique : utilisateur.employe_id (côté Utilisateur).
     * Employe est le côté inverse de la relation — pas de FK dans la table employe.
     */
    @OneToOne(mappedBy = "employe", fetch = FetchType.LAZY)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agence_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employe_agence"))
    private Agence agence;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "site_id", nullable = true, foreignKey = @ForeignKey(name = "fk_employe_site"))
    private Site site;

    public BigDecimal getTotalRemuneration() {
        BigDecimal base  = salaireBase  != null ? salaireBase  : BigDecimal.ZERO;
        BigDecimal prime = primeFixe    != null ? primeFixe    : BigDecimal.ZERO;
        BigDecimal bonus = bonusVariable != null ? bonusVariable : BigDecimal.ZERO;
        return base.add(prime).add(bonus);
    }
}
