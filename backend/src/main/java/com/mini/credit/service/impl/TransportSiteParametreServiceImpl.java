package com.mini.credit.service.impl;

import com.mini.credit.dto.referentiel.TransportSiteParametreRequest;
import com.mini.credit.dto.referentiel.TransportSiteParametreResponse;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.TransportSiteParametre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.referentiel.TransportSiteParametreRepository;
import com.mini.credit.service.TransportSiteParametreService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TransportSiteParametreServiceImpl implements TransportSiteParametreService {

    private final TransportSiteParametreRepository repository;
    private final SiteRepository siteRepository;

    @Override
    public List<TransportSiteParametreResponse> getAll() {
        return repository.findAllByOrderBySiteNomSiteAscDateDebutValiditeDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public TransportSiteParametreResponse save(TransportSiteParametreRequest request) {
        if (request == null || request.getSiteId() == null) {
            throw new BusinessException("Le site est obligatoire");
        }
        if (request.getMontantTransportJournalierParAgent() == null || request.getMontantTransportJournalierParAgent().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le montant transport journalier par Agent Terrain doit être positif ou nul");
        }
        if (request.getCommentaire() == null || request.getCommentaire().isBlank()) {
            throw new BusinessException("Le commentaire est obligatoire pour modifier le transport site");
        }
        LocalDate dateDebut = request.getDateDebutValidite() != null ? request.getDateDebutValidite() : LocalDate.now();
        Site site = siteRepository.findById(request.getSiteId()).orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
        repository.findActiveBySiteAtDate(site.getId(), dateDebut).ifPresent(existing -> {
            existing.setActif(false);
            existing.setDateFinValidite(dateDebut.minusDays(1));
            existing.setUpdatedBy(currentUserId());
            repository.save(existing);
        });
        TransportSiteParametre parametre = TransportSiteParametre.builder()
                .site(site)
                .montantTransportJournalierParAgent(request.getMontantTransportJournalierParAgent())
                .actif(request.getActif() == null || request.getActif())
                .dateDebutValidite(dateDebut)
                .dateFinValidite(request.getDateFinValidite())
                .commentaire(request.getCommentaire().trim())
                .createdBy(currentUserId())
                .updatedBy(currentUserId())
                .build();
        return toResponse(repository.save(parametre));
    }

    @Override
    public TransportSiteParametreResponse deactivate(Long id, String commentaire) {
        if (commentaire == null || commentaire.isBlank()) {
            throw new BusinessException("Le commentaire est obligatoire pour désactiver le transport site");
        }
        TransportSiteParametre parametre = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Paramètre transport introuvable"));
        parametre.setActif(false);
        parametre.setDateFinValidite(LocalDate.now());
        parametre.setCommentaire(commentaire.trim());
        parametre.setUpdatedBy(currentUserId());
        return toResponse(repository.save(parametre));
    }

    private TransportSiteParametreResponse toResponse(TransportSiteParametre parametre) {
        Site site = parametre.getSite();
        return TransportSiteParametreResponse.builder()
                .id(parametre.getId())
                .siteId(site != null ? site.getId() : null)
                .siteNom(site != null ? site.getNomSite() : null)
                .agenceId(site != null && site.getAgence() != null ? site.getAgence().getId() : null)
                .agenceNom(site != null && site.getAgence() != null ? site.getAgence().getNomAgence() : null)
                .montantTransportJournalierParAgent(parametre.getMontantTransportJournalierParAgent())
                .actif(parametre.getActif())
                .dateDebutValidite(parametre.getDateDebutValidite())
                .dateFinValidite(parametre.getDateFinValidite())
                .commentaire(parametre.getCommentaire())
                .createdBy(parametre.getCreatedBy())
                .createdAt(parametre.getDateCreation())
                .updatedBy(parametre.getUpdatedBy())
                .updatedAt(parametre.getDateModification())
                .build();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Utilisateur utilisateur) {
            return utilisateur.getId();
        }
        return null;
    }
}
