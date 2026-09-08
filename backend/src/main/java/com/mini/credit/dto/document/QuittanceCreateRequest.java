package com.mini.credit.dto.document;

import com.mini.credit.enums.TypeQuittance;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class QuittanceCreateRequest {

    private Long membreId;

    @NotNull
    private TypeQuittance typeQuittance;

    private String referenceOperation;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal montant;

    private String devise = "CDF";

    @NotNull
    private LocalDateTime dateEmission;

    private Long createdBy;
}