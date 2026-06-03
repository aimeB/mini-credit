package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.CategorieParametre;
import com.mini.credit.enums.TypeParametre;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entité représentant les paramètres métier du système.
 * Centralise tous les paramètres de configuration finançière, taux, seuils, etc.
 * Permet de modifier les valeurs sans recompilation.
 *
 * PHASE 1: Infrastructure de base pour l'externalisation des constantes métier.
 * - cle : identifiant unique (ex: "MONTANT_CREDIT_MAX")
 * - valeurs : triples valeur (décimale, entière, texte) selon le type
 * - actif : permet de désactiver un paramètre sans le supprimer
 * - modifiable : indique si l'admin peut modifier cette valeur en runtime
 */
@Entity
@Table(name = "parametre_metier", indexes = {
    @Index(name = "idx_cle", columnList = "cle", unique = true),
    @Index(name = "idx_categorie", columnList = "categorie"),
    @Index(name = "idx_actif", columnList = "actif")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreMetier extends BaseEntity {

    @Column(name = "cle", nullable = false, unique = true, length = 100)
    private String cle;

    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_parametre", nullable = false, length = 20)
    private TypeParametre typeParametre;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 50)
    private CategorieParametre categorie;

    @Column(name = "valeur_decimale", precision = 19, scale = 4)
    private BigDecimal valeurDecimale;

    @Column(name = "valeur_entiere")
    private Long valeurEntiere;

    @Column(name = "valeur_texte", length = 500)
    private String valeurTexte;

    @Column(name = "unite", length = 50)
    private String unite;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "valeur_par_defaut", length = 500)
    private String valeurParDefaut;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "modifiable", nullable = false)
    @Builder.Default
    private Boolean modifiable = true;

    @Column(name = "modifie_par", length = 100)
    private String modifiePar;

    /**
     * Retourne la valeur du paramètre selon son type.
     * Utilise le type stocké pour retourner la bonne valeur.
     */
    public Object getValeur() {
        return switch (this.typeParametre) {
            case DECIMAL -> this.valeurDecimale;
            case ENTIER -> this.valeurEntiere;
            case TEXTE -> this.valeurTexte;
        };
    }
}
