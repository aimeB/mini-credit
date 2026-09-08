package com.mini.credit.service.impl;

import com.mini.credit.constants.PaiePersonnelConstants;
import com.mini.credit.dto.caisse.*;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DepenseCaisseCategorie;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.ModeCalculPaie;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeChargeFixe;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.TypePaiementPersonnel;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.DepenseCaisseMapper;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.referentiel.TransportSiteParametreRepository;
import com.mini.credit.service.DepenseCaisseService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.SessionCaisseValidationService;
import com.mini.credit.service.audit.AuditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class DepenseCaisseServiceImpl implements DepenseCaisseService {

    private final DepenseCaisseRepository depenseCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final OperationCaisseService operationCaisseService;
    private final SessionCaisseValidationService sessionCaisseValidationService;
    private final DepenseCaisseMapper depenseCaisseMapper;
        private final UtilisateurRepository utilisateurRepository;
        private final EmployeRepository employeRepository;
        private final AgentTerrainRepository agentTerrainRepository;
        private final CollecteMembreLigneRepository collecteMembreLigneRepository;
        private final SiteRepository siteRepository;
        private final TransportSiteParametreRepository transportSiteParametreRepository;
    private final AuditService auditService;

        private static final List<RoleCode> AGENCY_SALARY_ROLES = List.of(
            RoleCode.CHEF_BUREAU,
            RoleCode.CONTROLEUR,
            RoleCode.CAISSIER,
            RoleCode.GESTIONNAIRE,
            RoleCode.AGENT_TERRAIN
        );

        private static final List<DepenseCaisseStatus> PAYROLL_LOCK_STATUSES = List.of(
            DepenseCaisseStatus.VALIDEE,
            DepenseCaisseStatus.PAYEE
        );

    @Override
    public DepenseCaisseResponse creer(DepenseCaisseCreateRequest request) {
        if (request == null) {
            throw new BusinessException("La requête de dépense caisse est obligatoire");
        }
        if (request.getCaisseId() == null) {
            throw new BusinessException("La caisse est obligatoire");
        }
        if (request.getCategorie() == null) {
            throw new BusinessException("La catégorie de dépense est obligatoire");
        }
        if (cleanNullableText(request.getMotif()) == null) {
            throw new BusinessException("Le motif de la dépense est obligatoire");
        }

        Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        PersonnelPaymentContext personnelPayment = resolvePersonnelPayment(request, caisse);
        BigDecimal montant = personnelPayment != null ? personnelPayment.montantPaye() : request.getMontant();
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de la dépense doit être supérieur à zéro");
        }

        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("Utilisateur authentifié introuvable");
        }

        DepenseCaisse depense = DepenseCaisse.builder()
                .caisse(caisse)
                .site(caisse.getSite())
                .categorie(request.getCategorie())
                .montant(montant)
                .devise(normalizeDevise(request.getDevise()))
                .motif(cleanNullableText(request.getMotif()))
                .beneficiaire(resolveBeneficiaireTexte(request, personnelPayment))
                .beneficiaireUtilisateur(personnelPayment != null && personnelPayment.employe().getUtilisateur() != null ? personnelPayment.employe().getUtilisateur() : null)
                .beneficiaireNom(personnelPayment != null ? personnelPayment.employe().getNomComplet() : null)
                .beneficiaireRole(personnelPayment != null ? personnelPayment.roleLabel() : null)
                .beneficiaireAgence(personnelPayment != null ? personnelPayment.agenceNom() : null)
                .employe(personnelPayment != null ? personnelPayment.employe() : null)
                .periodePaie(personnelPayment != null ? personnelPayment.periodePaie() : null)
                .typePaiementPersonnel(personnelPayment != null ? personnelPayment.typePaiementPersonnel() : null)
                .montantRemunerationReference(personnelPayment != null ? personnelPayment.reference() : null)
                .montantEcartRemuneration(personnelPayment != null ? personnelPayment.ecart() : null)
                .motifEcartRemuneration(personnelPayment != null ? personnelPayment.motifEcart() : null)
                .naturePaiementPaie(personnelPayment != null ? personnelPayment.typePaiementPersonnel().name() : null)
                .montantSalaireDu(personnelPayment != null ? personnelPayment.reference() : null)
                .montantDejaPaye(personnelPayment != null ? personnelPayment.montantDejaPayeAvant() : null)
                .montantRestantApresPaiement(personnelPayment != null ? personnelPayment.resteApresPaiement() : null)
                .montantRetenue(personnelPayment != null ? personnelPayment.montantRetenue() : null)
                .motifRetenue(personnelPayment != null ? personnelPayment.motifRetenue() : null)
                .motifPaiementPartiel(personnelPayment != null ? personnelPayment.motifPaiementPartiel() : null)
                .commentairePaie(personnelPayment != null ? personnelPayment.commentairePaie() : null)
                .epargneCollecteeReference(personnelPayment != null ? personnelPayment.preview().getEpargneCollecteeValidee() : null)
                .remboursementCollecteReference(personnelPayment != null ? personnelPayment.preview().getRemboursementCreditCollecteValide() : null)
                .nombreCarnetsVendus(personnelPayment != null ? personnelPayment.preview().getNombreCarnetsVendus() : null)
                .primeMobilisationEpargne(personnelPayment != null ? personnelPayment.preview().getPrimeMobilisationEpargne() : null)
                .primeMobilisationRemboursement(personnelPayment != null ? personnelPayment.preview().getPrimeMobilisationRemboursement() : null)
                .bonusCarnets(personnelPayment != null ? personnelPayment.preview().getBonusCarnets() : null)
                .primeMotivationManuelle(personnelPayment != null ? personnelPayment.preview().getPrimeMotivationManuelle() : null)
                .motifPrimeMotivationManuelle(personnelPayment != null ? personnelPayment.motifPrimeMotivation() : null)
                .modeCalculPaie(personnelPayment != null ? personnelPayment.preview().getModeCalcul() : null)
                .detailCalculPaieJson(personnelPayment != null ? buildPaieDetailJson(personnelPayment.preview()) : null)
                .justificatifUrl(cleanNullableText(request.getJustificatifUrl()))
                .statut(DepenseCaisseStatus.BROUILLON)
                .demandePar(currentUser)
                .dateDemande(LocalDateTime.now())
                .build();

        depense = depenseCaisseRepository.save(depense);
        auditService.logSuccess(AuditAction.DEPENSE_CAISSE_CREATED, "DepenseCaisse", depense.getId(), "Création de dépense caisse");
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public List<DepenseCaisseBeneficiaireSalaireResponse> getBeneficiairesSalaire(Long caisseId) {
        if (caisseId == null) {
            throw new BusinessException("La caisse est obligatoire");
        }

        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        Long agenceId = caisse.getAgence() != null ? caisse.getAgence().getId() : null;
        Map<Long, DepenseCaisseBeneficiaireSalaireResponse> options = new LinkedHashMap<>();

        for (Employe employe : employeRepository.findByAgenceIdAndActifTrue(agenceId)) {
            if (employe.getId() == null || Boolean.FALSE.equals(employe.getActif())) {
                continue;
            }
            options.put(employe.getId(), toBeneficiaireResponse(employe));
        }

        return options.values().stream()
                .sorted(Comparator.comparing(DepenseCaisseBeneficiaireSalaireResponse::getAffichage, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public PaieEmployePreviewResponse getPaiePreview(Long employeId, String periodePaie) {
        if (employeId == null) {
            throw new BusinessException("L'employé est obligatoire");
        }
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new BusinessException("Employé introuvable"));
        if (Boolean.FALSE.equals(employe.getActif())) {
            throw new BusinessException("Impossible de prévisualiser la paie d'un employé inactif");
        }
        return buildPaiePreview(employe, periodePaie, BigDecimal.ZERO);
    }

    @Override
    public List<DepenseCaisseResponse> getAll(String statut, Long caisseId, Long siteId, Long sessionCaisseId, LocalDate dateDebut, LocalDate dateFin) {
        DepenseCaisseStatus statutFilter = parseStatut(statut);

        List<DepenseCaisse> depenses = depenseCaisseRepository.findAllByOrderByDateDemandeDesc();
        List<DepenseCaisse> filtered = new ArrayList<>();

        for (DepenseCaisse depense : depenses) {
            if (statutFilter != null && depense.getStatut() != statutFilter) {
                continue;
            }
            if (caisseId != null && (depense.getCaisse() == null || !caisseId.equals(depense.getCaisse().getId()))) {
                continue;
            }
            if (siteId != null && (depense.getSite() == null || !siteId.equals(depense.getSite().getId()))) {
                continue;
            }
            if (sessionCaisseId != null && (depense.getSessionCaisse() == null || !sessionCaisseId.equals(depense.getSessionCaisse().getId()))) {
                continue;
            }
            if (dateDebut != null && depense.getDateDemande() != null && depense.getDateDemande().toLocalDate().isBefore(dateDebut)) {
                continue;
            }
            if (dateFin != null && depense.getDateDemande() != null && depense.getDateDemande().toLocalDate().isAfter(dateFin)) {
                continue;
            }
            filtered.add(depense);
        }

        return filtered.stream().map(depenseCaisseMapper::toResponse).toList();
    }

    @Override
    public DepenseCaisseResponse getById(Long id) {
        DepenseCaisse depense = depenseCaisseRepository.findByIdWithContext(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dépense caisse introuvable"));
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public DepenseCaisseResponse rattacherPaie(Long id, DepenseCaisseRattachementPaieRequest request) {
        if (request == null) {
            throw new BusinessException("La demande de rattachement paie est obligatoire");
        }
        String commentaireCorrection = cleanNullableText(request.getCommentaireCorrection());
        if (commentaireCorrection == null) {
            throw new BusinessException("Le commentaire de correction est obligatoire");
        }
        DepenseCaisse depense = loadDepense(id);
        if (depense.getCategorie() != DepenseCaisseCategorie.SALAIRE) {
            throw new BusinessException("Seules les dépenses de catégorie SALAIRE peuvent être rattachées à une paie");
        }
        if (depense.getMontant() == null || depense.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La dépense à rattacher doit avoir un montant positif");
        }

        Employe employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new BusinessException("Employé introuvable"));
        if (Boolean.FALSE.equals(employe.getActif())) {
            throw new BusinessException("Impossible de rattacher une paie à un employé inactif");
        }
        String periodePaie = parsePeriodePaie(request.getPeriodePaie()).toString();
        PaieEmployePreviewResponse preview = buildPaiePreview(employe, periodePaie, BigDecimal.ZERO);
        BigDecimal reference = safeAmount(preview.getTotalAPayer());
        TypePaiementPersonnel typePaiement = normalizeTypePaiementPaie(request.getTypePaiementPersonnel());
        String motif = cleanNullableText(request.getMotif());
        QualificationPaie qualification = qualifyPayrollPayment(
                typePaiement,
                depense.getMontant(),
                reference,
                motif,
                cleanNullableText(request.getMotifRetenue()),
                cleanNullableText(request.getMotifPaiementPartiel()),
                commentaireCorrection,
                Boolean.TRUE.equals(request.getRetenueDefinitive()),
                employe.getId(),
                periodePaie,
                depense.getId()
        );

        TypePaiementPersonnel ancienType = depense.getTypePaiementPersonnel();
        String anciennePeriode = depense.getPeriodePaie();
        Long ancienEmployeId = depense.getEmploye() != null ? depense.getEmploye().getId() : null;

        depense.setEmploye(employe);
        depense.setBeneficiaireUtilisateur(employe.getUtilisateur());
        depense.setBeneficiaireNom(employe.getNomComplet());
        depense.setBeneficiaireRole(employe.getFonction() != null ? employe.getFonction().name() : null);
        depense.setBeneficiaireAgence(employe.getAgence() != null ? employe.getAgence().getNomAgence() : null);
        depense.setBeneficiaire(resolveBeneficiaireTexteForRattachement(employe, periodePaie));
        depense.setPeriodePaie(periodePaie);
        depense.setTypePaiementPersonnel(typePaiement);
        depense.setMontantRemunerationReference(reference);
        depense.setMontantEcartRemuneration(depense.getMontant().subtract(reference));
        depense.setMotifEcartRemuneration(motif);
        depense.setNaturePaiementPaie(typePaiement.name());
        depense.setMontantSalaireDu(reference);
        depense.setMontantDejaPaye(qualification.montantDejaPayeAvant());
        depense.setMontantRestantApresPaiement(qualification.resteApresPaiement());
        depense.setMontantRetenue(qualification.montantRetenue());
        depense.setMotifRetenue(qualification.motifRetenue());
        depense.setMotifPaiementPartiel(qualification.motifPaiementPartiel());
        depense.setCommentairePaie(commentaireCorrection);
        depense.setEpargneCollecteeReference(preview.getEpargneCollecteeValidee());
        depense.setRemboursementCollecteReference(preview.getRemboursementCreditCollecteValide());
        depense.setNombreCarnetsVendus(preview.getNombreCarnetsVendus());
        depense.setPrimeMobilisationEpargne(preview.getPrimeMobilisationEpargne());
        depense.setPrimeMobilisationRemboursement(preview.getPrimeMobilisationRemboursement());
        depense.setBonusCarnets(preview.getBonusCarnets());
        depense.setPrimeMotivationManuelle(preview.getPrimeMotivationManuelle());
        depense.setModeCalculPaie(preview.getModeCalcul());
        depense.setDetailCalculPaieJson(buildPaieDetailJson(preview));

        depense = depenseCaisseRepository.save(depense);
        auditService.logSuccess(
            AuditAction.MODIFICATION_OPERATION,
                "DepenseCaisse",
                depense.getId(),
                "Rattachement paie: employé " + ancienEmployeId + " -> " + employe.getId()
                        + ", période " + firstNonBlank(anciennePeriode, "N/A") + " -> " + periodePaie
                        + ", type " + (ancienType != null ? ancienType.name() : "N/A") + " -> " + typePaiement.name()
                        + ", salaire prévu " + reference
                        + ", montant payé " + depense.getMontant()
                        + ", retenue " + safeAmount(depense.getMontantRetenue())
                        + ", reste " + safeAmount(depense.getMontantRestantApresPaiement())
                        + ", commentaire " + commentaireCorrection
        );
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public DepenseCaisseResponse rattacherTransport(Long id, DepenseCaisseRattachementTransportRequest request) {
        if (request == null) {
            throw new BusinessException("La demande de rattachement transport est obligatoire");
        }
        String commentaire = cleanNullableText(request.getCommentaireCorrection());
        if (commentaire == null) {
            throw new BusinessException("Le commentaire de correction est obligatoire");
        }
        String periodeCharge = cleanNullableText(request.getPeriodeCharge());
        if (periodeCharge == null || !periodeCharge.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException("La période charge est obligatoire au format YYYY-MM");
        }
        if (request.getTypeChargeFixe() != TypeChargeFixe.TRANSPORT_SITE) {
            throw new BusinessException("Le type de charge fixe transport doit être TRANSPORT_SITE");
        }

        DepenseCaisse depense = loadDepense(id);
        if (depense.getCategorie() != DepenseCaisseCategorie.TRANSPORT) {
            throw new BusinessException("Seules les dépenses de catégorie TRANSPORT peuvent être rattachées au transport terrain");
        }
        Employe employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new BusinessException("Employé introuvable"));
        if (!isAgentTerrain(employe)) {
            throw new BusinessException("Le transport par site concerne uniquement les Agents Terrain");
        }
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new BusinessException("Site introuvable"));
        AgentTerrain agentTerrain = resolveAgentTerrain(employe);
        Site siteAffectation = resolvePrimarySite(agentTerrain);
        if (siteAffectation == null) {
            throw new BusinessException("L'Agent Terrain doit avoir un site d'affectation");
        }
        if (!Objects.equals(siteAffectation.getId(), site.getId()) && commentaire.length() < 8) {
            throw new BusinessException("Le site choisi diffère du site d'affectation: commentaire de correction détaillé obligatoire");
        }

        YearMonth periode = parsePeriodePaie(periodeCharge);
        LocalDate dateDebutPeriode = request.getDateDebutPeriode() != null ? request.getDateDebutPeriode() : periode.atDay(1);
        LocalDate dateFinPeriode = request.getDateFinPeriode() != null ? request.getDateFinPeriode() : periode.atEndOfMonth();
        if (dateFinPeriode.isBefore(dateDebutPeriode)) {
            throw new BusinessException("La date de fin de période transport doit être supérieure ou égale à la date de début");
        }
        int nombreJoursPeriode = Math.toIntExact(ChronoUnit.DAYS.between(dateDebutPeriode, dateFinPeriode) + 1);
        BigDecimal reference = transportSiteParametreRepository.findActiveBySiteAtDate(site.getId(), periode.atEndOfMonth())
            .map(parametre -> safeAmount(parametre.getMontantTransportJournalierParAgent()).multiply(BigDecimal.valueOf(nombreJoursPeriode)))
                .orElse(BigDecimal.ZERO);
        BigDecimal ecart = safeAmount(depense.getMontant()).subtract(reference);
        if (ecart.compareTo(BigDecimal.ZERO) != 0 && commentaire == null) {
            throw new BusinessException("Le commentaire est obligatoire si le montant transport payé diffère du montant prévu");
        }

        Long ancienEmploye = depense.getEmploye() != null ? depense.getEmploye().getId() : null;
        Long ancienSite = depense.getSiteCharge() != null ? depense.getSiteCharge().getId() : null;
        String anciennePeriode = depense.getPeriodeCharge();
        depense.setEmploye(employe);
        depense.setBeneficiaireUtilisateur(employe.getUtilisateur());
        depense.setBeneficiaireNom(employe.getNomComplet());
        depense.setBeneficiaireRole(employe.getFonction() != null ? employe.getFonction().name() : null);
        depense.setBeneficiaireAgence(employe.getAgence() != null ? employe.getAgence().getNomAgence() : null);
        depense.setBeneficiaire(employe.getNomComplet() + " — TRANSPORT — " + periodeCharge);
        depense.setPeriodeCharge(periodeCharge);
        depense.setTypeChargeFixe(TypeChargeFixe.TRANSPORT_SITE);
        depense.setSiteCharge(site);
        depense.setMontantChargeFixeReference(reference);
        depense.setMontantEcartChargeFixe(ecart);
        depense.setCommentaireRapprochement(commentaire);
        depense = depenseCaisseRepository.save(depense);

        auditService.logSuccess(
                AuditAction.MODIFICATION_OPERATION,
                "DepenseCaisse",
                depense.getId(),
                "Rattachement transport: employé " + ancienEmploye + " -> " + employe.getId()
                        + ", site " + ancienSite + " -> " + site.getId()
                        + ", période " + firstNonBlank(anciennePeriode, "N/A") + " -> " + periodeCharge
                        + ", jours période " + nombreJoursPeriode
                        + ", montant prévu période " + reference
                        + ", montant payé " + safeAmount(depense.getMontant())
                        + ", écart " + ecart
                        + ", commentaire " + commentaire
        );
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public DepenseCaisseResponse soumettre(Long id, DepenseCaisseSubmitRequest request) {
        DepenseCaisse depense = loadDepense(id);
        if (!depense.canSubmit()) {
            throw new BusinessException("La dépense ne peut pas être soumise dans son état actuel");
        }

        depense.setStatut(DepenseCaisseStatus.EN_ATTENTE_VALIDATION);
        depense.setDateSoumission(LocalDateTime.now());
        if (request != null) {
            depense.setCommentaireValidation(cleanNullableText(request.getCommentaire()));
        }

        depense = depenseCaisseRepository.save(depense);
        auditService.logSuccess(AuditAction.DEPENSE_CAISSE_SUBMITTED, "DepenseCaisse", depense.getId(), "Soumission de dépense caisse");
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public DepenseCaisseResponse valider(Long id, DepenseCaisseValidateRequest request) {
        DepenseCaisse depense = loadDepense(id);
        if (!depense.canValidate()) {
            throw new BusinessException("La dépense ne peut pas être validée dans son état actuel");
        }

        depense.setStatut(DepenseCaisseStatus.VALIDEE);
        depense.setValidePar(getCurrentUser());
        depense.setDateValidation(LocalDateTime.now());
        depense.setCommentaireValidation(cleanNullableText(request != null ? request.getCommentaireValidation() : null));

        depense = depenseCaisseRepository.save(depense);
        auditService.logSuccess(AuditAction.DEPENSE_CAISSE_VALIDATED, "DepenseCaisse", depense.getId(), "Validation de dépense caisse");
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public DepenseCaisseResponse rejeter(Long id, DepenseCaisseRejectRequest request) {
        DepenseCaisse depense = loadDepense(id);
        if (!depense.canReject()) {
            throw new BusinessException("La dépense ne peut pas être rejetée dans son état actuel");
        }

        String commentaire = request == null ? null : cleanNullableText(request.getCommentaire());
        if (commentaire == null) {
            throw new BusinessException("Le commentaire de rejet est obligatoire");
        }

        depense.setStatut(DepenseCaisseStatus.REJETEE);
        depense.setValidePar(getCurrentUser());
        depense.setDateValidation(LocalDateTime.now());
        depense.setMotifRejet(commentaire);

        depense = depenseCaisseRepository.save(depense);
        auditService.logSuccess(AuditAction.DEPENSE_CAISSE_REJECTED, "DepenseCaisse", depense.getId(), "Rejet de dépense caisse");
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public DepenseCaisseResponse payer(Long id, DepenseCaissePayRequest request) {
        DepenseCaisse depense = loadDepense(id);

        if (depense.getStatut() == DepenseCaisseStatus.PAYEE || depense.getOperationCaisse() != null) {
            auditService.logWarning(
                    AuditAction.DEPENSE_CAISSE_PAID,
                    AuditModule.DEPENSE_CAISSE,
                    "DepenseCaisse",
                    depense.getId(),
                    "Tentative de double paiement refusée",
                    "DEPENSE-" + depense.getId()
            );
            throw new BusinessException("Dépense déjà payée.");
        }

        if (!depense.canPay()) {
            throw new BusinessException("La dépense doit être validée avant paiement");
        }

        SessionCaisse session = sessionCaisseRepository
                .findFirstByCaisseIdAndDateComptableAndStatutInOrderByDateOuvertureDesc(
                        depense.getCaisse().getId(),
                        LocalDate.now(),
                        List.of(StatutSessionCaisse.OUVERTE)
                )
                .orElseThrow(() -> new BusinessException("Impossible de payer la dépense: aucune session caisse ouverte aujourd'hui"));

        sessionCaisseValidationService.validateSessionForOperation(session);

        OperationCaisseRequest operationRequest = new OperationCaisseRequest();
        operationRequest.setSessionCaisseId(session.getId());
        operationRequest.setCaisseId(session.getCaisse().getId());
        operationRequest.setDateOperation(LocalDateTime.now());
        operationRequest.setTypeOperation(TypeOperationCaisse.SORTIE);
        operationRequest.setCategorieOperation(CategorieOperationCaisse.DEPENSE);
        operationRequest.setMontant(depense.getMontant());
        operationRequest.setDevise(depense.getDevise());
        operationRequest.setDescription(depense.getMotif());
        operationRequest.setObservation(buildPaymentObservation(depense, request));
        operationRequest.setSource(SourceOperationCaisse.DEPENSE_CAISSE);
        operationRequest.setReferenceExterne("DEPENSE-" + depense.getId());
        operationRequest.setReferenceMetier("DEPENSE-" + depense.getId());
        operationRequest.setDepenseCaisseId(depense.getId());
        operationRequest.setCreatedBy(getCurrentUser() != null ? getCurrentUser().getId() : null);

        OperationCaisseResponse operationResult;
        try {
            operationResult = operationCaisseService.enregistrer(operationRequest);
        } catch (RuntimeException exception) {
            throw exception instanceof BusinessException ? (BusinessException) exception : new BusinessException(exception.getMessage());
        }

        if (operationResult == null || operationResult.getId() == null) {
            throw new BusinessException("Le paiement de la dépense a échoué: opération caisse introuvable");
        }

        OperationCaisse operationEntity = operationCaisseRepository.findById(operationResult.getId())
                .orElseThrow(() -> new BusinessException("Opération caisse créée mais introuvable pour liaison avec la dépense"));

        depense.setSessionCaisse(session);
        depense.setSite(session.getCaisse() != null ? session.getCaisse().getSite() : depense.getSite());
        depense.setPayePar(getCurrentUser());
        depense.setDatePaiement(LocalDateTime.now());
        depense.setOperationCaisse(operationEntity);
        depense.setStatut(DepenseCaisseStatus.PAYEE);
        depense = depenseCaisseRepository.save(depense);

        auditService.logSuccess(AuditAction.DEPENSE_CAISSE_PAID, "DepenseCaisse", depense.getId(), "Paiement de dépense caisse");
        return depenseCaisseMapper.toResponse(depense);
    }

    @Override
    public DepenseCaisseResponse annuler(Long id, String commentaire) {
        DepenseCaisse depense = loadDepense(id);

        if (depense.getStatut() == DepenseCaisseStatus.PAYEE) {
            throw new BusinessException("Impossible d'annuler une dépense déjà payée");
        }
        if (depense.getStatut() == DepenseCaisseStatus.ANNULEE) {
            return depenseCaisseMapper.toResponse(depense);
        }
        if (!depense.canCancel()) {
            throw new BusinessException("La dépense ne peut pas être annulée dans son état actuel");
        }

        depense.setStatut(DepenseCaisseStatus.ANNULEE);
        depense.setMotifRejet(cleanNullableText(commentaire));
        depense = depenseCaisseRepository.save(depense);
        auditService.logSuccess(AuditAction.DEPENSE_CAISSE_CANCELLED, "DepenseCaisse", depense.getId(), "Annulation de dépense caisse");
        return depenseCaisseMapper.toResponse(depense);
    }

    private DepenseCaisse loadDepense(Long id) {
        return depenseCaisseRepository.findByIdWithContext(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dépense caisse introuvable"));
    }

    private DepenseCaisseStatus parseStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        try {
            return DepenseCaisseStatus.valueOf(statut.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Statut de dépense invalide: " + statut);
        }
    }

    private String normalizeDevise(String devise) {
        String value = cleanNullableText(devise);
        return value == null ? "CDF" : value.toUpperCase(Locale.ROOT);
    }

    private String cleanNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PersonnelPaymentContext resolvePersonnelPayment(DepenseCaisseCreateRequest request, Caisse caisse) {
        if (request.getCategorie() != DepenseCaisseCategorie.SALAIRE) {
            return null;
        }
        Long employeId = request.getEmployeId() != null ? request.getEmployeId() : request.getBeneficiaireId();
        if (employeId == null) {
            throw new BusinessException("L'employé concerné est obligatoire pour une dépense salaire");
        }
        String periodePaie = cleanNullableText(request.getPeriodePaie());
        if (periodePaie == null || !periodePaie.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException("La période de paie est obligatoire au format YYYY-MM");
        }
        TypePaiementPersonnel typePaiement = normalizeTypePaiementPaie(request.getTypePaiementPersonnel() != null
            ? request.getTypePaiementPersonnel()
            : TypePaiementPersonnel.SALAIRE_COMPLET);

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new BusinessException("Employé introuvable"));
        if (Boolean.FALSE.equals(employe.getActif())) {
            throw new BusinessException("Impossible de payer un employé inactif");
        }
        if (caisse.getAgence() != null && employe.getAgence() != null && !Objects.equals(caisse.getAgence().getId(), employe.getAgence().getId())) {
            throw new BusinessException("L'employé sélectionné n'appartient pas à l'agence de la caisse");
        }

        BigDecimal primeMotivationManuelle = safeAmount(request.getPrimeMotivationManuelle());
        String motifPrimeMotivation = cleanNullableText(request.getMotifPrimeMotivationManuelle());
        if (primeMotivationManuelle.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("La prime de motivation ne peut pas être négative");
        }
        if (isAgentTerrain(employe) && primeMotivationManuelle.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("L'Agent Terrain utilise uniquement les primes automatiques liées aux collectes validées");
        }
        if (!isAgentTerrain(employe) && primeMotivationManuelle.compareTo(BigDecimal.ZERO) > 0 && !isPrimeMotivationAllowed(employe.getFonction())) {
            throw new BusinessException("La prime de motivation manuelle n'est pas autorisée pour ce poste");
        }
        if (primeMotivationManuelle.compareTo(BigDecimal.ZERO) > 0 && motifPrimeMotivation == null) {
            throw new BusinessException("Le motif de la prime de motivation est obligatoire si la prime est supérieure à zéro");
        }

        PaieEmployePreviewResponse preview = buildPaiePreview(employe, periodePaie, primeMotivationManuelle);
        BigDecimal reference = safeAmount(preview.getTotalAPayer());
        if (reference.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La rémunération calculée de l'employé doit être supérieure à zéro");
        }
        BigDecimal montantPaye = request.getMontant() != null ? request.getMontant() : reference;
        if (montantPaye.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de la dépense doit être supérieur à zéro");
        }
        String motifEcart = cleanNullableText(request.getMotifEcartRemuneration());
        QualificationPaie qualification = qualifyPayrollPayment(
            typePaiement,
            montantPaye,
            reference,
            motifEcart,
            cleanNullableText(request.getMotifRetenue()),
            cleanNullableText(request.getMotifPaiementPartiel()),
            cleanNullableText(request.getCommentairePaie()),
            Boolean.TRUE.equals(request.getRetenueDefinitive()),
            employe.getId(),
            periodePaie,
            null
        );
        BigDecimal ecart = montantPaye.subtract(reference);

        String roleLabel = employe.getFonction() != null ? employe.getFonction().name() : "Poste non renseigné";
        String agenceNom = employe.getAgence() != null ? employe.getAgence().getNomAgence() : null;
        return new PersonnelPaymentContext(
                employe,
                periodePaie,
                typePaiement,
                reference,
                montantPaye,
                ecart,
                motifEcart,
                motifPrimeMotivation,
                qualification.motifRetenue(),
                qualification.motifPaiementPartiel(),
                qualification.commentairePaie(),
                qualification.montantDejaPayeAvant(),
                qualification.resteApresPaiement(),
                qualification.montantRetenue(),
                roleLabel,
                agenceNom,
                preview
        );
    }

    private QualificationPaie qualifyPayrollPayment(
            TypePaiementPersonnel requestedType,
            BigDecimal montantPaye,
            BigDecimal remunerationReference,
            String motif,
            String motifRetenue,
            String motifPaiementPartiel,
            String commentairePaie,
            boolean retenueDefinitive,
            Long employeId,
            String periodePaie,
            Long excludeDepenseId
    ) {
        TypePaiementPersonnel type = normalizeTypePaiementPaie(requestedType);
        BigDecimal paye = scaleCurrency(montantPaye);
        BigDecimal reference = scaleCurrency(remunerationReference);
        BigDecimal ecart = paye.subtract(reference);

        if (ecart.compareTo(BigDecimal.ZERO) == 0 && !isSalaireComplet(type)) {
            throw new BusinessException("Le montant correspond au salaire prévu: utilisez SALAIRE_COMPLET");
        }
        if (ecart.compareTo(BigDecimal.ZERO) < 0 && !isQualificationSalaireInferieur(type)) {
            throw new BusinessException("Le montant payé est inférieur à la rémunération prévue: choisissez SALAIRE_PARTIEL, AVANCE_SALAIRE ou RETENUE_SALAIRE");
        }
        if (ecart.compareTo(BigDecimal.ZERO) > 0 && !isPaiementComplementaire(type)) {
            throw new BusinessException("Le montant payé dépasse la rémunération prévue: choisissez PRIME, COMMISSION, REGULARISATION ou AUTRE avec motif");
        }

        String motifNet = cleanNullableText(motif);
        String motifRetenueNet = cleanNullableText(motifRetenue);
        String motifPartielNet = cleanNullableText(motifPaiementPartiel);
        String commentaireNet = cleanNullableText(commentairePaie);
        if (type == TypePaiementPersonnel.RETENUE_SALAIRE && motifRetenueNet == null) {
            throw new BusinessException("Le motif de retenue est obligatoire");
        }
        if (type == TypePaiementPersonnel.SALAIRE_PARTIEL && motifPartielNet == null) {
            throw new BusinessException("Le motif du paiement partiel est obligatoire");
        }
        if (type == TypePaiementPersonnel.AVANCE_SALAIRE && motifNet == null) {
            throw new BusinessException("Le motif de l'avance sur salaire est obligatoire");
        }
        if (isPaiementComplementaire(type) && motifNet == null) {
            throw new BusinessException("Le motif est obligatoire pour une prime, commission, régularisation ou autre paiement supérieur au salaire prévu");
        }

        List<DepenseCaisse> paiementsExistants = depenseCaisseRepository.findPersonnelPaymentsForPeriod(
                employeId,
                periodePaie,
                PAYROLL_LOCK_STATUSES,
                excludeDepenseId
        );
        boolean hasSalaireComplet = paiementsExistants.stream().anyMatch(depense -> isSalaireComplet(normalizeTypePaiementPaie(depense.getTypePaiementPersonnel())));
        if (isSalaireComplet(type) && !paiementsExistants.isEmpty()) {
            throw new BusinessException("Un salaire complet ne peut pas être enregistré si des paiements de paie existent déjà pour cette période");
        }
        if (!isSalaireComplet(type) && hasSalaireComplet && isQualificationSalaireInferieur(type)) {
            throw new BusinessException("Un salaire complet existe déjà pour cet employé et cette période");
        }

        BigDecimal dejaPayeReducteur = paiementsExistants.stream()
                .filter(depense -> isPaiementReducteurSalaire(normalizeTypePaiementPaie(depense.getTypePaiementPersonnel())))
                .map(this::montantReducingSalaire)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montantRetenue = type == TypePaiementPersonnel.RETENUE_SALAIRE ? reference.subtract(paye).max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal montantReducteurCourant = type == TypePaiementPersonnel.RETENUE_SALAIRE
                ? (retenueDefinitive ? paye.add(montantRetenue) : paye)
                : (isPaiementReducteurSalaire(type) ? paye : BigDecimal.ZERO);
        BigDecimal totalReducteur = dejaPayeReducteur.add(montantReducteurCourant);
        if (totalReducteur.compareTo(reference) > 0 && motifNet == null && commentaireNet == null) {
            throw new BusinessException("Le total salaire/avances/retenues dépasse le salaire prévu: une justification est obligatoire");
        }
        BigDecimal reste = reference.subtract(totalReducteur).max(BigDecimal.ZERO);

        return new QualificationPaie(dejaPayeReducteur, reste, montantRetenue, motifRetenueNet, motifPartielNet, commentaireNet);
    }

    private TypePaiementPersonnel normalizeTypePaiementPaie(TypePaiementPersonnel type) {
        if (type == null || type == TypePaiementPersonnel.SALAIRE) {
            return TypePaiementPersonnel.SALAIRE_COMPLET;
        }
        if (type == TypePaiementPersonnel.AVANCE) {
            return TypePaiementPersonnel.AVANCE_SALAIRE;
        }
        return type;
    }

    private boolean isSalaireComplet(TypePaiementPersonnel type) {
        return type == TypePaiementPersonnel.SALAIRE_COMPLET;
    }

    private boolean isQualificationSalaireInferieur(TypePaiementPersonnel type) {
        return type == TypePaiementPersonnel.SALAIRE_PARTIEL
                || type == TypePaiementPersonnel.AVANCE_SALAIRE
                || type == TypePaiementPersonnel.RETENUE_SALAIRE;
    }

    private boolean isPaiementComplementaire(TypePaiementPersonnel type) {
        return type == TypePaiementPersonnel.PRIME
                || type == TypePaiementPersonnel.COMMISSION
                || type == TypePaiementPersonnel.REGULARISATION
                || type == TypePaiementPersonnel.AUTRE;
    }

    private boolean isPaiementReducteurSalaire(TypePaiementPersonnel type) {
        return isSalaireComplet(type)
                || type == TypePaiementPersonnel.SALAIRE_PARTIEL
                || type == TypePaiementPersonnel.AVANCE_SALAIRE
                || type == TypePaiementPersonnel.RETENUE_SALAIRE;
    }

    private BigDecimal montantReducingSalaire(DepenseCaisse depense) {
        TypePaiementPersonnel type = normalizeTypePaiementPaie(depense.getTypePaiementPersonnel());
        if (type == TypePaiementPersonnel.RETENUE_SALAIRE && depense.getMontantRetenue() != null) {
            return safeAmount(depense.getMontant()).add(safeAmount(depense.getMontantRetenue()));
        }
        return safeAmount(depense.getMontant());
    }

    private String resolveBeneficiaireTexteForRattachement(Employe employe, String periodePaie) {
        String roleLabel = employe.getFonction() != null ? employe.getFonction().name() : "Poste non renseigné";
        return employe.getNomComplet() + " — " + roleLabel + " — " + periodePaie;
    }

    private PaieEmployePreviewResponse buildPaiePreview(Employe employe, String periodePaie, BigDecimal primeMotivationManuelle) {
        YearMonth periode = parsePeriodePaie(periodePaie);
        LocalDate dateDebut = periode.atDay(1);
        LocalDate dateFin = periode.atEndOfMonth();
        BigDecimal salaireBase = scaleCurrency(safeAmount(employe.getSalaireBase()));
        BigDecimal remunerationFixe = scaleCurrency(safeAmount(employe.getTotalRemuneration()));
        BigDecimal epargneCollectee = BigDecimal.ZERO;
        BigDecimal remboursementCollecte = BigDecimal.ZERO;
        Integer nombreCarnets = 0;
        BigDecimal primeEpargne = BigDecimal.ZERO;
        BigDecimal primeRemboursement = BigDecimal.ZERO;
        BigDecimal bonusCarnets = BigDecimal.ZERO;
        BigDecimal primeMotivation = scaleCurrency(safeAmount(primeMotivationManuelle));
        ModeCalculPaie modeCalcul = ModeCalculPaie.PERSONNEL_BUREAU_MANUEL;

        if (isAgentTerrain(employe)) {
            modeCalcul = ModeCalculPaie.AGENT_TERRAIN_AUTOMATIQUE;
            AgentTerrain agentTerrain = resolveAgentTerrain(employe);
            epargneCollectee = scaleCurrency(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
                    agentTerrain.getId(),
                    TypeLigneCollecte.EPARGNE,
                    dateDebut,
                    dateFin
            ));
            remboursementCollecte = scaleCurrency(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
                    agentTerrain.getId(),
                    TypeLigneCollecte.REMBOURSEMENT_CREDIT,
                    dateDebut,
                    dateFin
            ));
            Long carnets = collecteMembreLigneRepository.sumValidatedCarnetsByAgentAndPeriod(agentTerrain.getId(), dateDebut, dateFin);
            nombreCarnets = carnets != null ? Math.toIntExact(carnets) : 0;
            primeEpargne = scaleCurrency(epargneCollectee.multiply(PaiePersonnelConstants.TAUX_PRIME_EPARGNE_AGENT_TERRAIN));
            primeRemboursement = scaleCurrency(remboursementCollecte.multiply(PaiePersonnelConstants.TAUX_PRIME_REMBOURSEMENT_AGENT_TERRAIN));
            bonusCarnets = scaleCurrency(PaiePersonnelConstants.BONUS_CARNET_AGENT_TERRAIN.multiply(BigDecimal.valueOf(nombreCarnets)));
            primeMotivation = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalPrimes = scaleCurrency(primeEpargne.add(primeRemboursement).add(primeMotivation));
        BigDecimal totalBonus = scaleCurrency(bonusCarnets);
        BigDecimal totalAPayer = scaleCurrency(remunerationFixe.add(totalPrimes).add(totalBonus));

        return PaieEmployePreviewResponse.builder()
                .employeId(employe.getId())
                .matricule(employe.getMatricule())
                .nomComplet(employe.getNomComplet())
                .poste(employe.getFonction() != null ? employe.getFonction().name() : null)
                .periodePaie(periode.toString())
                .salaireBase(salaireBase)
                .epargneCollecteeValidee(epargneCollectee)
                .remboursementCreditCollecteValide(remboursementCollecte)
                .nombreCarnetsVendus(nombreCarnets)
                .primeMobilisationEpargne(primeEpargne)
                .primeMobilisationRemboursement(primeRemboursement)
                .bonusCarnets(bonusCarnets)
                .primeMotivationManuelle(primeMotivation)
                .primeMotivationManuelleAutorisee(!isAgentTerrain(employe) && isPrimeMotivationAllowed(employe.getFonction()))
                .totalPrimes(totalPrimes)
                .totalBonus(totalBonus)
                .totalAPayer(totalAPayer)
                .modeCalcul(modeCalcul)
                .build();
    }

    private YearMonth parsePeriodePaie(String periodePaie) {
        String value = cleanNullableText(periodePaie);
        if (value == null || !value.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException("La période de paie est obligatoire au format YYYY-MM");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException("La période de paie est invalide");
        }
    }

    private boolean isAgentTerrain(Employe employe) {
        return employe != null && employe.getFonction() == PosteEmploye.AGENT_TERRAIN;
    }

    private boolean isPrimeMotivationAllowed(PosteEmploye fonction) {
        return fonction == PosteEmploye.GESTIONNAIRE
                || fonction == PosteEmploye.CAISSIER
                || fonction == PosteEmploye.CONTROLEUR
                || fonction == PosteEmploye.CHEF_BUREAU;
    }

    private AgentTerrain resolveAgentTerrain(Employe employe) {
        if (employe.getUtilisateur() == null || employe.getUtilisateur().getId() == null) {
            throw new BusinessException("L'Agent Terrain doit être lié à un compte utilisateur pour calculer les primes de collecte");
        }
        return agentTerrainRepository.findByUtilisateurId(employe.getUtilisateur().getId())
                .orElseThrow(() -> new BusinessException("Fiche Agent Terrain introuvable pour cet employé"));
    }

    private Site resolvePrimarySite(AgentTerrain agentTerrain) {
        if (agentTerrain == null) {
            return null;
        }
        if (agentTerrain.getSite() != null) {
            return agentTerrain.getSite();
        }
        return agentTerrain.getSitesAffectes() == null ? null : agentTerrain.getSitesAffectes().stream().findFirst().orElse(null);
    }

    private BigDecimal scaleCurrency(BigDecimal value) {
        return safeAmount(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildPaieDetailJson(PaieEmployePreviewResponse preview) {
        return String.format(Locale.ROOT,
                "{\"modeCalcul\":\"%s\",\"periodePaie\":\"%s\",\"salaireBase\":%s,\"epargneCollecteeValidee\":%s,\"remboursementCreditCollecteValide\":%s,\"nombreCarnetsVendus\":%d,\"primeMobilisationEpargne\":%s,\"primeMobilisationRemboursement\":%s,\"bonusCarnets\":%s,\"primeMotivationManuelle\":%s,\"totalAPayer\":%s}",
                preview.getModeCalcul(),
                preview.getPeriodePaie(),
                preview.getSalaireBase(),
                preview.getEpargneCollecteeValidee(),
                preview.getRemboursementCreditCollecteValide(),
                preview.getNombreCarnetsVendus(),
                preview.getPrimeMobilisationEpargne(),
                preview.getPrimeMobilisationRemboursement(),
                preview.getBonusCarnets(),
                preview.getPrimeMotivationManuelle(),
                preview.getTotalAPayer());
    }

    private String resolveBeneficiaireTexte(DepenseCaisseCreateRequest request, PersonnelPaymentContext personnelPayment) {
        if (personnelPayment != null) {
            Employe employe = personnelPayment.employe();
            return employe.getNomComplet() + " — " + personnelPayment.roleLabel() + " — " + personnelPayment.periodePaie();
        }
        return cleanNullableText(request.getBeneficiaire());
    }

    private DepenseCaisseBeneficiaireSalaireResponse toBeneficiaireResponse(Employe employe) {
        String nom = cleanNullableText(employe.getNomComplet());
        String role = employe.getFonction() != null ? employe.getFonction().name() : "Poste non renseigné";
        Agence agence = employe.getAgence();
        String agenceNom = agence != null ? agence.getNomAgence() : null;
        String agenceDisplay = agenceNom != null ? agenceNom : "Central";
        BigDecimal salaireBase = safeAmount(employe.getSalaireBase());
        BigDecimal primeFixe = safeAmount(employe.getPrimeFixe());
        BigDecimal bonusVariable = safeAmount(employe.getBonusVariable());
        BigDecimal totalRemuneration = safeAmount(employe.getTotalRemuneration());

        return DepenseCaisseBeneficiaireSalaireResponse.builder()
                .id(employe.getId())
                .employeId(employe.getId())
                .matricule(employe.getMatricule())
                .nom(nom)
                .role(role)
                .agenceId(agence != null ? agence.getId() : null)
                .agenceNom(agenceNom)
                .salaireBase(salaireBase)
                .primeFixe(primeFixe)
                .bonusVariable(bonusVariable)
                .totalRemuneration(totalRemuneration)
                .affichage(employe.getMatricule() + " — " + nom + " — " + role + " — " + agenceDisplay)
                .build();
    }

    private String buildPaymentObservation(DepenseCaisse depense, DepenseCaissePayRequest request) {
        String commentaire = cleanNullableText(request != null ? request.getCommentaire() : null);
        if (depense.getEmploye() == null) {
            return commentaire;
        }
        Employe employe = depense.getEmploye();
        String personnelInfo = "Paiement personnel: "
                + firstNonBlank(employe.getNomComplet(), depense.getBeneficiaireNom(), "Employé")
                + " / matricule " + firstNonBlank(employe.getMatricule(), "N/A")
                + " / période " + firstNonBlank(depense.getPeriodePaie(), "N/A")
            + " / type " + (depense.getTypePaiementPersonnel() != null ? depense.getTypePaiementPersonnel().name() : "N/A")
            + " / nature " + firstNonBlank(depense.getNaturePaiementPaie(), "N/A")
            + " / mode " + (depense.getModeCalculPaie() != null ? depense.getModeCalculPaie().name() : "N/A")
            + " / base " + safeAmount(depense.getEmploye().getSalaireBase())
            + " / salaire dû " + safeAmount(depense.getMontantSalaireDu())
            + " / reste après paiement " + safeAmount(depense.getMontantRestantApresPaiement())
            + " / retenue " + safeAmount(depense.getMontantRetenue())
            + " / primes " + safeAmount(depense.getPrimeMobilisationEpargne()).add(safeAmount(depense.getPrimeMobilisationRemboursement())).add(safeAmount(depense.getPrimeMotivationManuelle()))
            + " / bonus " + safeAmount(depense.getBonusCarnets())
            + " / total " + safeAmount(depense.getMontant());
        return commentaire != null ? personnelInfo + " — " + commentaire : personnelInfo;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = cleanNullableText(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    private record PersonnelPaymentContext(
            Employe employe,
            String periodePaie,
            TypePaiementPersonnel typePaiementPersonnel,
            BigDecimal reference,
            BigDecimal montantPaye,
            BigDecimal ecart,
            String motifEcart,
                String motifPrimeMotivation,
                String motifRetenue,
                String motifPaiementPartiel,
                String commentairePaie,
                BigDecimal montantDejaPayeAvant,
                BigDecimal resteApresPaiement,
                BigDecimal montantRetenue,
            String roleLabel,
                String agenceNom,
                PaieEmployePreviewResponse preview
    ) {}

    private record QualificationPaie(
            BigDecimal montantDejaPayeAvant,
            BigDecimal resteApresPaiement,
            BigDecimal montantRetenue,
            String motifRetenue,
            String motifPaiementPartiel,
            String commentairePaie
    ) {}

    private RoleCode resolveEffectiveRole(RoleCode roleCode) {
        return roleCode;
    }

    private Long resolveAgenceId(Utilisateur utilisateur) {
        Agence agence = resolveAgence(utilisateur);
        return agence != null ? agence.getId() : null;
    }

    private Agence resolveAgence(Utilisateur utilisateur) {
        if (utilisateur.getEmploye() != null && utilisateur.getEmploye().getAgence() != null) {
            return utilisateur.getEmploye().getAgence();
        }
        if (utilisateur.getSite() != null && utilisateur.getSite().getAgence() != null) {
            return utilisateur.getSite().getAgence();
        }
        return null;
    }

    private Agence resolveAgence(AgentTerrain agent, Long agenceId) {
        if (agent.getSite() != null && agent.getSite().getAgence() != null) {
            if (agenceId == null || Objects.equals(agent.getSite().getAgence().getId(), agenceId)) {
                return agent.getSite().getAgence();
            }
        }
        if (agent.getSitesAffectes() == null) {
            return null;
        }
        Agence agenceAffectee = agent.getSitesAffectes().stream()
                .map(site -> site.getAgence())
                .filter(Objects::nonNull)
                .filter(agence -> agenceId == null || Objects.equals(agence.getId(), agenceId))
                .findFirst()
                .orElse(null);
        if (agenceAffectee != null) {
            return agenceAffectee;
        }
        return agent.getSite() != null ? agent.getSite().getAgence() : null;
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Utilisateur utilisateur) {
            return utilisateur;
        }
        return null;
    }

}