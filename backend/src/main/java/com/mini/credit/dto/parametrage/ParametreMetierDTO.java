package com.mini.credit.dto.parametrage;

import com.mini.credit.enums.CategorieParametre;
import com.mini.credit.enums.TypeParametre;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO pour les paramètres métier.
 * Utilisé pour les réponses API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreMetierDTO implements Serializable {

    private Long id;
    private String cle;
    private String libelle;
    private TypeParametre typeParametre;
    private CategorieParametre categorie;
    private BigDecimal valeurDecimale;
    private Long valeurEntiere;
    private String valeurTexte;
    private String unite;
    private String description;
    private String valeurParDefaut;
    private Boolean actif;
    private Boolean modifiable;
    private String modifiePar;

    /**
     * Retourne la valeur du paramètre selon son type.
     */
    public Object getValeur() {
        return switch (this.typeParametre) {
            case DECIMAL -> this.valeurDecimale;
            case ENTIER -> this.valeurEntiere;
            case TEXTE -> this.valeurTexte;
        };
    }
}
