package com.mini.credit.dto.referentiel;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransportSiteParametreResponse {
    private Long id;
    private Long siteId;
    private String siteNom;
    private Long agenceId;
    private String agenceNom;
    private BigDecimal montantTransportJournalierParAgent;
    private Boolean actif;
    private LocalDate dateDebutValidite;
    private LocalDate dateFinValidite;
    private String commentaire;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
