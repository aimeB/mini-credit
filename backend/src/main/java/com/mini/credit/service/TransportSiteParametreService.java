package com.mini.credit.service;

import com.mini.credit.dto.referentiel.TransportSiteParametreRequest;
import com.mini.credit.dto.referentiel.TransportSiteParametreResponse;

import java.util.List;

public interface TransportSiteParametreService {
    List<TransportSiteParametreResponse> getAll();
    TransportSiteParametreResponse save(TransportSiteParametreRequest request);
    TransportSiteParametreResponse deactivate(Long id, String commentaire);
}
