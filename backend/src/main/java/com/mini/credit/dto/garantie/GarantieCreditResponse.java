package com.mini.credit.dto.garantie;

import com.mini.credit.enums.StatutGarantieCredit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarantieCreditResponse {
    private Long id;
    private Long demandeCreditId;
    private Long creditId;
    private Long membreId;
    private Long compteEpargneId;
    private String devise;
    private BigDecimal montantDemande;
    private String gagePropose;
    private BigDecimal montantCredit;
    private BigDecimal montantGarantieRequis;
    private BigDecimal montantGarantieBloque;
    private BigDecimal montantGarantieManquant;
    private StatutGarantieCredit statutGarantieEpargne;
    private Long controleParId;
    private String controleParNom;
    private LocalDateTime dateControle;
    private LocalDateTime dateBlocage;
    private LocalDateTime dateLiberation;
    private String commentaireControle;
    private BigDecimal soldeDisponible;
    private BigDecimal soldeBloque;
    private BigDecimal montantMaterielTotal;
    private BigDecimal valeurMinimaleGageMateriel;
    private BigDecimal valeurTotaleGarantiesMateriellesAcceptees;
    private BigDecimal ratioCouvertureMaterielle;
    private Boolean garantieMaterielleSuffisante;
    private String statutGlobal;
    private List<GarantieMaterielleResponse> garantiesMaterielles;
}