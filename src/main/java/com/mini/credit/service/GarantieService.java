package com.mini.credit.service;

import com.mini.credit.dto.garantie.GarantieCreateRequest;
import com.mini.credit.dto.garantie.GarantieResponse;
import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;

import java.time.LocalDateTime;
import java.util.List;

public interface GarantieService {
    
    List<GarantieResponse> getAllGaranties();
    
    GarantieResponse getGarantieById(Long id);
    
    List<GarantieResponse> getGarantiesByCreditId(Long creditId);
    
    List<GarantieResponse> getGarantiesByMembreId(Long membreId);
    
    List<GarantieResponse> getGarantiesByDemandeCreditId(Long demandeCreditId);
    
    List<GarantieResponse> getGarantiesByType(TypeGarantie type);
    
    List<GarantieResponse> getGarantiesByStatut(StatutGarantie statut);
    
    List<GarantieResponse> getGarantiesByDateRange(LocalDateTime debut, LocalDateTime fin);
    
    GarantieResponse createGarantie(GarantieCreateRequest request);
    
    GarantieResponse updateGarantie(Long id, GarantieCreateRequest request);
    
    void deleteGarantie(Long id);
    
    long countByStatut(StatutGarantie statut);
    
    long countByType(TypeGarantie type);
}
