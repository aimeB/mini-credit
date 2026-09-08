package com.mini.credit.service;

import com.mini.credit.dto.dashboard.controleinterne.DashboardControleInterneResponse;

import java.time.LocalDate;

public interface DashboardControleInterneService {
    DashboardControleInterneResponse getDashboardControleInterne(
            LocalDate date,
            LocalDate dateDebut,
            LocalDate dateFin,
            Long siteId,
            Long caisseId
    );
}
