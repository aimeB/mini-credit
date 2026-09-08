package com.mini.credit.dto.caisse;

import com.mini.credit.enums.TypeAnomalieSessionCaisse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DemanderAnnulationSessionRequest {
    private TypeAnomalieSessionCaisse typeAnomalie;
    private String motif;
    private String commentaire;
}
