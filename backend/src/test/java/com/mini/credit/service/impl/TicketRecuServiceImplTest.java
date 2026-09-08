package com.mini.credit.service.impl;

import com.mini.credit.dto.document.TicketDuplicataRequest;
import com.mini.credit.dto.document.TicketPrintRequest;
import com.mini.credit.dto.document.TicketRecuGenerationRequest;
import com.mini.credit.dto.document.TicketRecuResponse;
import com.mini.credit.dto.document.TicketVerificationResponse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.document.TicketRecu;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutTicketRecu;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.TypeTicketRecu;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.document.TicketRecuRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.security.ScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketRecuServiceImplTest {

    @Mock private TicketRecuRepository ticketRecuRepository;
    @Mock private OperationEpargneRepository operationEpargneRepository;
    @Mock private OperationCaisseRepository operationCaisseRepository;
    @Mock private DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;
    @Mock private CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;
    @Mock private CollecteMembreLigneRepository collecteMembreLigneRepository;
    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private CaisseRepository caisseRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private CompteEpargneRepository compteEpargneRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ScopeService scopeService;
    @Mock private AuditService auditService;

    @InjectMocks private TicketRecuServiceImpl service;

    private Membre membre;
    private CompteEpargne compte;
    private OperationEpargne operationEpargne;
    private OperationCaisse operationCaisse;
    private SessionCaisse session;
    private Caisse caisse;
    private Utilisateur admin;

    @BeforeEach
    void setUp() {
        Agence agence = Agence.builder().codeAgence("DELVAUX").nomAgence("DELVAUX").build();
        agence.setId(10L);
        Site site = Site.builder().codeSite("MAT").nomSite("MATERNITE").agence(agence).build();
        site.setId(20L);
        membre = Membre.builder().codeMembre("MB-001").nomComplet("Membre Test").nom("Test").site(site).build();
        membre.setId(1L);
        compte = CompteEpargne.builder()
                .membre(membre)
                .numeroCompte("EP-00001234")
                .typeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE)
                .soldeDisponible(new BigDecimal("150000.00"))
                .build();
        compte.setId(2L);
        caisse = Caisse.builder().codeCaisse("CAISSE-001").libelle("Caisse 1").agence(agence).site(site).build();
        caisse.setId(3L);
        session = SessionCaisse.builder().caisse(caisse).build();
        session.setId(4L);
        operationEpargne = OperationEpargne.builder()
                .membre(membre)
                .compteEpargne(compte)
                .typeOperation(TypeOperationEpargne.EPARGNE)
                .montant(new BigDecimal("50000.00"))
                .sessionCaisse(session)
                .build();
        operationEpargne.setId(5L);
        operationCaisse = OperationCaisse.builder().sessionCaisse(session).caisse(caisse).membre(membre).operationEpargne(operationEpargne).build();
        operationCaisse.setId(6L);
        admin = Utilisateur.builder()
                .username("admin")
                .nomComplet("Admin Test")
                .role(Role.builder().code(RoleCode.ADMIN).build())
                .build();
        admin.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        AtomicLong ids = new AtomicLong(100L);
        lenient().when(ticketRecuRepository.save(any(TicketRecu.class))).thenAnswer(inv -> {
            TicketRecu ticket = inv.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(ids.getAndIncrement());
            }
            return ticket;
        });
        lenient().when(ticketRecuRepository.existsByNumeroTicket(any())).thenReturn(false);
        lenient().when(ticketRecuRepository.existsByCodeVerification(any())).thenReturn(false);
    }

    @Test
    void depotEpargneGenereTicket() {
        mockGenerationLookups();

        TicketRecuResponse response = service.genererDepuisOperation(baseDepotRequest());

        assertThat(response.getTypeTicket()).isEqualTo(TypeTicketRecu.DEPOT_EPARGNE);
        assertThat(response.getMontantPrincipal()).isEqualByComparingTo("50000.00");
        assertThat(response.getAncienSolde()).isEqualByComparingTo("100000.00");
        assertThat(response.getNouveauSolde()).isEqualByComparingTo("150000.00");
        assertThat(response.getNumeroTicket()).startsWith("TIC-");
        assertThat(response.getCodeVerification()).isNotBlank();
        verify(auditService).logAction(eq(AuditAction.TICKET_RECU_GENERATED), any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void impossibleCreerTicketSansOperation() {
        TicketRecuGenerationRequest request = TicketRecuGenerationRequest.builder()
                .typeTicket(TypeTicketRecu.DEPOT_EPARGNE)
                .membreId(1L)
                .compteEpargneId(2L)
                .montantPrincipal(BigDecimal.TEN)
                .ancienSolde(BigDecimal.ZERO)
                .nouveauSolde(BigDecimal.TEN)
                .build();

        assertThatThrownBy(() -> service.genererDepuisOperation(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("opération épargne");
    }

    @Test
    void numeroTicketUniqueEtCodeVerificationUnique() {
        mockGenerationLookups();

        TicketRecuResponse response = service.genererDepuisOperation(baseDepotRequest());

        assertThat(response.getNumeroTicket()).isNotBlank();
        assertThat(response.getCodeVerification()).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
    }

    @Test
    void duplicataExigeMotif() {
        assertThatThrownBy(() -> service.genererDuplicata(100L, new TicketDuplicataRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motif");
    }

    @Test
    void duplicataMarqueDuplicataEtIncrementeCompteur() {
        TicketRecu original = originalTicket();
        when(ticketRecuRepository.findById(100L)).thenReturn(Optional.of(original));
        TicketDuplicataRequest request = new TicketDuplicataRequest();
        request.setMotif("Ticket perdu par le membre");

        TicketRecuResponse duplicata = service.genererDuplicata(100L, request);

        assertThat(duplicata.getDuplicata()).isTrue();
        assertThat(duplicata.getTypeTicket()).isEqualTo(TypeTicketRecu.DUPLICATA);
        assertThat(duplicata.getNumeroTicket()).contains("-D01");
        assertThat(original.getNombreDuplicatas()).isEqualTo(1);
    }

    @Test
    void impressionTraceAudit() {
        TicketRecu ticket = originalTicket();
        when(ticketRecuRepository.findById(100L)).thenReturn(Optional.of(ticket));
        TicketPrintRequest request = new TicketPrintRequest();
        request.setImpressionReussie(true);

        TicketRecuResponse response = service.marquerImpression(100L, request);

        assertThat(response.getStatut()).isEqualTo(StatutTicketRecu.IMPRIME);
        assertThat(response.getNombreImpressions()).isEqualTo(1);
        verify(auditService).logAction(eq(AuditAction.TICKET_RECU_PRINTED), any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void echecImpressionTraceAudit() {
        TicketRecu ticket = originalTicket();
        when(ticketRecuRepository.findById(100L)).thenReturn(Optional.of(ticket));
        TicketPrintRequest request = new TicketPrintRequest();
        request.setImpressionReussie(false);

        TicketRecuResponse response = service.marquerImpression(100L, request);

        assertThat(response.getStatut()).isEqualTo(StatutTicketRecu.ECHEC_IMPRESSION);
        verify(auditService).logAction(eq(AuditAction.TICKET_RECU_PRINT_FAILED), any(), any(), any(), eq(false), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void verificationRetourneDonneesLimitees() {
        TicketRecu ticket = originalTicket();
        when(ticketRecuRepository.findByCodeVerification("ABCD-2345")).thenReturn(Optional.of(ticket));

        TicketVerificationResponse response = service.verify("ABCD-2345");

        assertThat(response.isValide()).isTrue();
        assertThat(response.getMembreMasque()).contains("***");
        assertThat(response.getNumeroTicket()).isEqualTo("TIC-20260729-DELVAUX-000001");
    }

    @Test
    void impossibleModifierMontantsTicketGenere() {
        assertThat(TicketRecuServiceImpl.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("modifiermontant"));
    }

    private void mockGenerationLookups() {
        when(ticketRecuRepository.findByOperationEpargneIdAndTypeTicketAndOriginalTicketIsNull(5L, TypeTicketRecu.DEPOT_EPARGNE))
                .thenReturn(Optional.empty());
        when(operationEpargneRepository.findById(5L)).thenReturn(Optional.of(operationEpargne));
        when(operationCaisseRepository.findById(6L)).thenReturn(Optional.of(operationCaisse));
        when(sessionCaisseRepository.findById(4L)).thenReturn(Optional.of(session));
        when(caisseRepository.findById(3L)).thenReturn(Optional.of(caisse));
        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(compteEpargneRepository.findById(2L)).thenReturn(Optional.of(compte));
        when(utilisateurRepository.findById(7L)).thenReturn(Optional.of(admin));
    }

    private TicketRecuGenerationRequest baseDepotRequest() {
        return TicketRecuGenerationRequest.builder()
                .typeTicket(TypeTicketRecu.DEPOT_EPARGNE)
                .operationEpargneId(5L)
                .operationCaisseId(6L)
                .sessionCaisseId(4L)
                .caisseId(3L)
                .membreId(1L)
                .compteEpargneId(2L)
                .utilisateurCreateurId(7L)
                .devise("CDF")
                .montantPrincipal(new BigDecimal("50000.00"))
                .ancienSolde(new BigDecimal("100000.00"))
                .nouveauSolde(new BigDecimal("150000.00"))
                .build();
    }

    private TicketRecu originalTicket() {
        TicketRecu ticket = TicketRecu.builder()
                .numeroTicket("TIC-20260729-DELVAUX-000001")
                .typeTicket(TypeTicketRecu.DEPOT_EPARGNE)
                .statut(StatutTicketRecu.GENERE)
                .membre(membre)
                .compteEpargne(compte)
                .operationEpargne(operationEpargne)
                .operationCaisse(operationCaisse)
                .sessionCaisse(session)
                .caisse(caisse)
                .agence(caisse.getAgence())
                .site(caisse.getSite())
                .utilisateurCreateur(admin)
                .devise("CDF")
                .montantPrincipal(new BigDecimal("50000.00"))
                .ancienSolde(new BigDecimal("100000.00"))
                .nouveauSolde(new BigDecimal("150000.00"))
                .codeVerification("ABCD-2345")
                .qrPayload("TICKET:TIC-20260729-DELVAUX-000001:ABCD-2345")
                .dateGeneration(java.time.LocalDateTime.now())
                .build();
        ticket.setId(100L);
        return ticket;
    }
}
