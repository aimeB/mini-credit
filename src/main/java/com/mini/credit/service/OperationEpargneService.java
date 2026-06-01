package com.mini.credit.service;

import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OperationEpargneService {
    OperationEpargneResponse enregistrer(OperationEpargneRequest request);
    List<OperationEpargneResponse> getByCompte(Long compteId);
    List<OperationEpargneResponse> getByMembre(Long membreId);
    List<OperationEpargneResponse> getAll();  // Keep for backward compatibility
    Page<OperationEpargneResponse> getAll(Pageable pageable);  // PHASE 3B: New paginated version
    boolean isCurrentUserAccount(Long compteId);
    boolean isCurrentUserMembre(Long membreId);
}