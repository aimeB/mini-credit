package com.mini.credit.service;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service de génération automatique du matricule employé.
 *
 * Format : AGENCE-FONCTION-AA-SEQ
 *   - AGENCE   : code agence (ex. DEL1)
 *   - FONCTION : code court de la fonction (ex. GES, AT, CAI…)
 *   - AA       : deux derniers chiffres de l'année (ex. 26)
 *   - SEQ      : séquence 3 chiffres, réinitialisée par agence + fonction + année
 *
 * Exemples : DEL1-GES-26-001 / DEL1-AT-26-003 / KIN2-CAI-26-001
 */
@Service
@RequiredArgsConstructor
public class MatriculeGeneratorService {

    private final EmployeRepository employeRepository;

    /** Correspondance PosteEmploye → code court */
    private static final Map<PosteEmploye, String> CODES_FONCTION = Map.ofEntries(
            Map.entry(PosteEmploye.AGENT_TERRAIN,         "AT"),
            Map.entry(PosteEmploye.GESTIONNAIRE,          "GES"),
            Map.entry(PosteEmploye.CONTROLEUR,            "CTR"),
            Map.entry(PosteEmploye.CAISSIER,              "CAI"),
            Map.entry(PosteEmploye.CHEF_BUREAU,           "CB"),
            Map.entry(PosteEmploye.COO,                   "COO"),
            Map.entry(PosteEmploye.RCI,                   "RCI"),
            Map.entry(PosteEmploye.GERANT_GENERAL,        "GG"),
            Map.entry(PosteEmploye.ADMINISTRATIF,         "ADM"),
            Map.entry(PosteEmploye.CHARGE_OPERATIONS,     "CO"),
            Map.entry(PosteEmploye.RESPONSABLE_CONTROLES, "RC"),
            Map.entry(PosteEmploye.AUTRE,                 "AUT")
    );

    /**
     * Génère le prochain matricule unique pour cet agence/fonction/année.
     *
     * @param agence   entité Agence — fournit le codeAgence
     * @param fonction poste de l'employé
     * @return matricule au format AGENCE-FONCTION-AA-SEQ (ex. DEL1-GES-26-001)
     */
    public String generer(Agence agence, PosteEmploye fonction) {
        return generer(agence, fonction, LocalDate.now().getYear());
    }

    /**
     * Variante testable avec l'année explicite.
     */
    public String generer(Agence agence, PosteEmploye fonction, int annee) {
        String codeAgence   = agence.getCodeAgence().toUpperCase();
        String codeFonction = CODES_FONCTION.getOrDefault(fonction, "AUT");
        String aa           = String.valueOf(annee).substring(2); // "2026" → "26"

        // Préfixe de recherche : ex. "DEL1-GES-26-"
        String prefixe = codeAgence + "-" + codeFonction + "-" + aa + "-";

        int prochainSeq = determinerProchainSeq(prefixe);
        return prefixe + String.format("%03d", prochainSeq);
    }

    /**
     * Retourne le code court correspondant à une fonction.
     * Utile pour les tests unitaires.
     */
    public String getCodeFonction(PosteEmploye fonction) {
        return CODES_FONCTION.getOrDefault(fonction, "AUT");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Méthodes privées
    // ──────────────────────────────────────────────────────────────────────────

    private int determinerProchainSeq(String prefixe) {
        // Récupère tous les matricules dont le début correspond au préfixe
        List<String> matricules = employeRepository.findMatriculesByPrefix(prefixe);

        if (matricules.isEmpty()) {
            return 1;
        }

        // Extrait le numéro de séquence (partie après le préfixe) et retourne max + 1
        int max = matricules.stream()
                .map(m -> {
                    try {
                        return Integer.parseInt(m.substring(prefixe.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(0);

        return max + 1;
    }
}
