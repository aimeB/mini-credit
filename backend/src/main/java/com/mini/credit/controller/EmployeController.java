package com.mini.credit.controller;

import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.UpdateEmployeRequest;
import com.mini.credit.service.EmployeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeController {

    private final EmployeService employeService;

    @GetMapping
    public ResponseEntity<List<EmployeDTO>> getAll() {
        return ResponseEntity.ok(employeService.getAll());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<EmployeDTO>> getAllActive() {
        return ResponseEntity.ok(employeService.getAllActive());
    }

    /**
     * EmployÃ©s actifs sans compte utilisateur liÃ©.
     * UtilisÃ© par le formulaire "CrÃ©er Utilisateur" pour peupler le dropdown.
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<EmployeDTO>> getDisponibles() {
        return ResponseEntity.ok(employeService.getDisponibles());
    }

    @GetMapping("/gestionnaires")
    public ResponseEntity<List<EmployeDTO>> getGestionnaires() {
        return ResponseEntity.ok(employeService.getGestionnaires());
    }

    /**
     * EmployÃ©s actifs rattachÃ©s Ã  une agence donnÃ©e.
     * UtilisÃ© par le dÃ©tail Agence pour afficher le personnel.
     */
    @GetMapping("/by-agence/{agenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public ResponseEntity<List<EmployeDTO>> getByAgence(@PathVariable Long agenceId) {
        return ResponseEntity.ok(employeService.getEmployesByAgence(agenceId));
    }

    /**
     * EmployÃ©s actifs n'appartenant PAS Ã  l'agence donnÃ©e.
     * UtilisÃ© par la modal "Affecter un employÃ© existant" dans le dÃ©tail Agence.
     */
    @GetMapping("/a-affecter-agence/{agenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public ResponseEntity<List<EmployeDTO>> getEmployesAAffecterAgence(@PathVariable Long agenceId) {
        return ResponseEntity.ok(employeService.getEmployesAAffecterAgence(agenceId));
    }

    /**
     * TransfÃ¨re un employÃ© vers une nouvelle agence.
     * Body : { agenceId: Long, siteId: Long }
     * RÃ©servÃ© Ã  l'ADMIN.
     */
    @PatchMapping("/{id}/agence")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeDTO> changerAgence(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        Long agenceId = body.get("agenceId");
        Long siteId = body.get("siteId");
        if (agenceId == null || siteId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "agenceId et siteId obligatoires");
        }
        return ResponseEntity.ok(employeService.changerAgence(id, agenceId, siteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeService.getById(id));
    }

    @GetMapping("/matricule/{matricule}")
    public ResponseEntity<EmployeDTO> getByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(employeService.getByMatricule(matricule));
    }

    @PostMapping
    public ResponseEntity<EmployeDTO> create(@RequestBody CreateEmployeRequest request) {
        EmployeDTO created = employeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeDTO> update(
            @PathVariable Long id,
            @RequestBody UpdateEmployeRequest request) {
        EmployeDTO updated = employeService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeDTO> uploadPhoto(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(employeService.uploadPhoto(id, file));
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeDTO> deletePhoto(@PathVariable Long id) {
        return ResponseEntity.ok(employeService.deletePhoto(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

