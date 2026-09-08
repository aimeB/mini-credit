import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { UtilisateurService } from '../../services/utilisateur.service';
import { EmployeService } from '../../../employes/services/employe.service';
import { UtilisateurCreateRequest } from '../../models/utilisateur-create-request';
import { UtilisateurUpdateRequest } from '../../models/utilisateur-update-request';
import { UtilisateurResponse } from '../../models/utilisateur-response';
import { EmployeResponse } from '../../../employes/models/employe-response';
import { ROLES, ROLE_LABELS, ROLE_DESCRIPTIONS, ROLE_MODULES, OPERATIONAL_ROLES, GLOBAL_SUPERVISION_ROLES } from '../../models/role-enum';
import { FONCTION_LABELS, FonctionEmploye } from '../../../employes/models/fonction-employe';

@Component({
  selector: 'app-utilisateur-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './utilisateur-form.component.html'
})
export class UtilisateurFormComponent implements OnInit {
  private fb          = inject(FormBuilder);
  private utilisateurService = inject(UtilisateurService);
  private employeService     = inject(EmployeService);
  private router      = inject(Router);
  private route       = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  loading      = false;
  loadingData  = false;
  error        = '';
  success      = '';
  isEditMode   = false;
  utilisateurId: number | null = null;

  employes: EmployeResponse[] = [];
  loadingEmployes = false;
  selectedEmploye: EmployeResponse | null = null;

  showPassword        = false;
  showConfirmPassword = false;

  roles        = Object.values(ROLES);
  roleLabels   = ROLE_LABELS;
  roleDescriptions = ROLE_DESCRIPTIONS;
  roleModules  = ROLE_MODULES;

  /** Vrai si le rôle sélectionné est ADMIN ou MEMBER (pas besoin d'employé) */
  get isNonOperationalRole(): boolean {
    const selectedRoles = this.form.get('roles')?.value || [];
    if (selectedRoles.length === 0) return false;
    return !OPERATIONAL_ROLES.has(selectedRoles[0] as any);
  }

  /** Vrai si le rôle sélectionné est ADMIN */
  get isAdminRole(): boolean {
    return (this.form.get('roles')?.value || []).includes(ROLES.ADMIN);
  }

  /** Vrai si le rôle sélectionné est opérationnel */
  get isOperationalRole(): boolean {
    const selectedRoles = this.form.get('roles')?.value || [];
    return selectedRoles.length > 0 && OPERATIONAL_ROLES.has(selectedRoles[0] as any);
  }

  /** Vrai si le rôle sélectionné est Agent Terrain. */
  get isAgentTerrainRole(): boolean {
    return (this.form.get('roles')?.value || []).includes(ROLES.AGENT_TERRAIN);
  }

  /** Vrai si le rôle sélectionné supervise l'organisation sans rattachement site obligatoire. */
  get isGlobalSupervisionRole(): boolean {
    const selectedRoles = this.form.get('roles')?.value || [];
    return selectedRoles.length > 0 && GLOBAL_SUPERVISION_ROLES.has(selectedRoles[0] as any);
  }

  /**
   * Mapping rôle → fonction employé attendue — identique au backend.
   * Utilisé pour la validation côté client avant soumission.
   */
  private static readonly ROLE_TO_FONCTION: Record<string, string> = {
    'AGENT_TERRAIN': 'AGENT_TERRAIN',
    'GESTIONNAIRE':  'GESTIONNAIRE',
    'CONTROLEUR':    'CONTROLEUR',
    'CAISSIER':      'CAISSIER',
    'CHEF_BUREAU':   'CHEF_BUREAU',
    'RCI':           'RCI',
    'COO':           'COO',
    'GERANT_GENERAL':'GERANT_GENERAL',
  };

  /**
   * Vrai si le rôle sélectionné est incompatible avec la fonction de l'employé sélectionné.
   * ADMIN et MEMBER sont exemptés.
   */
  get roleFonctionMismatch(): boolean {
    if (!this.selectedEmploye?.fonction) return false;
    const selectedRoles = this.form.get('roles')?.value || [];
    if (selectedRoles.length === 0) return false;
    const fonctionAttendue = UtilisateurFormComponent.ROLE_TO_FONCTION[selectedRoles[0]];
    if (!fonctionAttendue) return false; // ADMIN, MEMBER → pas de contrainte
    return this.selectedEmploye.fonction !== fonctionAttendue;
  }

  /** Message d'erreur de mismatch rôle/fonction — lisible par l'utilisateur */
  get roleFonctionMismatchMessage(): string {
    if (!this.selectedEmploye?.fonction) return '';
    const selectedRoles = this.form.get('roles')?.value || [];
    if (selectedRoles.length === 0) return '';
    const selectedRole = selectedRoles[0];
    const fonctionAttendue = UtilisateurFormComponent.ROLE_TO_FONCTION[selectedRole];
    if (!fonctionAttendue) return '';
    const fonctionAttendueLabel = FONCTION_LABELS[fonctionAttendue as FonctionEmploye] ?? fonctionAttendue;
    const fonctionActuelleLabel = FONCTION_LABELS[this.selectedEmploye.fonction] ?? this.selectedEmploye.fonction;
    const roleLabel = this.roleLabels[selectedRole] ?? selectedRole;
    return `Le rôle "« ${roleLabel} »" attend un employé avec la fonction "${fonctionAttendueLabel}" mais cet employé a la fonction "${fonctionActuelleLabel}". Veuillez choisir le rôle correspondant.`;
  }

  get selectedEmployeHasNoSite(): boolean {
    return !!this.selectedEmploye && !this.selectedEmploye.siteId && !this.selectedEmploye.nomSite;
  }

  get showPasswordMismatch(): boolean {
    const confirmCtrl  = this.form.get('confirmPassword');
    const confirmValue = confirmCtrl?.value;
    const passwordValue = this.form.get('password')?.value;
    return !!((confirmCtrl?.touched || confirmCtrl?.dirty)
      && confirmValue && passwordValue
      && this.form.hasError('passwordMismatch'));
  }

  form = this.fb.group({
    username:        ['', [Validators.required, Validators.minLength(3)]],
    password:        ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: [''],
    active:          [true],
    roles:           [[] as string[], Validators.required],
    employeId:       [null as number | null]
  }, {
    validators: (group) => {
      if (!this.isEditMode) {
        const password        = group.get('password')?.value;
        const confirmPassword = group.get('confirmPassword')?.value;
        return password === confirmPassword ? null : { passwordMismatch: true };
      }
      return null;
    }
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');

    if (idParam) {
      this.isEditMode   = true;
      this.utilisateurId = Number(idParam);
      this.form.get('password')?.clearValidators();
      this.form.get('password')?.updateValueAndValidity();
      this.chargerUtilisateur(this.utilisateurId);
    } else {
      // Mode création — charger les employés disponibles (actifs + sans compte)
      this.chargerEmployes();

      // Pré-remplir l'employé depuis l'URL (?employeId=X)
      const employeIdParam = this.route.snapshot.queryParamMap.get('employeId');
      if (employeIdParam) {
        const id = Number(employeIdParam);
        this.form.patchValue({ employeId: id });
        // On récupère le détail pour afficher le résumé
        this.employeService.getById(id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({ next: (e) => { this.selectedEmploye = e; } });
      }
    }
  }

  chargerEmployes(): void {
    this.loadingEmployes = true;
    this.employeService.getDisponibles()
      .pipe(finalize(() => { this.loadingEmployes = false; }), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => { this.employes = data; },
        error: () => {}
      });
  }

  onEmployeChange(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    if (!id) {
      this.selectedEmploye = null;
      return;
    }
    const found = this.employes.find(e => e.id === id);
    this.selectedEmploye = found ?? null;
  }

  chargerUtilisateur(id: number): void {
    this.loadingData = true;
    this.utilisateurService.getById(id)
      .pipe(finalize(() => { this.loadingData = false; }), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (u: UtilisateurResponse) => {
          this.form.patchValue({
            username:  u.username,
            active:    u.active,
            roles:     u.roles,
            employeId: u.employeId ?? null
          });
          // Reconstituer le résumé de l'employé lié (mode édition)
          if (u.employeId) {
            this.selectedEmploye = {
              id:          u.employeId,
              matricule:   u.employeMatricule  || '',
              nomComplet:  u.employeNomComplet || '',
              nom:         '',
              prenom:      '',
              telephone:   u.employeTelephone  || '',
              fonction:    u.employeFonction   as FonctionEmploye | undefined,
              nomAgence:   u.employeAgenceNom,
              nomSite:     u.employeSiteNom,
              actif:       true,
              agenceId:    0,
              siteId:      0,
              dateEmbauche: '',
              salaireBase:  0,
              primeFixe:    0,
              bonusVariable: 0,
              totalRemuneration: 0,
              dateCreation: '',
              dateModification: ''
            };
          }
        },
        error: () => { this.error = "Erreur lors du chargement de l'utilisateur"; }
      });
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    // Validation front : rôle opérationnel sans employé
    if (this.isOperationalRole && !this.form.get('employeId')?.value && !this.isEditMode) {
      this.error = "Sélectionnez d'abord l'employé lié à ce compte. Un employé est obligatoire pour ce rôle.";
      return;
    }

    if (this.isAgentTerrainRole && this.selectedEmployeHasNoSite) {
      this.error = 'Le site est obligatoire pour un Agent Terrain. Complétez le site sur la fiche Employé.';
      return;
    }

    this.loading = true;
    this.error   = '';
    this.success = '';
    const raw    = this.form.getRawValue();

    if (this.isEditMode && this.utilisateurId) {
      const request: UtilisateurUpdateRequest = {
        active:    raw.active ?? undefined,
        roles:     raw.roles  || [],
        employeId: raw.employeId ?? undefined
      };

      this.utilisateurService.update(this.utilisateurId, request)
        .pipe(finalize(() => { this.loading = false; }), takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.success = 'Utilisateur mis à jour avec succès !';
            setTimeout(() => this.router.navigate(['/utilisateurs']), 1500);
          },
          error: (err) => {
            this.error = err?.error?.message || "Erreur lors de la modification de l'utilisateur";
          }
        });
      return;
    }

    const createRequest: UtilisateurCreateRequest = {
      username:  raw.username  ?? '',
      password:  raw.password  ?? '',
      roles:     raw.roles     || [],
      employeId: raw.employeId ?? undefined
    };

    this.utilisateurService.create(createRequest)
      .pipe(finalize(() => { this.loading = false; }), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.success = 'Utilisateur créé avec succès !';
          setTimeout(() => this.router.navigate(['/utilisateurs']), 1500);
        },
        error: (err) => {
          this.error = err?.error?.message || "Erreur lors de la création de l'utilisateur";
        }
      });
  }

  toggleRole(role: string): void {
    const rolesControl = this.form.get('roles');
    const roles        = rolesControl?.value || [];
    // Sélection unique (un seul rôle à la fois)
    rolesControl?.setValue(roles.includes(role) ? [] : [role]);
  }

  isRoleSelected(role: string): boolean {
    return (this.form.get('roles')?.value || []).includes(role);
  }

  getFonctionLabel(f: string): string {
    return FONCTION_LABELS[f as FonctionEmploye] ?? f;
  }

  getFieldError(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control || !control.errors || !control.touched) return '';
    if (control.hasError('required'))   return 'Ce champ est obligatoire';
    if (control.hasError('minlength'))  return `Minimum ${control.getError('minlength').requiredLength} caractères`;
    if (control.hasError('pattern'))    return 'Format invalide';
    if (control.hasError('email'))      return 'Email invalide';
    return '';
  }
}
