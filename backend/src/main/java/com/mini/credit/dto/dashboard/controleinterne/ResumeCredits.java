package com.mini.credit.dto.dashboard.controleinterne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeCredits {
    private Long approuves;
    private Long enCours;
    private Long enRetard;
    private Long contentieux;
    private Long clotures;
}
