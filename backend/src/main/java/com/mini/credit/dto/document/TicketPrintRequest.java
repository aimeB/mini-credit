package com.mini.credit.dto.document;

import com.mini.credit.enums.FormatTicketRecu;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketPrintRequest {
    @NotNull
    private FormatTicketRecu format = FormatTicketRecu.THERMIQUE_80MM;
    private boolean marquerImprime = true;
    private Boolean impressionReussie = true;
    private String commentaire;
}
