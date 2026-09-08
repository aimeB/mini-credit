package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DepenseCaisseCategorie;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.ModeCalculPaie;
import com.mini.credit.enums.TypeChargeFixe;
import com.mini.credit.enums.TypePaiementPersonnel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "depense_caisse",
        indexes = {
                @Index(name = "idx_depense_caisse_statut", columnList = "statut"),
                @Index(name = "idx_depense_caisse_date_demande", columnList = "date_demande"),
                @Index(name = "idx_depense_caisse_session_caisse", columnList = "session_caisse_id"),
                @Index(name = "idx_depense_caisse_caisse", columnList = "caisse_id"),
                @Index(name = "idx_depense_caisse_site", columnList = "site_id"),
                @Index(name = "idx_depense_caisse_operation_caisse", columnList = "operation_caisse_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_depense_caisse_operation_caisse", columnNames = "operation_caisse_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepenseCaisse extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_caisse_id")
    private SessionCaisse sessionCaisse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DepenseCaisseCategorie categorie;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String devise = "CDF";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(length = 255)
    private String beneficiaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiaire_id")
    private Utilisateur beneficiaireUtilisateur;

    @Column(name = "beneficiaire_nom", length = 150)
    private String beneficiaireNom;

    @Column(name = "beneficiaire_role", length = 80)
    private String beneficiaireRole;

    @Column(name = "beneficiaire_agence", length = 150)
    private String beneficiaireAgence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id")
    private Employe employe;

    @Column(name = "periode_paie", length = 7)
    private String periodePaie;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_paiement_personnel", length = 30)
    private TypePaiementPersonnel typePaiementPersonnel;

    @Column(name = "montant_remuneration_reference", precision = 18, scale = 2)
    private BigDecimal montantRemunerationReference;

    @Column(name = "montant_ecart_remuneration", precision = 18, scale = 2)
    private BigDecimal montantEcartRemuneration;

    @Column(name = "motif_ecart_remuneration", columnDefinition = "TEXT")
    private String motifEcartRemuneration;

    @Column(name = "nature_paiement_paie", length = 40)
    private String naturePaiementPaie;

    @Column(name = "montant_salaire_du", precision = 18, scale = 2)
    private BigDecimal montantSalaireDu;

    @Column(name = "montant_deja_paye", precision = 18, scale = 2)
    private BigDecimal montantDejaPaye;

    @Column(name = "montant_restant_apres_paiement", precision = 18, scale = 2)
    private BigDecimal montantRestantApresPaiement;

    @Column(name = "montant_retenue", precision = 18, scale = 2)
    private BigDecimal montantRetenue;

    @Column(name = "motif_retenue", columnDefinition = "TEXT")
    private String motifRetenue;

    @Column(name = "motif_paiement_partiel", columnDefinition = "TEXT")
    private String motifPaiementPartiel;

    @Column(name = "commentaire_paie", columnDefinition = "TEXT")
    private String commentairePaie;

    @Column(name = "periode_charge", length = 7)
    private String periodeCharge;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_charge_fixe", length = 40)
    private TypeChargeFixe typeChargeFixe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_charge_id")
    private Site siteCharge;

    @Column(name = "montant_charge_fixe_reference", precision = 18, scale = 2)
    private BigDecimal montantChargeFixeReference;

    @Column(name = "montant_ecart_charge_fixe", precision = 18, scale = 2)
    private BigDecimal montantEcartChargeFixe;

    @Column(name = "commentaire_rapprochement", columnDefinition = "TEXT")
    private String commentaireRapprochement;

    @Column(name = "epargne_collectee_reference", precision = 18, scale = 2)
    private BigDecimal epargneCollecteeReference;

    @Column(name = "remboursement_collecte_reference", precision = 18, scale = 2)
    private BigDecimal remboursementCollecteReference;

    @Column(name = "nombre_carnets_vendus")
    private Integer nombreCarnetsVendus;

    @Column(name = "prime_mobilisation_epargne", precision = 18, scale = 2)
    private BigDecimal primeMobilisationEpargne;

    @Column(name = "prime_mobilisation_remboursement", precision = 18, scale = 2)
    private BigDecimal primeMobilisationRemboursement;

    @Column(name = "bonus_carnets", precision = 18, scale = 2)
    private BigDecimal bonusCarnets;

    @Column(name = "prime_motivation_manuelle", precision = 18, scale = 2)
    private BigDecimal primeMotivationManuelle;

    @Column(name = "motif_prime_motivation_manuelle", columnDefinition = "TEXT")
    private String motifPrimeMotivationManuelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_calcul_paie", length = 40)
    private ModeCalculPaie modeCalculPaie;

    @Column(name = "detail_calcul_paie_json", columnDefinition = "LONGTEXT")
    private String detailCalculPaieJson;

    @Column(name = "justificatif_url", length = 500)
    private String justificatifUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DepenseCaisseStatus statut = DepenseCaisseStatus.BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_par_id")
    private Utilisateur demandePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paye_par_id")
    private Utilisateur payePar;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_caisse_id")
    private OperationCaisse operationCaisse;

    @Column(name = "commentaire_validation", columnDefinition = "TEXT")
    private String commentaireValidation;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @Transient
    public boolean canSubmit() {
        return statut == DepenseCaisseStatus.BROUILLON;
    }

    @Transient
    public boolean canValidate() {
        return statut == DepenseCaisseStatus.EN_ATTENTE_VALIDATION;
    }

    @Transient
    public boolean canReject() {
        return statut == DepenseCaisseStatus.EN_ATTENTE_VALIDATION;
    }

    @Transient
    public boolean canPay() {
        return statut == DepenseCaisseStatus.VALIDEE && operationCaisse == null;
    }

    @Transient
    public boolean canCancel() {
        return statut == DepenseCaisseStatus.BROUILLON
                || statut == DepenseCaisseStatus.EN_ATTENTE_VALIDATION
                || statut == DepenseCaisseStatus.VALIDEE;
    }
}