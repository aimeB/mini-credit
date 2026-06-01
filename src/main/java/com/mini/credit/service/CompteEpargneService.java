package com.mini.credit.service;

import com.mini.credit.dto.epargne.CompteEpargneCreateRequest;
import com.mini.credit.dto.epargne.CompteEpargneResponse;

import java.util.List;

public interface CompteEpargneService {
    CompteEpargneResponse create(CompteEpargneCreateRequest request);
    CompteEpargneResponse getById(Long id);
    List<CompteEpargneResponse> getAll();
    List<CompteEpargneResponse> getByMembre(Long membreId);
    boolean isCurrentUserAccount(Long compteId);
    boolean isCurrentUserMembre(Long membreId);
}