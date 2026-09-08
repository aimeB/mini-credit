package com.mini.credit.dto.caisse;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class CaisseResponse {
    private Long id;
    private String codeCaisse;
    private String libelle;
    private Long agenceId;
    private String agenceNom;
    private Long siteId;
    private String siteNom;
    private Long antenneId;
    private String antenneNom;
    private Long caissierResponsableId;
    private String caissierResponsableNom;
    private Long caissierAffecteId;
    private String caissierAffecteNom;
    private String devise;
    private Boolean actif;
    private BigDecimal soldeTheoriqueSessionOuverte;
    private BigDecimal dernierSoldeCloture;
    private BigDecimal soldeDisponibleActuel;
    private String statutSession;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}