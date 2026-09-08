package com.mini.credit.dto.document;

import com.mini.credit.enums.StatutTicketRecu;
import com.mini.credit.enums.TypeTicketRecu;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TicketVerificationResponse {
    private boolean valide;
    private String numeroTicket;
    private TypeTicketRecu typeTicket;
    private LocalDateTime dateGeneration;
    private BigDecimal montantPrincipal;
    private String devise;
    private String membreMasque;
    private StatutTicketRecu statut;
    private boolean duplicata;
    private String message;
}
