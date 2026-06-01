package com.mini.credit.dto.caisse;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CaisseResponse {
    private Long id;
    private String codeCaisse;
    private String libelle;
    private Long siteId;
    private String siteNom;
    private String devise;
    private Boolean actif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}