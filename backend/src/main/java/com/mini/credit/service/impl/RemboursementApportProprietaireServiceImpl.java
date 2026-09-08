package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.RemboursementApportPaiementRequest;
import com.mini.credit.dto.caisse.RemboursementApportRequest;
import com.mini.credit.dto.caisse.RemboursementApportResponse;
import com.mini.credit.dto.caisse.RemboursementApportValidationRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.RemboursementApportProprietaire;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutRemboursementApport;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.RemboursementApportProprietaireRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.ReferenceGeneratorService;
import com.mini.credit.service.RemboursementApportProprietaireService;
import com.mini.credit.service.SessionCaisseValidationService;
import com.mini.credit.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class RemboursementApportProprietaireServiceImpl implements RemboursementApportProprietaireService {

    private final RemboursementApportProprietaireRepository remboursementRepository;
    private final AgenceRepository agenceRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final SessionCaisseValidationService sessionCaisseValidationService;
    private final AuditService auditService;

    @Override
    public RemboursementApportResponse demander(RemboursementApportRequest request) {
        Utilisateur currentUser = requireCurrentUser();
        Agence antenne = agenceRepository.findById(request.getAntenneId())
                .orElseThrow(() -> new ResourceNotFoundException("Antenne introuvable"));

        RemboursementApportProprietaire remboursement = RemboursementApportProprietaire.builder()
                .reference(referenceGeneratorService.genererReference("RAP"))
                .antenne(antenne)
                .montant(request.getMontant())
                .statut(StatutRemboursementApport.DEMANDE)
                .motifDemande(cleanRequired(request.getMotifDemande(), "Le motif de demande est obligatoire"))
                .demandePar(currentUser)
                .dateDemande(LocalDateTime.now())
                .build();

        RemboursementApportProprietaire saved = remboursementRepository.save(remboursement);
        auditService.logBusinessEvent(AuditAction.MODIFICATION_OPERATION, AuditModule.CAISSE, "RemboursementApportProprietaire", saved.getId(), true, "Demande remboursement apport propriétaire", saved.getReference());
        return toResponse(saved);
    }

    @Override
    public RemboursementApportResponse valider(Long id, RemboursementApportValidationRequest request) {
        RemboursementApportProprietaire remboursement = find(id);
        if (remboursement.getStatut() != StatutRemboursementApport.DEMANDE) {
            throw new BusinessException("Seule une demande peut être validée");
        }
        remboursement.setStatut(StatutRemboursementApport.VALIDEE);
        remboursement.setValidePar(requireCurrentUser());
        remboursement.setDateValidation(LocalDateTime.now());
        remboursement.setMotifValidation(cleanRequired(request.getCommentaire(), "Le commentaire de validation est obligatoire"));
        RemboursementApportProprietaire saved = remboursementRepository.save(remboursement);
        auditService.logBusinessEvent(AuditAction.MODIFICATION_OPERATION, AuditModule.CAISSE, "RemboursementApportProprietaire", saved.getId(), true, "Validation remboursement apport propriétaire", saved.getReference());
        return toResponse(saved);
    }

    @Override
    public RemboursementApportResponse rejeter(Long id, RemboursementApportValidationRequest request) {
        RemboursementApportProprietaire remboursement = find(id);
        if (remboursement.getStatut() != StatutRemboursementApport.DEMANDE) {
            throw new BusinessException("Seule une demande peut être rejetée");
        }
        remboursement.setStatut(StatutRemboursementApport.REJETEE);
        remboursement.setValidePar(requireCurrentUser());
        remboursement.setDateValidation(LocalDateTime.now());
        remboursement.setMotifValidation(cleanRequired(request.getCommentaire(), "Le motif de rejet est obligatoire"));
        RemboursementApportProprietaire saved = remboursementRepository.save(remboursement);
        auditService.logBusinessEvent(AuditAction.MODIFICATION_OPERATION, AuditModule.CAISSE, "RemboursementApportProprietaire", saved.getId(), true, "Rejet remboursement apport propriétaire", saved.getReference());
        return toResponse(saved);
    }

    @Override
    public RemboursementApportResponse payer(Long id, RemboursementApportPaiementRequest request) {
        RemboursementApportProprietaire remboursement = find(id);
        if (remboursement.getStatut() != StatutRemboursementApport.VALIDEE) {
            throw new BusinessException("Seul un remboursement validé peut être payé");
        }
        if (remboursement.getOperationCaisse() != null) {
            throw new BusinessException("Ce remboursement d'apport est déjà rattaché à une opération caisse");
        }

        SessionCaisse session = sessionCaisseRepository.findById(request.getSessionCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));
        sessionCaisseValidationService.validateSessionForOperation(session);
        Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));
        if (!Objects.equals(session.getCaisse().getId(), caisse.getId())) {
            throw new BusinessException("La session de caisse n'appartient pas à la caisse fournie");
        }
        if (!Objects.equals(caisse.getAgence().getId(), remboursement.getAntenne().getId())) {
            throw new BusinessException("La caisse de paiement n'appartient pas à l'antenne de la demande");
        }

        BigDecimal soldeCourant = session.getSoldeOuverture().add(session.getTotalEntrees()).subtract(session.getTotalSorties());
        if (soldeCourant.compareTo(remboursement.getMontant()) < 0) {
            throw new BusinessException("Solde caisse insuffisant pour payer ce remboursement d'apport");
        }

        Utilisateur currentUser = requireCurrentUser();
        OperationCaisse operation = OperationCaisse.builder()
                .numeroPiece(referenceGeneratorService.genererReference("PCS"))
                .sessionCaisse(session)
                .caisse(caisse)
                .dateOperation(LocalDateTime.now())
                .typeOperation(TypeOperationCaisse.SORTIE)
                .categorieOperation(CategorieOperationCaisse.REMBOURSEMENT_APPORT_PROPRIETAIRE)
                .montant(remboursement.getMontant())
                .soldeApresOperation(soldeCourant.subtract(remboursement.getMontant()))
                .devise(caisse.getDevise())
                .source(SourceOperationCaisse.REMBOURSEMENT_APPORT_PROPRIETAIRE)
                .referenceExterne(remboursement.getReference())
                .referenceMetier(remboursement.getReference())
                .description("Remboursement d'apport propriétaire")
                .observation(cleanRequired(request.getCommentaire(), "Le commentaire de paiement est obligatoire"))
                .commentaire(request.getCommentaire().trim())
                .createdBy(currentUser)
                .utilisateur(currentUser)
                .roleUtilisateur(currentUser.getRole() != null ? currentUser.getRole().getCode() : null)
                .site(caisse.getSite())
                .build();

        OperationCaisse savedOperation = operationCaisseRepository.save(operation);
        session.setTotalSorties(session.getTotalSorties().add(remboursement.getMontant()));
        session.setSoldeTheorique(session.getSoldeOuverture().add(session.getTotalEntrees()).subtract(session.getTotalSorties()));
        sessionCaisseRepository.save(session);

        remboursement.setStatut(StatutRemboursementApport.PAYEE);
        remboursement.setPayePar(currentUser);
        remboursement.setDatePaiement(LocalDateTime.now());
        remboursement.setOperationCaisse(savedOperation);
        remboursement.setCommentaire(request.getCommentaire().trim());
        RemboursementApportProprietaire saved = remboursementRepository.save(remboursement);
        auditService.logBusinessEvent(AuditAction.MODIFICATION_OPERATION, AuditModule.CAISSE, "RemboursementApportProprietaire", saved.getId(), true, "Paiement remboursement apport propriétaire", saved.getReference());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RemboursementApportResponse> lister() {
        return remboursementRepository.findAll().stream()
                .sorted((a, b) -> b.getDateDemande().compareTo(a.getDateDemande()))
                .map(this::toResponse)
                .toList();
    }

    private RemboursementApportProprietaire find(Long id) {
        return remboursementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remboursement d'apport introuvable"));
    }

    private RemboursementApportResponse toResponse(RemboursementApportProprietaire remboursement) {
        return RemboursementApportResponse.builder()
                .id(remboursement.getId())
                .reference(remboursement.getReference())
                .antenneId(remboursement.getAntenne() != null ? remboursement.getAntenne().getId() : null)
                .antenneNom(remboursement.getAntenne() != null ? remboursement.getAntenne().getNomAgence() : null)
                .montant(remboursement.getMontant())
                .statut(remboursement.getStatut())
                .motifDemande(remboursement.getMotifDemande())
                .demandeParId(remboursement.getDemandePar() != null ? remboursement.getDemandePar().getId() : null)
                .demandeParNom(remboursement.getDemandePar() != null ? remboursement.getDemandePar().getNomComplet() : null)
                .dateDemande(remboursement.getDateDemande())
                .valideParId(remboursement.getValidePar() != null ? remboursement.getValidePar().getId() : null)
                .valideParNom(remboursement.getValidePar() != null ? remboursement.getValidePar().getNomComplet() : null)
                .dateValidation(remboursement.getDateValidation())
                .motifValidation(remboursement.getMotifValidation())
                .payeParId(remboursement.getPayePar() != null ? remboursement.getPayePar().getId() : null)
                .payeParNom(remboursement.getPayePar() != null ? remboursement.getPayePar().getNomComplet() : null)
                .datePaiement(remboursement.getDatePaiement())
                .operationCaisseId(remboursement.getOperationCaisse() != null ? remboursement.getOperationCaisse().getId() : null)
                .commentaire(remboursement.getCommentaire())
                .createdAt(remboursement.getDateCreation())
                .updatedAt(remboursement.getDateModification())
                .build();
    }

    private Utilisateur requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Utilisateur utilisateur) {
            if (utilisateur.getId() != null) {
                return utilisateurRepository.findByIdWithValidationContext(utilisateur.getId()).orElse(utilisateur);
            }
            return utilisateur;
        }
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return utilisateurRepository.findByUsernameWithValidationContext(authentication.getName())
                    .orElseThrow(() -> new BusinessException("Utilisateur authentifié introuvable"));
        }
        throw new BusinessException("Utilisateur authentifié introuvable");
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
