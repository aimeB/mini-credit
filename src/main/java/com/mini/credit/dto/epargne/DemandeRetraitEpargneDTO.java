package com.mini.credit.dto.epargne;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("id")
    private Long id;

    @JsonProperty("compte_epargne_id")
    private Long compteEpargneId;

    @JsonProperty("membre_id")
    private Long membreId;

    @JsonProperty("montant_demande")
    private BigDecimal montantDemande;

    @JsonProperty("statut")
    private String statut;

    @JsonProperty("date_demande")
    private LocalDateTime dateDemande;

    @JsonProperty("motif_rejet")
    private String motifRejet;

    @JsonProperty("valide_par_id")
    private Long valideParId;

    @JsonProperty("date_validation")
    private LocalDateTime dateValidation;

    @JsonProperty("observation")
    private String observation;
}
