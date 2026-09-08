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
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutTicketRecu;
import com.mini.credit.enums.TypeTicketRecu;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
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
import com.mini.credit.service.TicketRecuService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service("ticketRecuService")
@RequiredArgsConstructor
@Transactional
public class TicketRecuServiceImpl implements TicketRecuService {

    private static final DateTimeFormatter DATE_NUMERO = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TicketRecuRepository ticketRecuRepository;
    private final OperationEpargneRepository operationEpargneRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;
    private final CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;
    private final CollecteMembreLigneRepository collecteMembreLigneRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final MembreRepository membreRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ScopeService scopeService;
    private final AuditService auditService;

    @Override
    public TicketRecuResponse genererDepuisOperation(TicketRecuGenerationRequest request) {
        validateGenerationRequest(request);

        if (ticketRecuRepository.findByOperationEpargneIdAndTypeTicketAndOriginalTicketIsNull(
                request.getOperationEpargneId(), request.getTypeTicket()).isPresent()) {
            throw new BusinessException("Un ticket original existe déjà pour cette opération épargne");
        }

        OperationEpargne operationEpargne = operationEpargneRepository.findById(request.getOperationEpargneId())
                .orElseThrow(() -> new ResourceNotFoundException("Opération épargne introuvable"));
        OperationCaisse operationCaisse = request.getOperationCaisseId() != null
                ? operationCaisseRepository.findById(request.getOperationCaisseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Opération caisse introuvable"))
                : null;
        OperationCaisse operationCaisseCommission = request.getOperationCaisseCommissionId() != null
                ? operationCaisseRepository.findById(request.getOperationCaisseCommissionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Opération caisse commission introuvable"))
                : null;
        DemandeRetraitEpargne demandeRetrait = request.getDemandeRetraitEpargneId() != null
                ? demandeRetraitEpargneRepository.findById(request.getDemandeRetraitEpargneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Demande retrait introuvable"))
                : null;
        CollecteJournaliereTerrain collecte = request.getCollecteJournaliereId() != null
                ? collecteJournaliereTerrainRepository.findById(request.getCollecteJournaliereId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collecte journalière introuvable"))
                : null;
        CollecteMembreLigne ligneCollecte = request.getCollecteMembreLigneId() != null
                ? collecteMembreLigneRepository.findById(request.getCollecteMembreLigneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ligne de collecte introuvable"))
                : null;
        SessionCaisse session = request.getSessionCaisseId() != null
                ? sessionCaisseRepository.findById(request.getSessionCaisseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"))
                : operationEpargne.getSessionCaisse();
        Caisse caisse = request.getCaisseId() != null
                ? caisseRepository.findById(request.getCaisseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"))
                : session != null ? session.getCaisse() : null;
        Membre membre = membreRepository.findById(request.getMembreId())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));
        CompteEpargne compte = compteEpargneRepository.findById(request.getCompteEpargneId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable"));
        Utilisateur createur = request.getUtilisateurCreateurId() != null
                ? utilisateurRepository.findById(request.getUtilisateurCreateurId()).orElse(null)
                : SecurityUtils.getCurrentUser();

        if (!operationEpargne.getMembre().getId().equals(membre.getId()) || !operationEpargne.getCompteEpargne().getId().equals(compte.getId())) {
            throw new BusinessException("Incohérence entre ticket, opération épargne, membre et compte");
        }

        TicketRecu ticket = TicketRecu.builder()
                .numeroTicket(generateNumeroTicket(resolveAgence(caisse, session, membre), LocalDateTime.now()))
                .typeTicket(request.getTypeTicket())
                .statut(StatutTicketRecu.GENERE)
                .dateGeneration(LocalDateTime.now())
                .operationEpargne(operationEpargne)
                .operationCaisse(operationCaisse)
                .operationCaisseCommission(operationCaisseCommission)
                .demandeRetraitEpargne(demandeRetrait)
                .collecteJournaliere(collecte)
                .collecteMembreLigne(ligneCollecte)
                .sessionCaisse(session)
                .caisse(caisse)
                .membre(membre)
                .compteEpargne(compte)
                .agence(resolveAgence(caisse, session, membre))
                .site(resolveSite(caisse, session, membre))
                .utilisateurCreateur(createur)
                .devise(request.getDevise() != null ? request.getDevise() : "CDF")
                .montantPrincipal(requireNonNegative(request.getMontantPrincipal(), "montantPrincipal"))
                .tauxCommission(request.getTauxCommission())
                .montantCommission(nullToZero(request.getMontantCommission()))
                .montantTotalDebite(request.getMontantTotalDebite())
                .montantRemisMembre(request.getMontantRemisMembre())
                .ancienSolde(requireNonNegative(request.getAncienSolde(), "ancienSolde"))
                .nouveauSolde(requireNonNegative(request.getNouveauSolde(), "nouveauSolde"))
                .commentaire(clean(request.getCommentaire()))
                .codeVerification(generateVerificationCode())
                .createdBy(createur)
                .build();
        ticket.setQrPayload(buildQrPayload(ticket));
        ticket = ticketRecuRepository.save(ticket);
        audit(ticket, AuditAction.TICKET_RECU_GENERATED, "Génération ticket reçu", null, true);
        return toResponse(ticket);
    }

    @Override
    public TicketRecuResponse getById(Long id) {
        TicketRecu ticket = findTicketScoped(id);
        return toResponse(ticket);
    }

    @Override
    public List<TicketRecuResponse> getByOperationEpargne(Long operationEpargneId) {
        return ticketRecuRepository.findByOperationEpargneIdOrderByDateGenerationDesc(operationEpargneId).stream()
                .filter(ticket -> canReadTicket(ticket.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TicketRecuResponse> getByDemandeRetrait(Long demandeRetraitId) {
        return ticketRecuRepository.findByDemandeRetraitEpargneIdOrderByDateGenerationDesc(demandeRetraitId).stream()
                .filter(ticket -> canReadTicket(ticket.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TicketRecuResponse> getByMembre(Long membreId) {
        if (!canReadMembreTickets(membreId)) {
            throw new BusinessException("Accès refusé aux tickets de ce membre");
        }
        return ticketRecuRepository.findByMembreIdOrderByDateGenerationDesc(membreId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TicketRecuResponse marquerImpression(Long id, TicketPrintRequest request) {
        TicketRecu ticket = findTicketScoped(id);
        Utilisateur user = SecurityUtils.getCurrentUser();
        boolean success = request == null || request.getImpressionReussie() == null || Boolean.TRUE.equals(request.getImpressionReussie());
        if (success && (request == null || request.isMarquerImprime())) {
            ticket.setStatut(StatutTicketRecu.IMPRIME);
            ticket.setNombreImpressions(ticket.getNombreImpressions() + 1);
            ticket.setDateDerniereImpression(LocalDateTime.now());
            ticket.setUtilisateurImpression(user);
            ticket.setUpdatedBy(user);
            ticket.setCommentaire(append(ticket.getCommentaire(), request != null ? request.getCommentaire() : null));
            audit(ticket, AuditAction.TICKET_RECU_PRINTED, "Impression ticket reçu", null, true);
        } else {
            ticket.setStatut(StatutTicketRecu.ECHEC_IMPRESSION);
            ticket.setUpdatedBy(user);
            ticket.setCommentaire(append(ticket.getCommentaire(), request != null ? request.getCommentaire() : null));
            audit(ticket, AuditAction.TICKET_RECU_PRINT_FAILED, "Échec impression ticket reçu", null, false);
        }
        return toResponse(ticketRecuRepository.save(ticket));
    }

    @Override
    public TicketRecuResponse genererDuplicata(Long id, TicketDuplicataRequest request) {
        if (request == null || request.getMotif() == null || request.getMotif().isBlank()) {
            throw new BusinessException("Le motif du duplicata est obligatoire");
        }
        TicketRecu original = findTicketScoped(id);
        TicketRecu root = original.getOriginalTicket() != null ? original.getOriginalTicket() : original;
        Utilisateur user = SecurityUtils.getCurrentUser();
        int nextDuplicata = root.getNombreDuplicatas() + 1;
        root.setNombreDuplicatas(nextDuplicata);
        root.setDateDernierDuplicata(LocalDateTime.now());
        root.setUtilisateurDuplicata(user);
        root.setUpdatedBy(user);

        TicketRecu duplicata = TicketRecu.builder()
                .numeroTicket(root.getNumeroTicket() + "-D" + String.format("%02d", nextDuplicata))
                .typeTicket(TypeTicketRecu.DUPLICATA)
                .statut(StatutTicketRecu.DUPLICATA_GENERE)
                .originalTicket(root)
                .dateGeneration(LocalDateTime.now())
                .operationEpargne(root.getOperationEpargne())
                .operationCaisse(root.getOperationCaisse())
                .operationCaisseCommission(root.getOperationCaisseCommission())
                .demandeRetraitEpargne(root.getDemandeRetraitEpargne())
                .collecteJournaliere(root.getCollecteJournaliere())
                .collecteMembreLigne(root.getCollecteMembreLigne())
                .sessionCaisse(root.getSessionCaisse())
                .caisse(root.getCaisse())
                .membre(root.getMembre())
                .compteEpargne(root.getCompteEpargne())
                .agence(root.getAgence())
                .site(root.getSite())
                .utilisateurCreateur(root.getUtilisateurCreateur())
                .utilisateurDuplicata(user)
                .devise(root.getDevise())
                .montantPrincipal(root.getMontantPrincipal())
                .tauxCommission(root.getTauxCommission())
                .montantCommission(root.getMontantCommission())
                .montantTotalDebite(root.getMontantTotalDebite())
                .montantRemisMembre(root.getMontantRemisMembre())
                .ancienSolde(root.getAncienSolde())
                .nouveauSolde(root.getNouveauSolde())
                .commentaire(clean(request.getCommentaire()))
                .motifDuplicata(request.getMotif().trim())
                .codeVerification(generateVerificationCode())
                .createdBy(user)
                .build();
        duplicata.setQrPayload(buildQrPayload(duplicata));
        ticketRecuRepository.save(root);
        duplicata = ticketRecuRepository.save(duplicata);
        audit(duplicata, AuditAction.TICKET_RECU_DUPLICATED, "Duplicata ticket reçu", request.getMotif(), true);
        return toResponse(duplicata);
    }

    @Override
    public String getPrintableHtml(Long id, boolean duplicata) {
        TicketRecu ticket = findTicketScoped(id);
        return buildPrintableHtml(ticket, duplicata || ticket.getOriginalTicket() != null);
    }

    @Override
    public TicketVerificationResponse verify(String codeVerification) {
        TicketRecu ticket = ticketRecuRepository.findByCodeVerification(codeVerification)
                .orElse(null);
        if (ticket == null) {
            return TicketVerificationResponse.builder().valide(false).message("Ticket introuvable").build();
        }
        audit(ticket, AuditAction.TICKET_RECU_VERIFIED, "Vérification authenticité ticket", null, true);
        return TicketVerificationResponse.builder()
                .valide(true)
                .numeroTicket(ticket.getNumeroTicket())
                .typeTicket(ticket.getTypeTicket())
                .dateGeneration(ticket.getDateGeneration())
                .montantPrincipal(ticket.getMontantPrincipal())
                .devise(ticket.getDevise())
                .membreMasque(maskMember(ticket.getMembre()))
                .statut(ticket.getStatut())
                .duplicata(ticket.getOriginalTicket() != null || ticket.getTypeTicket() == TypeTicketRecu.DUPLICATA)
                .message("Ticket authentique")
                .build();
    }

    @Override
    public boolean canReadTicket(Long id) {
        return ticketRecuRepository.findById(id)
                .map(ticket -> canReadMembreTickets(ticket.getMembre().getId()))
                .orElse(false);
    }

    @Override
    public boolean canReadMembreTickets(Long membreId) {
        Utilisateur user = SecurityUtils.getCurrentUser();
        if (user == null || user.getRole() == null) {
            return false;
        }
        RoleCode role = user.getRole().getCode();
        if (role == RoleCode.ADMIN || role == RoleCode.CHEF_BUREAU || role == RoleCode.CAISSIER
                || role == RoleCode.CONTROLEUR || role == RoleCode.RCI || role == RoleCode.COO
                || role == RoleCode.GERANT_GENERAL) {
            return true;
        }
        return scopeService.canReadMembreEpargne(membreId);
    }

    private void validateGenerationRequest(TicketRecuGenerationRequest request) {
        if (request == null) {
            throw new BusinessException("La demande de génération ticket est obligatoire");
        }
        if (request.getTypeTicket() == null || request.getTypeTicket() == TypeTicketRecu.DUPLICATA) {
            throw new BusinessException("Le type de ticket original est invalide");
        }
        if (request.getOperationEpargneId() == null) {
            throw new BusinessException("Impossible de créer un ticket sans opération épargne existante");
        }
        if (request.getMembreId() == null || request.getCompteEpargneId() == null) {
            throw new BusinessException("Le membre et le compte épargne sont obligatoires");
        }
        if (request.getMontantPrincipal() == null || request.getAncienSolde() == null || request.getNouveauSolde() == null) {
            throw new BusinessException("Les montants du ticket doivent provenir de l'opération métier");
        }
        if (request.getTypeTicket() == TypeTicketRecu.RETRAIT_EPARGNE
                && (request.getDemandeRetraitEpargneId() == null || request.getOperationCaisseId() == null)) {
            throw new BusinessException("Le ticket retrait doit être lié à la demande et à la sortie caisse");
        }
        if (request.getTypeTicket() == TypeTicketRecu.COLLECTE_TERRAIN
                && (request.getCollecteJournaliereId() == null || request.getCollecteMembreLigneId() == null)) {
            throw new BusinessException("Le ticket collecte terrain doit être lié à la collecte et à la ligne membre");
        }
    }

    private TicketRecu findTicketScoped(Long id) {
        TicketRecu ticket = ticketRecuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket reçu introuvable"));
        if (!canReadMembreTickets(ticket.getMembre().getId())) {
            throw new BusinessException("Accès refusé au ticket reçu");
        }
        return ticket;
    }

    private TicketRecuResponse toResponse(TicketRecu ticket) {
        TicketRecu original = ticket.getOriginalTicket();
        DemandeRetraitEpargne demande = ticket.getDemandeRetraitEpargne();
        return TicketRecuResponse.builder()
                .id(ticket.getId())
                .numeroTicket(ticket.getNumeroTicket())
                .typeTicket(ticket.getTypeTicket())
                .statut(ticket.getStatut())
                .duplicata(original != null || ticket.getTypeTicket() == TypeTicketRecu.DUPLICATA)
                .numeroOriginal(original != null ? original.getNumeroTicket() : null)
                .nombreImpressions(ticket.getNombreImpressions())
                .nombreDuplicatas(ticket.getNombreDuplicatas())
                .dateGeneration(ticket.getDateGeneration())
                .dateDerniereImpression(ticket.getDateDerniereImpression())
                .dateDernierDuplicata(ticket.getDateDernierDuplicata())
                .operationEpargneId(idOf(ticket.getOperationEpargne()))
                .operationCaisseId(idOf(ticket.getOperationCaisse()))
                .operationCaisseCommissionId(idOf(ticket.getOperationCaisseCommission()))
                .demandeRetraitEpargneId(idOf(demande))
                .collecteJournaliereId(idOf(ticket.getCollecteJournaliere()))
                .collecteMembreLigneId(idOf(ticket.getCollecteMembreLigne()))
                .membreNomComplet(ticket.getMembre().getNomComplet())
                .numeroMembre(ticket.getMembre().getCodeMembre())
                .numeroCompte(ticket.getCompteEpargne().getNumeroCompte())
                .typeOperation(ticket.getTypeTicket().name())
                .devise(ticket.getDevise())
                .montantPrincipal(ticket.getMontantPrincipal())
                .tauxCommission(ticket.getTauxCommission())
                .montantCommission(ticket.getMontantCommission())
                .montantTotalDebite(ticket.getMontantTotalDebite())
                .montantRemisMembre(ticket.getMontantRemisMembre())
                .ancienSolde(ticket.getAncienSolde())
                .nouveauSolde(ticket.getNouveauSolde())
                .agenceNom(ticket.getAgence() != null ? ticket.getAgence().getNomAgence() : null)
                .siteNom(ticket.getSite() != null ? ticket.getSite().getNomSite() : null)
                .caisseCode(ticket.getCaisse() != null ? ticket.getCaisse().getCodeCaisse() : null)
                .sessionCaisseId(idOf(ticket.getSessionCaisse()))
                .utilisateurNom(ticket.getUtilisateurCreateur() != null ? ticket.getUtilisateurCreateur().getNomComplet() : null)
                .controleurNom(demande != null && demande.getValidePar() != null ? demande.getValidePar().getNomComplet() : null)
                .caissierNom(ticket.getUtilisateurCreateur() != null ? ticket.getUtilisateurCreateur().getNomComplet() : null)
                .codeVerification(ticket.getCodeVerification())
                .qrPayload(ticket.getQrPayload())
                .commentaire(ticket.getCommentaire())
                .motifDuplicata(ticket.getMotifDuplicata())
                .build();
    }

    private String generateNumeroTicket(Agence agence, LocalDateTime now) {
        String agenceCode = agence != null && agence.getCodeAgence() != null ? agence.getCodeAgence() : "GEN";
        String prefix = "TIC-" + DATE_NUMERO.format(now) + "-" + sanitizeCode(agenceCode) + "-";
        for (int i = 0; i < 10; i++) {
            String candidate = prefix + String.format("%06d", Math.abs(RANDOM.nextInt(999999)) + 1);
            if (!ticketRecuRepository.existsByNumeroTicket(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("Impossible de générer un numéro de ticket unique");
    }

    private String generateVerificationCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomBlock(4) + "-" + randomBlock(4);
            if (!ticketRecuRepository.existsByCodeVerification(code)) {
                return code;
            }
        }
        throw new BusinessException("Impossible de générer un code de vérification unique");
    }

    private String randomBlock(int size) {
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private String buildQrPayload(TicketRecu ticket) {
        return "TICKET:" + ticket.getNumeroTicket() + ":" + ticket.getCodeVerification();
    }

    private String buildPrintableHtml(TicketRecu ticket, boolean duplicata) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\"><style>")
            .append("@page{size:80mm auto;margin:4mm}body{font-family:monospace;font-size:12px;color:#111}.ticket{width:72mm}.center{text-align:center}.line{border-top:1px dashed #111;margin:6px 0}.row{display:flex;justify-content:space-between;gap:8px}.dup{font-weight:bold;font-size:16px;text-align:center;margin:4px 0}.sig{margin-top:14px}")
            .append("</style></head><body><div class=\"ticket\">");
        if (duplicata) {
            html.append("<div class=\"dup\">*** DUPLICATA ***</div>");
            if (ticket.getMotifDuplicata() != null) {
                html.append("<div>Motif: ").append(escape(ticket.getMotifDuplicata())).append("</div>");
            }
        }
        html.append("<div class=\"center\"><strong>MINI-CREDIT 3N</strong></div>")
            .append(line("Agence", value(ticket.getAgence() != null ? ticket.getAgence().getNomAgence() : null)))
            .append(line("Site", value(ticket.getSite() != null ? ticket.getSite().getNomSite() : null)))
            .append(line("Ticket", ticket.getNumeroTicket()))
            .append(line("Date", formatDate(ticket.getDateGeneration())))
            .append(line("Session", ticket.getSessionCaisse() != null ? "#" + ticket.getSessionCaisse().getId() : "-"))
            .append(line("Caisse", value(ticket.getCaisse() != null ? ticket.getCaisse().getCodeCaisse() : null)))
            .append("<div class=\"line\"></div><strong>MEMBRE</strong>")
            .append(line("Nom", ticket.getMembre().getNomComplet()))
            .append(line("N membre", ticket.getMembre().getCodeMembre()))
            .append(line("Compte", maskAccount(ticket.getCompteEpargne().getNumeroCompte())))
            .append("<div class=\"line\"></div><strong>OPERATION</strong>")
            .append(line("Type", ticket.getOriginalTicket() != null ? ticket.getOriginalTicket().getTypeTicket().name() : ticket.getTypeTicket().name()))
            .append(line("Montant", money(ticket.getMontantPrincipal(), ticket.getDevise())));
        if (ticket.getMontantCommission() != null && ticket.getMontantCommission().compareTo(BigDecimal.ZERO) > 0) {
            html.append(line("Taux commission", ticket.getTauxCommission() + " %"))
                .append(line("Commission", money(ticket.getMontantCommission(), ticket.getDevise())))
                .append(line("Total debite", money(ticket.getMontantTotalDebite(), ticket.getDevise())))
                .append(line("Montant remis", money(ticket.getMontantRemisMembre(), ticket.getDevise())));
        }
        html.append(line("Ancien solde", money(ticket.getAncienSolde(), ticket.getDevise())))
            .append(line("Nouveau solde", money(ticket.getNouveauSolde(), ticket.getDevise())))
            .append("<div class=\"line\"></div>")
            .append(line("Ref operation", ticket.getOperationEpargne() != null ? "OPE-" + ticket.getOperationEpargne().getId() : "-"));
        if (ticket.getDemandeRetraitEpargne() != null) {
            html.append(line("Ref retrait", ticket.getDemandeRetraitEpargne().getReferenceRetrait()))
                .append(line("Controleur", ticket.getDemandeRetraitEpargne().getValidePar() != null ? ticket.getDemandeRetraitEpargne().getValidePar().getNomComplet() : "-"));
        }
        html.append(line("Agent/Caisse", ticket.getUtilisateurCreateur() != null ? ticket.getUtilisateurCreateur().getNomComplet() : "-"))
            .append(line("Code", ticket.getCodeVerification()))
            .append("<div class=\"line\"></div>")
            .append("<div class=\"sig\">Signature membre: __________</div>")
            .append("<div class=\"sig\">Signature caisse: __________</div>")
            .append("<p class=\"center\">Merci de conserver ce recu.</p>")
            .append("</div></body></html>");
        return html.toString();
    }

    private void audit(TicketRecu ticket, AuditAction action, String commentaire, String errorMessage, boolean success) {
        auditService.logAction(
                action,
                AuditModule.TICKET_RECU,
                "TicketRecu",
                ticket.getId(),
                success,
                success ? AuditSeverity.INFO : AuditSeverity.WARNING,
                commentaire,
                ticket.getNumeroTicket(),
                null,
                null,
                errorMessage,
                ticket.getCaisse() != null ? ticket.getCaisse().getId() : null,
                ticket.getSessionCaisse() != null ? ticket.getSessionCaisse().getId() : null,
                ticket.getSite() != null ? ticket.getSite().getId() : null,
                ticket.getSite() != null ? ticket.getSite().getNomSite() : null
        );
    }

    private Agence resolveAgence(Caisse caisse, SessionCaisse session, Membre membre) {
        if (caisse != null && caisse.getAgence() != null) return caisse.getAgence();
        if (session != null && session.getCaisse() != null && session.getCaisse().getAgence() != null) return session.getCaisse().getAgence();
        if (membre != null && membre.getSite() != null) return membre.getSite().getAgence();
        return null;
    }

    private Site resolveSite(Caisse caisse, SessionCaisse session, Membre membre) {
        if (caisse != null && caisse.getSite() != null) return caisse.getSite();
        if (session != null && session.getCaisse() != null && session.getCaisse().getSite() != null) return session.getCaisse().getSite();
        return membre != null ? membre.getSite() : null;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(field + " doit être positif ou nul");
        }
        return value;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String sanitizeCode(String code) {
        return code.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String append(String current, String addition) {
        String cleanAddition = clean(addition);
        if (cleanAddition == null) return current;
        return current == null || current.isBlank() ? cleanAddition : current + " | " + cleanAddition;
    }

    private String line(String label, String value) {
        return "<div class=\"row\"><span>" + escape(label) + "</span><strong>" + escape(value(value)) + "</strong></div>";
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String money(BigDecimal amount, String devise) {
        return (amount != null ? amount.stripTrailingZeros().toPlainString() : "0") + " " + value(devise);
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-";
    }

    private String maskAccount(String numeroCompte) {
        if (numeroCompte == null || numeroCompte.length() <= 4) return "****";
        return "****" + numeroCompte.substring(numeroCompte.length() - 4);
    }

    private String maskMember(Membre membre) {
        if (membre == null || membre.getNomComplet() == null || membre.getNomComplet().isBlank()) return "Membre";
        String[] parts = membre.getNomComplet().trim().split("\\s+");
        return parts[0] + " ***";
    }

    private String escape(String value) {
        return value(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private Long idOf(Object entity) {
        if (entity instanceof OperationEpargne value) return value.getId();
        if (entity instanceof OperationCaisse value) return value.getId();
        if (entity instanceof DemandeRetraitEpargne value) return value.getId();
        if (entity instanceof CollecteJournaliereTerrain value) return value.getId();
        if (entity instanceof CollecteMembreLigne value) return value.getId();
        if (entity instanceof SessionCaisse value) return value.getId();
        return null;
    }
}
