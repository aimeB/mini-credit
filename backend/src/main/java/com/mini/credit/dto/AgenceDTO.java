package com.mini.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour répondre avec les données d'une agence (Response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgenceDTO {

    private Long id;

    private String codeAgence;

    private String nomAgence;

    private String adresse;

    private String commune;

    private String quartier;

    private String reference;

    private String telephone;

    private String email;

    private String ville;

    private Boolean actif;

    private Long chefBureauId;          // 🆕 Chef de Bureau de l'agence

    private String description;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    /**
     * Indique si l'agence est active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.actif);
    }
}
