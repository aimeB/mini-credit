package com.mini.credit.dto.rapport;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BilanHebdomadaireDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal totalEncaissementsSemaine;
    private BigDecimal totalDecaissementsSemaine;
    private BigDecimal soldeSemaine;
    private List<BilanJournalierDTO> bilansJournaliers;
}