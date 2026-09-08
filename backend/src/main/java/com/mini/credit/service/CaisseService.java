package com.mini.credit.service;


import com.mini.credit.dto.caisse.CaisseCreateRequest;
import com.mini.credit.dto.caisse.CaisseResponse;

import java.util.List;

public interface CaisseService {
    CaisseResponse create(CaisseCreateRequest request);
    CaisseResponse initialiserMaCaisse();
    CaisseResponse getById(Long id);
    List<CaisseResponse> getAll();
    List<CaisseResponse> getActives();
    List<CaisseResponse> getAccessibles();
}