package com.mini.credit.service;

import com.mini.credit.entity.credit.PaiementCredit;
import com.mini.credit.enums.CanalPaiement;

import java.math.BigDecimal;

public interface PaiementCreditService {

    PaiementCredit initierPaiement(Long contratId, Long echeanceId, BigDecimal montant, CanalPaiement canal);

    PaiementCredit confirmerPaiement(String referenceInterne, String referenceExterne, String payloadBrut);

    PaiementCredit echouerPaiement(String referenceInterne, String motif);

    void affecterPaiementAEcheance(PaiementCredit paiement);
}
