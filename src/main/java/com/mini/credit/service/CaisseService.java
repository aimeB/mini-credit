package com.mini.credit.service;


import com.mini.credit.dto.caisse.CaisseCreateRequest;
import com.mini.credit.dto.caisse.CaisseResponse;

import java.util.List;

public interface CaisseService {
    CaisseResponse create(CaisseCreateRequest request);
    CaisseResponse getById(Long id);
    List<CaisseResponse> getAll();
}