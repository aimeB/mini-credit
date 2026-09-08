package com.mini.credit.service.impl;

import com.mini.credit.dto.epargne.CompteEpargneCreateRequest;
import com.mini.credit.dto.epargne.CompteEpargneResponse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.SavingMapper;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.service.CompteEpargneService;
import com.mini.credit.service.ReferenceGeneratorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompteEpargneServiceImpl implements CompteEpargneService {

    private final CompteEpargneRepository compteEpargneRepository;
    private final MembreRepository membreRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final SavingMapper savingMapper;
    private final ReferenceGeneratorService referenceGeneratorService;

    @Override
    public CompteEpargneResponse create(CompteEpargneCreateRequest request) {
        validerCreateRequest(request);

        Membre membre = membreRepository.findById(request.getMembreId())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));

        if (membre.getStatut() != StatutMembre.ACTIF) {
            throw new BusinessException(
                    "Impossible d'ouvrir un compte pour un membre non actif (statut : " + membre.getStatut() + ")"
            );
        }

        if (compteEpargneRepository.existsByMembreIdAndStatut(membre.getId(), StatutCompte.ACTIF)) {
            throw new BusinessException(
                "Ce membre possède déjà un compte épargne actif. Un seul compte actif est autorisé par membre"
            );
        }

        CompteEpargne compte = CompteEpargne.builder()
                .membre(membre)
                .numeroCompte(referenceGeneratorService.genererReference("CEP"))
                .typeCompte(request.getTypeCompte())
                .dateOuverture(request.getDateOuverture())
                .soldeDisponible(BigDecimal.ZERO)
                .soldeBloque(BigDecimal.ZERO)
                .statut(StatutCompte.ACTIF)
                .build();

        return savingMapper.toResponse(compteEpargneRepository.save(compte));
    }

    @Override
    public CompteEpargneResponse createMissingForMember(Long membreId) {
        if (membreId == null) {
            throw new BusinessException("Le membre est obligatoire");
        }

        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));

        if (membre.getStatut() != StatutMembre.ACTIF) {
            throw new BusinessException("Impossible de créer un compte épargne pour un membre non actif");
        }

        if (compteEpargneRepository.existsByMembreIdAndStatut(membreId, StatutCompte.ACTIF)) {
            throw new BusinessException("Ce membre possède déjà un compte épargne actif");
        }

        CompteEpargne compte = CompteEpargne.builder()
                .membre(membre)
                .numeroCompte(referenceGeneratorService.genererReference("CEP"))
                .typeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE)
                .dateOuverture(LocalDate.now())
                .soldeDisponible(BigDecimal.ZERO)
                .soldeBloque(BigDecimal.ZERO)
                .statut(StatutCompte.ACTIF)
                .build();

        return savingMapper.toResponse(compteEpargneRepository.save(compte));
    }

    @Override
    public CompteEpargneResponse getById(Long id) {
        return savingMapper.toResponse(
                compteEpargneRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable"))
        );
    }

    @Override
    public List<CompteEpargneResponse> getAll() {
        return compteEpargneRepository.findAll().stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    @Override
    public List<CompteEpargneResponse> getComptesActifsPourRetraitGuichet() {
        return compteEpargneRepository.findByStatut(StatutCompte.ACTIF).stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    @Override
    public List<CompteEpargneResponse> getForCurrentAgentPerimeter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur utilisateur)) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        AgentTerrain agent = agentTerrainRepository.findByUtilisateurId(utilisateur.getId())
                .orElseThrow(() -> new BusinessException("Profil AGENT_TERRAIN introuvable"));

        if (agent.getSite() == null || agent.getSite().getId() == null) {
            throw new BusinessException("Aucun site affecté à cet agent terrain");
        }

        return compteEpargneRepository.findByMembreSiteId(agent.getSite().getId()).stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    @Override
    public List<CompteEpargneResponse> getForCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur utilisateur)) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        if (utilisateur.getMembre() == null || utilisateur.getMembre().getId() == null) {
            throw new BusinessException("Profil MEMBER introuvable");
        }

        return compteEpargneRepository.findByMembreId(utilisateur.getMembre().getId()).stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    @Override
    public List<CompteEpargneResponse> getByMembre(Long membreId) {
        return compteEpargneRepository.findByMembreId(membreId).stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<CompteEpargneResponse> getActiveByMembre(Long membreId) {
        return compteEpargneRepository.findFirstByMembreIdAndStatut(membreId, StatutCompte.ACTIF)
                .map(savingMapper::toResponse);
    }

    private void validerCreateRequest(CompteEpargneCreateRequest request) {
        if (request == null) {
            throw new BusinessException("La requête de création du compte épargne est obligatoire");
        }

        if (request.getMembreId() == null) {
            throw new BusinessException("Le membre est obligatoire");
        }

        if (request.getTypeCompte() == null) {
            throw new BusinessException("Le type de compte est obligatoire");
        }

        if (request.getDateOuverture() == null) {
            throw new BusinessException("La date d'ouverture est obligatoire");
        }

        if (request.getDateOuverture().isAfter(LocalDate.now())) {
            throw new BusinessException("La date d'ouverture ne peut pas être dans le futur");
        }
    }

    @Override
    public boolean isCurrentUserAccount(Long compteId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur)) {
            return false;
        }

        Utilisateur utilisateur = (Utilisateur) authentication.getPrincipal();

        // Pour les membres, vérifier si le compte leur appartient
        if (utilisateur.getMembre() != null) {
            CompteEpargne compte = compteEpargneRepository.findById(compteId).orElse(null);
            return compte != null && compte.getMembre() != null &&
                   compte.getMembre().getId().equals(utilisateur.getMembre().getId());
        }

        // Pour les autres rôles (ADMIN, CREDIT_MANAGER), accès complet
        return true;
    }

    @Override
    public boolean isCurrentUserMembre(Long membreId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur)) {
            return false;
        }

        Utilisateur utilisateur = (Utilisateur) authentication.getPrincipal();

        // Pour les utilisateurs avec un membre, vérifier si l'ID demandé correspond
        if (utilisateur.getMembre() != null) {
            return utilisateur.getMembre().getId().equals(membreId);
        }

        // Pour les autres rôles (ADMIN, CREDIT_MANAGER), accès complet
        return true;
    }
}