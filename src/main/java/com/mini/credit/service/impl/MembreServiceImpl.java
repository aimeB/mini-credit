package com.mini.credit.service.impl;

import com.mini.credit.dto.membre.MembreCreateRequest;
import com.mini.credit.dto.membre.MembreCreationResponseDTO;
import com.mini.credit.dto.membre.MembreActivationResponseDTO;
import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.dto.membre.MembreUpdateRequest;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.MembreMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.site.SiteRepository;
import com.mini.credit.service.MembreService;
import com.mini.credit.service.UtilisateurService;
import com.mini.credit.service.ActivationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

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

    @Override
    @Transactional
    public MembreResponse create(MembreCreateRequest request) {
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));

        AgentTerrain agent = null;
        if (request.getAgentId() != null) {
            agent = agentTerrainRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
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
            
            System.out.println("✓ Utilisateur créé automatiquement pour le membre " + savedMembre.getId());
        } catch (Exception e) {
            // ❌ TRANSACTION ROLLBACK - pas de membre sans utilisateur !
            System.err.println("❌ Erreur CRITIQUE lors de la création de l'utilisateur pour le membre: " + e.getMessage());
            throw new BusinessException("Impossible de créer le membre: la création de l'utilisateur a échoué. " + e.getMessage(), e);
        }

        return membreMapper.toResponse(savedMembre);
    }

    @Override
    public MembreResponse update(Long id, MembreUpdateRequest request) {
        // 🔒 Utilise findByIdActiveWithEagerLoad() pour exclure les membres CLOTURE
        Membre membre = membreRepository.findByIdActiveWithEagerLoad(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable ou clôturé - modification impossible"));

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
            Site site = siteRepository.findById(request.getSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
            membre.setSite(site);
        }

        if (request.getAgentId() != null) {
            AgentTerrain agent = agentTerrainRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
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

        // Pour les autres rôles (ADMIN, RESPONSABLE, AGENT_BUREAU, AGENT_TERRAIN, CAISSIER), accès complet
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
        // PHASE 3B: Return paginated list of all active members
        return membreRepository.findAll(pageable).map(membreMapper::toResponse);
    }
}