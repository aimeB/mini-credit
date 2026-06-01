package com.mini.credit.service;

import com.mini.credit.dto.credit.PenaliteCreditResponse;

import java.time.LocalDate;

public interface PenaliteService {
    PenaliteCreditResponse calculerPenalitesCredit(Long creditId, LocalDate dateReference);
    void appliquerPenalitesCredit(Long creditId, LocalDate dateReference);
}
