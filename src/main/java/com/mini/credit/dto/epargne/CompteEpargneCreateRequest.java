package com.mini.credit.dto.epargne;

import com.mini.credit.enums.TypeCompteEpargne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CompteEpargneCreateRequest {

    @NotNull
    private Long membreId;

    @NotNull
    private TypeCompteEpargne typeCompte;

    @NotNull
    private LocalDate dateOuverture;
}
