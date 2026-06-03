package com.mini.credit.service.impl;

import com.mini.credit.dto.employe.CommissionDTO;
import com.mini.credit.entity.employe.Commission;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutCommission;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CommissionMapper;
import com.mini.credit.repository.employe.CommissionRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.CommissionService;
import com.mini.credit.service.ParametreMetierService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 8: Implémentation service commissions agents.
 *
 * Logique:
 * 1. Calcul commission: totalRecettes * tauxCommission (TAUX_COMMISSION_AGENT)
 * 2. Création commission (CREEE)
 * 3. Validation (VALIDEE)
 * 4. Paiement (PAYEE)
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRepository commissionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreMetierService parametreMetierService;
    private final CommissionMapper commissionMapper;

    /**
     * PHASE 8: Crée une commission pour un agent
     * Calcule montant = totalRecettes * TAUX_COMMISSION_AGENT
     */
    @Override
    public CommissionDTO creerCommission(
            Long agentId, LocalDate dateDebut, LocalDate dateFin,
            BigDecimal totalRecettes, Long nbRecettes) {

        log.info("Création commission: agent={}, période={} à {}, totalRecettes={}, nbRecettes={}",
                agentId, dateDebut, dateFin, totalRecettes, nbRecettes);

        // Validations
        if (totalRecettes == null || totalRecettes.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Total recettes doit être >= 0");
        }

        if (nbRecettes == null || nbRecettes < 0) {
            throw new BusinessException("Nombre de recettes doit être >= 0");
        }

        // Récupère l'agent
        Utilisateur agent = utilisateurRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent non trouvé: " + agentId));

        // Vérif unicité: une seule commission par période
        if (commissionRepository.findByAgentIdAndDatePeriodeDebutAndDatePeriodeFin(
                agentId, dateDebut, dateFin).isPresent()) {
            throw new BusinessException("Commission déjà existante pour cette période");
        }

        // Récupère taux commission (TAUX_COMMISSION_AGENT = 2.5% = 0.025)
        BigDecimal tauxCommission = parametreMetierService.getDecimal("TAUX_COMMISSION_AGENT");
        if (tauxCommission == null) {
            tauxCommission = new BigDecimal("0.025"); // Default 2.5%
        }

        // Calcule montant commission
        BigDecimal montantCommission = totalRecettes.multiply(tauxCommission);

        // Crée la commission
        Commission commission = Commission.builder()
                .agent(agent)
                .datePeriodeDebut(dateDebut)
                .datePeriodeFin(dateFin)
                .totalRecettes(totalRecettes)
                .tauxCommission(tauxCommission)
                .montantCommission(montantCommission)
                .nbRecettes(nbRecettes)
                .statut(StatutCommission.CREEE)
                .build();

        commission = commissionRepository.save(commission);
        log.info("Commission créée: id={}, montant={}, statut={}", commission.getId(), commission.getMontantCommission(), commission.getStatut());

        return commissionMapper.toDTO(commission);
    }

    /**
     * Récupère une commission par ID
     */
    @Override
    public CommissionDTO getById(Long id) {
        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commission non trouvée: " + id));

        return commissionMapper.toDTO(commission);
    }

    /**
     * Récupère les commissions d'un agent
     */
    @Override
    public List<CommissionDTO> getByAgent(Long agentId) {
        List<Commission> commissions = commissionRepository.findByAgentIdOrderByDatePeriodeDebutDesc(agentId);

        return commissions.stream()
                .map(commissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les commissions d'une période
     */
    @Override
    public List<CommissionDTO> getByPeriode(LocalDate dateDebut, LocalDate dateFin) {
        List<Commission> commissions = commissionRepository
                .findByDatePeriodeDebutAndDatePeriodeFinOrderByAgentIdAsc(dateDebut, dateFin);

        return commissions.stream()
                .map(commissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les commissions en attente de validation
     */
    @Override
    public List<CommissionDTO> getEnAttenteValidation() {
        List<Commission> commissions = commissionRepository
                .findByStatutOrderByDateCreationAsc(StatutCommission.CREEE);

        return commissions.stream()
                .map(commissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les commissions validées en attente de paiement
     */
    @Override
    public List<CommissionDTO> getEnAttentePaiement() {
        List<Commission> commissions = commissionRepository
                .findByStatutOrderByDateCreationAsc(StatutCommission.VALIDEE);

        return commissions.stream()
                .map(commissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * PHASE 8: Valide une commission
     * Passe en VALIDEE
     */
    @Override
    public CommissionDTO validerCommission(Long commissionId) {
        log.info("Validation commission: id={}", commissionId);

        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission non trouvée: " + commissionId));

        if (!commission.canBeValidated()) {
            throw new BusinessException("Commission ne peut pas être validée: statut=" + commission.getStatut());
        }

        commission.setStatut(StatutCommission.VALIDEE);
        commission.setValideePar(getCurrentUtilisateur());
        commission.setDateValidation(LocalDateTime.now());

        commission = commissionRepository.save(commission);
        log.info("Commission validée: id={}", commissionId);

        return commissionMapper.toDTO(commission);
    }

    /**
     * PHASE 8: Paie une commission
     * Passe en PAYEE
     */
    @Override
    public CommissionDTO payerCommission(Long commissionId) {
        log.info("Paiement commission: id={}", commissionId);

        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission non trouvée: " + commissionId));

        if (!commission.canBePaid()) {
            throw new BusinessException("Commission ne peut pas être payée: statut=" + commission.getStatut());
        }

        commission.setStatut(StatutCommission.PAYEE);
        commission.setPayeeA(commission.getAgent()); // Paie à l'agent
        commission.setDatePaiement(LocalDateTime.now());

        commission = commissionRepository.save(commission);
        log.info("Commission payée: id={}, montant={}", commissionId, commission.getMontantCommission());

        return commissionMapper.toDTO(commission);
    }

    /**
     * Annule une commission
     */
    @Override
    public CommissionDTO annulerCommission(Long commissionId, String raison) {
        log.info("Annulation commission: id={}, raison={}", commissionId, raison);

        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission non trouvée: " + commissionId));

        if (commission.getStatut() == StatutCommission.PAYEE) {
            throw new BusinessException("Impossible d'annuler une commission payée");
        }

        commission.setStatut(StatutCommission.ANNULEE);
        commission.setObservation(raison);

        commission = commissionRepository.save(commission);

        return commissionMapper.toDTO(commission);
    }

    /**
     * Total commissions payées par agent
     */
    @Override
    public BigDecimal getTotalPayeByAgent(Long agentId) {
        BigDecimal total = commissionRepository.sumMontantPayeByAgentId(agentId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Recalcule les commissions pour une période
     * (Généralement appelé si les recettes changent)
     */
    @Override
    public void recalculerCommissions(LocalDate dateDebut, LocalDate dateFin) {
        log.info("Recalcul commissions: période {} à {}", dateDebut, dateFin);

        // Pour l'instant, implémentation simple
        // Dans un vrai système: recalculer depuis les recettes validées
        // puis mettre à jour les commissions existantes
        // Cette logique serait plus complexe
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
