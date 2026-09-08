package com.mini.credit.dto.credit;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprobationCreditRequest {

    @NotNull
    private Long decidedBy;

    private boolean genererEcheancier = true;
}