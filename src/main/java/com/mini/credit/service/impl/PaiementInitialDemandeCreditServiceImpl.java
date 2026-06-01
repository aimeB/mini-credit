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
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.OperationEpargneService;
import com.mini.credit.service.PaiementInitialDemandeCreditService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaiementInitialDemandeCreditServiceImpl implements PaiementInitialDemandeCreditService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final DemandeCreditRepository demandeCreditRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CreditMapper creditMapper;
    private final CashMapper cashMapper;
    private final OperationEpargneService operationEpargneService;
    private final CompteEpargneRepository compteEpargneRepository;
    private final OperationCaisseService operationCaisseService;
    private final OperationCaisseRepository operationCaisseRepository;

    @Override
    @Auditable(action = AuditAction.PAIEMENT_INITIAL_DEMANDE_CREATED, entityType = "DemandeCredit", entityIdParameter = "demandeId")
    public PaiementInitialDemandeCreditResponse enregistrerPaiementInitial(
            Long demandeId,
            PaiementInitialDemandeCreditRequest request
    ) {
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

        SessionCaisse sessionCaisse = sessionCaisseRepository.findById(request.getSessionCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        validerSessionEtCaisse(sessionCaisse, caisse);

        AgentTerrain agent = request.getAgentId() != null
                ? agentTerrainRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"))
                : null;

        Utilisateur createdBy = request.getCreatedById() != null
                ? utilisateurRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"))
                : null;

        BigDecimal fraisPayes = normalizeMoney(request.getFraisPayes());
        BigDecimal depotGarantiePaye = normalizeMoney(request.getDepotGarantiePaye());

        validatePaiementInitial(demande, fraisPayes, depotGarantiePaye, request.getModePaiement());

        LocalDateTime datePaiement = request.getDatePaiement();

        demande.setFraisDemandePayes(
                demande.getFraisDemandePayes().add(fraisPayes).setScale(2, RoundingMode.HALF_UP)
        );
        demande.setDepotGarantiePaye(
                demande.getDepotGarantiePaye().add(depotGarantiePaye).setScale(2, RoundingMode.HALF_UP)
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
            opFraisRequest.setAgentId(request.getAgentId());
            opFraisRequest.setModePaiement(request.getModePaiement());
            opFraisRequest.setDescription(buildDescription(
                    "Paiement frais de demande - " + savedDemande.getNumeroDemande(),
                    request.getObservation()
            ));
            opFraisRequest.setObservation(cleanNullableText(request.getObservation()));
            opFraisRequest.setCreatedBy(request.getCreatedById());

            OperationCaisseResponse opFraisResponse = operationCaisseService.enregistrer(opFraisRequest);
            operationsCaisse.add(opFraisResponse);
        }

        // 2) Dépôt de garantie -> cotisation puis blocage
        if (depotGarantiePaye.compareTo(ZERO) > 0) {
            CompteEpargne compte = compteEpargneRepository
                    .findByMembreId(savedDemande.getMembre().getId())
                    .stream()
                    .filter(c -> c.getTypeCompte() == TypeCompteEpargne.COTISATION
                            || c.getTypeCompte() == TypeCompteEpargne.MIXTE)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Aucun compte épargne de type COTISATION ou MIXTE n'a été trouvé pour recevoir la garantie"
                    ));

            OperationEpargneRequest depot = new OperationEpargneRequest();
            depot.setCompteEpargneId(compte.getId());
            depot.setMembreId(savedDemande.getMembre().getId());
            depot.setDateOperation(datePaiement);
            depot.setTypeOperation(TypeOperationEpargne.COTISATION);
            depot.setMontant(depotGarantiePaye);
            depot.setModePaiement(request.getModePaiement());
            depot.setSessionCaisseId(sessionCaisse.getId());
            depot.setAgentId(request.getAgentId());
            depot.setCreatedBy(request.getCreatedById());
            depot.setObservation("Dépôt de garantie crédit - " + savedDemande.getNumeroDemande());

            OperationEpargneResponse depotResponse = operationEpargneService.enregistrer(depot);

            List<OperationCaisse> operationsDepot = operationCaisseRepository
                    .findByOperationEpargneIdOrderByDateOperationDesc(depotResponse.getId());

            if (operationsDepot.isEmpty()) {
                throw new BusinessException("L'opération de caisse générée pour le dépôt de garantie est introuvable");
            }

            operationsCaisse.add(cashMapper.toResponse(operationsDepot.get(0)));

            OperationEpargneRequest blocage = new OperationEpargneRequest();
            blocage.setCompteEpargneId(compte.getId());
            blocage.setMembreId(savedDemande.getMembre().getId());
            blocage.setDateOperation(datePaiement);
            blocage.setTypeOperation(TypeOperationEpargne.BLOCAGE_GARANTIE);
            blocage.setMontant(depotGarantiePaye);
            blocage.setCreatedBy(request.getCreatedById());
            blocage.setObservation("Blocage garantie crédit - " + savedDemande.getNumeroDemande());

            operationEpargneService.enregistrer(blocage);
        }

        BigDecimal totalPaye = fraisPayes.add(depotGarantiePaye).setScale(2, RoundingMode.HALF_UP);

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

        if (request.getSessionCaisseId() == null) {
            throw new BusinessException("La session de caisse est obligatoire");
        }

        if (request.getCaisseId() == null) {
            throw new BusinessException("La caisse est obligatoire");
        }

        if (request.getDatePaiement() == null) {
            throw new BusinessException("La date du paiement est obligatoire");
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

        if (depotGarantiePaye.compareTo(ZERO) < 0) {
            throw new BusinessException("Le dépôt de garantie payé ne peut pas être négatif");
        }

        if (fraisPayes.add(depotGarantiePaye).compareTo(ZERO) <= 0) {
            throw new BusinessException("Vous devez payer au moins un montant");
        }

        BigDecimal fraisRestantsAvant = demande.getFraisDemande()
                .subtract(demande.getFraisDemandePayes())
                .setScale(2, RoundingMode.HALF_UP);

        if (fraisPayes.compareTo(fraisRestantsAvant) > 0) {
            throw new BusinessException("Le montant payé pour les frais dépasse le reste à payer");
        }

        BigDecimal depotRestantAvant = demande.getDepotGarantieRequis()
                .subtract(demande.getDepotGarantiePaye())
                .setScale(2, RoundingMode.HALF_UP);

        if (depotGarantiePaye.compareTo(depotRestantAvant) > 0) {
            throw new BusinessException("Le montant payé pour le dépôt de garantie dépasse le reste à payer");
        }
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