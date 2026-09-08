package com.mini.credit.controller;

import com.mini.credit.dto.workflow.WorkflowTaskCountDTO;
import com.mini.credit.dto.workflow.WorkflowTaskDashboardDTO;
import com.mini.credit.dto.workflow.WorkflowTaskItemDTO;
import com.mini.credit.dto.workflow.WorkflowTaskReconcileResultDTO;
import com.mini.credit.dto.workflow.WorkflowTaskUpdateRequest;
import com.mini.credit.service.WorkflowTaskReconciliationService;
import com.mini.credit.service.WorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "WorkflowTask", description = "Mes actions à faire")
@SecurityRequirement(name = "bearer-jwt")
public class WorkflowTaskController {

    private final WorkflowTaskService workflowTaskService;
    private final WorkflowTaskReconciliationService workflowTaskReconciliationService;

    @GetMapping("/api/me/actions")
    @PreAuthorize("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT') or hasAnyRole('ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI', 'COO', 'GERANT_GENERAL')")
    @Operation(summary = "Lister mes actions", description = "Retourne les actions visibles pour l'utilisateur connecté")
    public List<WorkflowTaskItemDTO> getMyActions(@RequestParam(required = false) String statut) {
        return workflowTaskService.getMyActions(statut);
    }

    @GetMapping("/api/actions/supervision")
    @PreAuthorize("hasAnyAuthority('TASK_SUPERVISE', 'TASK_AUDIT') or hasAnyRole('ADMIN', 'RCI', 'COO', 'GERANT_GENERAL')")
    @Operation(summary = "Superviser les actions", description = "Retourne les actions visibles en supervision, sans droit d'exécution métier")
    public List<WorkflowTaskItemDTO> getSupervisionActions(@RequestParam(required = false) String statut) {
        return workflowTaskService.getSupervisionActions(statut);
    }

    @GetMapping("/api/me/actions/count")
    @PreAuthorize("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT') or hasAnyRole('ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI', 'COO', 'GERANT_GENERAL')")
    @Operation(summary = "Compter mes actions à faire", description = "Retourne le nombre de tâches A_FAIRE visibles")
    public WorkflowTaskCountDTO countMyActions() {
        return new WorkflowTaskCountDTO(workflowTaskService.getMyActionCount());
    }

    @GetMapping("/api/actions/dashboard")
    @PreAuthorize("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT') or hasAnyRole('ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI', 'COO', 'GERANT_GENERAL')")
    @Operation(summary = "Dashboard actions", description = "Retourne count + top actions")
    public WorkflowTaskDashboardDTO getDashboard() {
        return workflowTaskService.getDashboard();
    }

    @PostMapping("/api/actions/{id}/marquer-vue")
    @PreAuthorize("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE')")
    @Operation(summary = "Marquer une action en cours", description = "Passe la tâche de A_FAIRE à EN_COURS")
    public WorkflowTaskItemDTO markAsViewed(@PathVariable Long id,
                                            @Valid @RequestBody(required = false) WorkflowTaskUpdateRequest request) {
        return workflowTaskService.markAsViewed(id, request != null ? request.getCommentaire() : null);
    }

    @PostMapping("/api/actions/{id}/terminer")
    @PreAuthorize("hasAuthority('TASK_COMPLETE')")
    @Operation(summary = "Terminer une action", description = "Termine une tâche visible par l'utilisateur")
    public WorkflowTaskItemDTO complete(@PathVariable Long id,
                                        @Valid @RequestBody(required = false) WorkflowTaskUpdateRequest request) {
        return workflowTaskService.complete(id, request != null ? request.getCommentaire() : null);
    }

    @PostMapping("/api/workflow-tasks/reconcile/caisse")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reconciliation tâches caisse", description = "Backfill des tâches workflow manquantes pour les sessions caisse")
    public WorkflowTaskReconcileResultDTO reconcileCaisseTasks() {
        return workflowTaskReconciliationService.reconcileCaisseSessions();
    }

    @PostMapping("/api/workflow-tasks/reconcile/credit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reconciliation tâches crédit", description = "Backfill des tâches workflow manquantes pour les demandes crédit soumises")
    public WorkflowTaskReconcileResultDTO reconcileCreditTasks() {
        return workflowTaskReconciliationService.reconcileSubmittedCreditDemands();
    }
}
