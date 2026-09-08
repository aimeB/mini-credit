package com.mini.credit.entity.agence;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.employe.Employe;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entité Agence - Conteneur physique des opérations de microcrédit
 * Une agence regroupe sites, agents, employés, caisses et opérations
 */
@Entity
@Table(name = "agence", uniqueConstraints = {
    @UniqueConstraint(columnNames = "code_agence", name = "uk_agence_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agence extends BaseEntity {

    @Column(name = "code_agence", nullable = false, unique = true, length = 50)
    private String codeAgence;

    @Column(name = "nom_agence", nullable = false, length = 100)
    private String nomAgence;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "commune", length = 100)
    private String commune;

    @Column(name = "quartier", length = 100)
    private String quartier;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "actif", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Chef de Bureau de cette agence (OneToOne optional)
     * Doit être un Employe actif de la même agence
     */
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "chef_bureau_id", nullable = true, foreignKey = @ForeignKey(name = "fk_agence_chef_bureau"))
    private Employe chefBureau;

    /**
     * Validations métier de l'agence
     */
    @PrePersist
    @PreUpdate
    private void validateBeforePersist() {
        if (this.codeAgence != null) {
            this.codeAgence = this.codeAgence.trim().toUpperCase();
        }
        if (this.nomAgence != null) {
            this.nomAgence = this.nomAgence.trim().toUpperCase();
        }
    }

    /**
     * Activer l'agence
     */
    public void activate() {
        this.actif = true;
    }

    /**
     * Désactiver l'agence
     */
    public void deactivate() {
        this.actif = false;
    }

    /**
     * Assigner un Chef de Bureau à cette agence
     * Validations:
     * - Chef doit être un Employe (pas null)
     * - Chef doit appartenir à cette agence
     * - Chef doit être actif
     * 
     * @param employe Employe à assigner comme Chef, ou null pour retirer le chef
     * @throws IllegalArgumentException si validations échouent
     */
    public void setChefBureau(Employe employe) {
        if (employe != null) {
            // Chef doit être dans la même agence
            if (!employe.getAgence().getId().equals(this.getId())) {
                throw new IllegalArgumentException(
                    "Le Chef de Bureau doit appartenir à la même agence"
                );
            }
            // Chef doit être actif
            if (!employe.getActif()) {
                throw new IllegalArgumentException(
                    "Le Chef de Bureau doit être actif"
                );
            }
        }
        this.chefBureau = employe;
    }
}
