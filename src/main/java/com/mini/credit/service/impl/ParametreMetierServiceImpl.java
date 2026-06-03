package com.mini.credit.service.impl;

import com.mini.credit.dto.parametrage.ParametreMetierDTO;
import com.mini.credit.entity.referentiel.ParametreMetier;
import com.mini.credit.enums.CategorieParametre;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.ParametreMetierMapper;
import com.mini.credit.repository.referentiel.ParametreMetierRepository;
import com.mini.credit.service.ParametreMetierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implémentation du service ParametreMetier avec cache simple.
 *
 * PHASE 1: Infrastructure de cache pour éviter des appels répétés à la BD.
 * Cache strategy:
 * - Cache en mémoire (ConcurrentHashMap) pour rapidité
 * - Recharger via rechargerCache() après modification
 * - Pas de TTL pour éviter la complexité Phase 1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParametreMetierServiceImpl implements ParametreMetierService {

    private final ParametreMetierRepository parametreMetierRepository;
    private final ParametreMetierMapper mapper;

    // Cache simple : clé → DTO
    private final Map<String, ParametreMetierDTO> cache = new ConcurrentHashMap<>();

    // Flag pour savoir si le cache est chargé
    private volatile boolean cacheInitialized = false;

    /**
     * Initialise le cache au premier accès.
     */
    private void initializeCache() {
        if (!cacheInitialized) {
            synchronized (this) {
                if (!cacheInitialized) {
                    rechargerCache();
                    cacheInitialized = true;
                    log.info("Cache ParametreMetier initialisé avec {} paramètres", cache.size());
                }
            }
        }
    }

    @Override
    public BigDecimal getDecimal(String cle) {
        ParametreMetierDTO param = getFromCache(cle);
        if (param == null) {
            log.warn("Paramètre decimal non trouvé : {}", cle);
            return BigDecimal.ZERO;
        }
        return param.getValeurDecimale() != null ? param.getValeurDecimale() : BigDecimal.ZERO;
    }

    @Override
    public Long getEntier(String cle) {
        ParametreMetierDTO param = getFromCache(cle);
        if (param == null) {
            log.warn("Paramètre entier non trouvé : {}", cle);
            return 0L;
        }
        return param.getValeurEntiere() != null ? param.getValeurEntiere() : 0L;
    }

    @Override
    public String getTexte(String cle) {
        ParametreMetierDTO param = getFromCache(cle);
        if (param == null) {
            log.warn("Paramètre texte non trouvé : {}", cle);
            return "";
        }
        return param.getValeurTexte() != null ? param.getValeurTexte() : "";
    }

    @Override
    public Object getValeur(String cle) {
        ParametreMetierDTO param = getFromCache(cle);
        if (param == null) {
            log.warn("Paramètre non trouvé : {}", cle);
            return null;
        }
        return param.getValeur();
    }

    @Override
    public Optional<ParametreMetierDTO> getByKey(String cle) {
        return Optional.ofNullable(getFromCache(cle));
    }

    @Override
    public List<ParametreMetierDTO> getByCategorie(CategorieParametre categorie) {
        initializeCache();
        return cache.values().stream()
                .filter(p -> p.getCategorie() == categorie)
                .toList();
    }

    @Override
    public List<ParametreMetierDTO> getAllActifs() {
        initializeCache();
        return cache.values().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActif()))
                .toList();
    }

    @Override
    public void rechargerCache() {
        log.debug("Rechargement du cache ParametreMetier...");
        cache.clear();
        List<ParametreMetier> parametres = parametreMetierRepository.findAllActifs();
        parametres.forEach(p -> cache.put(p.getCle(), mapper.toDTO(p)));
        log.debug("Cache reloadé avec {} paramètres", cache.size());
    }

    @Override
    public boolean existsKey(String cle) {
        initializeCache();
        return cache.containsKey(cle);
    }

    /**
     * Récupère un paramètre du cache, avec initialisation lazy.
     */
    private ParametreMetierDTO getFromCache(String cle) {
        initializeCache();
        return cache.get(cle);
    }
}
