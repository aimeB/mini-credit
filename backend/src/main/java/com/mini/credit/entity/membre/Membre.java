package com.mini.credit.entity.membre;


import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.Sexe;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.listener.MembreEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "membre")
@EntityListeners(MembreEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membre extends BaseEntity {


    @Column(name = "code_membre", nullable = false, unique = true, length = 50)
    private String codeMembre;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 100)
    private String postnom;

    @Column(length = 100)
    private String prenom;

    @Column(name = "nom_complet", nullable = false, length = 250)
    private String nomComplet;

    @Enumerated(EnumType.STRING)
    private Sexe sexe;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "telephone_principal", length = 30)
    private String telephonePrincipal;

    @Column(name = "telephone_secondaire", length = 30)
    private String telephoneSecondaire;

    private String adresse;
    private String quartier;
    private String commune;

    @Column(nullable = false, length = 100)
    private String ville = "Kinshasa";

    @Column(name = "profession_activite", length = 150)
    private String professionActivite;

    @Column(name = "lieu_activite")
    private String lieuActivite;

    @Column(name = "source_inscription", length = 100)
    private String sourceInscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentTerrain agent;

    @Column(name = "date_adhesion", nullable = false)
    private LocalDate dateAdhesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutMembre statut = StatutMembre.ACTIF;

    @Column(name = "photo_url")
    private String photoUrl;

    private String observation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<CompteEpargne> comptesEpargne = new ArrayList<>();

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<DemandeCredit> demandesCredit = new ArrayList<>();

    @OneToMany(mappedBy = "membre")
    @JsonIgnore
    @Builder.Default
    private List<Credit> credits = new ArrayList<>();

    /**
     * ⚠️ IMPORTANT: Synchronise bidirectionnellement la relation Membre <-> Utilisateur
     * Cela garantit que les deux côtés de la relation sont toujours à jour.
     * 
     * Si vous assignez un Utilisateur, utilisez TOUJOURS cette méthode:
     *   membre.setUtilisateurSync(user);
     * 
     * Et JAMAIS:
     *   membre.setUtilisateur(user);  ❌ N'utilise pas le setter normal!
     */
    public void setUtilisateurSync(Utilisateur utilisateur) {
        // Détacher l'ancien utilisateur s'il existe
        if (this.utilisateur != null) {
            this.utilisateur.setMembre(null);
        }

        // Assigner le nouvel utilisateur
        this.utilisateur = utilisateur;

        // Synchroniser l'autre côté de la relation
        if (utilisateur != null) {
            utilisateur.setMembre(this);
        }
    }
}
