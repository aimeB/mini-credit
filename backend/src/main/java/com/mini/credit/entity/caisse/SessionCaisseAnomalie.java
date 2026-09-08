package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutDossierAnomalieSession;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeAnomalieSessionCaisse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_caisse_anomalie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCaisseAnomalie extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionCaisse session;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_anomalie", nullable = false, length = 80)
    private TypeAnomalieSessionCaisse typeAnomalie;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", nullable = false, length = 40)
    private StatutSessionCaisse ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", length = 40)
    private StatutSessionCaisse nouveauStatut;

    @Column(name = "motif", nullable = false, length = 1000)
    private String motif;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_par_id")
    private Utilisateur demandePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_dossier", nullable = false, length = 30)
    private StatutDossierAnomalieSession statutDossier;

    @Column(name = "action_executee", length = 120)
    private String actionExecutee;

    @Column(name = "audit_id")
    private Long auditId;
}
