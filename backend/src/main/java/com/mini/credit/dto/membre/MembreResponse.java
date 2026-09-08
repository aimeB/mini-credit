package com.mini.credit.dto.membre;

import com.mini.credit.enums.Sexe;
import com.mini.credit.enums.StatutMembre;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MembreResponse {

    private Long id;

    private String codeMembre;

    private String nom;
    private String postnom;
    private String prenom;
    private String nomComplet;

    private Sexe sexe;
    private LocalDate dateNaissance;

    private String telephonePrincipal;
    private String telephoneSecondaire;
    private String email;

    private String adresse;
    private String quartier;
    private String commune;
    private String ville;

    private String professionActivite;
    private String lieuActivite;
    private String sourceInscription;

    private Long siteId;
    private String siteNom;

    private Long agentId;
    private String agentMatricule;

    private LocalDate dateAdhesion;
    private StatutMembre statut;

    private String photoUrl;
    private String observation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}