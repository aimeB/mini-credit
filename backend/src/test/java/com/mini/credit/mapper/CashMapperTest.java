package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutSessionCaisse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("CashMapper Tests")
class CashMapperTest {

    private CashMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CashMapper();
    }

    @Test
    @DisplayName("Maps CaisseResponse antenne fields from site.agence")
    void toResponseCaisse_ShouldMapAntenneFromAgence() {
        Agence agence = Agence.builder()
                .codeAgence("ANT-SAK")
                .nomAgence("Sakombi")
                .build();
        agence.setId(12L);

        Site site = Site.builder()
                .nomSite("Site de Sakombi")
                .agence(agence)
                .build();
        site.setId(4L);

        Caisse caisse = Caisse.builder()
                .codeCaisse("CAI-001")
                .libelle("Caisse Sakombi")
                .site(site)
                .devise("CDF")
                .actif(true)
                .build();
        caisse.setId(1L);

        CaisseResponse response = mapper.toResponse(caisse);

        assertNotNull(response);
        assertEquals(4L, response.getSiteId());
        assertEquals("Site de Sakombi", response.getSiteNom());
        assertEquals(12L, response.getAntenneId());
        assertEquals("Sakombi", response.getAntenneNom());
    }

    @Test
    @DisplayName("Does not fallback antenneNom to siteNom when agence is missing")
    void toResponseCaisse_ShouldKeepAntenneNullWhenNoAgence() {
        Site siteSansAgence = Site.builder()
                .nomSite("Site de Sakombi")
                .build();
        siteSansAgence.setId(4L);

        Caisse caisse = Caisse.builder()
                .codeCaisse("CAI-001")
                .libelle("Caisse Sakombi")
                .site(siteSansAgence)
                .devise("CDF")
                .actif(true)
                .build();

        CaisseResponse response = mapper.toResponse(caisse);

        assertNotNull(response);
        assertEquals("Site de Sakombi", response.getSiteNom());
        assertNull(response.getAntenneId());
        assertNull(response.getAntenneNom());
    }

    @Test
    @DisplayName("Maps SessionCaisseResponse site and antenne fields for traceability")
    void toResponseSession_ShouldMapSiteAndAntenne() {
        Agence agence = Agence.builder()
                .codeAgence("ANT-SAK")
                .nomAgence("Sakombi")
                .build();
        agence.setId(12L);

        Site site = Site.builder()
                .nomSite("Site de Sakombi")
                .agence(agence)
                .build();
        site.setId(4L);

        Caisse caisse = Caisse.builder()
                .codeCaisse("CAI-001")
                .libelle("Caisse Sakombi")
                .site(site)
                .devise("CDF")
                .actif(true)
                .build();
        caisse.setId(1L);

        Utilisateur utilisateur = Utilisateur.builder()
                .username("cashier")
                .motDePasseHash("x")
                .nomComplet("Caissier Test")
                .build();
        utilisateur.setId(20L);

        SessionCaisse session = SessionCaisse.builder()
                .caisse(caisse)
                .utilisateur(utilisateur)
                .dateOuverture(LocalDateTime.now())
                .soldeOuverture(new BigDecimal("1000"))
                .totalEntrees(BigDecimal.ZERO)
                .totalSorties(BigDecimal.ZERO)
                .soldeTheorique(new BigDecimal("1000"))
                .statut(StatutSessionCaisse.OUVERTE)
                .build();
        session.setId(100L);

        SessionCaisseResponse response = mapper.toResponse(session);

        assertNotNull(response);
        assertEquals(4L, response.getSiteId());
        assertEquals("Site de Sakombi", response.getSiteNom());
        assertEquals(12L, response.getAntenneId());
        assertEquals("Sakombi", response.getAntenneNom());
    }
}
