package com.mini.credit.controller;

import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.UpdateEmployeRequest;
import com.mini.credit.service.EmployeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
