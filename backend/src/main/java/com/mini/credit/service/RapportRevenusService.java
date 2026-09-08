package com.mini.credit.service;

import com.mini.credit.dto.rapport.revenus.RapportRevenusResponse;

import java.time.LocalDate;

public interface RapportRevenusService {
    RapportRevenusResponse getRapportRevenus(
            LocalDate dateDebut,
            LocalDate dateFin,
            Long agenceId,
            String categorie,
            String source
    );
}
