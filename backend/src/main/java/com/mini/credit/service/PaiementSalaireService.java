package com.mini.credit.service;

import com.mini.credit.dto.employe.PaiementSalaireDTO;
import com.mini.credit.dto.employe.CreatePaiementSalaireRequest;

import java.time.LocalDate;
import java.util.List;

public interface PaiementSalaireService {
    PaiementSalaireDTO create(CreatePaiementSalaireRequest request);
    PaiementSalaireDTO getById(Long id);
    List<PaiementSalaireDTO> getByEmployeId(Long employeId);
    List<PaiementSalaireDTO> getByDateRange(LocalDate startDate, LocalDate endDate);
    List<PaiementSalaireDTO> getByEmployeAndDateRange(Long employeId, LocalDate startDate, LocalDate endDate);
    List<PaiementSalaireDTO> getAll();
}
