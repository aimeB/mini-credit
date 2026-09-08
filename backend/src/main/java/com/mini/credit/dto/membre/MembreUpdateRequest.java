package com.mini.credit.dto.membre;

import com.mini.credit.enums.Sexe;
import com.mini.credit.enums.StatutMembre;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MembreUpdateRequest {

    private String nom;
    private String postnom;
    private String prenom;
    private String nomComplet;

    private Sexe sexe;
    private LocalDate dateNaissance;

    private String telephonePrincipal;
    private String telephoneSecondaire;

    private String adresse;
    private String ville;
    private String commune;
    private String quartier;

    private String professionActivite;
    private String lieuActivite;
    private String sourceInscription;

    private LocalDate dateAdhesion;
    private StatutMembre statut;

    private String observation;
    private Long agentId;
    private Long siteId;
}