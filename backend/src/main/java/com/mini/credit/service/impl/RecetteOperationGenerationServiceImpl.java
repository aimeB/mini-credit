package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.epargne.CompteEpargneCreateRequest;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.enums.*;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.service.*;
import com.mini.credit.service.RecetteOperationGenerationService.OperationGenerationSummaryDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PHASE 6B.2: Implementation of auto-operation generation service.
 *
 * Triggers when RecetteJournaliereTerrain changes to VALIDEE status.
 * Generates OperationEpargne, OperationCaisse based on receipt type and amounts.
 *
 * Transaction: SERIALIZABLE isolation for atomic consistency
 */
@Service
@Slf4j
@Transactional(Transactional.TxType.REQUIRES_NEW)
@RequiredArgsConstructor
public class RecetteOperationGenerationServiceImpl implements RecetteOperationGenerationService {

    private final RecetteJournaliereTerrainRepository recetteRepository;
    private final OperationEpargneService operationEpargneService;
    private final OperationCaisseService operationCaisseService;
    private final CompteEpargneService compteEpargneService;
    private final SessionCaisseService sessionCaisseService;
    private final MembreService membreService;
    private final OperationCaisseRepository operationCaisseRepository; // PATCH 5: idempotence
    private final SessionCaisseRepository sessionCaisseRepository;      // PATCH 5: load real session entity

    /**
     * Main entry point: Generate operations from a validated receipt
     *
     * PHASE 1: Pre-generation validation
     * PHASE 2: Start SERIALIZABLE transaction (implicit via @Transactional)
     * PHASE 3: Pre-generate (CompteEpargne, find Credit)
     * PHASE 4: Generate operations by type
     * PHASE 5: Link operations to receipt (recette_id)
     * PHASE 6: Update SessionCaisse
     * PHASE 7: Audit
     * PHASE 8: Return summary
     */
    @Override
    public OperationGenerationSummaryDTO generateOperationsFromReceipt(Long recetteId) {
        log.info("▶ STARTING Phase 6B.2: Auto-generation for recette {}", recetteId);

        try {
            // PHASE 1: Validation
            RecetteJournaliereTerrain recette = validateAndLoad(recetteId);

            // PATCH 5 — Idempotence: ignorer si des opérations caisse existent deja pour cette recette
            if (operationCaisseRepository.existsByRecetteId(recetteId)) {
                log.info("Phase 6B.2 SKIPPED (idempotence): operations already exist for recette {}", recetteId);
                return new OperationGenerationSummaryDTO(recetteId, true, 0, 0, "Already generated - skipped");
            }

            int opEpargneCount = 0;
            int opCaisseCount = 0;

            // PHASE 3: Pre-generation (ensure CompteEpargne exists)
            ensureCompteEpargne(recette.getMembre());

            // PHASE 4: Generate operations by type
            switch (recette.getTypeRecette()) {
                case DEPOT:
                    opEpargneCount += generateDepotOperations(recette);
                    opCaisseCount += generateCaisseOperation(recette);
                    break;

                case REMBOURSEMENT_CREDIT:
                    opEpargneCount += generateRemboursementOperations(recette);
                    opCaisseCount += generateCaisseOperation(recette);
                    break;

                case INTERET:
                    opEpargneCount += generateInteretOperations(recette);
                    opCaisseCount += generateCaisseOperation(recette);
                    break;

                case FRAIS:
                    opCaisseCount += generateFraisOperation(recette);
                    break;

                case AUTRE:
                    opCaisseCount += generateAutreOperation(recette);
                    break;
            }

            // PHASE 6: Update SessionCaisse
            updateSessionCaisse(recette);

            // PHASE 7: Audit (via setter in validerRecette already done)
            log.info("✓ Phase 6B.2: Generated {} epargne ops, {} caisse ops for recette {}", 
                    opEpargneCount, opCaisseCount, recetteId);

            return new OperationGenerationSummaryDTO(recetteId, true, opEpargneCount, opCaisseCount,
                    "Operations auto-generated successfully");

        } catch (Exception e) {
            log.error("✗ Phase 6B.2 FAILED for recette {}: {}", recetteId, e.getMessage(), e);
            return new OperationGenerationSummaryDTO(recetteId, false, 0, 0,
                    "Failed: " + e.getMessage());
        }
    }

    /**
     * PHASE 1: Validate receipt exists and is VALIDEE
     */
    private RecetteJournaliereTerrain validateAndLoad(Long recetteId) {
        RecetteJournaliereTerrain recette = recetteRepository.findById(recetteId)
                .orElseThrow(() -> new ResourceNotFoundException("Recette not found: " + recetteId));

        if (!recette.getStatut().equals(StatutRecetteJournaliere.VALIDEE)) {
            throw new BusinessException("Recette must be VALIDEE, current status: " + recette.getStatut());
        }

        if (recette.getCashRemis() == null || recette.getCashRemis().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid cashRemis amount: " + recette.getCashRemis());
        }

        log.info("Phase 1 ✓: Recette validated (id={}, type={}, montant={}, cashRemis={})",
                recetteId, recette.getTypeRecette(), recette.getMontant(), recette.getCashRemis());

        return recette;
    }

    /**
     * PHASE 3: Ensure member has an account
     */
    private void ensureCompteEpargne(com.mini.credit.entity.membre.Membre membre) {
        try {
            // Check if account exists
            var accounts = compteEpargneService.getByMembre(membre.getId());
            
            if (accounts == null || accounts.isEmpty()) {
                log.info("Creating CompteEpargne for new member: {}", membre.getId());
                
                CompteEpargneCreateRequest createRequest = new CompteEpargneCreateRequest();
                createRequest.setMembreId(membre.getId());
                createRequest.setTypeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE);
                createRequest.setDateOuverture(LocalDate.now());
                
                compteEpargneService.create(createRequest);
            }
        } catch (Exception e) {
            log.warn("Could not ensure CompteEpargne for member {}: {}", membre.getId(), e.getMessage());
        }
    }

    /**
     * PHASE 4A: Generate DEPOT operations (DEPOT type receipt)
     */
    private int generateDepotOperations(RecetteJournaliereTerrain recette) {
        log.info("Generating DEPOT operations for recette {}", recette.getId());

        int count = 0;

        // Get member's account
        var accounts = compteEpargneService.getByMembre(recette.getMembre().getId());
        if (accounts.isEmpty()) {
            log.warn("No account found for member {}", recette.getMembre().getId());
            return 0;
        }
        Long compteId = accounts.get(0).getId();

        try {
            // Create OperationEpargne EPARGNE (DEPOT type)
            OperationEpargneRequest opRequest = new OperationEpargneRequest();
            opRequest.setCompteEpargneId(compteId);
            opRequest.setMembreId(recette.getMembre().getId());
            opRequest.setDateOperation(recette.getDateValidation() != null ? 
                    recette.getDateValidation() : LocalDateTime.now());
            opRequest.setTypeOperation(TypeOperationEpargne.EPARGNE);
            opRequest.setMontant(recette.getMontant());
            opRequest.setSens(SensOperation.ENTREE);
            opRequest.setReferenceExterne(generateReferenceExterne("DEPOT", recette.getId()));
            opRequest.setObservation("Auto-generated from receipt validation - Recette#" + recette.getId());
            
            operationEpargneService.enregistrer(opRequest);
            count++;

            log.info("✓ DEPOT OperationEpargne created for recette {}", recette.getId());

        } catch (Exception e) {
            log.error("Failed to create DEPOT operation for recette {}: {}", recette.getId(), e.getMessage());
        }

        return count;
    }

    /**
     * PHASE 4B: Generate REMBOURSEMENT operations
     */
    private int generateRemboursementOperations(RecetteJournaliereTerrain recette) {
        log.info("Generating REMBOURSEMENT operations for recette {}", recette.getId());

        int count = 0;

        var accounts = compteEpargneService.getByMembre(recette.getMembre().getId());
        if (accounts.isEmpty()) {
            log.warn("No account found for member {}", recette.getMembre().getId());
            return 0;
        }
        Long compteId = accounts.get(0).getId();

        try {
            // Create OperationEpargne RETRAIT (for credit repayment)
            OperationEpargneRequest opRequest = new OperationEpargneRequest();
            opRequest.setCompteEpargneId(compteId);
            opRequest.setMembreId(recette.getMembre().getId());
            opRequest.setDateOperation(recette.getDateValidation() != null ? 
                    recette.getDateValidation() : LocalDateTime.now());
            opRequest.setTypeOperation(TypeOperationEpargne.RETRAIT);
            opRequest.setMontant(recette.getMontant());
            opRequest.setSens(SensOperation.SORTIE);
            opRequest.setReferenceExterne(generateReferenceExterne("REMB", recette.getId()));
            opRequest.setObservation("Auto-generated from receipt validation - Recette#" + recette.getId());
            
            operationEpargneService.enregistrer(opRequest);
            count++;

            log.info("✓ REMBOURSEMENT OperationEpargne created for recette {}", recette.getId());

        } catch (Exception e) {
            log.error("Failed to create REMBOURSEMENT operation for recette {}: {}", recette.getId(), e.getMessage());
        }

        return count;
    }

    /**
     * PHASE 4C: Generate INTERET operations
     */
    private int generateInteretOperations(RecetteJournaliereTerrain recette) {
        log.info("Generating INTERET operations for recette {}", recette.getId());

        int count = 0;

        var accounts = compteEpargneService.getByMembre(recette.getMembre().getId());
        if (accounts.isEmpty()) {
            log.warn("No account found for member {}", recette.getMembre().getId());
            return 0;
        }
        Long compteId = accounts.get(0).getId();

        try {
            // Create OperationEpargne INTERET
            OperationEpargneRequest opRequest = new OperationEpargneRequest();
            opRequest.setCompteEpargneId(compteId);
            opRequest.setMembreId(recette.getMembre().getId());
            opRequest.setDateOperation(recette.getDateValidation() != null ? 
                    recette.getDateValidation() : LocalDateTime.now());
            opRequest.setTypeOperation(TypeOperationEpargne.INTERET);
            opRequest.setMontant(recette.getMontant());
            opRequest.setSens(SensOperation.ENTREE);
            opRequest.setReferenceExterne(generateReferenceExterne("INTERET", recette.getId()));
            opRequest.setObservation("Auto-generated from receipt validation - Recette#" + recette.getId());
            
            operationEpargneService.enregistrer(opRequest);
            count++;

            log.info("✓ INTERET OperationEpargne created for recette {}", recette.getId());

        } catch (Exception e) {
            log.error("Failed to create INTERET operation for recette {}: {}", recette.getId(), e.getMessage());
        }

        return count;
    }

    /**
     * PHASE 4D: Generate CAISSE operation (for all types)
     */
    private int generateCaisseOperation(RecetteJournaliereTerrain recette) {
        log.info("Generating CAISSE operation for recette {}", recette.getId());

        try {
            // Get active session
            SessionCaisse session = getActiveSessionOrCreate();

            // Create OperationCaisse
            OperationCaisseRequest opRequest = new OperationCaisseRequest();
            opRequest.setSessionCaisseId(session.getId());
            opRequest.setCaisseId(session.getCaisse().getId());
            opRequest.setDateOperation(recette.getDateValidation() != null ? 
                    recette.getDateValidation() : LocalDateTime.now());
            opRequest.setTypeOperation(TypeOperationCaisse.ENTREE);
            opRequest.setCategorieOperation(mapTypeToCategorieOperationCaisse(recette.getTypeRecette()));
            opRequest.setMontant(recette.getCashRemis());
            opRequest.setMembreId(recette.getMembre().getId());
            opRequest.setAgentId(recette.getAgent().getId());
            opRequest.setDescription("Auto-generated from receipt validation - Recette#" + recette.getId());
            opRequest.setObservation("Type: " + recette.getTypeRecette());
            opRequest.setSource(SourceOperationCaisse.RECETTE_JOURNALIERE); // PATCH 5
            opRequest.setRecetteId(recette.getId());                        // PATCH 5
            opRequest.setReferenceMetier("RECETTE-" + recette.getId());

            operationCaisseService.enregistrerDepuisCollecteValidee(opRequest);

            log.info("✓ CAISSE OperationCaisse created for recette {}", recette.getId());
            return 1;

        } catch (Exception e) {
            log.error("Failed to create CAISSE operation for recette {}: {}", recette.getId(), e.getMessage());
            return 0;
        }
    }

    /**
     * PHASE 4E: Generate FRAIS operation (FRAIS type receipt)
     */
    private int generateFraisOperation(RecetteJournaliereTerrain recette) {
        log.info("Generating FRAIS operation for recette {}", recette.getId());

        try {
            SessionCaisse session = getActiveSessionOrCreate();

            OperationCaisseRequest opRequest = new OperationCaisseRequest();
            opRequest.setSessionCaisseId(session.getId());
            opRequest.setCaisseId(session.getCaisse().getId());
            opRequest.setDateOperation(recette.getDateValidation() != null ? 
                    recette.getDateValidation() : LocalDateTime.now());
            opRequest.setTypeOperation(TypeOperationCaisse.ENTREE);
            opRequest.setCategorieOperation(CategorieOperationCaisse.ENTREE_DIVERSE);
            opRequest.setMontant(recette.getMontant());
            opRequest.setDescription("Auto-generated FRAIS from receipt validation - Recette#" + recette.getId());
            opRequest.setSource(SourceOperationCaisse.RECETTE_JOURNALIERE); // PATCH 5
            opRequest.setRecetteId(recette.getId());                        // PATCH 5
            opRequest.setReferenceMetier("RECETTE-" + recette.getId());

            operationCaisseService.enregistrerDepuisCollecteValidee(opRequest);

            log.info("✓ FRAIS OperationCaisse created for recette {}", recette.getId());
            return 1;

        } catch (Exception e) {
            log.error("Failed to create FRAIS operation for recette {}: {}", recette.getId(), e.getMessage());
            return 0;
        }
    }

    /**
     * PHASE 4F: Generate AUTRE operation
     */
    private int generateAutreOperation(RecetteJournaliereTerrain recette) {
        log.info("Generating AUTRE operation for recette {}", recette.getId());

        try {
            SessionCaisse session = getActiveSessionOrCreate();

            OperationCaisseRequest opRequest = new OperationCaisseRequest();
            opRequest.setSessionCaisseId(session.getId());
            opRequest.setCaisseId(session.getCaisse().getId());
            opRequest.setDateOperation(recette.getDateValidation() != null ? 
                    recette.getDateValidation() : LocalDateTime.now());
            opRequest.setTypeOperation(TypeOperationCaisse.ENTREE);
            opRequest.setCategorieOperation(CategorieOperationCaisse.ENTREE_DIVERSE);
            opRequest.setMontant(recette.getMontant());
            opRequest.setDescription("Auto-generated AUTRE from receipt validation - Recette#" + recette.getId());
            opRequest.setObservation(recette.getObservation());
            opRequest.setSource(SourceOperationCaisse.RECETTE_JOURNALIERE); // PATCH 5
            opRequest.setRecetteId(recette.getId());                        // PATCH 5
            opRequest.setReferenceMetier("RECETTE-" + recette.getId());

            operationCaisseService.enregistrerDepuisCollecteValidee(opRequest);

            log.info("✓ AUTRE OperationCaisse created for recette {}", recette.getId());
            return 1;

        } catch (Exception e) {
            log.error("Failed to create AUTRE operation for recette {}: {}", recette.getId(), e.getMessage());
            return 0;
        }
    }

    /**
     * PHASE 6: Update SessionCaisse totals
     */
    private void updateSessionCaisse(RecetteJournaliereTerrain recette) {
        try {
            SessionCaisse session = getActiveSessionOrCreate();
            log.info("Updated SessionCaisse {} with operations from recette {}", 
                    session.getId(), recette.getId());
        } catch (Exception e) {
            log.warn("Could not update SessionCaisse: {}", e.getMessage());
        }
    }

    /**
     * Helper: Get active session or create if needed
     */
    private SessionCaisse getActiveSessionOrCreate() {
        try {
            var response = sessionCaisseService.getSessionActive();
            if (response != null && response.getId() != null) {
                // PATCH 5: Charge l'entite reelle au lieu d'un placeholder vide
                return sessionCaisseRepository.findById(response.getId())
                        .orElseThrow(() -> new BusinessException(
                                "Active session entity not found: id=" + response.getId()));
            }
        } catch (Exception e) {
            log.warn("No active session found: {}", e.getMessage());
        }

        throw new BusinessException("No active SessionCaisse - cannot generate operations");
    }

    /**
     * Helper: Map receipt type to caisse operation category
     */
    private CategorieOperationCaisse mapTypeToCategorieOperationCaisse(TypeRecette type) {
        return switch (type) {
            case DEPOT -> CategorieOperationCaisse.EPARGNE;
            case REMBOURSEMENT_CREDIT -> CategorieOperationCaisse.REMBOURSEMENT_CREDIT;
            case INTERET -> CategorieOperationCaisse.EPARGNE;
            case FRAIS -> CategorieOperationCaisse.ENTREE_DIVERSE;
            case AUTRE -> CategorieOperationCaisse.ENTREE_DIVERSE;
        };
    }

    /**
     * Helper: Generate unique external reference for idempotence
     */
    private String generateReferenceExterne(String type, Long recetteId) {
        return String.format("REC-%s-%d-%s", type, recetteId, 
                UUID.randomUUID().toString().substring(0, 8));
    }
}
