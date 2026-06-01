package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.SessionCaisseOpenRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.SessionCaisseService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionCaisseServiceImpl implements SessionCaisseService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CashMapper cashMapper;

    @Override
    @Auditable(action = AuditAction.SESSION_CAISSE_OPENED, entityType = "SessionCaisse")
    public SessionCaisseResponse ouvrir(SessionCaisseOpenRequest request) {
        validerOuvertureRequest(request);

        Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        if (Boolean.FALSE.equals(caisse.getActif())) {
            throw new BusinessException("La caisse est inactive");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (sessionCaisseRepository.existsByCaisseIdAndStatut(request.getCaisseId(), StatutSessionCaisse.OUVERTE)) {
            throw new BusinessException("Une session est déjà ouverte pour cette caisse");
        }

        if (sessionCaisseRepository.existsByUtilisateurIdAndStatut(request.getUtilisateurId(), StatutSessionCaisse.OUVERTE)) {
            throw new BusinessException("Cet utilisateur a déjà une session de caisse ouverte");
        }

        SessionCaisse session = SessionCaisse.builder()
                .caisse(caisse)
                .utilisateur(utilisateur)
                .dateOuverture(request.getDateOuverture())
                .soldeOuverture(request.getSoldeOuverture())
                .totalEntrees(BigDecimal.ZERO)
                .totalSorties(BigDecimal.ZERO)
                .soldeTheorique(request.getSoldeOuverture())
                .statut(StatutSessionCaisse.OUVERTE)
                .observation(cleanNullableText(request.getObservation()))
                .build();

        return cashMapper.toResponse(sessionCaisseRepository.save(session));
    }

    @Override
    @Auditable(action = AuditAction.SESSION_CAISSE_CLOSED, entityType = "SessionCaisse", entityIdParameter = "sessionId")
    public SessionCaisseResponse cloturer(Long sessionId, SessionCaisseCloseRequest request) {
        validerClotureRequest(request);

        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("Cette session n'est pas ouverte");
        }

        if (session.getDateCloture() != null) {
            throw new BusinessException("Cette session est déjà clôturée");
        }

        if (request.getDateCloture().isBefore(session.getDateOuverture())) {
            throw new BusinessException("La date de clôture ne peut pas être antérieure à la date d'ouverture");
        }

        session.setDateCloture(request.getDateCloture());
        session.setSoldePhysique(request.getSoldePhysique());
        session.setEcartCaisse(request.getSoldePhysique().subtract(session.getSoldeTheorique()));
        session.setStatut(StatutSessionCaisse.FERMEE);
        session.setObservation(cleanNullableText(request.getObservation()));

        return cashMapper.toResponse(sessionCaisseRepository.save(session));
    }

    @Override
    public List<SessionCaisseResponse> getAll() {
        return sessionCaisseRepository.findAllByOrderByDateOuvertureDesc().stream()
                .map(cashMapper::toResponse)
                .toList();
    }

    @Override
    public SessionCaisseResponse getById(Long id) {
        return cashMapper.toResponse(
                sessionCaisseRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"))
        );
    }

    @Override
    public SessionCaisseResponse getSessionActive() {
        SessionCaisse session = sessionCaisseRepository
                .findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune session de caisse ouverte"));

        return cashMapper.toResponse(session);
    }

    private void validerOuvertureRequest(SessionCaisseOpenRequest request) {
        if (request == null) {
            throw new BusinessException("La requête d'ouverture de session est obligatoire");
        }

        if (request.getCaisseId() == null) {
            throw new BusinessException("La caisse est obligatoire");
        }

        if (request.getUtilisateurId() == null) {
            throw new BusinessException("L'utilisateur est obligatoire");
        }

        if (request.getDateOuverture() == null) {
            throw new BusinessException("La date d'ouverture est obligatoire");
        }

        if (request.getSoldeOuverture() == null) {
            throw new BusinessException("Le solde d'ouverture est obligatoire");
        }

        if (request.getSoldeOuverture().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le solde d'ouverture ne peut pas être négatif");
        }
    }

    private void validerClotureRequest(SessionCaisseCloseRequest request) {
        if (request == null) {
            throw new BusinessException("La requête de clôture de session est obligatoire");
        }

        if (request.getDateCloture() == null) {
            throw new BusinessException("La date de clôture est obligatoire");
        }

        if (request.getSoldePhysique() == null) {
            throw new BusinessException("Le solde physique est obligatoire");
        }

        if (request.getSoldePhysique().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le solde physique ne peut pas être négatif");
        }
    }

    private String cleanNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}