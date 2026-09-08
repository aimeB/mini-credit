package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.AgentTerrainResponse;
import com.mini.credit.dto.referentiel.PortefeuilleGestionnaireResponse;
import com.mini.credit.dto.referentiel.SiteResponse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.service.AgentTerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller REST pour les endpoints Portefeuille d'un Gestionnaire.
 * Un Gestionnaire est un Employe avec fonction = GESTIONNAIRE.
 *
 * Protection IDOR : un GESTIONNAIRE ne peut consulter que son propre portefeuille.
 * ADMIN et CHEF_BUREAU peuvent consulter n'importe quel portefeuille.
 */
@RestController
@RequestMapping("/api/gestionnaires")
@RequiredArgsConstructor
@Tag(name = "Gestionnaire", description = "Portefeuille et supervision des agents terrain")
@SecurityRequirement(name = "bearerAuth")
public class GestionnaireController {

    private final AgentTerrainService agentTerrainService;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Retourne les agents terrain actifs supervisÃ©s par un gestionnaire.
    * ADMIN et CHEF_BUREAU : accÃ¨s total.
     * GESTIONNAIRE : uniquement ses propres agents.
     */
    @GetMapping("/{gestionnaireId}/agents")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE')")
    @Operation(summary = "Agents d'un gestionnaire", description = "Liste les agents terrain actifs supervisÃ©s par le gestionnaire")
    public ResponseEntity<List<AgentTerrainResponse>> getAgents(
            @PathVariable Long gestionnaireId,
            Principal principal) {

        verifierAccesGestionnaire(principal, gestionnaireId);
        return ResponseEntity.ok(agentTerrainService.getAgentsByGestionnaire(gestionnaireId));
    }

    /**
     * Retourne les sites distincts couverts par les agents d'un gestionnaire.
     */
    @GetMapping("/{gestionnaireId}/sites")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE')")
    @Operation(summary = "Sites d'un gestionnaire", description = "Liste les sites couverts par les agents du gestionnaire")
    public ResponseEntity<List<SiteResponse>> getSites(
            @PathVariable Long gestionnaireId,
            Principal principal) {

        verifierAccesGestionnaire(principal, gestionnaireId);
        return ResponseEntity.ok(agentTerrainService.getSitesByGestionnaire(gestionnaireId));
    }

    /**
     * Retourne le portefeuille calculÃ© complet d'un gestionnaire :
     * agents, sites, totalAgents, totalSites, totalMembres.
     */
    @GetMapping("/{gestionnaireId}/portefeuille")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE')")
    @Operation(summary = "Portefeuille d'un gestionnaire", description = "Vue agrÃ©gÃ©e : agents, sites et membres sous la supervision du gestionnaire")
    public ResponseEntity<PortefeuilleGestionnaireResponse> getPortefeuille(
            @PathVariable Long gestionnaireId,
            Principal principal) {

        verifierAccesGestionnaire(principal, gestionnaireId);

        List<AgentTerrainResponse> agents = agentTerrainService.getAgentsByGestionnaire(gestionnaireId);
        List<SiteResponse> sites = agentTerrainService.getSitesByGestionnaire(gestionnaireId);
        long totalMembres = agentTerrainService.countMembresByGestionnaire(gestionnaireId);

        // Nom du gestionnaire : rÃ©cupÃ©rÃ© depuis le premier agent ou directement
        String gestionnaireNomComplet = agents.isEmpty()
                ? null
                : agents.get(0).getGestionnaireNomComplet();

        PortefeuilleGestionnaireResponse portefeuille = PortefeuilleGestionnaireResponse.builder()
                .gestionnaireId(gestionnaireId)
                .gestionnaireNomComplet(gestionnaireNomComplet)
                .agents(agents)
                .sites(sites)
                .totalAgents(agents.size())
                .totalSites(sites.size())
                .totalMembres(totalMembres)
                .build();

        return ResponseEntity.ok(portefeuille);
    }

    // ===== Helpers =====

    /**
    * Protection IDOR : un GESTIONNAIRE ne peut accÃ©der qu'Ã  son propre portefeuille.
    * ADMIN et CHEF_BUREAU ont accÃ¨s sans restriction.
     */
    private void verifierAccesGestionnaire(Principal principal, Long gestionnaireId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentification requise");
        }

        Utilisateur utilisateur = utilisateurRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non trouvÃ©"));

        // ADMIN et CHEF_BUREAU : pas de restriction
        boolean isAdminOuChefBureau = utilisateur.getRole() != null &&
            (utilisateur.getRole().getCode().name().equals("ADMIN") ||
             utilisateur.getRole().getCode().name().equals("CHEF_BUREAU"));

        if (isAdminOuChefBureau) {
            return; // AccÃ¨s autorisÃ© sans restriction
        }

        // GESTIONNAIRE : vÃ©rifier que l'employe connectÃ© est bien le gestionnaire demandÃ©
        Employe employe = utilisateur.getEmploye();
        if (employe == null || !employe.getId().equals(gestionnaireId)) {
            throw new AccessDeniedException(
                "AccÃ¨s refusÃ© : vous ne pouvez consulter que votre propre portefeuille");
        }
    }
}

