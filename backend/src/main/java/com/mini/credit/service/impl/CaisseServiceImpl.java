package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.CaisseCreateRequest;
import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.CaisseService;
import com.mini.credit.service.ReferenceGeneratorService;
import com.mini.credit.service.audit.AuditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CaisseServiceImpl implements CaisseService {

    private static final List<StatutSessionCaisse> STATUTS_SESSION_ACTIVE = List.of(
            StatutSessionCaisse.OUVERTE,
            StatutSessionCaisse.PRE_CLOTUREE,
            StatutSessionCaisse.VALIDEE_CONTROLE
    );

    private final CaisseRepository caisseRepository;
    private final AgenceRepository agenceRepository;
    private final SiteRepository siteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CashMapper cashMapper;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final AuditService auditService;

    @Override
    public CaisseResponse create(CaisseCreateRequest request) {
        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Agence introuvable"));
        if (!Boolean.TRUE.equals(agence.getActif())) {
            throw new BusinessException("Impossible de créer une caisse sur une agence inactive");
        }

        Site site = null;
        if (request.getSiteId() != null) {
            site = siteRepository.findById(request.getSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));

            if (site.getAgence() == null || site.getAgence().getId() == null
                    || !site.getAgence().getId().equals(agence.getId())) {
                throw new BusinessException("Le site sélectionné n'appartient pas à l'agence choisie");
            }
        }

        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("Utilisateur authentifié introuvable");
        }

        RoleCode currentRole = currentUser.getRole() != null ? currentUser.getRole().getCode() : null;
        if (caisseRepository.existsByActifTrueAndAgenceId(request.getAgenceId())) {
            throw new BusinessException("Une caisse active existe déjà pour cette agence");
        }

        if (currentRole == RoleCode.CAISSIER) {
            Long currentUserAgenceId = resolveCurrentUserAgenceId(currentUser);
            if (currentUserAgenceId == null) {
                throw new BusinessException("Le caissier connecté n'est rattaché à aucune agence. Contactez l'administrateur.");
            }

            if (!currentUserAgenceId.equals(request.getAgenceId())) {
                throw new BusinessException("Un caissier ne peut initialiser que la caisse de son agence");
            }
        }

        Utilisateur caissierResponsable = null;
        Long caissierAffecteId = request.getCaissierAffecteId() != null
                ? request.getCaissierAffecteId()
                : request.getCaissierResponsableId();

        if (caissierAffecteId != null) {
            caissierResponsable = utilisateurRepository.findById(caissierAffecteId)
                .orElseThrow(() -> new ResourceNotFoundException("Caissier responsable introuvable"));

            if (caissierResponsable.getRole() == null || caissierResponsable.getRole().getCode() != RoleCode.CAISSIER) {
            throw new BusinessException("L'utilisateur sélectionné n'a pas le rôle CAISSIER");
            }

            Long caissierAgenceId = resolveCurrentUserAgenceId(caissierResponsable);
            if (caissierAgenceId == null || !caissierAgenceId.equals(agence.getId())) {
                throw new BusinessException("Le caissier affecté doit appartenir à la même agence que la caisse");
            }
        } else if (currentRole == RoleCode.CAISSIER) {
            // Pour un caissier qui initialise sa caisse, l'affectation implicite est lui-même.
            caissierResponsable = currentUser;
        }

        String codeCaisse = referenceGeneratorService.genererReference("CAI");

        Caisse caisse = Caisse.builder()
                .codeCaisse(codeCaisse)
                .libelle(request.getLibelle())
            .agence(agence)
                .site(site)
                .caissierResponsable(caissierResponsable)
                .devise(request.getDevise() != null ? request.getDevise() : "CDF")
                .actif(true)
                .build();

        return cashMapper.toResponse(caisseRepository.save(caisse));
    }

    @Override
    public CaisseResponse initialiserMaCaisse() {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null) {
            throw new BusinessException("Utilisateur authentifié introuvable");
        }

        if (currentUser.getRole().getCode() != RoleCode.CAISSIER) {
            throw new BusinessException("Seul un Caissier peut initialiser sa caisse opérationnelle");
        }

        if (currentUser.getEmploye() == null || !Boolean.TRUE.equals(currentUser.getEmploye().getActif())) {
            throw new BusinessException("Le compte caissier doit être lié à un employé actif");
        }

        Agence agence = currentUser.getEmploye().getAgence();
        if (agence == null || agence.getId() == null) {
            throw new BusinessException("Le caissier connecté n'est rattaché à aucune agence. Contactez l'administrateur.");
        }

        if (!Boolean.TRUE.equals(agence.getActif())) {
            throw new BusinessException("Impossible d'initialiser une caisse sur une agence inactive");
        }

        if (caisseRepository.existsByActifTrueAndAgenceId(agence.getId())) {
            throw new BusinessException("Une caisse active existe déjà pour cette agence");
        }

        Caisse caisse = Caisse.builder()
                .codeCaisse(referenceGeneratorService.genererReference("CAI"))
                .libelle("Caisse principale - " + agence.getNomAgence())
                .agence(agence)
                .site(currentUser.getEmploye().getSite())
                .caissierResponsable(currentUser)
                .devise("CDF")
                .actif(true)
                .build();

        Caisse saved = caisseRepository.save(caisse);
        auditService.logInfo(
                AuditAction.CAISSE_CREATED,
                AuditModule.CAISSE,
                "Caisse",
                saved.getId(),
                "Initialisation première caisse Caissier | utilisateur=" + currentUser.getUsername()
                        + " | role=CAISSIER | agence=" + agence.getId(),
                saved.getCodeCaisse()
        );

        return cashMapper.toResponse(saved);
    }

    private Long resolveCurrentUserAgenceId(Utilisateur currentUser) {
        if (currentUser.getEmploye() != null
                && currentUser.getEmploye().getAgence() != null
                && currentUser.getEmploye().getAgence().getId() != null) {
            return currentUser.getEmploye().getAgence().getId();
        }

        if (currentUser.getSite() != null
                && currentUser.getSite().getAgence() != null
                && currentUser.getSite().getAgence().getId() != null) {
            return currentUser.getSite().getAgence().getId();
        }

        if (currentUser.getEmploye() != null
                && currentUser.getEmploye().getSite() != null
                && currentUser.getEmploye().getSite().getAgence() != null
                && currentUser.getEmploye().getSite().getAgence().getId() != null) {
            return currentUser.getEmploye().getSite().getAgence().getId();
        }

        return null;
    }

    @Override
    public CaisseResponse getById(Long id) {
        Caisse caisse = caisseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        verifierAccesCaisse(caisse);
        return enrichWithBalanceIndicators(caisse);
    }

    @Override
    public List<CaisseResponse> getAll() {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() != null
            && currentUser.getRole().getCode() == RoleCode.CAISSIER) {
            return getAccessibles();
        }

        return caisseRepository.findAll().stream()
            .map(this::enrichWithBalanceIndicators)
            .toList();
    }

    @Override
    public List<CaisseResponse> getActives() {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() != null
            && currentUser.getRole().getCode() == RoleCode.CAISSIER) {
            return getAccessibles();
        }

        return caisseRepository.findByActifTrue().stream()
            .map(this::enrichWithBalanceIndicators)
            .toList();
    }

    @Override
    public List<CaisseResponse> getAccessibles() {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null) {
            throw new BusinessException("Utilisateur authentifié introuvable");
        }

        RoleCode role = currentUser.getRole().getCode();
        if (role == RoleCode.CAISSIER) {
            Long currentUserAgenceId = resolveCurrentUserAgenceId(currentUser);
            if (currentUserAgenceId == null) {
                return List.of();
            }
            List<Caisse> caisses = caisseRepository.findByActifTrueAndAgenceId(currentUserAgenceId);
            if (currentUser.getId() != null) {
                List<Caisse> caissesAffectees = caisseRepository.findByActifTrueAndCaissierResponsableId(currentUser.getId());
                caissesAffectees.forEach(caisse -> {
                    if (caisses.stream().noneMatch(existing -> existing.getId().equals(caisse.getId()))) {
                        caisses.add(caisse);
                    }
                });
            }
            return caisses.stream()
                    .map(this::enrichWithBalanceIndicators)
                    .toList();
        }

        return getActives();
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        if (authentication.getPrincipal() instanceof Utilisateur utilisateur) {
            if (utilisateur.getId() != null) {
                return utilisateurRepository.findByIdWithValidationContext(utilisateur.getId()).orElse(utilisateur);
            }
            if (utilisateur.getUsername() != null && !utilisateur.getUsername().isBlank()) {
                return utilisateurRepository.findByUsername(utilisateur.getUsername()).orElse(utilisateur);
            }
            return utilisateur;
        }

        if (authentication.getName() != null && !authentication.getName().isBlank()) {
            return utilisateurRepository.findByUsername(authentication.getName()).orElse(null);
        }

        return null;
    }

    private void verifierAccesCaisse(Caisse caisse) {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null) {
            throw new BusinessException("Utilisateur authentifié introuvable");
        }

        RoleCode role = currentUser.getRole().getCode();
        if (role == RoleCode.ADMIN || role == RoleCode.CONTROLEUR || role == RoleCode.CHEF_BUREAU) {
            return;
        }

        if (role != RoleCode.CAISSIER) {
            throw new BusinessException("Accès caisse refusé pour ce rôle");
        }

        Long userAgenceId = resolveCurrentUserAgenceId(currentUser);
        Long caisseAgenceId = caisse.getAgence() != null ? caisse.getAgence().getId() : null;
        boolean agenceCompatible = userAgenceId != null && caisseAgenceId != null && userAgenceId.equals(caisseAgenceId);
        boolean affectationExplicite = currentUser.getId() != null
                && caisse.getCaissierResponsable() != null
                && caisse.getCaissierResponsable().getId() != null
                && currentUser.getId().equals(caisse.getCaissierResponsable().getId());

        if (!agenceCompatible && !affectationExplicite) {
            throw new BusinessException("Cette caisse n'est pas accessible à ce caissier");
        }
    }

        private CaisseResponse enrichWithBalanceIndicators(Caisse caisse) {
        CaisseResponse base = cashMapper.toResponse(caisse);

        SessionCaisse sessionActive = sessionCaisseRepository
            .findFirstByCaisseIdAndStatutInOrderByDateOuvertureDesc(caisse.getId(), STATUTS_SESSION_ACTIVE)
            .orElse(null);

        SessionCaisse derniereSessionCloturee = sessionCaisseRepository
            .findFirstByCaisseIdAndStatutOrderByDateComptableDescDateClotureDesc(
                caisse.getId(),
                StatutSessionCaisse.CLOTUREE
            )
            .orElse(null);

        BigDecimal soldeTheoriqueSessionOuverte = sessionActive != null
            ? defaultIfNull(sessionActive.getSoldeTheorique())
            : null;

        BigDecimal dernierSoldeCloture = deriveClosingBalance(derniereSessionCloturee);

        BigDecimal soldeDisponibleActuel = soldeTheoriqueSessionOuverte != null
            ? soldeTheoriqueSessionOuverte
            : (dernierSoldeCloture != null ? dernierSoldeCloture : BigDecimal.ZERO);

        String statutSession = sessionActive != null
            ? sessionActive.getStatut().name()
            : (derniereSessionCloturee != null ? StatutSessionCaisse.CLOTUREE.name() : "AUCUNE");

        return base.toBuilder()
            .soldeTheoriqueSessionOuverte(soldeTheoriqueSessionOuverte)
            .dernierSoldeCloture(dernierSoldeCloture)
            .soldeDisponibleActuel(soldeDisponibleActuel)
            .statutSession(statutSession)
            .build();
        }

        private BigDecimal deriveClosingBalance(SessionCaisse session) {
        if (session == null) {
            return null;
        }
        if (session.getSoldePhysique() != null) {
            return session.getSoldePhysique();
        }
        return defaultIfNull(session.getSoldeTheorique());
        }

        private BigDecimal defaultIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
        }
}