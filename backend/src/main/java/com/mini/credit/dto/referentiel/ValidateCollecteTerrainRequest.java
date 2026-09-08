package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCollecteTerrainRequest {
    @NotBlank(message = "decision obligatoire")
    private String decision; // VALIDEE ou REJETEE

    private String motifRejet;
}
