package com.mini.credit.service.impl;

import com.mini.credit.dto.document.ContratCreditCreateRequest;
import com.mini.credit.dto.document.ContratCreditResponse;
import com.mini.credit.entity.credit.ContratCredit;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.DocumentMapper;
import com.mini.credit.repository.credit.ContratCreditRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.service.ContratCreditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ContratCreditServiceImpl implements ContratCreditService {

    private final ContratCreditRepository contratCreditRepository;
    private final CreditRepository creditRepository;
    private final DocumentMapper documentMapper;

    @Override
    public ContratCreditResponse create(ContratCreditCreateRequest request) {
        Credit credit = creditRepository.findById(request.getCreditId())
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));

        if (contratCreditRepository.findByCreditId(credit.getId()).isPresent()) {
            throw new BusinessException("Un contrat existe déjà pour ce crédit");
        }

        String objet = request.getObjetContrat();
        if (objet == null || objet.isBlank()) {
            objet = buildObjetContratParDefaut(credit);
        }

        String clauses = request.getClausesSpecifiques();
        if (clauses == null || clauses.isBlank()) {
            clauses = buildClausesParDefaut(credit);
        }

        ContratCredit contrat = ContratCredit.builder()
                .credit(credit)
                .numeroContrat("CTR-" + System.currentTimeMillis())
                .dateSignature(request.getDateSignature())
                .lieuSignature(request.getLieuSignature())
                .objetContrat(objet)
                .clausesSpecifiques(clauses)
                .signeParMembre(Boolean.TRUE.equals(request.getSigneParMembre()))
                .signeParInstitution(Boolean.TRUE.equals(request.getSigneParInstitution()))
                .nomSignataireInstitution(request.getNomSignataireInstitution())
                .fonctionSignataireInstitution(request.getFonctionSignataireInstitution())
                .build();

        return documentMapper.toResponse(contratCreditRepository.save(contrat));
    }

    @Override
    public ContratCreditResponse getById(Long id) {
        return documentMapper.toResponse(
                contratCreditRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable"))
        );
    }

    @Override
    public ContratCreditResponse getByCreditId(Long creditId) {
        return documentMapper.toResponse(
                contratCreditRepository.findByCreditId(creditId)
                        .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable pour ce crédit"))
        );
    }

    private String buildObjetContratParDefaut(Credit credit) {
        return "Prêt de " + credit.getMontantOctroye() + " " + credit.getDevise()
                + " sur " + credit.getDureeValeur() + " " + credit.getDureeUnite().name().toLowerCase()
                + " au taux de " + credit.getTauxInteret() + "%";
    }

    private String buildClausesParDefaut(Credit credit) {
        return """
                L'emprunteur s'engage à respecter l'échéancier convenu.
                Toute échéance impayée à sa date entraîne une pénalité forfaitaire journalière.
                Les garanties enregistrées au crédit peuvent être mobilisées en cas de défaut.
                Le prêteur conserve un droit de suivi et de contrôle de l'activité financée.
                """;
    }
}