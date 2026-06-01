package com.mini.credit.service;

import com.mini.credit.dto.rapport.*;

import java.time.LocalDate;

public interface RapportService {
    BilanJournalierDTO genererBilanJournalier(LocalDate date);
    BilanHebdomadaireDTO genererBilanHebdomadaire(LocalDate dateDebut);
    BilanMensuelDTO genererBilanMensuel(LocalDate mois);
    
    // New KPI and Analytics methods
    KPIDashboardDTO getKPIDashboard();
    RapportFinancierDTO getRapportFinancier(LocalDate dateDebut, LocalDate dateFin);
    RapportRisqueDTO getRapportRisque();
    RapportCollecteDTO getRapportCollecte(LocalDate dateDebut, LocalDate dateFin);
}