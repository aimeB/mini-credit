package com.mini.credit.dto.dashboard.controleinterne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeRetraits {
    private Long creees;
    private Long valides;
    private Long rejetees;
    private Long decaissees;
}
