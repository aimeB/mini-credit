package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControleCoherenceDto {
    private String severite;
    private String type;
    private String reference;
    private String message;
    private Long antenneId;
    private String antenne;
}
