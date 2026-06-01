package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.CaisseCreateRequest;
import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.site.SiteRepository;
import com.mini.credit.service.CaisseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CaisseServiceImpl implements CaisseService {

    private final CaisseRepository caisseRepository;
    private final SiteRepository siteRepository;
    private final CashMapper cashMapper;

    @Override
    public CaisseResponse create(CaisseCreateRequest request) {
        if (caisseRepository.findByCodeCaisse(request.getCodeCaisse()).isPresent()) {
            throw new BusinessException("Une caisse avec ce code existe déjà");
        }

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));

        Caisse caisse = Caisse.builder()
                .codeCaisse(request.getCodeCaisse())
                .libelle(request.getLibelle())
                .site(site)
                .devise(request.getDevise())
                .actif(true)
                .build();

        return cashMapper.toResponse(caisseRepository.save(caisse));
    }

    @Override
    public CaisseResponse getById(Long id) {
        return cashMapper.toResponse(
                caisseRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"))
        );
    }

    @Override
    public List<CaisseResponse> getAll() {
        return caisseRepository.findAll().stream()
                .map(cashMapper::toResponse)
                .toList();
    }
}