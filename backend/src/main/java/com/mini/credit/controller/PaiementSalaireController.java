package com.mini.credit.controller;

import com.mini.credit.dto.employe.PaiementSalaireDTO;
import com.mini.credit.dto.employe.CreatePaiementSalaireRequest;
import com.mini.credit.service.PaiementSalaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/paiements-salaire")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PaiementSalaireController {

    private final PaiementSalaireService paiementSalaireService;

    @PostMapping
    public ResponseEntity<PaiementSalaireDTO> create(@RequestBody CreatePaiementSalaireRequest request) {
        PaiementSalaireDTO created = paiementSalaireService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaiementSalaireDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paiementSalaireService.getById(id));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<PaiementSalaireDTO>> getByEmployeId(@PathVariable Long employeId) {
        return ResponseEntity.ok(paiementSalaireService.getByEmployeId(employeId));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<PaiementSalaireDTO>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(paiementSalaireService.getByDateRange(startDate, endDate));
    }

    @GetMapping("/employe/{employeId}/date-range")
    public ResponseEntity<List<PaiementSalaireDTO>> getByEmployeAndDateRange(
            @PathVariable Long employeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(paiementSalaireService.getByEmployeAndDateRange(employeId, startDate, endDate));
    }

    @GetMapping
    public ResponseEntity<List<PaiementSalaireDTO>> getAll() {
        return ResponseEntity.ok(paiementSalaireService.getAll());
    }
}
