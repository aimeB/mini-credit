package com.mini.credit.entity.credit;

import com.mini.credit.enums.CanalPaiement;
import com.mini.credit.enums.StatutPaiement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement_credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_initiation")
    private LocalDateTime dateInitiation;

    @Column(name = "date_confirmation")
    private LocalDateTime dateConfirmation;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatutPaiement statut;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CanalPaiement canal;

    @Column(name = "reference_interne", length = 100)
    private String referenceInterne;

    @Column(name = "reference_externe", length = 100)
    private String referenceExterne;

    @Column(length = 100)
    private String fournisseur; // MTN, MOOV, WAVE, MANUEL

    @Column(name = "numero_payeur", length = 50)
    private String numeroPayeur;

    @Column(name = "payload_brut", length = 5000)
    private String payloadBrut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_credit_id")
    private ContratCredit contratCredit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "echeance_credit_id")
    private EcheanceCredit echeanceCredit;
}