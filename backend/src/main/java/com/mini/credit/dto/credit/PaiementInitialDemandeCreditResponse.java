package com.mini.credit.dto.credit;

import com.mini.credit.dto.caisse.OperationCaisseResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PaiementInitialDemandeCreditResponse {

    private Long demandeId;
    private String numeroDemande;
    private Long membreId;
    private String membreNomComplet;
    private String devise;

    private BigDecimal fraisPayesSurCetteOperation;
    private BigDecimal depotGarantiePayeSurCetteOperation;
    private BigDecimal totalPayeSurCetteOperation;

    private BigDecimal fraisDemandeTotal;
    private BigDecimal fraisDemandePayesTotal;
    private BigDecimal fraisDemandeRestants;

    private BigDecimal depotGarantieRequis;
    private BigDecimal depotGarantiePayeTotal;
    private BigDecimal depotGarantieRestant;

    private DemandeCreditResponse demande;

    private List<OperationCaisseResponse> operationsCaisse;
}