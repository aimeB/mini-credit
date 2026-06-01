package com.mini.credit.service.impl;

import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.UpdateEmployeRequest;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.service.EmployeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeServiceImpl implements EmployeService {

    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    public EmployeDTO create(CreateEmployeRequest request) {
        // Vérifie que le matricule n'existe pas
        if (employeRepository.existsByMatricule(request.getMatricule())) {
            throw new RuntimeException("Employé avec ce matricule existe déjà");
        }

        // Récupère l'utilisateur
        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifie qu'il n'y a pas déjà un employé pour cet utilisateur
        if (employeRepository.findByUtilisateurId(request.getUtilisateurId()).isPresent()) {
            throw new RuntimeException("Cet utilisateur est déjà un employé");
        }

        Employe employe = new Employe();
        employe.setMatricule(request.getMatricule());
        employe.setNom(request.getNom());
        employe.setPrenom(request.getPrenom());
        employe.setTelephone(request.getTelephone());
        employe.setAdresse(request.getAdresse());
        employe.setDateEmbauche(request.getDateEmbauche());
        employe.setSalaireBase(request.getSalaireBase());
        employe.setPrimeFixe(request.getPrimeFixe() != null ? request.getPrimeFixe() : java.math.BigDecimal.ZERO);
        employe.setBonusVariable(request.getBonusVariable() != null ? request.getBonusVariable() : java.math.BigDecimal.ZERO);
        employe.setUtilisateur(utilisateur);
        employe.setActif(true);

        Employe saved = employeRepository.save(employe);
        return toDTO(saved);
    }

    @Override
    public EmployeDTO update(Long id, UpdateEmployeRequest request) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        if (request.getNom() != null) {
            employe.setNom(request.getNom());
        }
        if (request.getPrenom() != null) {
            employe.setPrenom(request.getPrenom());
        }
        if (request.getTelephone() != null) {
            employe.setTelephone(request.getTelephone());
        }
        if (request.getAdresse() != null) {
            employe.setAdresse(request.getAdresse());
        }
        if (request.getDateEmbauche() != null) {
            employe.setDateEmbauche(request.getDateEmbauche());
        }
        if (request.getSalaireBase() != null) {
            employe.setSalaireBase(request.getSalaireBase());
        }
        if (request.getPrimeFixe() != null) {
            employe.setPrimeFixe(request.getPrimeFixe());
        }
        if (request.getBonusVariable() != null) {
            employe.setBonusVariable(request.getBonusVariable());
        }
        if (request.getActif() != null) {
            employe.setActif(request.getActif());
        }

        Employe updated = employeRepository.save(employe);
        return toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeDTO getById(Long id) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return toDTO(employe);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeDTO getByMatricule(String matricule) {
        Employe employe = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return toDTO(employe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getAll() {
        return employeRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getAllActive() {
        return employeRepository.findByActifTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!employeRepository.existsById(id)) {
            throw new RuntimeException("Employé non trouvé");
        }
        employeRepository.deleteById(id);
    }

    private EmployeDTO toDTO(Employe employe) {
        return EmployeDTO.builder()
                .id(employe.getId())
                .matricule(employe.getMatricule())
                .nom(employe.getNom())
                .prenom(employe.getPrenom())
                .telephone(employe.getTelephone())
                .adresse(employe.getAdresse())
                .dateEmbauche(employe.getDateEmbauche())
                .salaireBase(employe.getSalaireBase())
                .primeFixe(employe.getPrimeFixe())
                .bonusVariable(employe.getBonusVariable())
                .totalRemuneration(employe.getTotalRemuneration())
                .actif(employe.getActif())
                .utilisateurId(employe.getUtilisateur().getId())
                .roleUtilisateur(employe.getUtilisateur().getRole().getCode().name())
                .dateCreation(employe.getDateCreation())
                .dateModification(employe.getDateModification())
                .build();
    }
}
