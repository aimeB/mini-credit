package com.mini.credit.service;

import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class SessionCaisseValidationService {

    public void validateSessionForOperation(SessionCaisse session) {
        if (session == null) {
            throw new BusinessException("Session caisse introuvable");
        }

        if (session.getStatut() == null) {
            throw new BusinessException("Le statut de la session de caisse est invalide");
        }

        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("La session de caisse n'est pas ouverte");
        }

        if (session.getDateCloture() != null) {
            throw new BusinessException("La session de caisse est déjà clôturée");
        }

        if (session.getDateComptable() == null) {
            throw new BusinessException("La date comptable de la session est manquante");
        }

        LocalDate today = LocalDate.now();
        if (session.getDateComptable().isBefore(today)) {
            throw new BusinessException(
                    "La session de caisse est ancienne (date comptable "
                            + session.getDateComptable().format(DateTimeFormatter.ISO_DATE)
                            + "). Clôturez ou régularisez cette session avant de travailler aujourd'hui."
            );
        }

        if (session.getDateComptable().isAfter(today)) {
            throw new BusinessException("La session de caisse a une date comptable future et ne peut pas être utilisée aujourd'hui");
        }

        if (session.getCaisse() == null) {
            throw new BusinessException("La caisse liée à la session est introuvable");
        }

        if (Boolean.FALSE.equals(session.getCaisse().getActif())) {
            throw new BusinessException("La caisse liée à la session est inactive");
        }
    }
}