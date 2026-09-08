package com.mini.credit.dto.dashboard.controleinterne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardControleInterneResponse {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long siteId;
    private Long caisseId;

    private ResumeCaisse caisse;
    private ResumeDepenses depenses;
    private ResumeRecettes recettes;
    private ResumeRetraits retraits;
    private ResumeCredits credits;
    private ResumeAudit audit;

    private List<AlerteDashboardResponse> alertes;
}
