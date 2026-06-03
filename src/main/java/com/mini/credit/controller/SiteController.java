package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.SiteResponse;
import com.mini.credit.repository.referentiel.SiteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sites")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Site", description = "Site reference data endpoints")
public class SiteController {

    private final SiteRepository siteRepository;

    @GetMapping
    @Operation(summary = "Get all sites", description = "Retrieve list of all available sites")
    public List<SiteResponse> getAll() {
        return siteRepository.findAll()
                .stream()
                .map(SiteResponse::fromEntity)
                .toList();
    }
}