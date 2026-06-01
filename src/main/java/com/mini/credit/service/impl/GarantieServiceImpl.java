package com.mini.credit.service.impl;

import com.mini.credit.dto.garantie.GarantieCreateRequest;
import com.mini.credit.dto.garantie.GarantieResponse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.Garantie;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.GarantieRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.service.GarantieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GarantieServiceImpl implements GarantieService {

    private final GarantieRepository garantieRepository;
    private final CreditRepository creditRepository;
    private final DemandeCreditRepository demandeCreditRepository;
    private final MembreRepository membreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GarantieResponse> getAllGaranties() {
        return garantieRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GarantieResponse getGarantieById(Long id) {
        return garantieRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Garantie non trouvée avec ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarantieResponse> getGarantiesByCreditId(Long creditId) {
        return garantieRepository.findByCreditId(creditId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarantieResponse> getGarantiesByMembreId(Long membreId) {
        return garantieRepository.findByMembreIdWithEagerLoad(membreId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarantieResponse> getGarantiesByDemandeCreditId(Long demandeCreditId) {
        return garantieRepository.findByDemandeCreditId(demandeCreditId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarantieResponse> getGarantiesByType(TypeGarantie type) {
        return garantieRepository.findByTypeGarantie(type)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarantieResponse> getGarantiesByStatut(StatutGarantie statut) {
        return garantieRepository.findByStatut(statut)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarantieResponse> getGarantiesByDateRange(LocalDateTime debut, LocalDateTime fin) {
        return garantieRepository.findByDateRange(debut, fin)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public GarantieResponse createGarantie(GarantieCreateRequest request) {
        log.info("Création d'une nouvelle garantie de type: {}", request.getTypeGarantie());

        Garantie garantie = Garantie.builder()
                .typeGarantie(request.getTypeGarantie())
                .description(request.getDescription())
                .valeurEstimee(request.getValeurEstimee())
                .taux(request.getTaux())
                .localisation(request.getLocalisation())
                .notes(request.getNotes())
                .statut(StatutGarantie.ACTIF)
                .build();

        // Associer au crédit si fourni
        if (request.getCreditId() != null) {
            Credit credit = creditRepository.findById(request.getCreditId())
                    .orElseThrow(() -> new RuntimeException("Crédit non trouvé avec ID: " + request.getCreditId()));
            garantie.setCredit(credit);
        }

        // Associer au membre si fourni
        if (request.getMembreId() != null) {
            Membre membre = membreRepository.findById(request.getMembreId())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec ID: " + request.getMembreId()));
            garantie.setMembre(membre);
        }

        Garantie savedGarantie = garantieRepository.save(garantie);
        log.info("Garantie créée avec succès, ID: {}", savedGarantie.getId());

        return toResponse(savedGarantie);
    }

    @Override
    public GarantieResponse updateGarantie(Long id, GarantieCreateRequest request) {
        log.info("Mise à jour de la garantie avec ID: {}", id);

        Garantie garantie = garantieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Garantie non trouvée avec ID: " + id));

        garantie.setTypeGarantie(request.getTypeGarantie());
        garantie.setDescription(request.getDescription());
        garantie.setValeurEstimee(request.getValeurEstimee());
        garantie.setTaux(request.getTaux());
        garantie.setLocalisation(request.getLocalisation());
        garantie.setNotes(request.getNotes());

        if (request.getCreditId() != null) {
            Credit credit = creditRepository.findById(request.getCreditId())
                    .orElseThrow(() -> new RuntimeException("Crédit non trouvé avec ID: " + request.getCreditId()));
            garantie.setCredit(credit);
        }

        if (request.getMembreId() != null) {
            Membre membre = membreRepository.findById(request.getMembreId())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec ID: " + request.getMembreId()));
            garantie.setMembre(membre);
        }

        Garantie updatedGarantie = garantieRepository.save(garantie);
        log.info("Garantie mise à jour avec succès, ID: {}", updatedGarantie.getId());

        return toResponse(updatedGarantie);
    }

    @Override
    public void deleteGarantie(Long id) {
        log.info("Suppression de la garantie avec ID: {}", id);

        if (!garantieRepository.existsById(id)) {
            throw new RuntimeException("Garantie non trouvée avec ID: " + id);
        }

        garantieRepository.deleteById(id);
        log.info("Garantie supprimée avec succès, ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatut(StatutGarantie statut) {
        return garantieRepository.countByStatut(statut);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByType(TypeGarantie type) {
        return garantieRepository.countByTypeGarantie(type);
    }

    private GarantieResponse toResponse(Garantie garantie) {
        return GarantieResponse.builder()
                .id(garantie.getId())
                .creditId(garantie.getCredit() != null ? garantie.getCredit().getId() : null)
                .demandeCreditId(garantie.getDemandeCredit() != null ? garantie.getDemandeCredit().getId() : null)
                .membreId(garantie.getMembre() != null ? garantie.getMembre().getId() : null)
                .typeGarantie(garantie.getTypeGarantie())
                .description(garantie.getDescription())
                .valeurEstimee(garantie.getValeurEstimee())
                .taux(garantie.getTaux())
                .montantBloque(garantie.getMontantBloque())
                .localisation(garantie.getLocalisation())
                .statut(garantie.getStatut())
                .dateCreation(garantie.getDateCreation())
                .dateModification(garantie.getDateModification())
                .notes(garantie.getNotes())
                .build();
    }
}
