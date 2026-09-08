package com.mini.credit.dto.workflow;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class WorkflowTaskDashboardDTO {
    long totalAFaire;
    long totalEnCours;
    long totalTerminee;
    long urgentCount;
    long overdueCount;
    long countAFaire;
    List<WorkflowTaskItemDTO> tasks;
}
