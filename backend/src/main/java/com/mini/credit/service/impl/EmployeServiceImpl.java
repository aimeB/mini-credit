package com.mini.credit.service.impl;

import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.UpdateEmployeRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.mapper.EmployeMapper;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.service.EmployeService;
import com.mini.credit.service.EmployePhotoStorageService;
import com.mini.credit.service.MatriculeGeneratorService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeServiceImpl implements EmployeService {

    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AgenceRepository agenceRepository;
    private final SiteRepository siteRepository;
    private final EmployeMapper employeMapper;
    private final MatriculeGeneratorService matriculeGeneratorService;
    private final EmployePhotoStorageService employePhotoStorageService;
    private final AuditService auditService;

    @Override
    public EmployeDTO create(CreateEmployeRequest request) {

        validatePersonUniqueness(request.getTelephone(), request.getPrenom(), request.getNom(), null);

        // 2. Utilisateur OPTIONNEL — validation côté utilisateur (source unique de vérité)
        Utilisateur utilisateur = null;
        if (request.getUtilisateurId() != null) {
            utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            // Vérifier que ce compte n'est pas déjà lié à un employé (via utilisateur.employe_id)
            if (utilisateur.getEmploye() != null) {
                throw new RuntimeException("Cet utilisateur est déjà associé à un employé");
            }
        }

        // 3. Agence obligatoire
        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        Site site = resolveSiteForFunction(request.getSiteId(), agence, request.getFonction());

        // 1. Génération automatique du matricule AGENCE-FONCTION-AA-SEQ
        String matricule = matriculeGeneratorService.generer(agence, request.getFonction());

        // Création entité via mapper (pas de setUtilisateur — le lien est du côté Utilisateur)
        Employe employe = employeMapper.toEntity(request);
        employe.setMatricule(matricule);
        // Compatibilité : code_employe = matricule (alias technique, champ hérité)
        employe.setCode_employe(matricule);
        employe.setAgence(agence);
        employe.setSite(site);
        employe.setActif(true);

        Employe saved = employeRepository.save(employe);

        // Si utilisateurId fourni : poser le lien depuis le côté propriétaire (utilisateur.employe_id)
        if (utilisateur != null) {
            utilisateur.setEmploye(saved);
            utilisateurRepository.save(utilisateur);
        }

        return employeMapper.toDTO(saved);
    }

    @Override
    public EmployeDTO update(Long id, UpdateEmployeRequest request) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        String nextTelephone = request.getTelephone() != null ? request.getTelephone() : employe.getTelephone();
        String nextPrenom = request.getPrenom() != null ? request.getPrenom() : employe.getPrenom();
        String nextNom = request.getNom() != null ? request.getNom() : employe.getNom();
        validatePersonUniqueness(nextTelephone, nextPrenom, nextNom, id);

        // Si agenceId fourni, valider et changer
        if (request.getAgenceId() != null && !request.getAgenceId().equals(employe.getAgence().getId())) {
            Agence newAgence = agenceRepository.findById(request.getAgenceId())
                    .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
            employe.setAgence(newAgence);
        }

        // Mise à jour autres champs via mapper
        employeMapper.updateEntityFromDTO(request, employe);

        employe.setSite(resolveSiteForUpdate(request.getSiteId(), employe.getAgence(), employe.getFonction(), employe.getSite()));

        validateSiteRequirement(employe.getSite(), employe.getFonction());

        // Lier un utilisateur si fourni et pas encore lié
        if (request.getUtilisateurId() != null && employe.getUtilisateur() == null) {
            Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            // Vérifier que ce compte n'est pas déjà lié (source unique : utilisateur.employe_id)
            if (utilisateur.getEmploye() != null) {
                throw new RuntimeException("Cet utilisateur est déjà associé à un employé");
            }
            // Poser le lien depuis le côté propriétaire
            utilisateur.setEmploye(employe);
            utilisateurRepository.save(utilisateur);
        }

        Employe updated = employeRepository.save(employe);
        return employeMapper.toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeDTO getById(Long id) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return employeMapper.toDTO(employe);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeDTO getByMatricule(String matricule) {
        Employe employe = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return employeMapper.toDTO(employe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getAll() {
        return employeRepository.findAll().stream()
                .map(employeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getAllActive() {
        return employeRepository.findByActifTrue().stream()
                .map(employeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getDisponibles() {
        // Employés actifs sans compte utilisateur lié (utilisateur.employe_id IS NULL pour cet employé)
        return employeRepository.findDisponibles().stream()
                .map(employeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getGestionnaires() {
        return employeRepository.findByFonctionAndActifTrue(PosteEmploye.GESTIONNAIRE).stream()
                .map(employeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getEmployesByAgence(Long agenceId) {
        // Vérification existence agence
        if (!agenceRepository.existsById(agenceId)) {
            throw new RuntimeException("Agence non trouvée : id=" + agenceId);
        }
        return employeRepository.findByAgenceIdAndActifTrue(agenceId).stream()
                .map(employeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getEmployesAAffecterAgence(Long agenceId) {
        if (!agenceRepository.existsById(agenceId)) {
            throw new RuntimeException("Agence non trouvée : id=" + agenceId);
        }
        return employeRepository.findByAgenceIdNotAndActifTrue(agenceId).stream()
                .map(employeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeDTO changerAgence(Long employeId, Long agenceId, Long siteId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé : id=" + employeId));
        Agence nouvelleAgence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée : id=" + agenceId));
        Site nouveauSite = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site non trouvé : id=" + siteId));
        if (!nouveauSite.getAgence().getId().equals(agenceId)) {
            throw new RuntimeException(
                "Le site doit appartenir à la nouvelle agence (site.agenceId=" + nouveauSite.getAgence().getId()
                + " / agenceId demandé=" + agenceId + ")");
        }
        employe.setAgence(nouvelleAgence);
        employe.setSite(nouveauSite);
        return employeMapper.toDTO(employeRepository.save(employe));
    }

    @Override
    public EmployeDTO uploadPhoto(Long employeId, org.springframework.web.multipart.MultipartFile file) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        String oldPhotoUrl = employe.getPhotoUrl();
        String newPhotoUrl = employePhotoStorageService.store(employeId, file);
        employe.setPhotoUrl(newPhotoUrl);

        Employe updated;
        try {
            updated = employeRepository.save(employe);
        } catch (RuntimeException ex) {
            employePhotoStorageService.deleteIfManaged(newPhotoUrl);
            throw ex;
        }

        employePhotoStorageService.deleteIfManaged(oldPhotoUrl);
        auditService.logInfo(
                AuditAction.EMPLOYE_PHOTO_UPDATED,
                AuditModule.PERSONNEL,
                "Employe",
                employeId,
                oldPhotoUrl == null ? "Photo employé ajoutée" : "Photo employé remplacée",
                employe.getMatricule());
        return employeMapper.toDTO(updated);
    }

    @Override
    public EmployeDTO deletePhoto(Long employeId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        String oldPhotoUrl = employe.getPhotoUrl();
        employe.setPhotoUrl(null);
        Employe updated = employeRepository.save(employe);
        employePhotoStorageService.deleteIfManaged(oldPhotoUrl);

        if (oldPhotoUrl != null) {
            auditService.logInfo(
                    AuditAction.EMPLOYE_PHOTO_UPDATED,
                    AuditModule.PERSONNEL,
                    "Employe",
                    employeId,
                    "Photo employé supprimée",
                    employe.getMatricule());
        }
        return employeMapper.toDTO(updated);
    }

    @Override
    public void delete(Long id) {
        if (!employeRepository.existsById(id)) {
            throw new RuntimeException("Employé non trouvé");
        }
        employeRepository.deleteById(id);
    }

    private Site resolveSiteForFunction(Long siteId, Agence agence, PosteEmploye fonction) {
        if (fonction != PosteEmploye.AGENT_TERRAIN) {
            return null;
        }

        if (siteId == null) {
            validateSiteRequirement(null, fonction);
            return null;
        }

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site non trouvé"));

        if (!site.getAgence().getId().equals(agence.getId())) {
            throw new RuntimeException("Site doit appartenir à la même agence que l'employé");
        }

        return site;
    }

    private void validateSiteRequirement(Site site, PosteEmploye fonction) {
        if (site == null && fonction == PosteEmploye.AGENT_TERRAIN) {
            throw new RuntimeException("Le site est obligatoire pour un Agent Terrain.");
        }
    }

    private Site resolveSiteForUpdate(Long siteId, Agence agence, PosteEmploye fonction, Site currentSite) {
        if (fonction != PosteEmploye.AGENT_TERRAIN) {
            return null;
        }

        if (siteId == null) {
            validateSiteRequirement(currentSite, fonction);
            return currentSite;
        }

        return resolveSiteForFunction(siteId, agence, fonction);
    }

    private void validatePersonUniqueness(String telephone, String prenom, String nom, Long excludedId) {
        String normalizedTelephone = normalizePhone(telephone);
        if (normalizedTelephone != null
                && employeRepository.existsByNormalizedTelephoneExcludingId(normalizedTelephone, excludedId)) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé.");
        }

        String normalizedPrenom = normalizeName(prenom);
        String normalizedNom = normalizeName(nom);
        if (normalizedPrenom != null && normalizedNom != null
                && employeRepository.existsByNormalizedPrenomAndNomExcludingId(normalizedPrenom, normalizedNom, excludedId)) {
            throw new RuntimeException("Une personne avec le même prénom et le même nom existe déjà.");
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
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
