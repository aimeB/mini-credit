package com.mini.credit.service.impl;

import com.mini.credit.dto.document.QuittanceCreateRequest;
import com.mini.credit.dto.document.QuittanceResponse;
import com.mini.credit.entity.document.Quittance;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.DocumentMapper;
import com.mini.credit.repository.document.QuittanceRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.QuittanceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuittanceServiceImpl implements QuittanceService {

    private final QuittanceRepository quittanceRepository;
    private final MembreRepository membreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DocumentMapper documentMapper;

    @Override
    public QuittanceResponse create(QuittanceCreateRequest request) {
        Membre membre = null;
        if (request.getMembreId() != null) {
            membre = membreRepository.findById(request.getMembreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));
        }

        Utilisateur createdBy = null;
        if (request.getCreatedBy() != null) {
            createdBy = utilisateurRepository.findById(request.getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        Quittance quittance = Quittance.builder()
                .numeroQuittance("QTT-" + System.currentTimeMillis())
                .membre(membre)
                .typeQuittance(request.getTypeQuittance())
                .referenceOperation(request.getReferenceOperation())
                .montant(request.getMontant())
                .devise(request.getDevise() != null ? request.getDevise() : "CDF")
                .dateEmission(request.getDateEmission())
                .createdBy(createdBy)
                .build();

        return documentMapper.toResponse(quittanceRepository.save(quittance));
    }

    @Override
    public QuittanceResponse getById(Long id) {
        return documentMapper.toResponse(
                quittanceRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Quittance introuvable"))
        );
    }

    @Override
    public List<QuittanceResponse> getByMembre(Long membreId) {
        return quittanceRepository.findByMembreIdOrderByDateEmissionDesc(membreId).stream()
                .map(documentMapper::toResponse)
                .toList();
    }
}