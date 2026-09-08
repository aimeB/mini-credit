package com.mini.credit.service;

import com.mini.credit.dto.caisse.rapport.RapportCaisseDepensesDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseEcartsDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseJournalierDto;
import com.mini.credit.dto.caisse.rapport.RapportCaissePeriodeDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseSessionDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseSyntheseDto;

import java.time.LocalDate;

public interface RapportCaisseService {

    RapportCaisseSessionDto getRapportSession(Long sessionId);

    RapportCaisseJournalierDto getRapportJournalier(LocalDate date, Long caisseId, Long siteId);

    RapportCaissePeriodeDto getRapportPeriode(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId);

    RapportCaisseDepensesDto getRapportDepenses(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId);

    RapportCaisseEcartsDto getRapportEcarts(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId);

    RapportCaisseSyntheseDto getRapportSynthese(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId);

    byte[] exportRapportSessionCsv(Long sessionId);

    byte[] exportRapportJournalierCsv(LocalDate date, Long caisseId, Long siteId);

    byte[] exportRapportPeriodeCsv(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId);

    byte[] exportRapportDepensesCsv(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId);

    byte[] exportRapportEcartsCsv(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId);
}
