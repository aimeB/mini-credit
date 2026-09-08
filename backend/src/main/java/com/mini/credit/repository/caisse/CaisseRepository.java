package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.Caisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaisseRepository extends JpaRepository<Caisse, Long> {
    Optional<Caisse> findByCodeCaisse(String codeCaisse);
    List<Caisse> findByActifTrue();
    List<Caisse> findByActifTrueAndAgenceId(Long agenceId);
    List<Caisse> findByActifTrueAndCaissierResponsableId(Long caissierResponsableId);
    boolean existsByActifTrueAndAgenceId(Long agenceId);
    List<Caisse> findByActifTrueAndSiteId(Long siteId);
    boolean existsByActifTrueAndSiteId(Long siteId);
}
