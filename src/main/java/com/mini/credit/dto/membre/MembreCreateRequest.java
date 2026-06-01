package com.mini.credit.dto.membre;

import com.mini.credit.enums.Sexe;
import com.mini.credit.enums.StatutMembre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MembreCreateRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String postnom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotNull(message = "Le sexe est obligatoire")
    private Sexe sexe;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    @NotBlank(message = "Le téléphone principal est obligatoire")
    private String telephonePrincipal;

    private String telephoneSecondaire;

    @NotBlank(message = "L'adresse est obligatoire")
    private String adresse;

    @NotBlank(message = "L'adresse est obligatoire")
    private String ville;

    @NotBlank(message = "La commune est obligatoire")
    private String commune;

    @NotBlank(message = "Le quartier est obligatoire")
    private String quartier;

    @NotBlank(message = "La profession/activité est obligatoire")
    private String professionActivite;

    @NotBlank(message = "Le lieu d'activité est obligatoire")
    private String lieuActivite;

    @NotBlank(message = "L'email est obligatoire")
    private String email;

    private String sourceInscription;

    @NotNull(message = "Le site est obligatoire")
    private Long siteId;

    // Agent n'est PAS obligatoire - sera assigné ultérieurement
    private Long agentId;

    @NotNull(message = "La date d'adhésion est obligatoire")
    private LocalDate dateAdhesion;

    private String observation;
}