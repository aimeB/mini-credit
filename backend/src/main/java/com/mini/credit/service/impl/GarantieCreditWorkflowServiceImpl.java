package com.mini.credit.service.impl;

import com.mini.credit.dto.garantie.AjouterGarantieMaterielleRequest;
import com.mini.credit.dto.garantie.BloquerGarantieEpargneRequest;
import com.mini.credit.dto.garantie.GarantieCreditResponse;
import com.mini.credit.dto.garantie.GarantieMaterielleResponse;
import com.mini.credit.dto.garantie.RejeterGarantieRequest;
import com.mini.credit.dto.garantie.ValiderGarantieRequest;
import com.mini.credit.dto.garantie.VerifierGarantieCreditRequest;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.GarantieCredit;
import com.mini.credit.entity.credit.GarantieMaterielle;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.ParametreMetier;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.StatutGarantieCredit;
import com.mini.credit.enums.StatutGarantieMaterielle;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.GarantieCreditRepository;
import com.mini.credit.repository.credit.GarantieMaterielleRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.GarantieCreditWorkflowService;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.audit.AuditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GarantieCreditWorkflowServiceImpl implements GarantieCreditWorkflowService {

    private static final String DEVISE_PAR_DEFAUT = "CDF";
    private static final BigDecimal TAUX_GARANTIE_3N = BigDecimal.valueOf(20);
    private static final BigDecimal COEFFICIENT_MIN_GAGE_MATERIEL = BigDecimal.valueOf(2);

    private final DemandeCreditRepository demandeCreditRepository;
    private final GarantieCreditRepository garantieCreditRepository;
    private final GarantieMaterielleRepository garantieMaterielleRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final OperationEpargneRepository operationEpargneRepository;
    private final CreditRepository creditRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreMetierService parametreMetierService;
    private final AuditService auditService;

    @Override
    public GarantieCreditResponse getByDemandeCreditId(Long demandeCreditId) {
        GarantieCredit garantie = getOrCreateGarantieCredit(demandeCreditId);
        return toResponse(garantie);
    }

    @Override
    public GarantieCreditResponse verifier(Long demandeCreditId, VerifierGarantieCreditRequest request) {
        GarantieCredit garantie = getOrCreateGarantieCredit(demandeCreditId);
        DemandeCredit demande = garantie.getDemandeCredit();
        CompteEpargne compte = resolveCompteEpargne(garantie, null);

        BigDecimal requise = calculerGarantieRequise(demande.getMontantDemande());
        BigDecimal disponible = normaliser(compte.getSoldeDisponible());
        BigDecimal dejaBloque = resolveMontantDejaBloque(garantie, compte, requise);
        BigDecimal totalMobilisable = dejaBloque.add(disponible);
        BigDecimal manquant = requise.subtract(totalMobilisable).max(BigDecimal.ZERO);

        garantie.setCompteEpargne(compte);
        garantie.setMontantCredit(normaliser(demande.getMontantDemande()));
        garantie.setDevise(normalizeDevise(demande.getDevise()));
        garantie.setMontantGarantieRequis(requise);
        garantie.setMontantGarantieBloque(dejaBloque);
        garantie.setMontantGarantieManquant(manquant);
        garantie.setStatutGarantieEpargne(resolveStatutGarantie(dejaBloque, totalMobilisable, requise));
        garantie.setControlePar(getCurrentUser());
        garantie.setDateControle(LocalDateTime.now());
        garantie.setCommentaireControle(cleanText(request != null ? request.getCommentaire() : null));

        garantie = garantieCreditRepository.save(garantie);
        auditService.logSuccess(AuditAction.GARANTIE_VERIFIED, "GarantieCredit", garantie.getId(),
                "Vérification garantie demandeId=" + demandeCreditId + " | statut=" + garantie.getStatutGarantieEpargne());

        return toResponse(garantie);
    }

    @Override
    public GarantieCreditResponse bloquerEpargne(Long demandeCreditId, BloquerGarantieEpargneRequest request) {
        GarantieCredit garantie = getOrCreateGarantieCredit(demandeCreditId);
        DemandeCredit demande = garantie.getDemandeCredit();
        CompteEpargne compte = resolveCompteEpargne(garantie, request != null ? request.getCompteEpargneId() : null);

        BigDecimal requise = calculerGarantieRequise(demande.getMontantDemande());
        BigDecimal disponible = normaliser(compte.getSoldeDisponible());
        BigDecimal dejaBloque = resolveMontantDejaBloque(garantie, compte, requise);
        BigDecimal montantACompleter = requise.subtract(dejaBloque).max(BigDecimal.ZERO);
        if (disponible.compareTo(montantACompleter) < 0) {
            BigDecimal manquant = montantACompleter.subtract(disponible).max(BigDecimal.ZERO);
            garantie.setCompteEpargne(compte);
            garantie.setMontantCredit(normaliser(demande.getMontantDemande()));
            garantie.setDevise(normalizeDevise(demande.getDevise()));
            garantie.setMontantGarantieRequis(requise);
            garantie.setMontantGarantieBloque(dejaBloque);
            garantie.setMontantGarantieManquant(manquant);
            garantie.setStatutGarantieEpargne(StatutGarantieCredit.INSUFFISANTE);
            garantie.setControlePar(getCurrentUser());
            garantie.setDateControle(LocalDateTime.now());
            garantie.setCommentaireControle(cleanText(request != null ? request.getCommentaire() : null));
            garantieCreditRepository.save(garantie);
            throw new BusinessException("Solde disponible insuffisant. Dépôt complémentaire requis: " + manquant + " " + normalizeDevise(demande.getDevise()));
        }

            if (montantACompleter.compareTo(BigDecimal.ZERO) > 0) {
                compte.setSoldeDisponible(disponible.subtract(montantACompleter));
                compte.setSoldeBloque(normaliser(compte.getSoldeBloque()).add(montantACompleter));
                compteEpargneRepository.save(compte);

                OperationEpargne operation = OperationEpargne.builder()
                    .compteEpargne(compte)
                    .membre(demande.getMembre())
                    .demandeCredit(demande)
                    .credit(null)
                    .dateOperation(LocalDateTime.now())
                    .typeOperation(TypeOperationEpargne.BLOCAGE_GARANTIE)
                    .montant(montantACompleter)
                    .sens(SensOperation.SORTIE)
                    .referenceExterne("GARANTIE-" + demande.getNumeroDemande())
                    .observation(cleanText(request != null ? request.getCommentaire() : null))
                    .createdBy(getCurrentUser())
                    .build();
                operationEpargneRepository.save(operation);
            }

        garantie.setCompteEpargne(compte);
        garantie.setMontantCredit(normaliser(demande.getMontantDemande()));
        garantie.setDevise(normalizeDevise(demande.getDevise()));
        garantie.setMontantGarantieRequis(requise);
        garantie.setMontantGarantieBloque(requise);
        garantie.setMontantGarantieManquant(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        garantie.setStatutGarantieEpargne(StatutGarantieCredit.BLOQUEE);
        garantie.setControlePar(getCurrentUser());
        garantie.setDateControle(LocalDateTime.now());
        garantie.setDateBlocage(LocalDateTime.now());
        garantie.setCommentaireControle(cleanText(request != null ? request.getCommentaire() : null));
        garantie = garantieCreditRepository.save(garantie);

        demande.setDepotGarantieRequis(requise);
        demande.setDepotGarantiePaye(requise);
        demandeCreditRepository.save(demande);

        auditService.logSuccess(AuditAction.GARANTIE_BLOCKED, "GarantieCredit", garantie.getId(),
                "Blocage garantie demandeId=" + demandeCreditId + " | montant=" + requise);

        return toResponse(garantie);
    }

    @Override
    public GarantieMaterielleResponse ajouterGarantieMaterielle(Long demandeCreditId, AjouterGarantieMaterielleRequest request) {
        if (request == null) {
            throw new BusinessException("La garantie matérielle est obligatoire");
        }
        GarantieCredit garantie = getOrCreateGarantieCredit(demandeCreditId);

        GarantieMaterielle materielle = GarantieMaterielle.builder()
                .garantieCredit(garantie)
                .typeBien(cleanRequired(request.getTypeBien(), "Le type de bien est obligatoire"))
                .description(cleanRequired(request.getDescription(), "La description est obligatoire"))
                .valeurEstimee(normaliser(request.getValeurEstimee()))
                .devise(normalizeDevise(request.getDevise()))
                .proprietaireDeclare(cleanText(request.getProprietaireDeclare()))
                .localisation(cleanText(request.getLocalisation()))
                .referenceDocument(cleanText(request.getReferenceDocument()))
                .statut(StatutGarantieMaterielle.DECLAREE)
                .build();

        materielle = garantieMaterielleRepository.save(materielle);
        auditService.logSuccess(AuditAction.GARANTIE_MATERIAL_ADDED, "GarantieMaterielle", materielle.getId(),
            "Ajout garantie matérielle demandeId=" + demandeCreditId
                + " | type=" + materielle.getTypeBien()
                + " | valeurEstimee=" + materielle.getValeurEstimee() + " " + materielle.getDevise()
                + " | source=" + ("GAGE_PROPOSE_DEMANDE".equals(materielle.getTypeBien()) ? "gage proposé" : "saisie contrôleur"));
        return toMaterielleResponse(materielle);
    }

    @Override
    public GarantieMaterielleResponse accepterGarantieMaterielle(Long demandeCreditId, Long garantieMaterielleId, String commentaire) {
        GarantieMaterielle materielle = getMaterielleByIdAndDemande(garantieMaterielleId, demandeCreditId);
        materielle.setStatut(StatutGarantieMaterielle.ACCEPTEE);
        materielle.setControlePar(getCurrentUser());
        materielle.setDateControle(LocalDateTime.now());
        materielle.setCommentaire(cleanText(commentaire));
        materielle = garantieMaterielleRepository.save(materielle);
        auditService.logSuccess(AuditAction.GARANTIE_MATERIAL_ACCEPTED, "GarantieMaterielle", materielle.getId(),
            "Garantie matérielle acceptée demandeId=" + demandeCreditId + " | type=" + materielle.getTypeBien());
        return toMaterielleResponse(materielle);
    }

    @Override
    public GarantieMaterielleResponse refuserGarantieMaterielle(Long demandeCreditId, Long garantieMaterielleId, String commentaire) {
        GarantieMaterielle materielle = getMaterielleByIdAndDemande(garantieMaterielleId, demandeCreditId);
        materielle.setStatut(StatutGarantieMaterielle.REFUSEE);
        materielle.setControlePar(getCurrentUser());
        materielle.setDateControle(LocalDateTime.now());
        materielle.setCommentaire(cleanRequired(commentaire, "Le commentaire est obligatoire pour refuser une garantie matérielle"));
        materielle = garantieMaterielleRepository.save(materielle);
        auditService.logSuccess(AuditAction.GARANTIE_MATERIAL_REJECTED, "GarantieMaterielle", materielle.getId(),
            "Garantie matérielle refusée demandeId=" + demandeCreditId + " | type=" + materielle.getTypeBien());
        return toMaterielleResponse(materielle);
    }

    @Override
    public GarantieCreditResponse valider(Long demandeCreditId, ValiderGarantieRequest request) {
        GarantieCredit garantie = getOrCreateGarantieCredit(demandeCreditId);
        verifierGarantieSuffisante(garantie);
        verifierGarantiesMaterielles(garantie);

        garantie.setControlePar(getCurrentUser());
        garantie.setDateControle(LocalDateTime.now());
        garantie.setCommentaireControle(cleanRequired(request.getCommentaire(), "Le commentaire est obligatoire"));
        garantie.setStatutGarantieEpargne(StatutGarantieCredit.VALIDEE);
        garantie = garantieCreditRepository.save(garantie);

        auditService.logSuccess(AuditAction.GARANTIE_VALIDATED, "GarantieCredit", garantie.getId(),
                "Garantie validée demandeId=" + demandeCreditId);

        return toResponse(garantie);
    }

    @Override
    public GarantieCreditResponse rejeter(Long demandeCreditId, RejeterGarantieRequest request) {
        GarantieCredit garantie = getOrCreateGarantieCredit(demandeCreditId);
        garantie.setControlePar(getCurrentUser());
        garantie.setDateControle(LocalDateTime.now());
        garantie.setCommentaireControle(cleanRequired(request.getCommentaire(), "Le commentaire est obligatoire"));
        garantie.setStatutGarantieEpargne(StatutGarantieCredit.INSUFFISANTE);
        garantie = garantieCreditRepository.save(garantie);

        auditService.logSuccess(AuditAction.GARANTIE_REJECTED, "GarantieCredit", garantie.getId(),
                "Garantie rejetée demandeId=" + demandeCreditId);
        return toResponse(garantie);
    }

    @Override
    public List<GarantieMaterielleResponse> getGarantiesMaterielles(Long demandeCreditId) {
        return garantieMaterielleRepository.findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(demandeCreditId)
                .stream()
                .map(this::toMaterielleResponse)
                .toList();
    }

    @Override
    public boolean isGarantieBloquee(Long demandeCreditId) {
        GarantieCredit garantie = garantieCreditRepository.findByDemandeCreditId(demandeCreditId).orElse(null);
        if (garantie == null) {
            return false;
        }

        BigDecimal bloque = normaliser(garantie.getMontantGarantieBloque());
        BigDecimal requis = normaliser(garantie.getMontantGarantieRequis());
        boolean statutCompatible = garantie.getStatutGarantieEpargne() == StatutGarantieCredit.BLOQUEE
                || garantie.getStatutGarantieEpargne() == StatutGarantieCredit.VALIDEE;

        return statutCompatible && bloque.compareTo(requis) >= 0;
    }

    @Override
    public boolean isGarantieValidee(Long demandeCreditId) {
        GarantieCredit garantie = garantieCreditRepository.findByDemandeCreditId(demandeCreditId).orElse(null);
        if (garantie == null) {
            return false;
        }
        return garantie.getStatutGarantieEpargne() == StatutGarantieCredit.VALIDEE;
    }

    @Override
    public void libererGarantie(Long demandeCreditId, String commentaire) {
        GarantieCredit garantie = getOrCreateGarantieCredit(demandeCreditId);
        if (garantie.getCompteEpargne() == null || garantie.getMontantGarantieBloque() == null) {
            return;
        }

        CompteEpargne compte = garantie.getCompteEpargne();
        BigDecimal montant = normaliser(garantie.getMontantGarantieBloque());
        compte.setSoldeBloque(normaliser(compte.getSoldeBloque()).subtract(montant));
        compte.setSoldeDisponible(normaliser(compte.getSoldeDisponible()).add(montant));
        compteEpargneRepository.save(compte);

        OperationEpargne operation = OperationEpargne.builder()
                .compteEpargne(compte)
                .membre(garantie.getMembre())
                .demandeCredit(garantie.getDemandeCredit())
                .dateOperation(LocalDateTime.now())
                .typeOperation(TypeOperationEpargne.DEBLOCAGE_GARANTIE)
                .montant(montant)
                .sens(SensOperation.ENTREE)
                .referenceExterne("LIBERATION-GARANTIE-" + garantie.getDemandeCredit().getNumeroDemande())
                .observation(cleanText(commentaire))
                .createdBy(getCurrentUser())
                .build();
        operationEpargneRepository.save(operation);

        garantie.setMontantGarantieBloque(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        garantie.setMontantGarantieManquant(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        garantie.setStatutGarantieEpargne(StatutGarantieCredit.LIBEREE);
        garantie.setDateLiberation(LocalDateTime.now());
        garantieCreditRepository.save(garantie);
        auditService.logSuccess(AuditAction.GARANTIE_RELEASED, "GarantieCredit", garantie.getId(),
                "Garantie libérée demandeId=" + demandeCreditId);
    }

    private GarantieCredit getOrCreateGarantieCredit(Long demandeCreditId) {
        return garantieCreditRepository.findByDemandeCreditId(demandeCreditId)
                .orElseGet(() -> {
                    DemandeCredit demande = demandeCreditRepository.findById(demandeCreditId)
                            .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

                    GarantieCredit garantie = GarantieCredit.builder()
                            .demandeCredit(demande)
                            .membre(demande.getMembre())
                            .montantCredit(normaliser(demande.getMontantDemande()))
                            .devise(normalizeDevise(demande.getDevise()))
                            .montantGarantieRequis(calculerGarantieRequise(demande.getMontantDemande()))
                            .montantGarantieBloque(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .montantGarantieManquant(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .statutGarantieEpargne(StatutGarantieCredit.NON_VERIFIEE)
                            .build();
                    return garantieCreditRepository.save(garantie);
                });
    }

    private CompteEpargne resolveCompteEpargne(GarantieCredit garantie, Long compteEpargneId) {
        CompteEpargne compte;
        if (compteEpargneId != null) {
            compte = compteEpargneRepository.findById(compteEpargneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable"));
        } else if (garantie.getCompteEpargne() != null && garantie.getCompteEpargne().getId() != null) {
            compte = compteEpargneRepository.findById(garantie.getCompteEpargne().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable"));
        } else {
            compte = compteEpargneRepository.findFirstByMembreIdAndStatut(garantie.getMembre().getId(), com.mini.credit.enums.StatutCompte.ACTIF)
                    .orElseThrow(() -> new BusinessException("Aucun compte épargne actif trouvé pour le membre"));
        }

        if (compte.getMembre() == null || compte.getMembre().getId() == null || !compte.getMembre().getId().equals(garantie.getMembre().getId())) {
            throw new BusinessException("Le compte épargne ne correspond pas au membre du dossier");
        }

        return compte;
    }

    private GarantieMaterielle getMaterielleByIdAndDemande(Long garantieMaterielleId, Long demandeCreditId) {
        GarantieMaterielle materielle = garantieMaterielleRepository.findById(garantieMaterielleId)
                .orElseThrow(() -> new ResourceNotFoundException("Garantie matérielle introuvable"));
        if (materielle.getGarantieCredit() == null
                || materielle.getGarantieCredit().getDemandeCredit() == null
                || !materielle.getGarantieCredit().getDemandeCredit().getId().equals(demandeCreditId)) {
            throw new BusinessException("La garantie matérielle n'est pas rattachée à cette demande");
        }
        return materielle;
    }

    private void verifierGarantieSuffisante(GarantieCredit garantie) {
        if (garantie.getMontantGarantieBloque().compareTo(garantie.getMontantGarantieRequis()) < 0) {
            throw new BusinessException("La garantie épargne est insuffisante");
        }
    }

    private BigDecimal resolveMontantDejaBloque(GarantieCredit garantie, CompteEpargne compte, BigDecimal requise) {
        BigDecimal bloqueGarantie = normaliser(garantie.getMontantGarantieBloque());
        BigDecimal bloqueCompte = normaliser(compte.getSoldeBloque());
        BigDecimal dejaBloque = bloqueGarantie.max(bloqueCompte);
        return dejaBloque.min(requise);
    }

    private StatutGarantieCredit resolveStatutGarantie(BigDecimal dejaBloque, BigDecimal totalMobilisable, BigDecimal requise) {
        if (dejaBloque.compareTo(requise) >= 0) {
            return StatutGarantieCredit.BLOQUEE;
        }
        return totalMobilisable.compareTo(requise) >= 0 ? StatutGarantieCredit.SUFFISANTE : StatutGarantieCredit.INSUFFISANTE;
    }

    private void verifierGarantiesMaterielles(GarantieCredit garantie) {
        boolean obligatoire = Boolean.parseBoolean(parametreMetierService.getTexte("AUTORISER_GARANTIE_MATERIELLE_OBLIGATOIRE"));
        List<GarantieMaterielle> materielles = garantieMaterielleRepository.findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(garantie.getDemandeCredit().getId());
        BigDecimal montantDemande = normaliser(garantie.getDemandeCredit().getMontantDemande());
        BigDecimal minimumRequis = calculerValeurMinimaleGageMateriel(montantDemande);
        BigDecimal valeurAcceptee = calculerValeurGarantiesMateriellesAcceptees(materielles);

        if (obligatoire && materielles.isEmpty()) {
            throw new BusinessException("Au moins une garantie matérielle est requise");
        }

        boolean hasPending = materielles.stream()
                .anyMatch(m -> m.getStatut() == StatutGarantieMaterielle.DECLAREE || m.getStatut() == StatutGarantieMaterielle.CONTROLEE);
        boolean hasRefused = materielles.stream().anyMatch(m -> m.getStatut() == StatutGarantieMaterielle.REFUSEE);

        if (hasPending) {
            throw new BusinessException("Toutes les garanties matérielles doivent être contrôlées avant validation");
        }
        if (hasRefused) {
            throw new BusinessException("Une garantie matérielle refusée empêche la validation globale");
        }
        if (valeurAcceptee.compareTo(minimumRequis) < 0) {
            String message = "La valeur totale des garanties matérielles acceptées doit être au minimum égale à 2 fois le montant demandé. Montant demandé : "
                    + montantDemande + ", minimum requis : " + minimumRequis + ", valeur acceptée : " + valeurAcceptee + ".";
            auditService.logFailure(AuditAction.GARANTIE_VALIDATED, "GarantieCredit", garantie.getId(),
                    "Validation garantie refusée pour gage matériel insuffisant", message);
            throw new BusinessException(message);
        }
    }

    private BigDecimal calculerGarantieRequise(BigDecimal montantDemande) {
        return normaliser(montantDemande).multiply(TAUX_GARANTIE_3N).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerValeurMinimaleGageMateriel(BigDecimal montantDemande) {
        return normaliser(montantDemande).multiply(COEFFICIENT_MIN_GAGE_MATERIEL).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerValeurGarantiesMateriellesAcceptees(List<GarantieMaterielle> materielles) {
        return materielles.stream()
                .filter(m -> m.getStatut() == StatutGarantieMaterielle.ACCEPTEE)
                .map(GarantieMaterielle::getValeurEstimee)
                .map(this::normaliser)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
    }

    private BigDecimal normaliser(BigDecimal montant) {
        return montant == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : montant.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeDevise(String devise) {
        return (devise == null || devise.isBlank()) ? DEVISE_PAR_DEFAUT : devise.trim().toUpperCase();
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur utilisateur)) {
            return null;
        }
        return utilisateurRepository.findById(utilisateur.getId()).orElse(utilisateur);
    }

    private GarantieCreditResponse toResponse(GarantieCredit garantie) {
        List<GarantieMaterielleResponse> materielles = garantieMaterielleRepository.findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(garantie.getDemandeCredit().getId())
                .stream().map(this::toMaterielleResponse).toList();
        DemandeCredit demande = garantie.getDemandeCredit();
        BigDecimal montantDemande = demande != null ? normaliser(demande.getMontantDemande()) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valeurMinimaleGageMateriel = calculerValeurMinimaleGageMateriel(montantDemande);

        BigDecimal totalMateriel = materielles.stream()
                .filter(m -> m.getStatut() == com.mini.credit.enums.StatutGarantieMaterielle.ACCEPTEE || m.getStatut() == com.mini.credit.enums.StatutGarantieMaterielle.CONTROLEE || m.getStatut() == com.mini.credit.enums.StatutGarantieMaterielle.DECLAREE)
                .map(GarantieMaterielleResponse::getValeurEstimee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMaterielAccepte = materielles.stream()
            .filter(m -> m.getStatut() == StatutGarantieMaterielle.ACCEPTEE)
            .map(GarantieMaterielleResponse::getValeurEstimee)
            .map(this::normaliser)
            .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal ratioCouvertureMaterielle = valeurMinimaleGageMateriel.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : totalMaterielAccepte.multiply(BigDecimal.valueOf(100)).divide(valeurMinimaleGageMateriel, 2, RoundingMode.HALF_UP);

        String statutGlobal;
        if (garantie.getStatutGarantieEpargne() == StatutGarantieCredit.VALIDEE) {
            statutGlobal = materielles.isEmpty() || materielles.stream().allMatch(m -> m.getStatut() == com.mini.credit.enums.StatutGarantieMaterielle.ACCEPTEE) ? "VALIDEE" : "EN_ATTENTE";
        } else if (garantie.getStatutGarantieEpargne() == StatutGarantieCredit.INSUFFISANTE) {
            statutGlobal = "INSUFFISANTE";
        } else if (materielles.stream().anyMatch(m -> m.getStatut() == com.mini.credit.enums.StatutGarantieMaterielle.REFUSEE)) {
            statutGlobal = "REFUSEE";
        } else {
            statutGlobal = "EN_ATTENTE";
        }

        return GarantieCreditResponse.builder()
                .id(garantie.getId())
                .demandeCreditId(garantie.getDemandeCredit() != null ? garantie.getDemandeCredit().getId() : null)
                .creditId(garantie.getCredit() != null ? garantie.getCredit().getId() : null)
                .membreId(garantie.getMembre() != null ? garantie.getMembre().getId() : null)
                .compteEpargneId(garantie.getCompteEpargne() != null ? garantie.getCompteEpargne().getId() : null)
                .devise(garantie.getDevise())
                .montantDemande(montantDemande)
                .gagePropose(demande != null ? demande.getGagePropose() : null)
                .montantCredit(garantie.getMontantCredit())
                .montantGarantieRequis(garantie.getMontantGarantieRequis())
                .montantGarantieBloque(garantie.getMontantGarantieBloque())
                .montantGarantieManquant(garantie.getMontantGarantieManquant())
                .statutGarantieEpargne(garantie.getStatutGarantieEpargne())
                .controleParId(garantie.getControlePar() != null ? garantie.getControlePar().getId() : null)
                .controleParNom(garantie.getControlePar() != null ? garantie.getControlePar().getNomComplet() : null)
                .dateControle(garantie.getDateControle())
                .dateBlocage(garantie.getDateBlocage())
                .dateLiberation(garantie.getDateLiberation())
                .commentaireControle(garantie.getCommentaireControle())
                .soldeDisponible(garantie.getCompteEpargne() != null ? garantie.getCompteEpargne().getSoldeDisponible() : null)
                .soldeBloque(garantie.getCompteEpargne() != null ? garantie.getCompteEpargne().getSoldeBloque() : null)
                .montantMaterielTotal(totalMateriel)
                .valeurMinimaleGageMateriel(valeurMinimaleGageMateriel)
                .valeurTotaleGarantiesMateriellesAcceptees(totalMaterielAccepte)
                .ratioCouvertureMaterielle(ratioCouvertureMaterielle)
                .garantieMaterielleSuffisante(totalMaterielAccepte.compareTo(valeurMinimaleGageMateriel) >= 0)
                .statutGlobal(statutGlobal)
                .garantiesMaterielles(materielles)
                .build();
    }

    private GarantieMaterielleResponse toMaterielleResponse(GarantieMaterielle materielle) {
        return GarantieMaterielleResponse.builder()
                .id(materielle.getId())
                .garantieCreditId(materielle.getGarantieCredit() != null ? materielle.getGarantieCredit().getId() : null)
                .typeBien(materielle.getTypeBien())
                .description(materielle.getDescription())
                .valeurEstimee(materielle.getValeurEstimee())
                .devise(materielle.getDevise())
                .proprietaireDeclare(materielle.getProprietaireDeclare())
                .localisation(materielle.getLocalisation())
                .referenceDocument(materielle.getReferenceDocument())
                .statut(materielle.getStatut())
                .controleParId(materielle.getControlePar() != null ? materielle.getControlePar().getId() : null)
                .controleParNom(materielle.getControlePar() != null ? materielle.getControlePar().getNomComplet() : null)
                .dateControle(materielle.getDateControle())
                .commentaire(materielle.getCommentaire())
                .build();
    }
}