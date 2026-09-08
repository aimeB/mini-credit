package com.mini.credit.dto.dashboard.controleinterne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeCaisse {
    private Long sessionsOuvertes;
    private Long sessionsFermeesNonValidees;
    private Long ecartsDetectes;
    private Long ecartsEnInvestigation;
    private Long ecartsNonResolus;
}
