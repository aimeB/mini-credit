package com.mini.credit.dto.caisse;

import com.mini.credit.enums.StatutSessionCaisse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder(toBuilder = true)
public class SessionCaisseResponse {
    private Long id;
    private Long caisseId;
    private String caisseCode;
    private String devise;
    private Long siteId;
    private String siteNom;
    private Long antenneId;
    private String antenneNom;
    private String caissierResponsableNom;
    private Long utilisateurId;
    private String utilisateurNom;
    private LocalDate dateComptable;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateCloture;
    private BigDecimal soldeOuverture;
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal soldeTheorique;
    private BigDecimal soldePhysique;
    private BigDecimal ecart;
    private BigDecimal ecartCaisse;
    private StatutSessionCaisse statut;
    private String statutControle;
    private String observation;

    private Long clotureParId;
    private String clotureParNom;

    private Long fermeParId;
    private String fermeParNom;

    private Long controleValideParId;
    private String controleValideParNom;
    private LocalDateTime dateControle;

    private Boolean peutDemanderAnnulation;
    private Boolean peutValiderAnnulation;
    private Boolean peutAnnulerAdministrativement;
    private Boolean peutReouvrirControlee;
    private String statutCorrection;
    private Long derniereAnomalieId;
    private String motifAnnulation;
    private String annuleePar;
    private LocalDateTime dateAnnulation;

    private List<OperationCaisseResponse> mouvements;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}