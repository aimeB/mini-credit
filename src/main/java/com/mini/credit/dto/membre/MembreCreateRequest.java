package com.mini.credit.dto.membre;

import com.mini.credit.enums.Sexe;
import com.mini.credit.enums.StatutMembre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MembreCreateRequest {

    @NotBlank
    private String nom;

    private String postnom;
    private String prenom;

    @NotBlank
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

    @NotBlank
    private String codeMembre;

    @NotNull
    private LocalDate dateAdhesion;

    @NotNull
    private StatutMembre statut;

    private Long agentId;
    private Long siteId;
}