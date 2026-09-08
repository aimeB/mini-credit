import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { SiteService } from '../../../membres/services/site.service';
import { AgenceService } from '../../../employes/services/agence.service';
import { AgenceResponse } from '../../models/agence-response';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-site-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, WorkflowGuidanceBannerComponent],
  templateUrl: './site-form.component.html'
})
export class SiteFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private siteService = inject(SiteService);
  private agenceService = inject(AgenceService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loading = false;
  loadingAgences = false;
  error = '';
  success = '';
  agences: AgenceResponse[] = [];
  editId: number | null = null;
  get isEdit(): boolean { return this.editId !== null; }

  readonly formGuidance: WorkflowGuidance = {
    title: 'Gestion des sites',
    message: 'Cette page permet de gerer les sites suivis par les agents terrain. Les sites servent a rattacher les membres, organiser les collectes, suivre l activite terrain et produire les rapports. Un membre doit etre rattache a un site.',
    currentStep: 'Site consultable ou modifiable',
    nextStep: 'Affectation membres / agents / suivi terrain',
    expectedRole: 'Gestionnaire / Chef de Bureau selon les droits existants',
    expectedAction: 'Creer, verifier ou mettre a jour les sites selon les droits existants',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'Un membre ne doit pas etre cree sans site de rattachement.'
  };

  form = this.fb.group({
    agenceId: [null as number | null, Validators.required],
    codeSite: ['', [Validators.required, Validators.maxLength(30)]],
    nomSite:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    zone:     ['', [Validators.required, Validators.maxLength(255)]],
    actif:    [true]
  });

  ngOnInit(): void {
    this.loadingAgences = true;
    this.agenceService.getAll()
      .pipe(finalize(() => { this.loadingAgences = false; }))
      .subscribe({
        next: (data) => { this.agences = data.filter(a => a.actif); },
        error: () => { this.error = 'Impossible de charger les agences'; }
      });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = +id;
      this.form.get('codeSite')?.disable();
      this.siteService.getById(this.editId).subscribe({
        next: (site) => {
          this.form.patchValue({
            agenceId: site.agenceId ?? null,
            nomSite:  site.nomSite,
            zone:     site.zone ?? '',
            actif:    site.actif ?? true
          });
        },
        error: () => { this.error = 'Impossible de charger le site'; }
      });
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
        agenceId: raw.agenceId as number,
        nomSite:  this.requiredUpperString(raw.nomSite),
        zone:     raw.zone     ?? '',
        actif:    raw.actif ?? true
      };
      this.siteService.update(this.editId!, request)
        .pipe(finalize(() => { this.loading = false; }))
        .subscribe({
          next: () => {
            this.success = 'Site mis a jour avec succes !';
            setTimeout(() => { this.router.navigate(['/sites']); }, 1200);
          },
          error: (err) => { this.error = err?.error?.message || 'Erreur lors de la mise a jour'; }
        });
    } else {
      const request = {
        agenceId: raw.agenceId as number,
        codeSite: raw.codeSite ?? '',
        nomSite:  this.requiredUpperString(raw.nomSite),
        zone:     raw.zone     ?? ''
      };
      this.siteService.create(request)
        .pipe(finalize(() => { this.loading = false; }))
        .subscribe({
          next: () => {
            this.success = 'Site cree avec succes !';
            setTimeout(() => { this.router.navigate(['/sites']); }, 1200);
          },
          error: (err) => { this.error = err?.error?.message || 'Erreur lors de la creation du site'; }
        });
    }
  }

  getFieldError(field: string): string | null {
    const ctrl = this.form.get(field);
    if (!ctrl?.invalid || !ctrl.touched) return null;
    if (ctrl.hasError('required')) return 'Ce champ est obligatoire';
    if (ctrl.hasError('minlength')) return `Minimum ${ctrl.getError('minlength').requiredLength} caracteres`;
    if (ctrl.hasError('maxlength')) return `Maximum ${ctrl.getError('maxlength').requiredLength} caracteres`;
    return 'Valeur invalide';
  }

  private requiredUpperString(value: unknown): string {
    return `${value ?? ''}`.trim().toLocaleUpperCase('fr-FR');
  }
}
