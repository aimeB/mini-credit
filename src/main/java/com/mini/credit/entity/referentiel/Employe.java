package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.PosteEmploye;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entité représentant un employé du système.
 * Relie un utilisateur (authentification) à son poste métier et son site.
 * Un employé = Utilisateur + Poste + Site
 */
@Entity(name = "ReferentielEmploye")
@Table(name = "employe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String codeEmploye;

    @Column(nullable = false, length = 150)
    private String nomComplet;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 100)
    private String postnom;

    /**
     * Poste métier de l'employé
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PosteEmploye poste;

    /**
     * Site d'affectation principal
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employe_site"))
    private Site site;

    @Column(length = 20)
    private String telephone;

    @Column(length = 100)
    private String email;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @Column(name = "date_fin_emploi")
    private LocalDate dateFinEmploi;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(length = 500)
    private String observation;

    /**
     * Lien vers l'utilisateur associé (authentification)
     * Nullable car un employé peut ne pas avoir de compte système
     */
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "employe")
    private Utilisateur utilisateur;
}
