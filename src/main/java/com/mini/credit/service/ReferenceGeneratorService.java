package com.mini.credit.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReferenceGeneratorService {

    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String genererReference(String prefix) {
        String date = LocalDate.now().format(DATE_FORMAT);
        int sequence = counter.getAndIncrement();
        return String.format("%s%s%04d", prefix, date, sequence);
    }
}