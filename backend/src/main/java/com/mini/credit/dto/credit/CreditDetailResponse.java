package com.mini.credit.dto.credit;

import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutEcheance;
import com.mini.credit.enums.StatutGarantieCredit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CreditDetailResponse {
    private ResumeCredit resume;
    private Responsables responsables;
    private GarantieDetail garantie;
    private List<EcheanceDetail> echeancier;
    private SuiviFinancier suiviFinancier;
    private List<RemboursementDetail> remboursements;
    private List<HistoriqueEvent> historique;

    @Getter
    @Builder
    public static class ResumeCredit {
        private Long id;
        private String numeroCredit;
        private Long demandeCreditId;
        private String numeroDemande;
        private String membreNomComplet;
        private StatutCredit statut;
        private BigDecimal montantAccorde;
        private BigDecimal montantDecaisse;
        private LocalDate dateDemande;
        private LocalDate dateApprobation;
        private LocalDate dateDecaissement;
        private Integer dureeValeur;
        private String dureeUnite;
        private String periodiciteRemboursement;
        private String objetCredit;
        private String gagePropose;
        private Long antenneId;
        private String antenneNom;
        private String devise;
    }

    @Getter
    @Builder
    public static class Responsables {
        private Long agentTerrainId;
        private String agentTerrainNom;
        private Long gestionnaireId;
        private String gestionnaireNom;
        private Long controleurId;
        private String controleurNom;
        private Long chefBureauId;
        private String chefBureauNom;
        private Long caissierId;
        private String caissierNom;
    }

    @Getter
    @Builder
    public static class GarantieDetail {
        private BigDecimal montantGarantieRequis;
        private BigDecimal montantGarantieBloque;
        private String sourceGarantie;
        private String garantiesMateriellesAcceptees;
        private StatutGarantieCredit statutGarantie;
        private LocalDateTime dateBlocage;
        private LocalDateTime dateLiberation;
    }

    @Getter
    @Builder
    public static class EcheanceDetail {
        private Long id;
        private Integer numeroEcheance;
        private LocalDate dateEcheance;
        private BigDecimal capitalDu;
        private BigDecimal interetDu;
        private BigDecimal penalite;
        private BigDecimal totalDu;
        private BigDecimal montantPaye;
        private BigDecimal resteAPayer;
        private StatutEcheance statut;
    }

    @Getter
    @Builder
    public static class SuiviFinancier {
        private BigDecimal capitalInitial;
        private BigDecimal capitalRembourse;
        private BigDecimal capitalRestant;
        private BigDecimal interetsAttendus;
        private BigDecimal interetsPayes;
        private BigDecimal interetsRestants;
        private BigDecimal penalitesDues;
        private BigDecimal penalitesPayees;
        private BigDecimal totalPaye;
        private BigDecimal totalRestant;
    }

    @Getter
    @Builder
    public static class RemboursementDetail {
        private Long id;
        private String numeroRecu;
        private LocalDateTime datePaiement;
        private BigDecimal montantPaye;
        private BigDecimal capitalPaye;
        private BigDecimal interetPaye;
        private BigDecimal penalitePayee;
        private String utilisateurNom;
        private String source;
        private String observation;
    }

    @Getter
    @Builder
    public static class HistoriqueEvent {
        private String type;
        private String utilisateur;
        private LocalDateTime dateHeure;
        private Long antenneId;
        private String antenneNom;
        private String commentaire;
    }
}
