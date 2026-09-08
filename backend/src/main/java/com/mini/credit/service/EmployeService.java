package com.mini.credit.service;

import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.UpdateEmployeRequest;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface EmployeService {
    EmployeDTO create(CreateEmployeRequest request);
    EmployeDTO update(Long id, UpdateEmployeRequest request);
    EmployeDTO getById(Long id);
    EmployeDTO getByMatricule(String matricule);
    List<EmployeDTO> getAll();
    List<EmployeDTO> getAllActive();
    /** Employés actifs sans compte utilisateur lié — pour le dropdown "Créer utilisateur" */
    List<EmployeDTO> getDisponibles();
    /** Employés actifs avec fonction = GESTIONNAIRE — pour le dropdown "Créer Agent Terrain" */
    List<EmployeDTO> getGestionnaires();
    /** Employés actifs rattachés à une agence donnée — pour le détail Agence */
    List<EmployeDTO> getEmployesByAgence(Long agenceId);
    /**
     * Employés actifs n'appartenant PAS à l'agence donnée — candidats au transfert.
     * Utilisé par la modal "Affecter un employé existant" dans le détail Agence.
     */
    List<EmployeDTO> getEmployesAAffecterAgence(Long agenceId);
    /**
     * Transfère un employé vers une nouvelle agence (et un site de cette agence).
     * Le site doit appartenir à l'agence cible.
     * Réservé à l'ADMIN.
     */
    EmployeDTO changerAgence(Long employeId, Long agenceId, Long siteId);
    EmployeDTO uploadPhoto(Long employeId, MultipartFile file);
    EmployeDTO deletePhoto(Long employeId);
    void delete(Long id);
}
