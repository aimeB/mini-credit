package com.mini.credit.entity.document;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutTicketRecu;
import com.mini.credit.enums.TypeTicketRecu;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "ticket_recu",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_recu_numero", columnNames = "numero_ticket"),
        @UniqueConstraint(name = "uk_ticket_recu_code_verification", columnNames = "code_verification")
    },
    indexes = {
        @Index(name = "idx_ticket_recu_operation_epargne", columnList = "operation_epargne_id"),
        @Index(name = "idx_ticket_recu_demande_retrait", columnList = "demande_retrait_epargne_id"),
        @Index(name = "idx_ticket_recu_membre", columnList = "membre_id"),
        @Index(name = "idx_ticket_recu_session", columnList = "session_caisse_id"),
        @Index(name = "idx_ticket_recu_caisse", columnList = "caisse_id"),
        @Index(name = "idx_ticket_recu_agence", columnList = "agence_id"),
        @Index(name = "idx_ticket_recu_type_statut", columnList = "type_ticket, statut")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRecu extends BaseEntity {

    @Column(name = "numero_ticket", nullable = false, length = 50)
    private String numeroTicket;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_ticket", nullable = false, length = 30)
    private TypeTicketRecu typeTicket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutTicketRecu statut = StatutTicketRecu.GENERE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_ticket_id")
    private TicketRecu originalTicket;

    @Column(name = "nombre_impressions", nullable = false)
    @Builder.Default
    private Integer nombreImpressions = 0;

    @Column(name = "nombre_duplicatas", nullable = false)
    @Builder.Default
    private Integer nombreDuplicatas = 0;

    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration;

    @Column(name = "date_derniere_impression")
    private LocalDateTime dateDerniereImpression;

    @Column(name = "date_dernier_duplicata")
    private LocalDateTime dateDernierDuplicata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_epargne_id")
    private OperationEpargne operationEpargne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_caisse_id")
    private OperationCaisse operationCaisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_caisse_commission_id")
    private OperationCaisse operationCaisseCommission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_retrait_epargne_id")
    private DemandeRetraitEpargne demandeRetraitEpargne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_journaliere_id")
    private CollecteJournaliereTerrain collecteJournaliere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_membre_ligne_id")
    private CollecteMembreLigne collecteMembreLigne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_caisse_id")
    private SessionCaisse sessionCaisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id")
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_epargne_id", nullable = false)
    private CompteEpargne compteEpargne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_createur_id")
    private Utilisateur utilisateurCreateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_impression_id")
    private Utilisateur utilisateurImpression;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_duplicata_id")
    private Utilisateur utilisateurDuplicata;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String devise = "CDF";

    @Column(name = "montant_principal", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantPrincipal;

    @Column(name = "taux_commission", precision = 5, scale = 2)
    private BigDecimal tauxCommission;

    @Column(name = "montant_commission", precision = 18, scale = 2)
    private BigDecimal montantCommission;

    @Column(name = "montant_total_debite", precision = 18, scale = 2)
    private BigDecimal montantTotalDebite;

    @Column(name = "montant_remis_membre", precision = 18, scale = 2)
    private BigDecimal montantRemisMembre;

    @Column(name = "ancien_solde", nullable = false, precision = 18, scale = 2)
    private BigDecimal ancienSolde;

    @Column(name = "nouveau_solde", nullable = false, precision = 18, scale = 2)
    private BigDecimal nouveauSolde;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "motif_duplicata", columnDefinition = "TEXT")
    private String motifDuplicata;

    @Column(name = "code_verification", nullable = false, length = 20)
    private String codeVerification;

    @Column(name = "qr_payload", length = 300)
    private String qrPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Utilisateur updatedBy;
}
