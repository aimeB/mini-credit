package com.mini.credit.mapper;

import com.mini.credit.dto.parametrage.ParametreMetierDTO;
import com.mini.credit.entity.referentiel.ParametreMetier;
import org.springframework.stereotype.Component;

@Component
public class ParametreMetierMapper {

    public ParametreMetierDTO toDTO(ParametreMetier entity) {
        if (entity == null) {
            return null;
        }

        return ParametreMetierDTO.builder()
                .id(entity.getId())
                .cle(entity.getCle())
                .libelle(entity.getLibelle())
                .typeParametre(entity.getTypeParametre())
                .categorie(entity.getCategorie())
                .valeurDecimale(entity.getValeurDecimale())
                .valeurEntiere(entity.getValeurEntiere())
                .valeurTexte(entity.getValeurTexte())
                .unite(entity.getUnite())
                .description(entity.getDescription())
                .valeurParDefaut(entity.getValeurParDefaut())
                .actif(entity.getActif())
                .modifiable(entity.getModifiable())
                .modifiePar(entity.getModifiePar())
                .build();
    }

    public ParametreMetier toEntity(ParametreMetierDTO dto) {
        if (dto == null) {
            return null;
        }

        return ParametreMetier.builder()
                .cle(dto.getCle())
                .libelle(dto.getLibelle())
                .typeParametre(dto.getTypeParametre())
                .categorie(dto.getCategorie())
                .valeurDecimale(dto.getValeurDecimale())
                .valeurEntiere(dto.getValeurEntiere())
                .valeurTexte(dto.getValeurTexte())
                .unite(dto.getUnite())
                .description(dto.getDescription())
                .valeurParDefaut(dto.getValeurParDefaut())
                .actif(dto.getActif())
                .modifiable(dto.getModifiable())
                .modifiePar(dto.getModifiePar())
                .build();
    }
}
