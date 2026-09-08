package com.mini.credit.service;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
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
        void caissierEncaisseFraisAvecSessionActive() {
        com.mini.credit.entity.referentiel.Utilisateur caissier = com.mini.credit.entity.referentiel.Utilisateur.builder()
                .username("caissier")
                .role(com.mini.credit.entity.referentiel.Role.builder().code(RoleCode.CAISSIER).build())
                .build();
                caissier.setId(7L);
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
        when(sessionCaisseRepository.findByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(7L, LocalDate.now(), StatutSessionCaisse.OUVERTE))
                .thenReturn(List.of(session));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operationCaisseService.enregistrer(any(OperationCaisseRequest.class))).thenReturn(OperationCaisseResponse.builder()
                .sessionCaisseId(2L)
                .caisseId(1L)
                .typeOperation(TypeOperationCaisse.ENTREE)
                .categorieOperation(CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT)
                .montant(new BigDecimal("100.00"))
                .utilisateurId(7L)
                .referenceMetier("DEMANDE_CREDIT:10")
                .build());

        service.enregistrerPaiementInitial(demandeId, request);

        assertThat(demande.getFraisDemandePayes()).isEqualByComparingTo("100.00");
        ArgumentCaptor<OperationCaisseRequest> operationCaptor = ArgumentCaptor.forClass(OperationCaisseRequest.class);
        verify(operationCaisseService).enregistrer(operationCaptor.capture());
        OperationCaisseRequest operation = operationCaptor.getValue();
        assertThat(operation.getTypeOperation()).isEqualTo(TypeOperationCaisse.ENTREE);
        assertThat(operation.getCategorieOperation()).isEqualTo(CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT);
        assertThat(operation.getSessionCaisseId()).isEqualTo(2L);
        assertThat(operation.getCaisseId()).isEqualTo(1L);
        assertThat(operation.getUtilisateurId()).isEqualTo(7L);
        assertThat(operation.getReferenceMetier()).isEqualTo("DEMANDE_CREDIT:10");
    }

    @Test
    void paiementFraisSuperieurAuResteRefuse() {
        com.mini.credit.entity.referentiel.Utilisateur caissier = caissierConnecte();
        DemandeCredit demande = demandeFrais(11L, new BigDecimal("7500"), new BigDecimal("3000"));
        SessionCaisse session = sessionOuverte();
        PaiementInitialDemandeCreditRequest request = requestFrais(new BigDecimal("5000"), BigDecimal.ZERO);

        when(demandeCreditRepository.findById(11L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(caissier.getId(), LocalDate.now(), StatutSessionCaisse.OUVERTE))
                .thenReturn(List.of(session));

        assertThatThrownBy(() -> service.enregistrerPaiementInitial(11L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépasse le reste à payer");
    }

    @Test
    void paiementFraisSansSessionRefuse() {
        com.mini.credit.entity.referentiel.Utilisateur caissier = caissierConnecte();
        DemandeCredit demande = demandeFrais(12L, new BigDecimal("7500"), BigDecimal.ZERO);

        when(demandeCreditRepository.findById(12L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(caissier.getId(), LocalDate.now(), StatutSessionCaisse.OUVERTE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.enregistrerPaiementInitial(12L, requestFrais(new BigDecimal("1000"), BigDecimal.ZERO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aucune session caisse ouverte");
    }

    @Test
    void depotGarantiePayeRefuseSurEcranFrais() {
        com.mini.credit.entity.referentiel.Utilisateur caissier = caissierConnecte();
        DemandeCredit demande = demandeFrais(13L, new BigDecimal("7500"), BigDecimal.ZERO);
        SessionCaisse session = sessionOuverte();

        when(demandeCreditRepository.findById(13L)).thenReturn(Optional.of(demande));
        when(sessionCaisseRepository.findByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(caissier.getId(), LocalDate.now(), StatutSessionCaisse.OUVERTE))
                .thenReturn(List.of(session));

        assertThatThrownBy(() -> service.enregistrerPaiementInitial(13L, requestFrais(new BigDecimal("1000"), new BigDecimal("2000"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépôt de garantie n'est pas concerné");

        assertThat(demande.getDepotGarantiePaye()).isEqualByComparingTo("0.00");
    }

    private com.mini.credit.entity.referentiel.Utilisateur caissierConnecte() {
        com.mini.credit.entity.referentiel.Utilisateur caissier = com.mini.credit.entity.referentiel.Utilisateur.builder()
                .username("caissier")
                .role(com.mini.credit.entity.referentiel.Role.builder().code(RoleCode.CAISSIER).build())
                .build();
        caissier.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caissier, null, caissier.getAuthorities())
        );
        return caissier;
    }

    private DemandeCredit demandeFrais(Long id, BigDecimal frais, BigDecimal payes) {
        Membre membre = Membre.builder().nomComplet("Membre Test").statut(StatutMembre.ACTIF).build();
        membre.setId(99L);
        DemandeCredit demande = DemandeCredit.builder()
                .numeroDemande("DCR-" + id)
                .statut(StatutDemandeCredit.SOUMISE)
                .membre(membre)
                .fraisDemande(frais)
                .fraisDemandePayes(payes)
                .depotGarantieRequis(new BigDecimal("10000"))
                .depotGarantiePaye(BigDecimal.ZERO)
                .devise("CDF")
                .build();
        demande.setId(id);
        return demande;
    }

    private SessionCaisse sessionOuverte() {
        Caisse caisse = Caisse.builder().actif(true).build();
        caisse.setId(1L);
        SessionCaisse session = SessionCaisse.builder()
                .statut(StatutSessionCaisse.OUVERTE)
                .caisse(caisse)
                .build();
        session.setId(2L);
        return session;
    }

    private PaiementInitialDemandeCreditRequest requestFrais(BigDecimal frais, BigDecimal depotGarantie) {
        PaiementInitialDemandeCreditRequest request = new PaiementInitialDemandeCreditRequest();
        request.setDatePaiement(LocalDateTime.now());
        request.setModePaiement(ModePaiement.ESPECES);
        request.setFraisPayes(frais);
        request.setDepotGarantiePaye(depotGarantie);
        return request;
    }
}
