import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AgenceService } from '../../../employes/services/agence.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-agence-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, WorkflowGuidanceBannerComponent],
  templateUrl: './agence-form.component.html'
})
export class AgenceFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private agenceService = inject(AgenceService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loading = false;
  error = '';
  success = '';
  editId: number | null = null;
  get isEdit(): boolean { return this.editId !== null; }

  readonly formGuidance: WorkflowGuidance = {
    title: 'Gestion des agences et antennes',
    message: 'Cette page permet de gerer les structures operationnelles utilisees pour organiser les equipes, les operations, la caisse, les rapports et la tracabilite. L antenne est importante pour rattacher les operations a leur contexte operationnel.',
    currentStep: 'Agence ou antenne consultable ou modifiable',
    nextStep: 'Rattachement operationnel / rapports / controle',
    expectedRole: 'Chef de Bureau / COO / Admin selon les droits existants',
    expectedAction: 'Consulter ou mettre a jour les informations selon les droits existants',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'Toute operation sensible doit rester rattachee a son antenne lorsque le systeme expose cette information.'
  };

  form = this.fb.group({
    codeAgence:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    nomAgence:   ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    ville:       ['', [Validators.required, Validators.maxLength(100)]],
    commune:     ['', [Validators.required, Validators.maxLength(100)]],
    quartier:    ['', Validators.maxLength(100)],
    adresse:     ['', Validators.maxLength(255)],
    reference:   ['', Validators.maxLength(255)],
    telephone:   ['', Validators.maxLength(20)],
    actif:       [true, Validators.required]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = +id;
      this.form.get('codeAgence')?.disable();
      this.agenceService.getById(this.editId).subscribe({
        next: (a) => {
          this.form.patchValue({
            nomAgence:  a.nomAgence,
            ville:      a.ville ?? '',
            commune:    a.commune ?? '',
            quartier:   a.quartier ?? '',
            adresse:    a.adresse ?? '',
            reference:  a.reference ?? '',
            telephone:  a.telephone ?? '',
            actif:      a.actif
          });
        },
        error: () => { this.error = 'Impossible de charger l\'agence'; }
      });
    } else {
      // Alignement minimal avec le backend (CreateAgenceRequest): adresse et quartier requis en creation.
      this.form.get('quartier')?.setValidators([Validators.required, Validators.maxLength(100)]);
      this.form.get('adresse')?.setValidators([Validators.required, Validators.maxLength(255)]);
      this.form.get('quartier')?.updateValueAndValidity({ emitEvent: false });
      this.form.get('adresse')?.updateValueAndValidity({ emitEvent: false });
    }
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.error = '';
    this.success = '';

    const raw = this.form.getRawValue();

    if (this.isEdit) {
      const request = {
        nomAgence:   this.requiredUpperString(raw.nomAgence),
        ville:       this.requiredString(raw.ville),
        commune:     this.requiredString(raw.commune),
        quartier:    this.optionalString(raw.quartier),
        adresse:     this.optionalString(raw.adresse),
        reference:   this.optionalString(raw.reference),
        telephone:   this.optionalString(raw.telephone),
        actif:       raw.actif ?? true
      };
      this.agenceService.update(this.editId!, request)
        .pipe(finalize(() => { this.loading = false; }))
        .subscribe({
          next: () => {
            this.success = 'Agence mise a jour avec succes !';
            setTimeout(() => { this.router.navigate(['/agences']); }, 1200);
          },
          error: (err) => { this.handleBackendError(err, 'Erreur lors de la mise a jour'); }
        });
    } else {
      const request = {
        codeAgence:  this.requiredString(raw.codeAgence),
        nomAgence:   this.requiredUpperString(raw.nomAgence),
        ville:       this.requiredString(raw.ville),
        commune:     this.requiredString(raw.commune),
        quartier:    this.optionalString(raw.quartier),
        adresse:     this.optionalString(raw.adresse),
        reference:   this.optionalString(raw.reference),
        telephone:   this.optionalString(raw.telephone),
        actif:       raw.actif ?? true
      };
      this.agenceService.create(request)
        .pipe(finalize(() => { this.loading = false; }))
        .subscribe({
          next: () => {
            this.success = 'Agence creee avec succes !';
            setTimeout(() => { this.router.navigate(['/agences']); }, 1200);
          },
          error: (err) => { this.handleBackendError(err, 'Erreur lors de la creation de l\'agence'); }
        });
    }
  }

  getFieldError(field: string): string | null {
    const ctrl = this.form.get(field);
    if (!ctrl?.invalid || !ctrl.touched) return null;
    if (ctrl.hasError('backend')) return ctrl.getError('backend');
    if (ctrl.hasError('required')) return 'Ce champ est obligatoire';
    if (ctrl.hasError('minlength')) return `Minimum ${ctrl.getError('minlength').requiredLength} caracteres`;
    if (ctrl.hasError('maxlength')) return `Maximum ${ctrl.getError('maxlength').requiredLength} caracteres`;
    return 'Valeur invalide';
  }

  private requiredString(value: unknown): string {
    return `${value ?? ''}`.trim();
  }

  private requiredUpperString(value: unknown): string {
    return this.requiredString(value).toLocaleUpperCase('fr-FR');
  }

  private optionalString(value: unknown): string | undefined {
    const normalized = `${value ?? ''}`.trim();
    return normalized.length > 0 ? normalized : undefined;
  }

  private handleBackendError(err: any, fallbackMessage: string): void {
    const validationErrors = err?.error?.validationErrors as Record<string, string> | undefined;

    if (validationErrors && typeof validationErrors === 'object') {
      Object.entries(validationErrors).forEach(([field, message]) => {
        const ctrl = this.form.get(field);
        if (!ctrl) return;
        ctrl.setErrors({ ...(ctrl.errors || {}), backend: message });
        ctrl.markAsTouched();
      });

      const details = Object.entries(validationErrors)
        .map(([field, message]) => `${this.getFieldLabel(field)}: ${message}`)
        .join(' | ');

      this.error = details || err?.error?.message || fallbackMessage;
      return;
    }

    this.error = err?.error?.message || fallbackMessage;
  }

  private getFieldLabel(field: string): string {
    const labels: Record<string, string> = {
      codeAgence: 'Code Agence',
      nomAgence: 'Nom de l Agence',
      ville: 'Ville',
      commune: 'Commune',
      quartier: 'Quartier',
      adresse: 'Adresse',
      reference: 'Reference',
      telephone: 'Telephone',
      actif: 'Agence active'
    };

    return labels[field] || field;
  }
}

