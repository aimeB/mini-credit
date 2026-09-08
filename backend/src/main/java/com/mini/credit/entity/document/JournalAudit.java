package com.mini.credit.entity.document;

import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.ActionAudit;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionAudit action;

    @Column(name = "table_cible", nullable = false, length = 100)
    private String tableCible;

    @Column(name = "id_cible")
    private Long idCible;

    @Column(name = "ancienne_valeur", columnDefinition = "TEXT")
    private String ancienneValeur;

    @Column(name = "nouvelle_valeur", columnDefinition = "TEXT")
    private String nouvelleValeur;

    @Column(name = "adresse_ip", length = 100)
    private String adresseIp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}