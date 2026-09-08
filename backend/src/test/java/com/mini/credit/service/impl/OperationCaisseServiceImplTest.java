package com.mini.credit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeEvenementAuditOperation;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.ReferenceGeneratorService;
import com.mini.credit.service.SessionCaisseValidationService;
import com.mini.credit.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperationCaisseServiceImplTest {

    @Mock private OperationCaisseRepository operationCaisseRepository;
    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private CaisseRepository caisseRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private CreditRepository creditRepository;
    @Mock private RemboursementCreditRepository remboursementCreditRepository;
    @Mock private OperationEpargneRepository operationEpargneRepository;
    @Mock private AgentTerrainRepository agentTerrainRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ReferenceGeneratorService referenceGeneratorService;
    @Mock private CashMapper cashMapper;
    @Mock private AuditService auditService;
    @Mock private SessionCaisseValidationService sessionCaisseValidationService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private OperationCaisseServiceImpl service;

    private SessionCaisse session;
    private Caisse caisse;

    @BeforeEach
    void setUp() {
        Agence agence = Agence.builder().nomAgence("Agence 1").codeAgence("AG1").actif(true).build();
        agence.setId(1L);

        Site site = Site.builder().nomSite("Site 1").agence(agence).build();
        site.setId(11L);

        setCurrentUser(RoleCode.ADMIN, site);

        caisse = Caisse.builder().actif(true).agence(agence).build();
        caisse.setId(1L);

        session = new SessionCaisse();
        session.setId(10L);
        session.setCaisse(caisse);
        session.setStatut(StatutSessionCaisse.OUVERTE);
        session.setDateCloture(null);
        session.setSoldeOuverture(new BigDecimal("1000.00"));
        session.setTotalEntrees(BigDecimal.ZERO);
        session.setTotalSorties(BigDecimal.ZERO);
        session.setSoldeTheorique(new BigDecimal("1000.00"));

        when(sessionCaisseRepository.findById(10L)).thenReturn(Optional.of(session));
        when(caisseRepository.findById(1L)).thenReturn(Optional.of(caisse));
        when(referenceGeneratorService.genererReference("PCS")).thenReturn("PCS-001");
        when(operationCaisseRepository.save(any(OperationCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cashMapper.toResponse(any(OperationCaisse.class))).thenReturn(OperationCaisseResponse.builder().id(99L).build());
    }

    @Test
    void entree_updatesTotalEntrees_andSoldeTheorique() {
        OperationCaisseRequest request = baseRequest();
        request.setTypeOperation(TypeOperationCaisse.ENTREE);
        request.setCategorieOperation(CategorieOperationCaisse.COTISATION);
        request.setMontant(new BigDecimal("200.00"));
        request.setSource(SourceOperationCaisse.RECETTE_JOURNALIERE);
        request.setRecetteId(1L);

        service.enregistrer(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());

        assertThat(captor.getValue().getTotalEntrees()).isEqualByComparingTo("200.00");
        assertThat(captor.getValue().getTotalSorties()).isEqualByComparingTo("0.00");
        assertThat(captor.getValue().getSoldeTheorique()).isEqualByComparingTo("1200.00");
    }

    @Test
    void sortie_updatesTotalSorties_andSoldeTheorique() {
        OperationCaisseRequest request = baseRequest();
        request.setTypeOperation(TypeOperationCaisse.SORTIE);
        request.setCategorieOperation(CategorieOperationCaisse.RETRAIT_EPARGNE);
        request.setMontant(new BigDecimal("150.00"));
        request.setSource(SourceOperationCaisse.RETRAIT_EPARGNE);

        service.enregistrer(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());

        assertThat(captor.getValue().getTotalSorties()).isEqualByComparingTo("150.00");
        assertThat(captor.getValue().getTotalEntrees()).isEqualByComparingTo("0.00");
        assertThat(captor.getValue().getSoldeTheorique()).isEqualByComparingTo("850.00");
    }

    @Test
    void observation_isPersisted_inOperationCaisse() {
        OperationCaisseRequest request = baseRequest();
        request.setTypeOperation(TypeOperationCaisse.ENTREE);
        request.setCategorieOperation(CategorieOperationCaisse.COTISATION);
        request.setMontant(new BigDecimal("50.00"));
        request.setSource(SourceOperationCaisse.MANUEL);
        request.setObservation("Test observation P1");

        service.enregistrer(request);

        ArgumentCaptor<OperationCaisse> captor = ArgumentCaptor.forClass(OperationCaisse.class);
        verify(operationCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getObservation()).isEqualTo("Test observation P1");
    }

    @Test
    void noFlowShouldBypass_operationCaisseService() throws IOException {
        assertThat(hasFieldOfType(DemandeRetraitEpargneServiceImpl.class, OperationCaisseService.class)).isTrue();
        assertThat(hasFieldOfType(PaiementSalaireServiceImpl.class, OperationCaisseService.class)).isTrue();
        assertThat(readsOperationCaisseRepository(DemandeRetraitEpargneServiceImpl.class)).isTrue();
        assertThat(writesOperationCaisseDirectly(DemandeRetraitEpargneServiceImpl.class)).isFalse();
        assertThat(writesOperationCaisseDirectly(PaiementSalaireServiceImpl.class)).isFalse();
    }

    @Test
    void rciCannotCreateFreeCashOperation() {
        setCurrentUser(RoleCode.RCI, session.getCaisse().getAgence() != null ? Site.builder().agence(session.getCaisse().getAgence()).build() : null);
        OperationCaisseRequest request = freeCashRequest();

        assertThatThrownBy(() -> service.enregistrer(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("réservée à ADMIN ou CHEF_BUREAU");
    }

    @Test
    void adminCanCreateFreeCashOperation() {
        OperationCaisseRequest request = freeCashRequest();

        service.enregistrer(request);

        ArgumentCaptor<OperationCaisse> captor = ArgumentCaptor.forClass(OperationCaisse.class);
        verify(operationCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getCategorieOperation()).isEqualTo(CategorieOperationCaisse.ENTREE_DIVERSE);
    }

    @Test
    void chefBureauCanCreateFreeCashOperation() {
        setCurrentUser(RoleCode.CHEF_BUREAU, session.getCaisse().getAgence() != null ? Site.builder().agence(session.getCaisse().getAgence()).build() : null);
        OperationCaisseRequest request = freeCashRequest();

        service.enregistrer(request);

        ArgumentCaptor<OperationCaisse> captor = ArgumentCaptor.forClass(OperationCaisse.class);
        verify(operationCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getCategorieOperation()).isEqualTo(CategorieOperationCaisse.ENTREE_DIVERSE);
    }

    @Test
    void caissierNePeutPasCreerOperationLibreGenerique() {
        setCurrentUser(RoleCode.CAISSIER, session.getCaisse().getAgence() != null ? Site.builder().agence(session.getCaisse().getAgence()).build() : null);
        OperationCaisseRequest request = freeCashRequest();

        assertThatThrownBy(() -> service.enregistrer(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("interdite au CAISSIER");
    }

    @Test
    void adminOuChefBureauPeutOperationLibreSiRegleExistante() {
        setCurrentUser(RoleCode.ADMIN, session.getCaisse().getAgence() != null ? Site.builder().agence(session.getCaisse().getAgence()).build() : null);
        service.enregistrer(freeCashRequest());

        setCurrentUser(RoleCode.CHEF_BUREAU, session.getCaisse().getAgence() != null ? Site.builder().agence(session.getCaisse().getAgence()).build() : null);
        service.enregistrer(freeCashRequest());

        verify(operationCaisseRepository, atLeastOnce()).save(any(OperationCaisse.class));
    }

    @Test
    void operationLibreDoitAvoirMotif() {
        OperationCaisseRequest request = freeCashRequest();
        request.setObservation(" ");
        request.setDescription(null);
        request.setCommentaire(null);

        assertThatThrownBy(() -> service.enregistrer(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("motif/commentaire");
    }

    @Test
    void operationLibreDoitEtreAuditeeEtTracee() {
        OperationCaisseRequest request = freeCashRequest();
        request.setDescription("Correction manuelle validée");
        request.setReferenceMetier("AJUSTEMENT-TEST-1");
        request.setSource(SourceOperationCaisse.AJUSTEMENT);
        request.setTypeEvenementAudit(TypeEvenementAuditOperation.MODIFICATION_OPERATION);

        service.enregistrer(request);

        ArgumentCaptor<OperationCaisse> captor = ArgumentCaptor.forClass(OperationCaisse.class);
        verify(operationCaisseRepository).save(captor.capture());
        OperationCaisse operation = captor.getValue();
        assertThat(operation.getCreatedBy()).isNotNull();
        assertThat(operation.getUtilisateur()).isNotNull();
        assertThat(operation.getSessionCaisse()).isEqualTo(session);
        assertThat(operation.getCaisse()).isEqualTo(caisse);
        assertThat(operation.getReferenceMetier()).isEqualTo("AJUSTEMENT-TEST-1");
        verify(auditService, atLeastOnce()).logWithValues(any(), any(), any(), any(Boolean.class), any(), any(), any(), any());
    }

    private OperationCaisseRequest baseRequest() {
        OperationCaisseRequest req = new OperationCaisseRequest();
        req.setSessionCaisseId(10L);
        req.setCaisseId(1L);
        req.setDateOperation(LocalDateTime.now());
        return req;
    }

    private OperationCaisseRequest freeCashRequest() {
        OperationCaisseRequest request = baseRequest();
        request.setTypeOperation(TypeOperationCaisse.ENTREE);
        request.setCategorieOperation(CategorieOperationCaisse.ENTREE_DIVERSE);
        request.setMontant(new BigDecimal("75.00"));
        request.setSource(SourceOperationCaisse.MANUEL);
        request.setObservation("Correction caisse validée");
        return request;
    }

    private void setCurrentUser(RoleCode roleCode, Site site) {
        Utilisateur user = Utilisateur.builder()
            .username(roleCode.name().toLowerCase())
            .nomComplet(roleCode.name())
            .motDePasseHash("hash")
            .role(Role.builder().code(roleCode).libelle(roleCode.name()).build())
            .site(site)
            .build();
        user.setId(100L + roleCode.ordinal());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private boolean hasFieldOfType(Class<?> target, Class<?> expectedType) {
        for (Field field : target.getDeclaredFields()) {
            if (field.getType().equals(expectedType)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFieldNamed(Class<?> target, String name) {
        for (Field field : target.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean readsOperationCaisseRepository(Class<?> target) throws IOException {
        String source = readSource(target);
        return source.contains("operationCaisseRepository.find");
    }

    private boolean writesOperationCaisseDirectly(Class<?> target) throws IOException {
        String source = readSource(target);
        return source.contains("operationCaisseRepository.save(")
            || source.contains("operationCaisseRepository.saveAndFlush(")
            || source.contains("operationCaisseRepository.delete(")
            || source.contains("entityManager.persist(")
            || source.matches("(?s).*@Query\\s*\\([^)]*(insert|update|delete)\\s+[^)]*operation_caisse.*");
    }

    private String readSource(Class<?> target) throws IOException {
        Path sourcePath = Path.of("src", "main", "java", target.getName().replace('.', '/') + ".java");
        return Files.readString(sourcePath);
    }
}
