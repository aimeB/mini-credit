package com.mini.credit.service.impl;

import com.mini.credit.dto.epargne.DemandeRetraitEpargneDTO;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.document.TicketRecuGenerationRequest;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutDemandeRetrait;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.TypeTicketRecu;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.DemandeRetraitEpargneMapper;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.service.DemandeRetraitEpargneService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.RetraitEpargneCommissionService;
import com.mini.credit.service.TicketRecuService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.audit.AuditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de demande de retrait épargne (PHASE 5).
 *
 * Logique:
 * 1. Créer demande (CREEE)
 * 2. Valider demande - vérifie solde >= montant (VALIDEE ou REJETEE)
 * 3. Décaisser - crée OperationEpargne type RETRAIT (DECAISSEE)
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class DemandeRetraitEpargneServiceImpl implements DemandeRetraitEpargneService {

    private final DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final OperationEpargneRepository operationEpargneRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
        private final OperationCaisseRepository operationCaisseRepository;
    private final DemandeRetraitEpargneMapper demandeRetraitEpargneMapper;
        private final OperationCaisseService operationCaisseService;
        private final RetraitEpargneCommissionService retraitEpargneCommissionService;
        private final TicketRecuService ticketRecuService;
        private final AuditService auditService;
        private final WorkflowTaskService workflowTaskService;

    /**
     * Crée une nouvelle demande de retrait épargne
     */
    @Override
        public DemandeRetraitEpargneDTO creerDemande(Long compteEpargneId, BigDecimal montant, BigDecimal fraisRetrait, String observation) {
                log.info("Création demande retrait épargne: compteId={}, montant={}, fraisRetrait={}", compteEpargneId, montant, fraisRetrait);

        // Validations
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de retrait doit être > 0");
        }

                RetraitEpargneCommissionService.CommissionRetrait commission = retraitEpargneCommissionService.calculer("CDF", montant);

        // Récupère le compte
        CompteEpargne compte = compteEpargneRepository.findById(compteEpargneId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte épargne non trouvé: " + compteEpargneId));

        LocalDateTime dateDemande = LocalDateTime.now();

        // Crée la demande
        DemandeRetraitEpargne demande = DemandeRetraitEpargne.builder()
                .compteEpargne(compte)
                .membre(compte.getMembre())
                .montantDemande(commission.montantRetrait())
                .fraisRetrait(commission.commission())
                .tauxCommissionRetrait(commission.tauxPourcentage())
                .montantTotalDebite(commission.montantTotalDebite())
                .statut(StatutDemandeRetrait.CREEE)
                .dateDemande(dateDemande)
                .observation(observation)
                .build();

        demande = demandeRetraitEpargneRepository.save(demande);
        demande.setReferenceRetrait(generateRetraitReference(demande));
        demande = demandeRetraitEpargneRepository.save(demande);
        log.info("Demande retrait créée: reference={}, id={}, statut={}", demande.getReferenceRetrait(), demande.getId(), demande.getStatut());

        workflowTaskService.onRetraitDemande(
                demande.getId(),
                buildRetraitReferenceMetier(demande),
                resolveAntenneId(demande),
                resolveSiteId(demande)
        );

        return toDTOWithOperations(demande);
    }

    /**
     * Récupère une demande par ID
     */
    @Override
    public DemandeRetraitEpargneDTO getById(Long id) {
        DemandeRetraitEpargne demande = demandeRetraitEpargneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande retrait non trouvée: " + id));

        return toDTOWithOperations(demande);
    }

    @Override
    public List<DemandeRetraitEpargneDTO> getAll(StatutDemandeRetrait statut) {
        List<DemandeRetraitEpargne> demandes = statut != null
                ? demandeRetraitEpargneRepository.findByStatutOrderByDateDemandeAsc(statut)
                : demandeRetraitEpargneRepository.findAll();

        return demandes.stream()
                .map(this::toDTOWithOperations)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les demandes d'un compte épargne
     */
    @Override
    public List<DemandeRetraitEpargneDTO> getByCompteEpargne(Long compteEpargneId) {
        CompteEpargne compte = compteEpargneRepository.findById(compteEpargneId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte épargne non trouvé: " + compteEpargneId));

        List<DemandeRetraitEpargne> demandes = demandeRetraitEpargneRepository.findByCompteEpargne(compte);

        return demandes.stream()
                .map(this::toDTOWithOperations)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les demandes en attente de validation CONTROLEUR
     */
    @Override
    public List<DemandeRetraitEpargneDTO> getEnAttenteValidation() {
        List<DemandeRetraitEpargne> demandes = demandeRetraitEpargneRepository
                .findByStatutInOrderByDateDemandeAsc(List.of(
                        StatutDemandeRetrait.CREEE,
                        StatutDemandeRetrait.EN_ATTENTE_VALIDATION
                ));

        return demandes.stream()
                .map(this::toDTOWithOperations)
                .collect(Collectors.toList());
    }

    /**
     * PHASE 5: Valide une demande de retrait épargne
     * Vérifie si solde >= montantDemande
     */
    @Override
    public DemandeRetraitEpargneDTO validerDemande(Long demandeId) {
        log.info("Validation demande retrait: id={}", demandeId);

        DemandeRetraitEpargne demande = demandeRetraitEpargneRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande retrait non trouvée: " + demandeId));

        // Vérifie que la demande peut être validée
        if (!demande.canBeValidated()) {
            log.warn("Validation échouée: solde insuffisant. Solde={}, Demande={}",
                    demande.getCompteEpargne().getSoldeDisponible(),
                    resolveMontantTotalDebite(demande));

            demande.setStatut(StatutDemandeRetrait.REJETEE);
            demande.setMotifRejet("Solde insuffisant: " +
                    demande.getCompteEpargne().getSoldeDisponible() + " < " + resolveMontantTotalDebite(demande));
            demande.setValidePar(getCurrentUtilisateur());
            demande.setDateValidation(LocalDateTime.now());

            demande = demandeRetraitEpargneRepository.save(demande);
            log.info("Demande rejetée: id={}", demandeId);

            workflowTaskService.onRetraitRejete(
                    demande.getId(),
                    buildRetraitReferenceMetier(demande),
                    resolveAntenneId(demande),
                    resolveSiteId(demande)
            );

            return toDTOWithOperations(demande);
        }

        // Valide la demande
        demande.setStatut(StatutDemandeRetrait.VALIDEE);
        demande.setValidePar(getCurrentUtilisateur());
        demande.setDateValidation(LocalDateTime.now());

        demande = demandeRetraitEpargneRepository.save(demande);
        log.info("Demande validée: id={}, montant={}", demandeId, demande.getMontantDemande());

        workflowTaskService.onRetraitApprouve(
                demande.getId(),
                buildRetraitReferenceMetier(demande),
                resolveAntenneId(demande),
                resolveSiteId(demande)
        );

        return toDTOWithOperations(demande);
    }

    /**
     * Rejette une demande de retrait avec motif
     */
    @Override
    public DemandeRetraitEpargneDTO rejeterDemande(Long demandeId, String motif) {
        log.info("Rejet demande retrait: id={}, motif={}", demandeId, motif);

        DemandeRetraitEpargne demande = demandeRetraitEpargneRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande retrait non trouvée: " + demandeId));

        demande.setStatut(StatutDemandeRetrait.REJETEE);
        demande.setMotifRejet(motif);
        demande.setValidePar(getCurrentUtilisateur());
        demande.setDateValidation(LocalDateTime.now());

        demande = demandeRetraitEpargneRepository.save(demande);

        workflowTaskService.onRetraitRejete(
                demande.getId(),
                buildRetraitReferenceMetier(demande),
                resolveAntenneId(demande),
                resolveSiteId(demande)
        );

        return toDTOWithOperations(demande);
    }

    /**
     * PHASE 6B.3: Décaisse un retrait épargne
     * 
     * Workflow CRITIQUE (3N):
     * 1. Créer OperationEpargne type RETRAIT (historisation compte épargne)
     * 2. Créer OperationCaisse type SORTIE (impacte la caisse)
     * 3. Réduire soldeDisponible du compte
     * 4. Passer au statut DECAISSEE
     * 
     * Règles à respecter:
     * - La demande DOIT être VALIDEE avant décaissement
     * - Une session caisse ACTIVE doit exister
     * - L'idempotence: un retrait déjà payé ne crée PAS d'opérations dupliquées
     */
    @Override
    public DemandeRetraitEpargneDTO decaisserRetrait(Long demandeId) {
        log.info("Décaissement retrait épargne: id={}", demandeId);

        DemandeRetraitEpargne demande = demandeRetraitEpargneRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande retrait non trouvée: " + demandeId));

        // [IDEMPOTENCE FIRST] Si déjà décaissée, retourner sans recréer opérations
        if (demande.getStatut() == StatutDemandeRetrait.DECAISSEE) {
            log.warn("Retrait déjà décaissé, retour sans recréation: id={}", demandeId);
                        workflowTaskService.onRetraitPaye(
                                        demande.getId(),
                                        buildRetraitReferenceMetier(demande),
                                        resolveAntenneId(demande),
                                        resolveSiteId(demande)
                        );
                        return toDTOWithOperations(demande);
        }

        // Vérifie que la demande peut être décaissée
        if (!demande.canBeDisbursed()) {
            throw new BusinessException("Demande non validée: statut=" + demande.getStatut());
        }

        // ========== VÉRIFICATION SESSION CAISSE AVANT TOUT ==========
        // CRITIQUE: Vérifier que session existe AVANT de créer OperationEpargne
        SessionCaisse sessionActive = sessionCaisseRepository
                .findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)
                .orElseThrow(() -> new BusinessException(
                    "Impossible de décaisser: pas de session caisse ouverte"));

                Utilisateur currentUser = getCurrentUtilisateur();
                if (currentUser == null || currentUser.getId() == null) {
                        throw new BusinessException("Utilisateur authentifié introuvable pour le décaissement du retrait");
                }

                String referenceRetrait = buildRetraitReferenceMetier(demande);
                BigDecimal montantRetrait = demande.getMontantDemande();
                BigDecimal fraisRetrait = safeAmount(demande.getFraisRetrait());
                BigDecimal montantTotalDebite = resolveMontantTotalDebite(demande);

                if (demande.getCompteEpargne().getSoldeDisponible() == null
                                || demande.getCompteEpargne().getSoldeDisponible().compareTo(montantTotalDebite) < 0) {
                        throw new BusinessException("Solde insuffisant: "
                                        + demande.getCompteEpargne().getSoldeDisponible() + " < " + montantTotalDebite);
                }

                BigDecimal ancienSolde = demande.getCompteEpargne().getSoldeDisponible();

        // ========== OPERATION 1: OperationEpargne ==========
        // Crée l'opération épargne de type RETRAIT (historique compte)
        OperationEpargne operationEpargne = OperationEpargne.builder()
                .compteEpargne(demande.getCompteEpargne())
                .membre(demande.getMembre())
                .dateOperation(LocalDateTime.now())
                .typeOperation(TypeOperationEpargne.RETRAIT)
                .montant(montantTotalDebite)
                .sens(SensOperation.SORTIE)
                .modePaiement(ModePaiement.ESPECES)
                .referenceExterne(referenceRetrait)
                .sessionCaisse(sessionActive)
                .observation("Retrait épargne " + referenceRetrait
                                + " | montantRetrait=" + montantRetrait
                                + " | tauxCommission=" + demande.getTauxCommissionRetrait() + "%"
                                + " | commission=" + fraisRetrait
                                + " | totalDebite=" + montantTotalDebite)
                .createdBy(currentUser)
                .build();

        operationEpargne = operationEpargneRepository.save(operationEpargne);
        log.info("✓ OperationEpargne créée: id={}, type=RETRAIT, montant={}", 
                operationEpargne.getId(), operationEpargne.getMontant());

        // ========== OPERATION 2: OperationCaisse ==========
        // CRITIQUE: Créer OperationCaisse pour impacter le mouvement de caisse
        // Cette opération est ESSENTIELLE pour respecter la règle 3N:
        // "Toute sortie en caisse doit être tracée"
        
        OperationCaisseRequest operationCaisseRequest = new OperationCaisseRequest();
        operationCaisseRequest.setSessionCaisseId(sessionActive.getId());
        operationCaisseRequest.setCaisseId(sessionActive.getCaisse().getId());
        operationCaisseRequest.setDateOperation(LocalDateTime.now());
        operationCaisseRequest.setTypeOperation(TypeOperationCaisse.SORTIE);
        operationCaisseRequest.setCategorieOperation(CategorieOperationCaisse.RETRAIT_EPARGNE);
        operationCaisseRequest.setMontant(montantRetrait);
        operationCaisseRequest.setDevise("CDF");
        operationCaisseRequest.setMembreId(demande.getMembre().getId());
        operationCaisseRequest.setOperationEpargneId(operationEpargne.getId());

        operationCaisseRequest.setDescription("Retrait épargne demande " + referenceRetrait
                + " - Membre: " + demande.getMembre().getNom());
        operationCaisseRequest.setObservation(demande.getObservation());
        operationCaisseRequest.setCommentaire(buildDecaissementRetraitComment(
                demande,
                sessionActive,
                currentUser,
                montantRetrait,
                "RETRAIT_EPARGNE"
        ));
        operationCaisseRequest.setCreatedBy(currentUser.getId());
        operationCaisseRequest.setUtilisateurId(currentUser.getId());
        operationCaisseRequest.setModePaiement(ModePaiement.ESPECES);
        operationCaisseRequest.setSource(SourceOperationCaisse.RETRAIT_EPARGNE);
        operationCaisseRequest.setReferenceExterne(referenceRetrait);
        operationCaisseRequest.setReferenceMetier(referenceRetrait);
        operationCaisseRequest.setRetraitEpargneId(demande.getId());

        OperationCaisseResponse operationCaisse = operationCaisseService.enregistrer(operationCaisseRequest);
        log.info("✓ OperationCaisse créée: id={}, type=SORTIE, montant={}, categorie=RETRAIT_EPARGNE", 
                operationCaisse.getId(), operationCaisse.getMontant());

        OperationCaisseResponse operationCaisseFrais = null;
        if (fraisRetrait.compareTo(BigDecimal.ZERO) > 0) {
            OperationCaisseRequest fraisCaisseRequest = new OperationCaisseRequest();
            fraisCaisseRequest.setSessionCaisseId(sessionActive.getId());
            fraisCaisseRequest.setCaisseId(sessionActive.getCaisse().getId());
            fraisCaisseRequest.setDateOperation(LocalDateTime.now());
            fraisCaisseRequest.setTypeOperation(TypeOperationCaisse.ENTREE);
            fraisCaisseRequest.setCategorieOperation(CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE);
            fraisCaisseRequest.setMontant(fraisRetrait);
            fraisCaisseRequest.setDevise("CDF");
            fraisCaisseRequest.setMembreId(demande.getMembre().getId());
            fraisCaisseRequest.setDescription("Frais retrait épargne demande " + referenceRetrait
                    + " - Membre: " + demande.getMembre().getNom());
            fraisCaisseRequest.setModePaiement(ModePaiement.ESPECES);
            fraisCaisseRequest.setObservation(demande.getObservation());
            fraisCaisseRequest.setCommentaire(buildDecaissementRetraitComment(
                    demande,
                    sessionActive,
                    currentUser,
                    fraisRetrait,
                    "FRAIS_RETRAIT_EPARGNE"
            ));
            fraisCaisseRequest.setCreatedBy(currentUser.getId());
            fraisCaisseRequest.setUtilisateurId(currentUser.getId());
            fraisCaisseRequest.setSource(SourceOperationCaisse.RETRAIT_EPARGNE);
            fraisCaisseRequest.setReferenceExterne(referenceRetrait);
            fraisCaisseRequest.setReferenceMetier(referenceRetrait + "-FRAIS");
            fraisCaisseRequest.setRetraitEpargneId(demande.getId());

            operationCaisseFrais = operationCaisseService.enregistrer(fraisCaisseRequest);
            log.info("✓ OperationCaisse frais créée: id={}, type=ENTREE, montant={}, categorie=FRAIS_RETRAIT_EPARGNE",
                    operationCaisseFrais.getId(), operationCaisseFrais.getMontant());
        }

        // ========== UPDATE 3: Compte Épargne ==========
        // Réduit le soldeDisponible
        CompteEpargne compte = demande.getCompteEpargne();
        BigDecimal nouveauSolde = compte.getSoldeDisponible()
                .subtract(montantTotalDebite);
        compte.setSoldeDisponible(nouveauSolde);
        compteEpargneRepository.save(compte);
        log.info("✓ Compte épargne mis à jour: nouveau soldeDisponible={}", nouveauSolde);

        // ========== UPDATE 4: Demande Retrait ==========
        // Passe le statut à DECAISSEE
        demande.setStatut(StatutDemandeRetrait.DECAISSEE);
        demande = demandeRetraitEpargneRepository.save(demande);
        log.info("✓ Demande retrait décaissée: id={}, statut=DECAISSEE", demandeId);

        Long siteId = null;
        String siteLibelle = null;
        if (sessionActive.getCaisse() != null && sessionActive.getCaisse().getSite() != null) {
            siteId = sessionActive.getCaisse().getSite().getId();
            siteLibelle = sessionActive.getCaisse().getSite().getNomSite();
        }

        workflowTaskService.onRetraitPaye(
                demande.getId(),
                buildRetraitReferenceMetier(demande),
                resolveAntenneIdOrFallback(demande, sessionActive),
                resolveSiteIdOrFallback(demande, sessionActive)
        );

        auditService.logAction(
                AuditAction.DEMANDE_RETRAIT_EPARGNE_DISBURSED,
                AuditModule.RETRAIT_EPARGNE,
                "DemandeRetraitEpargne",
                demande.getId(),
                true,
                AuditSeverity.INFO,
                "Decaissement retrait epargne montant=" + demande.getMontantDemande()
                        + " tauxCommission=" + demande.getTauxCommissionRetrait()
                        + " fraisRetrait=" + fraisRetrait
                        + " montantTotalDebite=" + montantTotalDebite
                        + " operationEpargne=" + operationEpargne.getId()
                        + " operationCaisse=" + operationCaisse.getId()
                        + " operationCaisseFrais=" + (operationCaisseFrais != null ? operationCaisseFrais.getId() : null),
                referenceRetrait,
                null,
                null,
                null,
                sessionActive.getCaisse() != null ? sessionActive.getCaisse().getId() : null,
                sessionActive.getId(),
                siteId,
                siteLibelle
        );

        ticketRecuService.genererDepuisOperation(TicketRecuGenerationRequest.builder()
                .typeTicket(TypeTicketRecu.RETRAIT_EPARGNE)
                .operationEpargneId(operationEpargne.getId())
                .operationCaisseId(operationCaisse.getId())
                .operationCaisseCommissionId(operationCaisseFrais != null ? operationCaisseFrais.getId() : null)
                .demandeRetraitEpargneId(demande.getId())
                .sessionCaisseId(sessionActive.getId())
                .caisseId(sessionActive.getCaisse() != null ? sessionActive.getCaisse().getId() : null)
                .membreId(demande.getMembre().getId())
                .compteEpargneId(compte.getId())
                .utilisateurCreateurId(currentUser.getId())
                .devise("CDF")
                .montantPrincipal(montantRetrait)
                .tauxCommission(demande.getTauxCommissionRetrait())
                .montantCommission(fraisRetrait)
                .montantTotalDebite(montantTotalDebite)
                .montantRemisMembre(montantRetrait)
                .ancienSolde(ancienSolde)
                .nouveauSolde(nouveauSolde)
                .commentaire("Ticket retrait épargne payé")
                .build());

        log.info("Retrait décaissement COMPLET: demande={}, montant={}, " +
                "operationEpargne={}, operationCaisse={}", 
                demandeId, demande.getMontantDemande(), 
                operationEpargne.getId(), operationCaisse.getId());

        return toDTOWithOperations(demande);
    }

        private String buildDecaissementRetraitComment(
                        DemandeRetraitEpargne demande,
                        SessionCaisse sessionActive,
                        Utilisateur currentUser,
                        BigDecimal montant,
                        String action
        ) {
                String role = currentUser.getRole() != null && currentUser.getRole().getCode() != null
                                ? currentUser.getRole().getCode().name()
                                : "-";
                Long antenneId = resolveAntenneIdOrFallback(demande, sessionActive);
                Long siteId = resolveSiteIdOrFallback(demande, sessionActive);
                Long caisseId = sessionActive.getCaisse() != null ? sessionActive.getCaisse().getId() : null;
                String commentaire = "Decaissement retrait epargne"
                                + " | action=" + action
                                + " | role=" + role
                                + " | utilisateurId=" + currentUser.getId()
                                + " | antenneId=" + antenneId
                                + " | siteId=" + siteId
                                + " | caisseId=" + caisseId
                                + " | session=" + sessionActive.getId()
                                + " | retraitId=" + demande.getId()
                                + " | reference=" + buildRetraitReferenceMetier(demande)
                                + " | montant=" + montant;
                commentaire += " | montantRetrait=" + demande.getMontantDemande()
                                + " | tauxCommission=" + demande.getTauxCommissionRetrait()
                                + " | commission=" + safeAmount(demande.getFraisRetrait())
                                + " | totalDebite=" + resolveMontantTotalDebite(demande);
                if (demande.getObservation() != null && !demande.getObservation().isBlank()) {
                        commentaire += " | observation=" + demande.getObservation().trim();
                }
                return commentaire;
        }

    /**
     * Annule une demande de retrait
     */
    @Override
    public DemandeRetraitEpargneDTO annulerDemande(Long demandeId) {
        log.info("Annulation demande retrait: id={}", demandeId);

        DemandeRetraitEpargne demande = demandeRetraitEpargneRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande retrait non trouvée: " + demandeId));

        if (demande.getStatut() == StatutDemandeRetrait.DECAISSEE) {
            throw new BusinessException("Impossible d'annuler un retrait décaissé");
        }

        demande.setStatut(StatutDemandeRetrait.ANNULEE);
        demande = demandeRetraitEpargneRepository.save(demande);

                return toDTOWithOperations(demande);
    }

        private DemandeRetraitEpargneDTO toDTOWithOperations(DemandeRetraitEpargne demande) {
                DemandeRetraitEpargneDTO dto = demandeRetraitEpargneMapper.toDTO(demande);
                if (dto == null || demande == null || demande.getId() == null) {
                        return dto;
                }

                operationCaisseRepository.findByRetraitEpargneIdOrderByDateOperationDesc(demande.getId()).forEach(operation -> {
                        if (operation.getCategorieOperation() == CategorieOperationCaisse.RETRAIT_EPARGNE
                                        && dto.getOperationCaisseSortieId() == null) {
                                dto.setOperationCaisseSortieId(operation.getId());
                        }
                        if (operation.getCategorieOperation() == CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE
                                        && dto.getOperationCaisseFraisId() == null) {
                                dto.setOperationCaisseFraisId(operation.getId());
                        }
                });

                return dto;
        }

        private BigDecimal safeAmount(BigDecimal value) {
                return value == null ? BigDecimal.ZERO : value;
        }

        private BigDecimal resolveMontantTotalDebite(DemandeRetraitEpargne demande) {
                if (demande.getMontantTotalDebite() != null && demande.getMontantTotalDebite().compareTo(BigDecimal.ZERO) > 0) {
                        return demande.getMontantTotalDebite();
                }
                return safeAmount(demande.getMontantDemande()).add(safeAmount(demande.getFraisRetrait()));
        }

    /**
     * Récupère l'utilisateur actuellement authentifié
     */
    private Utilisateur getCurrentUtilisateur() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Utilisateur) {
            return (Utilisateur) authentication.getPrincipal();
        }
        return null;
    }

        private String buildRetraitReferenceMetier(DemandeRetraitEpargne demande) {
                if (demande == null || demande.getId() == null) {
                        return null;
                }
                if (demande.getReferenceRetrait() != null && !demande.getReferenceRetrait().isBlank()) {
                        return demande.getReferenceRetrait();
                }
                return generateRetraitReference(demande);
        }

        private String generateRetraitReference(DemandeRetraitEpargne demande) {
                LocalDateTime referenceDate = demande.getDateDemande() != null ? demande.getDateDemande() : LocalDateTime.now();
                return String.format("RET-%d-%04d", referenceDate.getYear(), demande.getId());
        }

        private Long resolveAntenneId(DemandeRetraitEpargne demande) {
                if (demande == null || demande.getMembre() == null || demande.getMembre().getSite() == null
                                || demande.getMembre().getSite().getAgence() == null) {
                        return null;
                }
                return demande.getMembre().getSite().getAgence().getId();
        }

        private Long resolveSiteId(DemandeRetraitEpargne demande) {
                if (demande == null || demande.getMembre() == null || demande.getMembre().getSite() == null) {
                        return null;
                }
                return demande.getMembre().getSite().getId();
        }

        private Long resolveAntenneIdOrFallback(DemandeRetraitEpargne demande, SessionCaisse sessionActive) {
                Long fromDemande = resolveAntenneId(demande);
                if (fromDemande != null) {
                        return fromDemande;
                }
                if (sessionActive == null || sessionActive.getCaisse() == null || sessionActive.getCaisse().getSite() == null
                                || sessionActive.getCaisse().getSite().getAgence() == null) {
                        return null;
                }
                return sessionActive.getCaisse().getSite().getAgence().getId();
        }

        private Long resolveSiteIdOrFallback(DemandeRetraitEpargne demande, SessionCaisse sessionActive) {
                Long fromDemande = resolveSiteId(demande);
                if (fromDemande != null) {
                        return fromDemande;
                }
                if (sessionActive == null || sessionActive.getCaisse() == null || sessionActive.getCaisse().getSite() == null) {
                        return null;
                }
                return sessionActive.getCaisse().getSite().getId();
        }
}
