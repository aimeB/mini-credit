package com.mini.credit.service;

import com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.impl.PaiementInitialDemandeCreditServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaiementInitialDemandeCreditService - règles statut")
class PaiementInitialDemandeCreditServiceTest {

    @Mock
    private DemandeCreditRepository demandeCreditRepository;
    @Mock
    private SessionCaisseRepository sessionCaisseRepository;
    @Mock
    private CaisseRepository caisseRepository;
    @Mock
    private AgentTerrainRepository agentTerrainRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private CreditMapper creditMapper;
    @Mock
    private CashMapper cashMapper;
    @Mock
    private OperationEpargneService operationEpargneService;
    @Mock
    private CompteEpargneRepository compteEpargneRepository;
    @Mock
    private OperationCaisseService operationCaisseService;
    @Mock
    private OperationCaisseRepository operationCaisseRepository;

    @InjectMocks
    private PaiementInitialDemandeCreditServiceImpl service;

    @Test
    void paiementInitial_shouldBeRejectedWhenDemandeIsSoumise() {
        com.mini.credit.entity.referentiel.Utilisateur caissier = com.mini.credit.entity.referentiel.Utilisateur.builder()
                .username("caissier")
                .role(com.mini.credit.entity.referentiel.Role.builder().code(RoleCode.CAISSIER).build())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caissier, null, caissier.getAuthorities())
        );

        Long demandeId = 10L;

        Membre membre = Membre.builder().nomComplet("Membre Test").statut(StatutMembre.ACTIF).build();
        membre.setId(99L);

        DemandeCredit demande = DemandeCredit.builder()
                .statut(StatutDemandeCredit.SOUMISE)
                .membre(membre)
                .fraisDemande(new BigDecimal("1000"))
                .fraisDemandePayes(BigDecimal.ZERO)
                .depotGarantieRequis(new BigDecimal("10000"))
                .depotGarantiePaye(BigDecimal.ZERO)
                .devise("CDF")
                .build();
        demande.setId(demandeId);

        Caisse caisse = Caisse.builder().actif(true).build();
        caisse.setId(1L);

        SessionCaisse session = SessionCaisse.builder()
                .statut(StatutSessionCaisse.OUVERTE)
                .caisse(caisse)
                .build();
        session.setId(2L);

        PaiementInitialDemandeCreditRequest request = new PaiementInitialDemandeCreditRequest();
        request.setSessionCaisseId(2L);
        request.setCaisseId(1L);
        request.setDatePaiement(LocalDateTime.now());
        request.setModePaiement(ModePaiement.ESPECES);
        request.setFraisPayes(new BigDecimal("100"));
        request.setDepotGarantiePaye(BigDecimal.ZERO);

        when(demandeCreditRepository.findById(demandeId)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findById(2L)).thenReturn(Optional.of(session));
        when(caisseRepository.findById(1L)).thenReturn(Optional.of(caisse));

        assertThatThrownBy(() -> service.enregistrerPaiementInitial(demandeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("demande APPROUVEE");
    }
}
