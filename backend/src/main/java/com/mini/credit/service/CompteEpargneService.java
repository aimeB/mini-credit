package com.mini.credit.service;

import com.mini.credit.dto.epargne.CompteEpargneCreateRequest;
import com.mini.credit.dto.epargne.CompteEpargneResponse;

import java.util.List;
import java.util.Optional;

public interface CompteEpargneService {
    CompteEpargneResponse create(CompteEpargneCreateRequest request);
    CompteEpargneResponse createMissingForMember(Long membreId);
    CompteEpargneResponse getById(Long id);
    List<CompteEpargneResponse> getAll();
    List<CompteEpargneResponse> getComptesActifsPourRetraitGuichet();
    List<CompteEpargneResponse> getForCurrentAgentPerimeter();
    List<CompteEpargneResponse> getForCurrentMember();
    List<CompteEpargneResponse> getByMembre(Long membreId);
    Optional<CompteEpargneResponse> getActiveByMembre(Long membreId);
    boolean isCurrentUserAccount(Long compteId);
    boolean isCurrentUserMembre(Long membreId);
}