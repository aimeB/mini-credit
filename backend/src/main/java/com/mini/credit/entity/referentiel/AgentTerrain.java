package com.mini.credit.entity.referentiel;


import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.employe.Employe;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "agent_terrain")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTerrain extends BaseEntity {



    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "date_affectation")
    private LocalDate dateAffectation;

    /**
     * Gestionnaire responsable de cet agent terrain.
     * Doit être un Employe actif avec fonction = GESTIONNAIRE.
     * Nullable pour compatibilité avec les données existantes.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "gestionnaire_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_agent_gestionnaire"))
    private Employe gestionnaire;

    @Column(nullable = false)
    private Boolean actif = true;

    // Phase 4: N:M relationship with Site
    // Allows agent to be assigned to multiple sites
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "agent_terrain_site",
            joinColumns = @JoinColumn(name = "agent_terrain_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "site_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_agent_site_unique",
                    columnNames = {"agent_terrain_id", "site_id"}
            )
    )
    @Builder.Default
    private Set<Site> sitesAffectes = new HashSet<>();
}
