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
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "membre")
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

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CompteEpargne> comptesEpargne = new ArrayList<>();

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DemandeCredit> demandesCredit = new ArrayList<>();

    @OneToMany(mappedBy = "membre")
    @Builder.Default
    private List<Credit> credits = new ArrayList<>();
}
