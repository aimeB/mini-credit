package com.mini.credit.entity.referentiel;


import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site extends BaseEntity {



    @Column(name = "code_site", nullable = false, unique = true, length = 30)
    private String codeSite;

    @Column(name = "nom_site", nullable = false, length = 100)
    private String nomSite;

    /**
     * Zone opérationnelle du site (ex: "De la Place Météo jusqu'à l'avenue X").
     * Remplace la notion d'adresse fixe — la localisation admin vient de l'Agence.
     */
    @Column(nullable = false, length = 255)
    private String zone;

    /**
     * Repère / référence pour localiser le site sur le terrain.
     * Ex: "près du marché Gambela", "arrêt bus école Saint-Pierre".
     */
    @Column(length = 255)
    private String reference;

    /** Observation libre (note opérationnelle). */
    @Column(columnDefinition = "TEXT")
    private String observation;

    // Champs géographiques conservés pour compatibilité DB (obsolètes côté métier).
    // La localisation administrative provient désormais de l'Agence.
    /** @deprecated Utiliser l'agence pour la ville. */
    @Deprecated
    private String adresse;
    /** @deprecated Utiliser l'agence pour la commune. */
    @Deprecated
    private String commune;
    /** @deprecated Utiliser l'agence pour la ville. */
    @Deprecated
    @Column(nullable = true, length = 100)
    private String ville;

    @Column(nullable = false)
    private Boolean actif = true;

    // NEW Phase 3: Site belongs to Agence
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agence_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_site_agence"))
    private Agence agence;

    @OneToMany(mappedBy = "site")
    @Builder.Default
    private List<Membre> membres = new ArrayList<>();

    // Phase 4: N:M relationship with AgentTerrain (inverse side)
    @ManyToMany(mappedBy = "sitesAffectes", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private Set<AgentTerrain> agentsTerrain = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void normalizeBeforeSave() {
        if (this.codeSite != null) {
            this.codeSite = this.codeSite.trim().toUpperCase();
        }
        if (this.nomSite != null) {
            this.nomSite = this.nomSite.trim().toUpperCase();
        }
    }
}
