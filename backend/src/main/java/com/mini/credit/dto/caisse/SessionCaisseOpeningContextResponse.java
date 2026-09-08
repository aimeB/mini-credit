package com.mini.credit.dto.caisse;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SessionCaisseOpeningContextResponse {
    private Long caisseId;
    private LocalDate dateComptable;
    private String devise;
    private boolean premiereSession;
    private BigDecimal soldeOuvertureAutomatique;
    private boolean forcageAutorise;
    private boolean soldeVerrouille;
    private boolean sessionExistante;
    private Long sessionExistanteId;
    private String sessionExistanteStatut;
    private java.time.LocalDateTime sessionExistanteDateOuverture;
    private java.time.LocalDateTime sessionExistanteDateCloture;
    private Long sessionExistanteUtilisateurId;
    private String sessionExistanteUtilisateurNom;
}
