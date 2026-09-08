package com.mini.credit.service;

import com.mini.credit.dto.membre.MembreCreateRequest;
import com.mini.credit.dto.membre.MembreActivationResponseDTO;
import com.mini.credit.dto.membre.MembreCreationResponseDTO;
import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.dto.membre.MembreUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MembreService {
    MembreResponse create(MembreCreateRequest request);
    MembreCreationResponseDTO createWithCredentials(MembreCreateRequest request);
    MembreActivationResponseDTO createWithActivationCode(MembreCreateRequest request);
    MembreResponse update(Long id, MembreUpdateRequest request);
    MembreResponse getById(Long id);
    List<MembreResponse> getAll();  // Keep for backward compatibility
    Page<MembreResponse> getAll(Pageable pageable);  // PHASE 3B: New paginated version
    Page<MembreResponse> search(String q, Long siteId, Pageable pageable);
    void delete(Long id);
    boolean isCurrentUser(Long membreId);
    MembreResponse getCurrentMember();
}
