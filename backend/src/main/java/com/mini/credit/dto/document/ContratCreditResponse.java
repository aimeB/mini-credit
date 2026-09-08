package com.mini.credit.dto.document;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ContratCreditResponse {
    private Long id;
    private Long creditId;
    private String numeroContrat;
    private LocalDate dateSignature;
    private String lieuSignature;
    private String objetContrat;
    private String clausesSpecifiques;
    private String fichierUrl;
    private Boolean signeParMembre;
    private Boolean signeParInstitution;
    private String nomSignataireInstitution;
    private String fonctionSignataireInstitution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
