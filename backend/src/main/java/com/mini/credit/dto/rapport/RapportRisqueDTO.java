package com.mini.credit.dto.rapport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportRisqueDTO {
    private long creditsEnRisque;
    private long creditsEnRetard;
    private long creditsEnDefaut;
    private String niveauRisqueGlobal;
    private List<RisqueAlerte> alertes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RisqueAlerte {
        private String description;
        private String niveau; // FAIBLE, MOYEN, ELEVE, CRITIQUE
        private Long creditId;
        private String membreName;
    }
}
