import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EmployeService } from '../../services/employe.service';
import { UtilisateurService } from '../../../utilisateurs/services/utilisateur.service';
import { AgenceService, AgenceSimple } from '../../services/agence.service';
import { SiteService } from '../../../membres/services/site.service';
import { SiteResponse } from '../../../membres/models/site-response';
import { EmployeResponse } from '../../models/employe-response';
import { UtilisateurResponse } from '../../../utilisateurs/models/utilisateur-response';
import { FONCTIONS_LIST, FONCTION_LABELS, FonctionEmploye } from '../../models/fonction-employe';

/** Regex téléphone DRC : +243 suivi de 9 chiffres */
const PHONE_PATTERN = /^\+243\d{9}$/;

@Component({
  selector: 'app-employe-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './form.component.html',
  styleUrls: ['./form.component.css']
})
export class EmployeFormComponent implements OnInit, OnDestroy {
  private static readonly MAX_PHOTO_SIZE = 2 * 1024 * 1024;
  private static readonly PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  form!: FormGroup;
  loading = false;
  submitting = false;
  error: string | null = null;
  success: string | null = null;
  photoError: string | null = null;
  photoSuccess: string | null = null;
  photoUploading = false;
  photoRemoving = false;
  isEditMode = false;
  employeId: number | null = null;
  /** Matricule courant (mode édition uniquement) — auto-généré par le backend, affiché en lecture seule */
  currentMatricule: string | null = null;

  /** ID agence présélectionné via query param ?agenceId= */
  agenceIdFromParam: number | null = null;
  /** ID agence de retour après création (query param ?returnTo=, défaut = agenceIdFromParam) */
  returnToAgenceId: number | null = null;

  /** Informations du compte utilisateur lié (mode édition) */
  linkedUserUsername: string | null = null;
  linkedUserRoles: string | null = null;

  agences: AgenceSimple[] = [];
  allSites: SiteResponse[] = [];
  filteredSites: SiteResponse[] = [];

  fonctions = FONCTIONS_LIST;
  fonctionLabels = FONCTION_LABELS;

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private employeService: EmployeService,
    private utilisateurService: UtilisateurService,
    private agenceService: AgenceService,
    private siteService: SiteService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    // 1. Lire les query params (synchrone) — agenceId présélectionné depuis Agence Détail
    const qp = this.route.snapshot.queryParamMap;
    const agenceIdParam = qp.get('agenceId');
    const returnToParam = qp.get('returnTo');
    if (agenceIdParam) {
      this.agenceIdFromParam = Number(agenceIdParam);
      // returnTo optionnel — par défaut on revient à l’agence d’origine
      this.returnToAgenceId = returnToParam ? Number(returnToParam) : this.agenceIdFromParam;
    }

    this.loadReferentiels();

    this.subscriptions.add(
      this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
        if (params['id']) {
          this.isEditMode = true;
          this.employeId = +params['id'];
          this.loadEmploye(+params['id']);
        }
      })
    );

    // Filtrer les sites quand l'agence change
    this.form.get('agenceId')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(agenceId => {
        this.filteredSites = agenceId
          ? this.allSites.filter(s => s.agenceId === +agenceId)
          : [];
        this.form.get('siteId')?.setValue('');
      });

    this.form.get('fonction')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updateSiteRequirement());
    this.updateSiteRequirement();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }

  initForm(): void {
    this.form = this.fb.group({
      nom:           ['', [Validators.required, Validators.minLength(2)]],
      // Note: matricule absent en création (auto-généré côté backend)
      prenom:        ['', [Validators.required, Validators.minLength(2)]],
      telephone:     ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
      photoUrl:      [''],
      adresse:       [''],
      commune:       [''],
      fonction:      ['', Validators.required],
      agenceId:      ['', Validators.required],
      siteId:        [''],
      dateEmbauche:  ['', Validators.required],
      salaireBase:   ['', [Validators.required, Validators.min(0)]],
      primeFixe:     [0, [Validators.min(0)]],
      bonusVariable: [0, [Validators.min(0)]],
      utilisateurId: [''],   // optionnel
      actif:         [true]
    });
  }

  get isAgentTerrainFunction(): boolean {
    return this.form.get('fonction')?.value === 'AGENT_TERRAIN';
  }

  get isTransverseFunction(): boolean {
    const fonction = this.form.get('fonction')?.value;
    return fonction === 'COO' || fonction === 'RCI';
  }

  private updateSiteRequirement(): void {
    const siteCtrl = this.form.get('siteId');
    if (!siteCtrl) return;

    if (this.isAgentTerrainFunction) {
      siteCtrl.setValidators([Validators.required]);
    } else {
      siteCtrl.clearValidators();
      siteCtrl.setValue('', { emitEvent: false });
    }
    siteCtrl.updateValueAndValidity({ emitEvent: false });
  }

  loadReferentiels(): void {
    this.subscriptions.add(
      this.agenceService.getAll().pipe(takeUntil(this.destroy$)).subscribe({
        next: data => { this.agences = data; },
        error: err => console.error('Erreur agences:', err)
      })
    );
    this.subscriptions.add(
      this.siteService.getAll().pipe(takeUntil(this.destroy$)).subscribe({
        next: data => {
          this.allSites = data;
          // Après chargement des sites, présélectionner l’agence si fournie en paramètre
          if (this.agenceIdFromParam && !this.isEditMode) {
            this.form.patchValue({ agenceId: this.agenceIdFromParam });
            // La subscription valueChanges sur agenceId gère automatiquement filteredSites
          }
        },
        error: err => console.error('Erreur sites:', err)
      })
    );
    // Note: les utilisateurs ne sont plus chargés en création (pas de sélecteur).
    // En édition, le compte lié est chargé via loadLinkedUser().
  }

  loadEmploye(id: number): void {
    this.loading = true;
    this.subscriptions.add(
      this.employeService.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
        next: (employe: EmployeResponse) => {
          // En mode édition, stocker le matricule en propriété (lecture seule, non modifiable)
          this.currentMatricule = employe.matricule;
          this.form.patchValue({
            nom:           employe.nom,
            prenom:        employe.prenom,
            telephone:     employe.telephone,
            photoUrl:      employe.photoUrl || '',
            adresse:       employe.adresse || '',
            commune:       employe.commune || '',
            fonction:      employe.fonction || '',
            agenceId:      employe.agenceId,
            siteId:        employe.siteId,
            dateEmbauche:  employe.dateEmbauche,
            salaireBase:   employe.salaireBase,
            primeFixe:     employe.primeFixe,
            bonusVariable: employe.bonusVariable,
            utilisateurId: employe.utilisateurId || '',
            actif:         employe.actif
          });
          // Charger les sites de l'agence en mode édition
          this.filteredSites = this.allSites.filter(s => s.agenceId === employe.agenceId);
          // Charger les infos du compte utilisateur lié (si existant)
          if (employe.utilisateurId) {
            this.loadLinkedUser(employe.utilisateurId);
          }

          this.loading = false;
        },
        error: () => {
          this.error = "Erreur lors du chargement de l'employé";
          this.loading = false;
        }
      })
    );
  }

  /** Charge le compte utilisateur lié pour l'affichage en lecture seule (mode édition) */
  loadLinkedUser(utilisateurId: number): void {
    this.subscriptions.add(
      this.utilisateurService.getById(utilisateurId).pipe(takeUntil(this.destroy$)).subscribe({
        next: u => {
          this.linkedUserUsername = u.username;
          this.linkedUserRoles = u.roles?.join(', ') ?? null;
        },
        error: () => {
          // Non bloquant — les infos de base restent accessibles via le form
          this.linkedUserUsername = null;
          this.linkedUserRoles = null;
        }
      })
    );
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Veuillez corriger les erreurs dans le formulaire';
      return;
    }

    this.submitting = true;
    this.error = null;
    this.success = null;

    const raw = this.form.getRawValue();
    const siteId = raw.fonction === 'AGENT_TERRAIN' && raw.siteId ? +raw.siteId : null;
    const payload = {
      ...raw,
      agenceId:      +raw.agenceId,
      siteId,
      salaireBase:   +raw.salaireBase,
      primeFixe:     +(raw.primeFixe || 0),
      bonusVariable: +(raw.bonusVariable || 0),
      utilisateurId: raw.utilisateurId ? +raw.utilisateurId : undefined,
      photoUrl: raw.photoUrl?.trim() || null,
      fonction:      raw.fonction as FonctionEmploye
    };

    if (this.isEditMode && this.employeId) {
      this.subscriptions.add(
        this.employeService.update(this.employeId, payload)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.success = 'Employé mis à jour avec succès !';
              setTimeout(() => this.router.navigate(['/employes']), 1500);
            },
            error: err => {
              this.error = err.error?.message || 'Erreur lors de la mise à jour';
              this.submitting = false;
            }
          })
      );
    } else {
      this.subscriptions.add(
        this.employeService.create(payload)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.success = 'Employé créé avec succès !';
              const destination = this.returnToAgenceId
                ? ['/agences', this.returnToAgenceId]
                : ['/employes'];
              setTimeout(() => this.router.navigate(destination), 1500);
            },
            error: err => {
              this.error = err.error?.message || 'Erreur lors de la création';
              this.submitting = false;
            }
          })
      );
    }
  }

  get phoneError(): string | null {
    const ctrl = this.form.get('telephone');
    if (!ctrl?.touched) return null;
    if (ctrl.hasError('required')) return 'Le téléphone est obligatoire';
    if (ctrl.hasError('pattern')) return 'Format attendu : +243XXXXXXXXX (9 chiffres)';
    return null;
  }

  clearPhoto(): void {
    const previousPhotoUrl = this.form.get('photoUrl')?.value;
    this.form.get('photoUrl')?.setValue('');

    if (this.isEditMode && this.employeId && previousPhotoUrl) {
      this.photoRemoving = true;
      this.employeService.deletePhoto(this.employeId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (employe) => {
            this.form.get('photoUrl')?.setValue(employe.photoUrl || '');
            this.photoRemoving = false;
          },
          error: () => {
            this.photoError = 'Impossible de retirer la photo';
            this.photoRemoving = false;
          }
        });
    }
  }

  onPhotoPreviewError(): void {
    this.form.get('photoUrl')?.setValue('');
    this.photoSuccess = null;
    this.photoError = 'Photo importée mais impossible à afficher. Vérifiez l’accès au fichier.';
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    this.photoError = null;
    this.photoSuccess = null;

    if (!file) {
      return;
    }
    if (!EmployeFormComponent.PHOTO_TYPES.includes(file.type)) {
      this.photoError = 'Format accepté : JPEG, PNG ou WebP';
      return;
    }
    if (file.size > EmployeFormComponent.MAX_PHOTO_SIZE) {
      this.photoError = 'La photo ne doit pas dépasser 2 Mo';
      return;
    }
    if (!this.isEditMode || !this.employeId) {
      this.photoError = 'Enregistrez d’abord l’employé avant d’ajouter une photo';
      return;
    }

    this.photoUploading = true;
    this.employeService.uploadPhoto(this.employeId, file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (employe) => {
          this.form.get('photoUrl')?.setValue(employe.photoUrl || '');
          this.photoUploading = false;
          this.photoSuccess = 'Photo importée avec succès';
        },
        error: (err) => {
          this.photoError = err.error?.message || 'Impossible de téléverser la photo';
          this.photoUploading = false;
          this.photoSuccess = null;
        }
      });
  }

  getFonctionLabel(f: string): string {
    return this.fonctionLabels[f as FonctionEmploye] ?? f;
  }
}
