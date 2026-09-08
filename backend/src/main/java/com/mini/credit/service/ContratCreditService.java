package com.mini.credit.service;


import com.mini.credit.dto.document.ContratCreditCreateRequest;
import com.mini.credit.dto.document.ContratCreditResponse;

public interface ContratCreditService {
    ContratCreditResponse create(ContratCreditCreateRequest request);
    ContratCreditResponse getById(Long id);
    ContratCreditResponse getByCreditId(Long creditId);
}