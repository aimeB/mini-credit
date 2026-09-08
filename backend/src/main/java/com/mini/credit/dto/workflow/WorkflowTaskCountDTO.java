package com.mini.credit.dto.workflow;

import lombok.Data;

@Data
public class WorkflowTaskCountDTO {
    private long total;
    private long totalAFaire;

    public WorkflowTaskCountDTO(long totalAFaire) {
        this.total = totalAFaire;
        this.totalAFaire = totalAFaire;
    }
}
