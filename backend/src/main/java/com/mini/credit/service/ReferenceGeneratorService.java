package com.mini.credit.service;

import com.mini.credit.repository.caisse.OperationCaisseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReferenceGeneratorService {

    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OperationCaisseRepository operationCaisseRepository;

    public ReferenceGeneratorService(OperationCaisseRepository operationCaisseRepository) {
        this.operationCaisseRepository = operationCaisseRepository;
    }

    public String genererReference(String prefix) {
        String date = LocalDate.now().format(DATE_FORMAT);
        String candidate;

        do {
            int sequence = counter.getAndIncrement();
            candidate = String.format("%s%s%04d", prefix, date, sequence);
        } while (requiresDatabaseUniquenessCheck(prefix)
                && operationCaisseRepository.existsByNumeroPiece(candidate));

        return candidate;
    }

    private boolean requiresDatabaseUniquenessCheck(String prefix) {
        return "PCS".equals(prefix);
    }
}