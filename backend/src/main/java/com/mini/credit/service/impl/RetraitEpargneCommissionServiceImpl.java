package com.mini.credit.service.impl;

import com.mini.credit.exception.BusinessException;
import com.mini.credit.service.RetraitEpargneCommissionService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RetraitEpargneCommissionServiceImpl implements RetraitEpargneCommissionService {

    private static final BigDecimal CENT = new BigDecimal("0.01");
    private static final BigDecimal FRANC_CDF = new BigDecimal("1.00");
    private static final Map<String, List<Tranche>> GRILLES = Map.of(
            "CDF", List.of(
                    tranche("1000.00", "10000.00", "9.0"),
                    tranche("10001.00", "24000.00", "8.0"),
                    tranche("24001.00", "34000.00", "6.0"),
                    tranche("34001.00", "49000.00", "6.5"),
                    tranche("49001.00", "69000.00", "5.5"),
                    tranche("69001.00", "94000.00", "5.0"),
                    tranche("94001.00", "100000.00", "4.5"),
                    tranche("100001.00", "200000.00", "4.0"),
                    tranche("200001.00", "400000.00", "3.5"),
                    tranche("400001.00", "600000.00", "3.0"),
                    tranche("600001.00", "800000.00", "2.5"),
                    tranche("800001.00", "999999.00", "2.0"),
                    tranche("1000000.00", "1500000.00", "2.0"),
                    tranche("1500001.00", null, "1.4")
            ),
            "USD", List.of(
                    tranche("1.00", "4.99", "9.0"),
                    tranche("5.00", "6.45", "8.0"),
                    tranche("6.46", "7.50", "6.0"),
                    tranche("7.51", "7.95", "6.5"),
                    tranche("7.96", "8.95", "5.5"),
                    tranche("8.96", "9.95", "5.0"),
                    tranche("9.96", "12.50", "4.5"),
                    tranche("12.51", "14.00", "4.0"),
                    tranche("14.01", "49.99", "3.5"),
                    tranche("50.00", "99.95", "3.0"),
                    tranche("99.96", "199.95", "2.5"),
                    tranche("199.96", "249.95", "1.6"),
                    tranche("249.96", "499.95", "2.0"),
                    tranche("499.96", null, "1.4")
            )
    );

    @PostConstruct
    @Override
    public void verifierContinuiteGrilles() {
        GRILLES.forEach((devise, tranches) -> {
                BigDecimal pas = "CDF".equals(devise) ? FRANC_CDF : CENT;
                List<Tranche> sorted = tranches.stream()
                    .sorted(Comparator.comparing(Tranche::min))
                    .toList();
            for (int i = 0; i < sorted.size(); i++) {
                Tranche current = sorted.get(i);
                if (current.max() != null && current.min().compareTo(current.max()) > 0) {
                    throw new IllegalStateException("Tranche retrait invalide " + devise + ": min > max");
                }
                if (i < sorted.size() - 1) {
                    Tranche next = sorted.get(i + 1);
                    BigDecimal expectedNextMin = current.max().add(pas);
                    if (current.max() == null || next.min().compareTo(expectedNextMin) != 0) {
                        throw new IllegalStateException("Grille retrait " + devise + " non continue entre " + current + " et " + next);
                    }
                } else if (current.max() != null) {
                    throw new IllegalStateException("La derniere tranche retrait " + devise + " doit etre ouverte");
                }
            }
        });
    }

    @Override
    public CommissionRetrait calculer(String devise, BigDecimal montantRetrait) {
        String normalizedDevise = normalizeDevise(devise);
        BigDecimal montant = normalizeAmount(montantRetrait);
        BigDecimal minimum = minimumFor(normalizedDevise);
        if (montant.compareTo(minimum) < 0) {
            throw new BusinessException(minimumMessage(normalizedDevise));
        }

        Tranche tranche = GRILLES.get(normalizedDevise).stream()
                .filter(t -> t.matches(montant))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Aucune tranche tarifaire retrait trouvée pour " + montant + " " + normalizedDevise));

        BigDecimal commission = montant
                .multiply(tranche.tauxPourcentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return new CommissionRetrait(
                normalizedDevise,
                montant,
                tranche.tauxPourcentage(),
                commission,
                montant.add(commission).setScale(2, RoundingMode.HALF_UP),
                tranche.min(),
                tranche.max()
        );
    }

    private BigDecimal minimumFor(String devise) {
        return "USD".equals(devise) ? new BigDecimal("1.00") : new BigDecimal("1000.00");
    }

    private String minimumMessage(String devise) {
        return "USD".equals(devise)
                ? "Le montant minimum de retrait est de 1 USD."
                : "Le montant minimum de retrait est de 1 000 CDF.";
    }

    private String normalizeDevise(String devise) {
        String normalized = devise == null || devise.isBlank() ? "CDF" : devise.trim().toUpperCase(Locale.ROOT);
        if (!GRILLES.containsKey(normalized)) {
            throw new BusinessException("Devise non supportée pour commission retrait: " + normalized);
        }
        return normalized;
    }

    private BigDecimal normalizeAmount(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de retrait doit être > 0");
        }
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    private static Tranche tranche(String min, String max, String tauxPourcentage) {
        return new Tranche(
                new BigDecimal(min).setScale(2, RoundingMode.HALF_UP),
                max == null ? null : new BigDecimal(max).setScale(2, RoundingMode.HALF_UP),
                new BigDecimal(tauxPourcentage).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private record Tranche(BigDecimal min, BigDecimal max, BigDecimal tauxPourcentage) {
        boolean matches(BigDecimal montant) {
            return montant.compareTo(min) >= 0 && (max == null || montant.compareTo(max) <= 0);
        }
    }
}