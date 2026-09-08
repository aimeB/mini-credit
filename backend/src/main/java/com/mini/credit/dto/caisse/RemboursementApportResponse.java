package com.mini.credit.dto.caisse;

import com.mini.credit.enums.StatutRemboursementApport;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RemboursementApportResponse {
    private Long id;
    private String reference;
    private Long antenneId;
    private String antenneNom;
    private BigDecimal montant;
    private StatutRemboursementApport statut;
    private String motifDemande;
    private Long demandeParId;
    private String demandeParNom;
    private LocalDateTime dateDemande;
    private Long valideParId;
    private String valideParNom;
    private LocalDateTime dateValidation;
    private String motifValidation;
    private Long payeParId;
    private String payeParNom;
    private LocalDateTime datePaiement;
    private Long operationCaisseId;
    private String commentaire;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
