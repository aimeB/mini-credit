package com.mini.credit.dto.contrat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ContratCreditCreateRequest {

    @NotNull
    private Long creditId;

    @NotNull
    private LocalDate dateSignature;

    @NotBlank
    private String lieuSignature;

    private String objetContrat;
    private String clausesSpecifiques;

    private Boolean signeParMembre = false;
    private Boolean signeParInstitution = false;

    private String nomSignataireInstitution;
    private String fonctionSignataireInstitution;
}