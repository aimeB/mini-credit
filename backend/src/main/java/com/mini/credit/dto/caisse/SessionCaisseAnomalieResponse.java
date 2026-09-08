package com.mini.credit.dto.caisse;

import com.mini.credit.enums.StatutDossierAnomalieSession;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeAnomalieSessionCaisse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionCaisseAnomalieResponse {
    private Long id;
    private Long sessionId;
    private TypeAnomalieSessionCaisse typeAnomalie;
    private StatutSessionCaisse ancienStatut;
    private StatutSessionCaisse nouveauStatut;
    private String motif;
    private String commentaire;
    private StatutDossierAnomalieSession statutDossier;
    private String demandePar;
    private String validePar;
    private LocalDateTime dateDemande;
    private LocalDateTime dateValidation;
    private String actionExecutee;
}
