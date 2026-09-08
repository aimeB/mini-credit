package com.mini.credit.service.impl;

import com.mini.credit.dto.membre.MembreCreateRequest;
import com.mini.credit.dto.membre.MembreCreationResponseDTO;
import com.mini.credit.dto.membre.MembreActivationResponseDTO;
import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.dto.membre.MembreUpdateRequest;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.MembreMapper;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.MembreService;
import com.mini.credit.service.UtilisateurService;
import com.mini.credit.service.ActivationService;
import com.mini.credit.service.ReferenceGeneratorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class MembreServiceImpl implements MembreService {

    private final MembreRepository membreRepository;
    private final SiteRepository siteRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final MembreMapper membreMapper;
    private final UtilisateurService utilisateurService;
    private final ActivationService activationService;
    private final CompteEpargneRepository compteEpargneRepository;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final CollecteJournaliereTerrainRepository collecteRepository;
    private final CollecteMembreLigneRepository collecteLigneRepository;
    private final ParametreMetierService parametreMetierService;

    @Override
    @Transactional
    public MembreResponse create(MembreCreateRequest request) {
        validatePersonUniqueness(request.getTelephonePrincipal(), request.getPrenom(), request.getNom(), null);

        Site site = resolveTargetSite(request.getSiteId());
        AgentTerrain currentAgent = resolveCurrentAgentIfAgentTerrain();

        AgentTerrain agent = null;
        if (currentAgent != null) {
            agent = currentAgent;
            ensureAgentBelongsToSite(agent, site);
        } else if (request.getAgentId() != null) {
            agent = agentTerrainRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
            ensureAgentBelongsToSite(agent, site);
        }

        String nomComplet = buildNomComplet(request.getNom(), request.getPostnom(), request.getPrenom());

        // ✅ Générer un code membre unique garantissant l'unicité historique
        String codeMembre = generateCodeMembre();

        Membre membre = Membre.builder()
                .codeMembre(codeMembre)
                .nom(request.getNom())
                .postnom(request.getPostnom())
                .prenom(request.getPrenom())
                .nomComplet(nomComplet)
                .sexe(request.getSexe())
                .dateNaissance(request.getDateNaissance())
                .telephonePrincipal(request.getTelephonePrincipal())
                .telephoneSecondaire(request.getTelephoneSecondaire())
                .adresse(request.getAdresse())
                .quartier(request.getQuartier())
                .commune(request.getCommune())
                .ville(request.getVille() != null ? request.getVille() : "Kinshasa")
                .professionActivite(request.getProfessionActivite())
                .lieuActivite(request.getLieuActivite())
                .sourceInscription(request.getSourceInscription())
                .site(site)
                .agent(agent)
                .dateAdhesion(request.getDateAdhesion())
                .statut(StatutMembre.ACTIF)
                .observation(request.getObservation())
                .build();

        Membre savedMembre = membreRepository.save(membre);

        // 🚀 Créer automatiquement un Utilisateur avec le même email
        // ⚠️ OBLIGATOIRE - la création échoue complètement si l'utilisateur n'est pas créé
        try {
            String username = generateUsername(request.getNom(), request.getPrenom());

            CreateUtilisateurRequest userRequest = CreateUtilisateurRequest.builder()
                    .username(username)
                    .email(request.getEmail())
                    .nomComplet(nomComplet)
                    .telephone(request.getTelephonePrincipal())
                    .roles(List.of("MEMBER"))
                    .build();

            Utilisateur utilisateur = utilisateurService.createRawEntity(userRequest);
            savedMembre.setUtilisateurSync(utilisateur);
            membreRepository.save(savedMembre);

            createDefaultSavingsAccount(savedMembre);
            enregistrerFraisCarnetAdhesionSiAgentTerrain(savedMembre, currentAgent, request.getObservation());

            System.out.println("✓ Utilisateur créé automatiquement pour le membre " + savedMembre.getId());
        } catch (Exception e) {
            // ❌ TRANSACTION ROLLBACK - pas de membre sans utilisateur !
            System.err.println("❌ Erreur CRITIQUE lors de la création de l'utilisateur pour le membre: " + e.getMessage());
            throw new BusinessException("Impossible de créer le membre: la création de l'utilisateur a échoué. " + e.getMessage(), e);
        }

        return membreMapper.toResponse(savedMembre);
    }

    private Site resolveTargetSite(Long requestedSiteId) {
        Utilisateur currentUser = getCurrentUtilisateur();

        if (currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().getCode() == com.mini.credit.enums.security.RoleCode.AGENT_TERRAIN) {
            AgentTerrain currentAgent = agentTerrainRepository.findByUtilisateurId(currentUser.getId())
                    .orElseThrow(() -> new BusinessException("Agent terrain non lié à l'utilisateur connecté"));

            if (currentAgent.getSite() == null || currentAgent.getSite().getId() == null) {
                throw new BusinessException("Aucun site affecté à votre compte");
            }

            return currentAgent.getSite();
        }

        if (requestedSiteId == null) {
            throw new BusinessException("Le site est obligatoire");
        }

        return siteRepository.findById(requestedSiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
    }

    private void ensureAgentBelongsToSite(AgentTerrain agent, Site site) {
        if (site == null || site.getId() == null) {
            throw new BusinessException("Le site est obligatoire");
        }
        if (agent.getSite() != null && agent.getSite().getId() != null && !agent.getSite().getId().equals(site.getId())) {
            throw new BusinessException("L'agent terrain sélectionné n'appartient pas à ce site");
        }
    }

    private void createDefaultSavingsAccount(Membre membre) {
        if (compteEpargneRepository.existsByMembreIdAndStatut(membre.getId(), StatutCompte.ACTIF)) {
            throw new BusinessException("Ce membre possède déjà un compte épargne actif");
        }

        CompteEpargne compte = CompteEpargne.builder()
                .membre(membre)
                .numeroCompte(referenceGeneratorService.genererReference("CEP"))
                .typeCompte(TypeCompteEpargne.EPARGNE_VOLONTAIRE)
                .dateOuverture(LocalDate.now())
                .soldeDisponible(BigDecimal.ZERO)
                .soldeBloque(BigDecimal.ZERO)
                .statut(StatutCompte.ACTIF)
                .build();

        compteEpargneRepository.save(compte);
    }

    private void enregistrerFraisCarnetAdhesionSiAgentTerrain(Membre membre, AgentTerrain agent, String commentaire) {
        if (agent == null || membre == null || membre.getId() == null) {
            return;
        }

        BigDecimal fraisCarnet = parametreMetierService.getDecimal("FRAIS_CARNET_EPARGNE");
        if (fraisCarnet == null || fraisCarnet.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le paramètre métier FRAIS_CARNET_EPARGNE est obligatoire pour créer un membre Agent Terrain");
        }

        LocalDate dateCollecte = membre.getDateAdhesion() != null ? membre.getDateAdhesion() : LocalDate.now();
        CollecteJournaliereTerrain collecte = collecteRepository
                .findByAgentTerrainIdAndDateCollecte(agent.getId(), dateCollecte)
                .orElseGet(() -> creerCollecteAdhesion(agent, dateCollecte));

        if (collecte.getStatut() != RecetteStatut.BROUILLON) {
            throw new BusinessException("La collecte journalière de l'agent est déjà soumise ou validée: impossible d'ajouter le frais carnet d'adhésion");
        }

        if (collecteLigneRepository.existsByCollecteIdAndMembreIdAndTypeLigne(
                collecte.getId(), membre.getId(), TypeLigneCollecte.CARNET)) {
            return;
        }

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
                .collecte(collecte)
                .membre(membre)
                .typeLigne(TypeLigneCollecte.CARNET)
                .montant(fraisCarnet)
                .quantite(1)
                .reference("ADHESION-MEMBRE-" + membre.getId())
                .commentaire(cleanNullableText(commentaire) != null ? cleanNullableText(commentaire) : "Frais carnet épargne - adhésion")
                .build();
        collecteLigneRepository.save(ligne);

        collecte.setTotalFraisCalcule(safeAmount(collecte.getTotalFraisCalcule()).add(fraisCarnet));
        collecte.setTotalCarnetsCalcule((collecte.getTotalCarnetsCalcule() != null ? collecte.getTotalCarnetsCalcule() : 0) + 1);
        collecte.setTotalGeneralCalcule(safeAmount(collecte.getTotalGeneralCalcule()).add(fraisCarnet));
        collecte.setEspecesDeclareesAgent(safeAmount(collecte.getEspecesDeclareesAgent()).add(fraisCarnet));
        collecte.setEspecesRemises(safeAmount(collecte.getEspecesRemises()).add(fraisCarnet));
        collecte.setEcartTresorerie(safeAmount(collecte.getEspecesDeclareesAgent()).subtract(safeAmount(collecte.getTotalGeneralCalcule())));
        collecteRepository.save(collecte);
    }

    private CollecteJournaliereTerrain creerCollecteAdhesion(AgentTerrain agent, LocalDate dateCollecte) {
        Long antenneId = agent.getSite() != null && agent.getSite().getAgence() != null
                ? agent.getSite().getAgence().getId()
                : null;
        if (antenneId == null) {
            throw new BusinessException("Antenne introuvable pour le site de l'agent terrain");
        }

        Utilisateur currentUser = getCurrentUtilisateur();
        CollecteJournaliereTerrain collecte = CollecteJournaliereTerrain.builder()
                .agentTerrain(agent)
                .site(agent.getSite())
                .antenneId(antenneId)
                .dateCollecte(dateCollecte)
                .statut(RecetteStatut.BROUILLON)
                .createdBy(currentUser != null ? currentUser.getId() : null)
                .observations("Créée automatiquement lors d'une adhésion membre")
                .build();
        return collecteRepository.save(collecte);
    }

    private AgentTerrain resolveCurrentAgentIfAgentTerrain() {
        Utilisateur currentUser = getCurrentUtilisateur();
        if (currentUser == null || currentUser.getRole() == null
                || currentUser.getRole().getCode() != com.mini.credit.enums.security.RoleCode.AGENT_TERRAIN) {
            return null;
        }
        return agentTerrainRepository.findByUtilisateurId(currentUser.getId())
                .orElseThrow(() -> new BusinessException("Agent terrain non lié à l'utilisateur connecté"));
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String cleanNullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public MembreResponse update(Long id, MembreUpdateRequest request) {
        // 🔒 Utilise findByIdActiveWithEagerLoad() pour exclure les membres CLOTURE
        Membre membre = membreRepository.findByIdActiveWithEagerLoad(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable ou clôturé - modification impossible"));

        String nextTelephone = request.getTelephonePrincipal() != null ? request.getTelephonePrincipal() : membre.getTelephonePrincipal();
        String nextPrenom = request.getPrenom() != null ? request.getPrenom() : membre.getPrenom();
        String nextNom = request.getNom() != null ? request.getNom() : membre.getNom();
        validatePersonUniqueness(nextTelephone, nextPrenom, nextNom, id);

        if (request.getNom() != null) membre.setNom(request.getNom());
        if (request.getPostnom() != null) membre.setPostnom(request.getPostnom());
        if (request.getPrenom() != null) membre.setPrenom(request.getPrenom());

        membre.setNomComplet(buildNomComplet(membre.getNom(), membre.getPostnom(), membre.getPrenom()));

        if (request.getSexe() != null) membre.setSexe(request.getSexe());
        if (request.getDateNaissance() != null) membre.setDateNaissance(request.getDateNaissance());
        if (request.getTelephonePrincipal() != null) membre.setTelephonePrincipal(request.getTelephonePrincipal());
        if (request.getTelephoneSecondaire() != null) membre.setTelephoneSecondaire(request.getTelephoneSecondaire());
        if (request.getAdresse() != null) membre.setAdresse(request.getAdresse());
        if (request.getQuartier() != null) membre.setQuartier(request.getQuartier());
        if (request.getCommune() != null) membre.setCommune(request.getCommune());
        if (request.getVille() != null) membre.setVille(request.getVille());
        if (request.getProfessionActivite() != null) membre.setProfessionActivite(request.getProfessionActivite());
        if (request.getLieuActivite() != null) membre.setLieuActivite(request.getLieuActivite());
        if (request.getSourceInscription() != null) membre.setSourceInscription(request.getSourceInscription());
        if (request.getDateAdhesion() != null) membre.setDateAdhesion(request.getDateAdhesion());
        
        // ⚠️ ATTENTION: Ne pas autoriser de changer le statut de ACTIF vers autre chose depuis ici
        // Le changement de statut doit passer par des endpoints dédiés (ex: suspension, blocage)
        if (request.getStatut() != null && request.getStatut() != StatutMembre.CLOTURE) {
            membre.setStatut(request.getStatut());
        }
        
        if (request.getObservation() != null) membre.setObservation(request.getObservation());

        if (request.getSiteId() != null) {
            Site site = resolveTargetSite(request.getSiteId());
            membre.setSite(site);
        }

        if (request.getAgentId() != null) {
            AgentTerrain agent = agentTerrainRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
            ensureAgentBelongsToSite(agent, membre.getSite());
            membre.setAgent(agent);
        }

        return membreMapper.toResponse(membreRepository.save(membre));
    }

    @Override
    public MembreResponse getById(Long id) {
        // 🔒 Utilise findByIdActiveWithEagerLoad() pour exclure les membres CLOTURE
        return membreMapper.toResponse(
                membreRepository.findByIdActiveWithEagerLoad(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable ou clôturé"))
        );
    }

    @Override
    public List<MembreResponse> getAll() {
        Utilisateur currentUser = getCurrentUtilisateur();

        if (currentUser == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        if (currentUser.getRole() != null
                && currentUser.getRole().getCode() == com.mini.credit.enums.security.RoleCode.AGENT_TERRAIN) {

            AgentTerrain agent = agentTerrainRepository.findByUtilisateurId(currentUser.getId())
                    .orElseThrow(() -> new BusinessException("Agent terrain non lié à l'utilisateur connecté"));

            if (agent.getSite() == null) {
                throw new BusinessException("Agent terrain sans site assigné");
            }

            // 🔒 Utilise findAllActive() pour exclure les membres CLOTURE
            return membreRepository.findAllActive().stream()
                    .filter(m -> m.getSite() != null && m.getSite().getId().equals(agent.getSite().getId()))
                    .map(membreMapper::toResponse)
                    .toList();
        }

        // 🔒 Utilise findAllActive() pour exclure les membres CLOTURE
        return membreRepository.findAllActive().stream()
                .map(membreMapper::toResponse)
                .toList();
    }


    private Utilisateur getCurrentUtilisateur() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur)) {
            return null;
        }

        return (Utilisateur) authentication.getPrincipal();
    }


    @Override
    public void delete(Long id) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));

        membre.setStatut(StatutMembre.CLOTURE);
        membreRepository.save(membre);
    }

    @Override
    public boolean isCurrentUser(Long membreId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur)) {
            return false;
        }

        Utilisateur utilisateur = (Utilisateur) authentication.getPrincipal();

        // Pour les utilisateurs avec un membre, vérifier si l'ID demandé correspond
        if (utilisateur.getMembre() != null) {
            return utilisateur.getMembre().getId().equals(membreId);
        }

        // Pour les autres rôles (ADMIN, CHEF_BUREAU, GESTIONNAIRE, AGENT_TERRAIN, CAISSIER), accès complet
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public MembreResponse getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur)) {
            throw new ResourceNotFoundException("Utilisateur non authentifié");
        }

        Utilisateur utilisateur = (Utilisateur) authentication.getPrincipal();
        String username = utilisateur.getUsername();

        // 🔒 Utilise findByUtilisateurUsernameActiveWithEagerLoad() pour exclure les membres CLOTURE
        Membre membre = membreRepository.findByUtilisateurUsernameActiveWithEagerLoad(username)
                .orElseThrow(() -> new ResourceNotFoundException("Accès non autorisé - utilisateur n'a pas de profil membre actif (compte clôturé?)"));

        return membreMapper.toResponse(membre);
    }

    private String generateUsername(String nom, String prenom) {
        // Format: "nom.prenom" en lowercase
        String baseUsername = (nom.toLowerCase() + "." + prenom.toLowerCase())
                .replaceAll("[^a-z0-9.]", "");
        
        // 🔒 Vérifier si le username existe déjà GLOBALEMENT (y compris membres clôturés)
        // Règle: Ne JAMAIS réutiliser un username, même d'un membre clôturé, sans confirmation
        List<String> existingUsernames = membreRepository.findAllExistingUsernames();
        
        String username = baseUsername;
        int counter = 1;
        while (existingUsernames.contains(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }

    /**
     * Génère un code membre unique en boucle jusqu'à obtenir un code non utilisé.
     * ✅ Garantit l'unicité historique du code (ne peut pas être réutilisé même si le membre est clôturé).
     * @return Un code membre unique au format MBR-XXXXXXXX
     */
    private String generateCodeMembre() {
        String code;
        do {
            code = "MBR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (membreRepository.existsByCodeMembre(code));  // ✅ Boucle jusqu'à unicité garantie
        return code;
    }

    private String buildNomComplet(String nom, String postnom, String prenom) {
        return String.join(" ",
                nom != null ? nom.trim() : "",
                postnom != null ? postnom.trim() : "",
                prenom != null ? prenom.trim() : ""
        ).trim().replaceAll("\\s+", " ");
    }

    private void validatePersonUniqueness(String telephone, String prenom, String nom, Long excludedId) {
        String normalizedTelephone = normalizePhone(telephone);
        if (normalizedTelephone != null
                && membreRepository.existsByNormalizedTelephonePrincipalExcludingId(normalizedTelephone, excludedId)) {
            throw new BusinessException("Ce numéro de téléphone est déjà utilisé.");
        }

        String normalizedPrenom = normalizeName(prenom);
        String normalizedNom = normalizeName(nom);
        if (normalizedPrenom != null && normalizedNom != null
                && membreRepository.existsByNormalizedPrenomAndNomExcludingId(normalizedPrenom, normalizedNom, excludedId)) {
            throw new BusinessException("Une personne avec le même prénom et le même nom existe déjà.");
        }
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replace(" ", "");
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public MembreCreationResponseDTO createWithCredentials(MembreCreateRequest request) {
        // Crée le membre normalement
        MembreResponse membreResponse = create(request);

        // Récupère les credentials du nouvel utilisateur
        Membre membre = membreRepository.findById(membreResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Membre créé introuvable"));
        
        Utilisateur utilisateur = membre.getUtilisateur();
        
        // Crée une nouvelle réponse avec credentials si l'utilisateur existe
        MembreCreationResponseDTO response = new MembreCreationResponseDTO();
        response.setMembre(membreResponse);
        response.setMessage("✅ Membre créé avec succès! Utilisateur provisoire généré.");
        
        if (utilisateur != null) {
            // On ne peut pas récupérer le mot de passe d'une autre façon
            // Car il est hashé en BD. On doit créer un nouveau.
            String motDePasse = com.mini.credit.util.PasswordGenerator.generateTemporaryPassword();
            
            response.setCredentials(com.mini.credit.dto.utilisateur.UtilisateurCredentialsDTO.of(
                    utilisateur.getUsername(),
                    motDePasse,
                    utilisateur.getEmail()
            ));
        }
        
        return response;
    }

    @Override
    public MembreActivationResponseDTO createWithActivationCode(MembreCreateRequest request) {
        // Crée le membre avec activation code
        MembreResponse membreResponse = create(request);
        
        // Récupère le membre créé
        Membre membre = membreRepository.findById(membreResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Membre créé introuvable"));
        
        Utilisateur utilisateur = membre.getUtilisateur();
        if (utilisateur == null) {
            throw new BusinessException("Utilisateur associé non trouvé pour le membre");
        }
        
        // Générer le code d'activation unique
        com.mini.credit.dto.auth.ActivationCodeDTO activationCode = activationService.generateActivationCode(utilisateur);
        
        // Créer les credentials avec le username pour affichage dans la modal
        com.mini.credit.dto.utilisateur.UtilisateurCredentialsDTO credentials = 
            com.mini.credit.dto.utilisateur.UtilisateurCredentialsDTO.builder()
                .username(utilisateur.getUsername())
                .email(utilisateur.getEmail())
                .message("✅ Username créé - À communiquer au membre")
                .build();
        
        // Créer et retourner la réponse avec le code ET les credentials
        return MembreActivationResponseDTO.builder()
                .membre(membreResponse)
                .activationCode(activationCode)
                .credentials(credentials)
                .message("✅ Membre créé avec succès! Code d'activation généré et prêt à être communiqué.")
                .build();
    }

    @Override
    public Page<MembreResponse> getAll(Pageable pageable) {
        // Aligne le endpoint pagine sur les memes regles de visibilite que la recherche:
        // membres non clotures + perimetre utilisateur (agent terrain force sur son site).
        return search(null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MembreResponse> search(String q, Long siteId, Pageable pageable) {
        Utilisateur currentUser = getCurrentUtilisateur();
        if (currentUser == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        Long effectiveSiteId = siteId;
        if (currentUser.getRole() != null
                && currentUser.getRole().getCode() == com.mini.credit.enums.security.RoleCode.AGENT_TERRAIN) {
            AgentTerrain agent = agentTerrainRepository.findByUtilisateurId(currentUser.getId())
                    .orElseThrow(() -> new BusinessException("Agent terrain non lié à l'utilisateur connecté"));
            if (agent.getSite() == null || agent.getSite().getId() == null) {
                throw new BusinessException("Aucun site affecté à votre compte");
            }
            effectiveSiteId = agent.getSite().getId();
        }

        return membreRepository.searchActive(q, effectiveSiteId, pageable).map(membreMapper::toResponse);
    }

}