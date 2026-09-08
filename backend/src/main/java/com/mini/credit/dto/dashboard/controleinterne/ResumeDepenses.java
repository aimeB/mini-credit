package com.mini.credit.dto.dashboard.controleinterne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDepenses {
    private Long brouillon;
    private Long enAttenteValidation;
    private Long valideesNonPayees;
    private Long payees;
    private Long rejetees;
}
