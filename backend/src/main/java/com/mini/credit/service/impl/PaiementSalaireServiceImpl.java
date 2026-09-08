package com.mini.credit.service.impl;

import com.mini.credit.dto.employe.PaiementSalaireDTO;
import com.mini.credit.dto.employe.CreatePaiementSalaireRequest;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.employe.PaiementSalaire;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.StatutPaiement;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.PaiementSalaireRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.PaiementSalaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaiementSalaireServiceImpl implements PaiementSalaireService {

    private final PaiementSalaireRepository paiementSalaireRepository;
    private final EmployeRepository employeRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final OperationCaisseService operationCaisseService;

    @Override
    public PaiementSalaireDTO create(CreatePaiementSalaireRequest request) {
        // Récupère l'employé
        Employe employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // Crée le paiement
        PaiementSalaire paiement = new PaiementSalaire();
        paiement.setEmploye(employe);
        paiement.setDatePaiement(request.getDatePaiement());
        paiement.setMontant(request.getMontant());
        paiement.setModePaiement(request.getModePaiement());
        paiement.setNotes(request.getNotes());
        paiement.setReferenceExterne(request.getReferenceExterne());
        paiement.setStatut(StatutPaiement.CONFIRME);

        PaiementSalaire savedPaiement = paiementSalaireRepository.save(paiement);

        // Crée l'opération caisse automatiquement
        try {
            createOperationCaisse(employe, savedPaiement, request);
        } catch (Exception e) {
            // Log l'erreur mais n'échoue pas le paiement
            System.err.println("Erreur lors de la création de l'opération caisse: " + e.getMessage());
        }

        return toDTO(savedPaiement);
    }

    private void createOperationCaisse(Employe employe, PaiementSalaire paiement, CreatePaiementSalaireRequest request) {
        // Cherche la session caisse ouverte
        SessionCaisse sessionOuverte = sessionCaisseRepository.findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)
                .orElseThrow(() -> new RuntimeException("Aucune session caisse ouverte"));

        OperationCaisseRequest operationRequest = new OperationCaisseRequest();
        operationRequest.setSessionCaisseId(sessionOuverte.getId());
        operationRequest.setCaisseId(sessionOuverte.getCaisse().getId());
        operationRequest.setDateOperation(LocalDateTime.now());
        operationRequest.setTypeOperation(TypeOperationCaisse.SORTIE);
        operationRequest.setCategorieOperation(CategorieOperationCaisse.SORTIE_DIVERSE);
        operationRequest.setMontant(request.getMontant());
        operationRequest.setModePaiement(request.getModePaiement());
        operationRequest.setDescription("Paiement de salaire - " + employe.getNom() + " " + employe.getPrenom() + " (" + employe.getMatricule() + ")");
        operationRequest.setSource(SourceOperationCaisse.MANUEL);
        operationRequest.setReferenceExterne(request.getReferenceExterne());

        OperationCaisseResponse savedOperation = operationCaisseService.enregistrer(operationRequest);

        // Enregistre le numéro d'opération caisse dans le paiement
        paiement.setNumeroOperationCaisse(savedOperation.getId());
        paiementSalaireRepository.save(paiement);
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementSalaireDTO getById(Long id) {
        PaiementSalaire paiement = paiementSalaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paiement de salaire non trouvé"));
        return toDTO(paiement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementSalaireDTO> getByEmployeId(Long employeId) {
        return paiementSalaireRepository.findByEmployeId(employeId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementSalaireDTO> getByDateRange(LocalDate startDate, LocalDate endDate) {
        return paiementSalaireRepository.findByDatePaiementBetween(startDate, endDate).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementSalaireDTO> getByEmployeAndDateRange(Long employeId, LocalDate startDate, LocalDate endDate) {
        return paiementSalaireRepository.findByEmployeIdAndDatePaiementBetween(employeId, startDate, endDate).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementSalaireDTO> getAll() {
        return paiementSalaireRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private PaiementSalaireDTO toDTO(PaiementSalaire paiement) {
        return PaiementSalaireDTO.builder()
                .id(paiement.getId())
                .employeId(paiement.getEmploye().getId())
                .employeNom(paiement.getEmploye().getNom())
                .employeMatricule(paiement.getEmploye().getMatricule())
                .datePaiement(paiement.getDatePaiement())
                .montant(paiement.getMontant())
                .modePaiement(paiement.getModePaiement())
                .statut(paiement.getStatut())
                .notes(paiement.getNotes())
                .numeroOperationCaisse(paiement.getNumeroOperationCaisse())
                .referenceExterne(paiement.getReferenceExterne())
                .dateCreation(paiement.getDateCreation())
                .dateModification(paiement.getDateModification())
                .build();
    }
}
