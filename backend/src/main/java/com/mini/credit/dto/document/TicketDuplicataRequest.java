package com.mini.credit.dto.document;

import com.mini.credit.enums.FormatTicketRecu;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketDuplicataRequest {
    @NotBlank
    private String motif;
    private FormatTicketRecu format = FormatTicketRecu.THERMIQUE_80MM;
    private String commentaire;
}
