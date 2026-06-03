package com.mini.credit.service.impl;

import com.mini.credit.dto.epargne.DemandeRetraitEpargneDTO;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.StatutDemandeRetrait;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.DemandeRetraitEpargneMapper;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.service.DemandeRetraitEpargneService;
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
    private final DemandeRetraitEpargneMapper demandeRetraitEpargneMapper;

    /**
     * Crée une nouvelle demande de retrait épargne
     */
    @Override
    public DemandeRetraitEpargneDTO creerDemande(Long compteEpargneId, BigDecimal montant, String observation) {
        log.info("Création demande retrait épargne: compteId={}, montant={}", compteEpargneId, montant);

        // Validations
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de retrait doit être > 0");
        }

        // Récupère le compte
        CompteEpargne compte = compteEpargneRepository.findById(compteEpargneId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte épargne non trouvé: " + compteEpargneId));

        // Crée la demande
        DemandeRetraitEpargne demande = DemandeRetraitEpargne.builder()
                .compteEpargne(compte)
                .membre(compte.getMembre())
                .montantDemande(montant)
                .statut(StatutDemandeRetrait.CREEE)
                .dateDemande(LocalDateTime.now())
                .observation(observation)
                .build();

        demande = demandeRetraitEpargneRepository.save(demande);
        log.info("Demande retrait créée: id={}, statut={}", demande.getId(), demande.getStatut());

        return demandeRetraitEpargneMapper.toDTO(demande);
    }

    /**
     * Récupère une demande par ID
     */
    @Override
    public DemandeRetraitEpargneDTO getById(Long id) {
        DemandeRetraitEpargne demande = demandeRetraitEpargneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande retrait non trouvée: " + id));

        return demandeRetraitEpargneMapper.toDTO(demande);
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
                .map(demandeRetraitEpargneMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les demandes en attente de validation CONTROLEUR
     */
    @Override
    public List<DemandeRetraitEpargneDTO> getEnAttenteValidation() {
        List<DemandeRetraitEpargne> demandes = demandeRetraitEpargneRepository
                .findByStatutOrderByDateDemandeAsc(StatutDemandeRetrait.EN_ATTENTE_VALIDATION);

        return demandes.stream()
                .map(demandeRetraitEpargneMapper::toDTO)
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
                    demande.getMontantDemande());

            demande.setStatut(StatutDemandeRetrait.REJETEE);
            demande.setMotifRejet("Solde insuffisant: " +
                    demande.getCompteEpargne().getSoldeDisponible() + " < " + demande.getMontantDemande());
            demande.setValidePar(getCurrentUtilisateur());
            demande.setDateValidation(LocalDateTime.now());

            demande = demandeRetraitEpargneRepository.save(demande);
            log.info("Demande rejetée: id={}", demandeId);

            return demandeRetraitEpargneMapper.toDTO(demande);
        }

        // Valide la demande
        demande.setStatut(StatutDemandeRetrait.VALIDEE);
        demande.setValidePar(getCurrentUtilisateur());
        demande.setDateValidation(LocalDateTime.now());

        demande = demandeRetraitEpargneRepository.save(demande);
        log.info("Demande validée: id={}, montant={}", demandeId, demande.getMontantDemande());

        return demandeRetraitEpargneMapper.toDTO(demande);
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

        return demandeRetraitEpargneMapper.toDTO(demande);
    }

    /**
     * PHASE 5: Décaisse un retrait épargne
     * Crée une OperationEpargne type RETRAIT et passe le statut à DECAISSEE
     */
    @Override
    public DemandeRetraitEpargneDTO decaisserRetrait(Long demandeId) {
        log.info("Décaissement retrait épargne: id={}", demandeId);

        DemandeRetraitEpargne demande = demandeRetraitEpargneRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande retrait non trouvée: " + demandeId));

        // Vérifie que la demande peut être décaissée
        if (!demande.canBeDisbursed()) {
            throw new BusinessException("Demande non validée: statut=" + demande.getStatut());
        }

        // Crée l'opération épargne de type RETRAIT
        OperationEpargne operation = OperationEpargne.builder()
                .compteEpargne(demande.getCompteEpargne())
                .membre(demande.getMembre())
                .dateOperation(LocalDateTime.now())
                .typeOperation(TypeOperationEpargne.RETRAIT)
                .montant(demande.getMontantDemande())
                .sens(SensOperation.SORTIE)
                .modePaiement(ModePaiement.ESPECES)
                .observation("Retrait demande #" + demande.getId())
                .createdBy(getCurrentUtilisateur())
                .build();

        operation = operationEpargneRepository.save(operation);
        log.info("OperationEpargne créée: id={}, type=RETRAIT, montant={}", 
                operation.getId(), operation.getMontant());

        // Met à jour le compte: réduit soldeDisponible
        CompteEpargne compte = demande.getCompteEpargne();
        compte.setSoldeDisponible(
                compte.getSoldeDisponible().subtract(demande.getMontantDemande())
        );
        compteEpargneRepository.save(compte);
        log.info("Compte épargne mis à jour: nouveau solde={}", compte.getSoldeDisponible());

        // Met à jour la demande
        demande.setStatut(StatutDemandeRetrait.DECAISSEE);
        demande = demandeRetraitEpargneRepository.save(demande);

        log.info("Retrait décaissé: id={}", demandeId);

        return demandeRetraitEpargneMapper.toDTO(demande);
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

        return demandeRetraitEpargneMapper.toDTO(demande);
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
}
