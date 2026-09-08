package com.mini.credit.dto.garantie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloquerGarantieEpargneRequest {
    private Long compteEpargneId;
    private String commentaire;
}