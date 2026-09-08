package com.mini.credit.dto.credit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreAnalyseRequest {

    public enum Action {
        TRANSMETTRE_ANALYSE,
        RETOUR_COMPLEMENT
    }

    @NotNull(message = "L'action de pré-analyse est obligatoire")
    private Action action;

    @NotBlank(message = "Le commentaire de pré-analyse est obligatoire")
    private String commentaire;

    private Boolean dossierComplet;
}
