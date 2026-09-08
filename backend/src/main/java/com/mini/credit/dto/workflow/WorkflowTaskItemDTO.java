package com.mini.credit.dto.workflow;

import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskPriority;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.enums.workflow.WorkflowTaskTypeAction;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class WorkflowTaskItemDTO {
    Long id;
    WorkflowTaskTypeAction typeAction;
    WorkflowTaskModule module;
    String referenceMetier;
    String entityType;
    Long entityId;
    String titre;
    String description;
    String type;
    String roleAttendu;
    String actionAttendue;
    String route;
    RoleCode roleDestinataire;
    Long utilisateurDestinataireId;
    Long antenneId;
    Long siteId;
    WorkflowTaskPriority priorite;
    WorkflowTaskStatus statut;
    LocalDateTime dateCreation;
    LocalDateTime dateEcheance;
    Long createdById;
    Long completedById;
    LocalDateTime completedAt;
    String commentaire;
}
