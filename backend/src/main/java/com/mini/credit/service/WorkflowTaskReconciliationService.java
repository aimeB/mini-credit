package com.mini.credit.service;

import com.mini.credit.dto.workflow.WorkflowTaskReconcileResultDTO;

public interface WorkflowTaskReconciliationService {

    WorkflowTaskReconcileResultDTO reconcileCaisseSessions();

    WorkflowTaskReconcileResultDTO reconcileSubmittedCreditDemands();
}
