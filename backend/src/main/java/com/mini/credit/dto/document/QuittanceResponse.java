package com.mini.credit.dto.document;

import com.mini.credit.enums.TypeQuittance;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class QuittanceResponse {
    private Long id;
    private String numeroQuittance;
    private Long membreId;
    private String membreNomComplet;
    private TypeQuittance typeQuittance;
    private String referenceOperation;
    private BigDecimal montant;
    private String devise;
    private LocalDateTime dateEmission;
    private String fichierUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}