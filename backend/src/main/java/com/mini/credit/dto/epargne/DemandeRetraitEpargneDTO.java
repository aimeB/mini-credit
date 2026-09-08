package com.mini.credit.dto.epargne;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour demande de retrait épargne (PHASE 5).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeRetraitEpargneDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long compteEpargneId;

    private String referenceRetrait;

    private String numeroCompte;

    private String compteEpargneNumero;

    private Long membreId;

    private String membreNom;

    private BigDecimal montantDemande;

    private BigDecimal fraisRetrait;

    private BigDecimal tauxCommissionRetrait;

    private BigDecimal montantTotalDebite;

    private BigDecimal montantRemisAuMembre;

    private BigDecimal soldeDisponible;

    private BigDecimal soldeBloque;

    private Long operationCaisseSortieId;

    private Long operationCaisseFraisId;

    private String statut;

    private LocalDateTime dateDemande;

    private LocalDateTime createdAt;

    private String motifRejet;

    private Long valideParId;

    private String valideParNom;

    private LocalDateTime dateValidation;

    private String observation;
}
