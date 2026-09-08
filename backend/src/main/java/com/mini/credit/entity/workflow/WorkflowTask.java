package com.mini.credit.entity.workflow;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskPriority;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.enums.workflow.WorkflowTaskTypeAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTask extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action", nullable = false, length = 50)
    private WorkflowTaskTypeAction typeAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 30)
    private WorkflowTaskModule module;

    @Column(name = "reference_metier", nullable = false, length = 120)
    private String referenceMetier;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "titre", nullable = false, length = 180)
    private String titre;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_destinataire", nullable = false, length = 50)
    private RoleCode roleDestinataire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_destinataire_id")
    private Utilisateur utilisateurDestinataire;

    @Column(name = "antenne_id", nullable = false)
    private Long antenneId;

    @Column(name = "site_id")
    private Long siteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", nullable = false, length = 20)
    private WorkflowTaskPriority priorite;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private WorkflowTaskStatus statut;

    @Column(name = "date_echeance")
    private LocalDateTime dateEcheance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private Utilisateur completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @Column(name = "active_key", length = 260, unique = true)
    private String activeKey;
}
