package com.mini.credit.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskReconcileResultDTO {
    private long scannedSessions;
    private long createdControleurTasks;
    private long createdChefBureauTasks;
    private long closedTasksForCloture;
    private long scannedSubmittedCreditDemands;
    private long createdGestionnaireTasks;
    private long migratedLegacyGestionnaireTasks;
}
