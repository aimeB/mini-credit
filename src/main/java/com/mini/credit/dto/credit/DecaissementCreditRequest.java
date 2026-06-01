package com.mini.credit.dto.credit;

import com.mini.credit.enums.ModePaiement;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DecaissementCreditRequest {

    @NotNull
    private LocalDateTime dateDecaissement;

    @NotNull
    private Long sessionCaisseId;

    private Long agentId;

    @NotNull
    private Long createdBy;

    @NotNull
    private ModePaiement modePaiement;

    private String observation;
}