package com.mini.credit.dto.dashboard.controleinterne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteDashboardResponse {
    private String code;
    private String type;
    private String module;
    private String niveau;
    private String titre;
    private String message;
    private Long nombre;
    private String routeFrontend;
    private String actionUrl;
    private Long referenceId;
    private String referenceType;
    private LocalDate date;
    private ActionDashboardResponse action;
}
