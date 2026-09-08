package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest;
import com.mini.credit.dto.credit.PaiementInitialDemandeCreditResponse;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.PaiementInitialDemandeCreditService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.service.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PaiementInitialDemandeCreditServiceImpl implements PaiementInitialDemandeCreditService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<StatutDemandeCredit> STATUTS_FRAIS_DEMANDE_AUTORISES = EnumSet.of(
            StatutDemandeCredit.SOUMISE,
            StatutDemandeCredit.EN_ANALYSE,
            StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE,
            StatutDemandeCredit.VALIDATION_CHEF,
            StatutDemandeCredit.VALIDATION_CONTROLEUR,
            StatutDemandeCredit.APPROUVEE
    );

    private final DemandeCreditRepository demandeCreditRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CreditMapper creditMapper;
    private final CashMapper cashMapper;
    private final OperationCaisseService operationCaisseService;
    private final OperationCaisseRepository operationCaisseRepository;

    @Override
    @Auditable(action = AuditAction.PAIEMENT_INITIAL_DEMANDE_CREATED, entityType = "DemandeCredit", entityIdParameter = "demandeId")
    public PaiementInitialDemandeCreditResponse enregistrerPaiementInitial(
            Long demandeId,
            PaiementInitialDemandeCreditRequest request
    ) {
        Utilisateur currentUser = requireCurrentCaissier();
        validerRequest(request);

        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getMembre() == null) {
            throw new BusinessException("La demande de crédit n'est liée à aucun membre");
        }

        if (demande.getMembre().getStatut() != StatutMembre.ACTIF) {
            throw new BusinessException(
                    "Impossible d'enregistrer un paiement initial pour un membre non actif (statut : "
                            + demande.getMembre().getStatut() + ")"
            );
        }

        SessionCaisse sessionCaisse = resolveSessionOuverteDuCaissier(currentUser);
        Caisse caisse = sessionCaisse.getCaisse();

        validerSessionEtCaisse(sessionCaisse, caisse);

        BigDecimal fraisPayes = normalizeMoney(request.getFraisPayes());
        BigDecimal depotGarantiePaye = normalizeMoney(request.getDepotGarantiePaye());

        validatePaiementInitial(demande, fraisPayes, depotGarantiePaye, request.getModePaiement());

        LocalDateTime datePaiement = LocalDateTime.now();

        demande.setFraisDemandePayes(
                demande.getFraisDemandePayes().add(fraisPayes).setScale(2, RoundingMode.HALF_UP)
        );

        DemandeCredit savedDemande = demandeCreditRepository.save(demande);

        List<OperationCaisseResponse> operationsCaisse = new ArrayList<>();

        // 1) Frais de demande -> caisse directe
        if (fraisPayes.compareTo(ZERO) > 0) {
            OperationCaisseRequest opFraisRequest = new OperationCaisseRequest();
            opFraisRequest.setSessionCaisseId(sessionCaisse.getId());
            opFraisRequest.setCaisseId(caisse.getId());
            opFraisRequest.setDateOperation(datePaiement);
            opFraisRequest.setTypeOperation(TypeOperationCaisse.ENTREE);
            opFraisRequest.setCategorieOperation(CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT);
            opFraisRequest.setMontant(fraisPayes);
            opFraisRequest.setDevise(savedDemande.getDevise() != null ? savedDemande.getDevise() : "CDF");
            opFraisRequest.setMembreId(savedDemande.getMembre().getId());
            opFraisRequest.setModePaiement(request.getModePaiement());
            opFraisRequest.setDescription(buildDescription(
                    "Paiement frais de demande - " + savedDemande.getNumeroDemande(),
                    request.getObservation()
            ));
            opFraisRequest.setObservation(cleanNullableText(request.getObservation()));
            opFraisRequest.setCommentaire(buildAuditComment(savedDemande, sessionCaisse, caisse, currentUser, fraisPayes, request.getObservation()));
            opFraisRequest.setCreatedBy(currentUser.getId());
            opFraisRequest.setUtilisateurId(currentUser.getId());
            opFraisRequest.setSource(SourceOperationCaisse.MANUEL);
            opFraisRequest.setReferenceMetier("DEMANDE_CREDIT:" + savedDemande.getId());
            opFraisRequest.setReferenceExterne(savedDemande.getNumeroDemande());

            OperationCaisseResponse opFraisResponse = operationCaisseService.enregistrer(opFraisRequest);
            operationsCaisse.add(opFraisResponse);
        }

            BigDecimal totalPaye = fraisPayes.setScale(2, RoundingMode.HALF_UP);

        BigDecimal fraisRestants = savedDemande.getFraisDemande()
                .subtract(savedDemande.getFraisDemandePayes())
                .max(ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal depotRestant = savedDemande.getDepotGarantieRequis()
                .subtract(savedDemande.getDepotGarantiePaye())
                .max(ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return PaiementInitialDemandeCreditResponse.builder()
                .demandeId(savedDemande.getId())
                .numeroDemande(savedDemande.getNumeroDemande())
                .membreId(savedDemande.getMembre().getId())
                .membreNomComplet(savedDemande.getMembre().getNomComplet())
                .fraisPayesSurCetteOperation(fraisPayes)
                .depotGarantiePayeSurCetteOperation(depotGarantiePaye)
                .totalPayeSurCetteOperation(totalPaye)
                .fraisDemandeTotal(savedDemande.getFraisDemande())
                .fraisDemandePayesTotal(savedDemande.getFraisDemandePayes())
                .fraisDemandeRestants(fraisRestants)
                .depotGarantieRequis(savedDemande.getDepotGarantieRequis())
                .depotGarantiePayeTotal(savedDemande.getDepotGarantiePaye())
                .depotGarantieRestant(depotRestant)
                .devise(savedDemande.getDevise())
                .demande(creditMapper.toResponse(savedDemande))
                .operationsCaisse(operationsCaisse)
                .build();
    }

    private void validerRequest(PaiementInitialDemandeCreditRequest request) {
        if (request == null) {
            throw new BusinessException("La requête de paiement initial est obligatoire");
        }
    }

    private void validerSessionEtCaisse(SessionCaisse sessionCaisse, Caisse caisse) {
        if (sessionCaisse.getStatut() == null) {
            throw new BusinessException("Le statut de la session de caisse est invalide");
        }

        if (sessionCaisse.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("La session de caisse doit être ouverte");
        }

        if (sessionCaisse.getDateCloture() != null) {
            throw new BusinessException("La session de caisse est déjà clôturée");
        }

        if (sessionCaisse.getCaisse() == null) {
            throw new BusinessException("La caisse liée à la session est introuvable");
        }

        if (!sessionCaisse.getCaisse().getId().equals(caisse.getId())) {
            throw new BusinessException("Session et caisse incohérentes");
        }

        if (Boolean.FALSE.equals(caisse.getActif())) {
            throw new BusinessException("La caisse est inactive");
        }
    }

    private void validatePaiementInitial(
            DemandeCredit demande,
            BigDecimal fraisPayes,
            BigDecimal depotGarantiePaye,
            ModePaiement modePaiement
    ) {
        if (modePaiement == null) {
            throw new BusinessException("Le mode de paiement est obligatoire");
        }

        if (fraisPayes.compareTo(ZERO) < 0) {
            throw new BusinessException("Les frais payés ne peuvent pas être négatifs");
        }

        if (fraisPayes.compareTo(ZERO) <= 0) {
            throw new BusinessException("Le montant des frais à encaisser doit être supérieur à zéro");
        }

        if (depotGarantiePaye.compareTo(ZERO) != 0) {
            throw new BusinessException("Le dépôt de garantie n'est pas concerné par cet écran");
        }

        if (!STATUTS_FRAIS_DEMANDE_AUTORISES.contains(demande.getStatut())) {
            throw new BusinessException("Frais de demande payables uniquement aux statuts SOUMISE, EN_ANALYSE, ANALYSE_TERRAIN_VALIDEE, VALIDATION_CHEF, VALIDATION_CONTROLEUR ou APPROUVEE");
        }

        BigDecimal fraisRestantsAvant = demande.getFraisDemande()
                .subtract(demande.getFraisDemandePayes())
                .setScale(2, RoundingMode.HALF_UP);

        if (fraisPayes.compareTo(fraisRestantsAvant) > 0) {
            throw new BusinessException("Le montant payé pour les frais dépasse le reste à payer");
        }
    }

    private Utilisateur requireCurrentCaissier() {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null || currentUser.getRole() == null
                || currentUser.getRole().getCode() != RoleCode.CAISSIER) {
            throw new BusinessException("Accès refusé: rôle CAISSIER requis pour le paiement");
        }

        return currentUser;
    }

    private SessionCaisse resolveSessionOuverteDuCaissier(Utilisateur caissier) {
        List<SessionCaisse> sessions = sessionCaisseRepository
                .findByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(
                        caissier.getId(),
                        LocalDate.now(),
                        StatutSessionCaisse.OUVERTE
                );

        if (sessions.isEmpty()) {
            throw new BusinessException("Aucune session caisse ouverte pour ce Caissier aujourd’hui. Veuillez ouvrir une session caisse avant d’encaisser les frais.");
        }

        if (sessions.size() > 1) {
            throw new BusinessException("Plusieurs sessions caisse ouvertes existent pour ce Caissier aujourd’hui. Veuillez régulariser la session active avant d’encaisser les frais.");
        }

        return sessions.get(0);
    }

    private String buildAuditComment(DemandeCredit demande, SessionCaisse sessionCaisse, Caisse caisse, Utilisateur currentUser, BigDecimal montant, String observation) {
        BigDecimal resteApresPaiement = demande.getFraisDemande()
                .subtract(demande.getFraisDemandePayes())
                .max(ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        String antenneNom = caisse.getAgence() != null ? caisse.getAgence().getNomAgence() : null;

        return buildDescription(
                "Encaissement frais demande crédit | role=" + currentUser.getRole().getCode()
                        + " | antenne=" + (antenneNom != null ? antenneNom : "-")
                        + " | caisse=" + caisse.getCodeCaisse()
                        + " | session=" + sessionCaisse.getId()
                        + " | demandeCreditId=" + demande.getId()
                        + " | montant=" + montant
                        + " | resteApresPaiement=" + resteApresPaiement,
                observation
        );
    }

    private String buildDescription(String libelleBase, String observation) {
        if (observation == null || observation.isBlank()) {
            return libelleBase;
        }
        return libelleBase + " | Observation: " + observation.trim();
    }

    private String cleanNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? ZERO
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}