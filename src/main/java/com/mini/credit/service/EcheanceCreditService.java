package com.mini.credit.service;

import com.mini.credit.dto.credit.EcheanceCreditResponse;

import java.util.List;

public interface EcheanceCreditService {

    List<EcheanceCreditResponse> getByCreditId(Long creditId);
}