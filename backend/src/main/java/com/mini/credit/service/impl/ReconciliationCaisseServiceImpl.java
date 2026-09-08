package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.ReconciliationCaisseDTO;
import com.mini.credit.entity.caisse.EcartCaisse;
import com.mini.credit.entity.caisse.ReconciliationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.StatutReconciliation;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeEcartCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.ReconciliationCaisseMapper;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.ReconciliationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.service.ReconciliationCaisseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 11: Implémentation service réconciliation caisse automatique
 *
 * Logique:
 * - Montant écart = soldePhysique - soldeTheorique
 * - Batch job quotidien: après clôtures de session
 * - Crée ReconciliationCaisse pour écarts détectés
 * - Peut créer EcartCaisse associé
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ReconciliationCaisseServiceImpl implements ReconciliationCaisseService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final ReconciliationCaisseRepository reconciliationCaisseRepository;
    private final EcartCaisseRepository ecartCaisseRepository;
    private final ReconciliationCaisseMapper reconciliationCaisseMapper;

    /**
     * PHASE 11: Batch job quotidien - 02:00 chaque jour
    * Crée réconciliations pour sessions clôturées avec écarts
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Override
    public List<ReconciliationCaisseDTO> genererReconciliationsToutes() {
        log.info("PHASE 11: Démarrage batch génération réconciliations caisse (quotidien)");

        // Cherche sessions clôturées
        List<SessionCaisse> sessionsCloturees = sessionCaisseRepository.findAll()
                .stream()
            .filter(s -> s.getStatut() == StatutSessionCaisse.CLOTUREE)
                .filter(s -> s.getSoldePhysique() != null)
                .filter(s -> s.getSoldePhysique().compareTo(s.getSoldeTheorique()) != 0)
                .collect(Collectors.toList());

        log.info("Nombre de sessions avec écarts détectés: {}", sessionsCloturees.size());

        // Crée réconciliations pour chaque session
        List<ReconciliationCaisseDTO> reconciliations = sessionsCloturees.stream()
                .map(session -> {
                    try {
                        // Vérifie si réconciliation n'existe pas déjà
                        if (!reconciliationCaisseRepository.existsBySessionCaisseId(session.getId())) {
                            return creerReconciliation(session.getId());
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("Erreur création réconciliation session {}: {}", session.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());

        log.info("Batch réconciliations terminé: {} réconciliations créées", reconciliations.size());
        return reconciliations;
    }

    /**
    * Crée une réconciliation pour une session caisse clôturée
     *
     * @param sessionCaisseId ID de la session
     * @return ReconciliationCaisseDTO créée
     */
    @Override
    public ReconciliationCaisseDTO creerReconciliation(Long sessionCaisseId) {
        log.debug("Création réconciliation pour session: {}", sessionCaisseId);

        // Récupère session
        SessionCaisse session = sessionCaisseRepository.findById(sessionCaisseId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse non trouvée: " + sessionCaisseId));

        // Vérifie que session est clôturée
        if (session.getStatut() != StatutSessionCaisse.CLOTUREE) {
            throw new BusinessException("Session non clôturée: " + sessionCaisseId);
        }

        // Vérifie que soldePhysique est saisi
        if (session.getSoldePhysique() == null) {
            throw new BusinessException("Solde physique non saisi pour session: " + sessionCaisseId);
        }

        // Calcule écart
        BigDecimal montantEcart = session.getSoldePhysique().subtract(session.getSoldeTheorique());
        if (montantEcart.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("Aucun écart détecté pour session: " + sessionCaisseId);
        }

        log.debug("Session {}: écart = {} ({} vs théorique {})",
                sessionCaisseId, montantEcart, session.getSoldePhysique(), session.getSoldeTheorique());

        // Crée ReconciliationCaisse
        ReconciliationCaisse reconciliation = ReconciliationCaisse.builder()
                .sessionCaisse(session)
                .montantAttendu(session.getSoldeTheorique())
                .montantObserve(session.getSoldePhysique())
                .montantEcart(montantEcart)
                .statut(StatutReconciliation.CREEE)
                .dateCreationReconciliation(LocalDateTime.now())
                .observation("Réconciliation créée automatiquement par batch - Écart détecté")
                .build();

        reconciliationCaisseRepository.save(reconciliation);

        // Crée automatiquement EcartCaisse associé si nécessaire
        try {
            String typeEcart = montantEcart.compareTo(BigDecimal.ZERO) < 0 ? "DEFICIT" : "EXCEDENT";
            EcartCaisse ecartCaisse = EcartCaisse.builder()
                    .sessionCaisse(session)
                    .dateJour(session.getDateCloture().toLocalDate())
                    .typeEcart(TypeEcartCaisse.valueOf("VARIANCE_" + typeEcart))
                    .montantEcart(montantEcart.abs())
                    .description("Écart détecté lors clôture session. Attendu: " + session.getSoldeTheorique() +
                            ", Observé: " + session.getSoldePhysique())
                    .statut(StatutEcartCaisse.DETECTE)
                    .build();
            EcartCaisse ecart = ecartCaisseRepository.save(ecartCaisse);
            reconciliation.setEcartCaisse(ecart);
            reconciliationCaisseRepository.save(reconciliation);
            log.info("EcartCaisse créé automatiquement pour session {}", sessionCaisseId);
        } catch (Exception e) {
            log.warn("EcartCaisse non créé pour session {}: {}", sessionCaisseId, e.getMessage());
        }

        log.info("Réconciliation créée pour session {}: écart={}, montant_attendu={}, montant_observe={}",
                sessionCaisseId, montantEcart, session.getSoldeTheorique(), session.getSoldePhysique());

        return reconciliationCaisseMapper.toDTO(reconciliation);
    }

    /**
     * Récupère réconciliations d'une session
     *
     * @param sessionCaisseId ID session
     * @return liste
     */
    @Override
    public List<ReconciliationCaisseDTO> getBySessionCaisseId(Long sessionCaisseId) {
        return reconciliationCaisseRepository.findBySessionCaisseIdOrderByDateCreationReconciliationDesc(sessionCaisseId)
                .stream()
                .map(reconciliationCaisseMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère réconciliations en attente (statut CREEE)
     *
     * @return liste
     */
    @Override
    public List<ReconciliationCaisseDTO> getEnAttente() {
        return reconciliationCaisseRepository.findByStatutOrderByDateCreationReconciliationDesc(StatutReconciliation.CREEE)
                .stream()
                .map(reconciliationCaisseMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Marque réconciliation comme rapprochée
     *
     * @param reconciliationId ID
     * @param motif motif du rapprochement
     * @return ReconciliationCaisseDTO mise à jour
     */
    @Override
    public ReconciliationCaisseDTO rapprocheer(Long reconciliationId, String motif) {
        log.debug("Rapprochement réconciliation: {}", reconciliationId);

        ReconciliationCaisse reconciliation = reconciliationCaisseRepository.findById(reconciliationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réconciliation non trouvée: " + reconciliationId));

        if (!reconciliation.canBeRapprochee()) {
            throw new BusinessException("Réconciliation ne peut pas être rapprochée: " + reconciliationId);
        }

        // Récupère utilisateur courant
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        reconciliation.setStatut(StatutReconciliation.RAPPROCHEE);
        reconciliation.setDateRapprochement(LocalDateTime.now());
        reconciliation.setMotifRapprochement(motif);
        // Nota: rapprochePar serait peuplé depuis le contrôleur avec l'utilisateur courant
        
        reconciliationCaisseRepository.save(reconciliation);
        log.info("Réconciliation {} marquée RAPPROCHEE par {}", reconciliationId, username);

        return reconciliationCaisseMapper.toDTO(reconciliation);
    }

    /**
     * Rejette réconciliation
     *
     * @param reconciliationId ID
     * @param raison raison du rejet
     * @return ReconciliationCaisseDTO mise à jour
     */
    @Override
    public ReconciliationCaisseDTO rejeter(Long reconciliationId, String raison) {
        log.debug("Rejet réconciliation: {}", reconciliationId);

        ReconciliationCaisse reconciliation = reconciliationCaisseRepository.findById(reconciliationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réconciliation non trouvée: " + reconciliationId));

        if (!reconciliation.canBeRejectee()) {
            throw new BusinessException("Réconciliation ne peut pas être rejetée: " + reconciliationId);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        reconciliation.setStatut(StatutReconciliation.REJETEE);
        reconciliation.setRaisonRejet(raison);

        reconciliationCaisseRepository.save(reconciliation);
        log.info("Réconciliation {} rejetée par {}", reconciliationId, username);

        return reconciliationCaisseMapper.toDTO(reconciliation);
    }

    /**
     * Total écarts en attente
     *
     * @return montant total
     */
    @Override
    public BigDecimal getTotalEcartsEnAttente() {
        BigDecimal total = reconciliationCaisseRepository.sumMontantEcartByStatut(StatutReconciliation.CREEE);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Nombre de réconciliations en attente
     *
     * @return nombre
     */
    @Override
    public Long getNombreReconciliationsEnAttente() {
        return reconciliationCaisseRepository.countByStatutCreee();
    }
}
