package com.mini.credit.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RetardEcheanceProjection {
    Long getCreditId();
    String getNumeroCredit();
    Long getMembreId();
    String getMembreNomComplet();
    Long getEcheanceId();
    Integer getNumeroEcheance();
    LocalDate getDateEcheance();
    BigDecimal getResteAPayer();
    BigDecimal getPenaliteCumulee();
}
