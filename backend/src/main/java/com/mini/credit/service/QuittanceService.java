package com.mini.credit.service;

import com.mini.credit.dto.document.QuittanceCreateRequest;
import com.mini.credit.dto.document.QuittanceResponse;

import java.util.List;

public interface QuittanceService {
    QuittanceResponse create(QuittanceCreateRequest request);
    QuittanceResponse getById(Long id);
    List<QuittanceResponse> getByMembre(Long membreId);
}
