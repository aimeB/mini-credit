package com.mini.credit.dto.caisse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValiderAnnulationSessionRequest {
    private String decision;
    private String motifDecision;
    private String commentaireDecision;
}
