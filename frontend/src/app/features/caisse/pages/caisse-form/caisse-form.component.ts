import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { CaisseCreateRequest } from '../../models/caisse-create-request';
import { CaisseService } from '../../services/caisse.service';
import { SiteResponse, SiteService } from '../../../../shared/services/site.service';
import { AgenceResponse } from '../../../organisation/models/agence-response';
import { AgenceService } from '../../../employes/services/agence.service';
import { UtilisateurResponse } from '../../../utilisateurs/models/utilisateur-response';
import { UtilisateurService } from '../../../utilisateurs/services/utilisateur.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-caisse-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './caisse-form.component.html'
})
export class CaisseFormComponent implements OnInit {
  loading = false;
  loadingAgences = false;
  loadingSites = false;
  loadingCaissiers = false;
  error = '';
  success = '';
  generatedCode = 'Généré automatiquement';
  isCaissier = false;
  currentUserId: number | null = null;
  currentUserSiteId: number | null = null;
  currentUserAgenceId: number | null = null;

  agences: AgenceResponse[] = [];
  sites: SiteResponse[] = [];
  caissiers: UtilisateurResponse[] = [];

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private caisseService: CaisseService,
    private agenceService: AgenceService,
    private siteService: SiteService,
    private utilisateurService: UtilisateurService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      libelle: ['', Validators.required],
      agenceId: [null, Validators.required],
      siteId: [null],
      devise: ['CDF', Validators.required],
      caissierId: [null]
    });

    this.form.get('agenceId')?.valueChanges.subscribe(() => {
      this.onAgenceChange();
    });

    const currentUser = this.authService.getCurrentUser();
    this.currentUserId = currentUser?.id ?? null;
    this.isCaissier = this.authService.hasRole('CAISSIER');
    this.currentUserSiteId = currentUser?.siteId ?? null;

    if (this.isCaissier && !this.currentUserSiteId) {
      this.error = 'Votre compte caissier n\'est rattaché à aucune agence. Contactez l\'administrateur.';
      this.form.get('agenceId')?.disable();
    }

    this.loadAgences();
    this.loadSites();
    this.loadCaissiers();
  }

  get filteredSites(): SiteResponse[] {
    const agenceId = this.form?.get('agenceId')?.value as number | null;
    if (!agenceId) {
      return [];
    }
    return this.sites.filter(site => site.agenceId === agenceId);
  }

  loadAgences(): void {
    this.loadingAgences = true;
    this.agenceService.getAll().pipe(
      finalize(() => {
        this.loadingAgences = false;
      })
    ).subscribe({
      next: (data) => {
        const allAgences = (data ?? []).filter(agence => agence.actif);
        if (this.isCaissier && this.currentUserAgenceId) {
          this.agences = allAgences.filter(agence => agence.id === this.currentUserAgenceId);
          this.form.patchValue({ agenceId: this.currentUserAgenceId });
          this.form.get('agenceId')?.disable();
          return;
        }

        this.agences = allAgences;
      },
      error: () => {
        this.error = 'Impossible de charger les agences actives. Veuillez réessayer.';
      }
    });
  }

  loadSites(): void {
    this.loadingSites = true;
    this.siteService.getActifs().pipe(
      finalize(() => {
        this.loadingSites = false;
      })
    ).subscribe({
      next: (data) => {
        const allSites = data ?? [];
        if (this.isCaissier && this.currentUserSiteId) {
          const userSite = allSites.find(site => site.id === this.currentUserSiteId) ?? null;
          if (!userSite || !userSite.agenceId) {
            this.error = 'Votre rattachement agence est introuvable. Contactez l\'administrateur.';
            this.form.get('agenceId')?.disable();
            this.form.get('siteId')?.disable();
            return;
          }

          this.currentUserAgenceId = userSite.agenceId;
          this.sites = allSites;
          this.form.patchValue({
            agenceId: this.currentUserAgenceId,
            siteId: this.currentUserSiteId
          });
          this.agences = this.agences.filter(agence => agence.id === this.currentUserAgenceId);
          this.form.get('agenceId')?.disable();
          this.form.get('siteId')?.disable();
          return;
        }

        this.sites = allSites;
      },
      error: () => {
        this.error = 'Impossible de charger les sites actifs. Veuillez réessayer.';
      }
    });
  }

  onAgenceChange(): void {
    if (this.isCaissier) {
      return;
    }

    const selectedSiteId = this.form.get('siteId')?.value as number | null;
    if (!selectedSiteId) {
      return;
    }

    const selectedAgenceId = this.form.get('agenceId')?.value as number | null;
    const selectedSite = this.sites.find(site => site.id === selectedSiteId);
    if (!selectedSite || selectedSite.agenceId !== selectedAgenceId) {
      this.form.patchValue({ siteId: null });
    }
  }

  loadCaissiers(): void {
    this.loadingCaissiers = true;
    this.utilisateurService.getByRole('CAISSIER').pipe(
      finalize(() => {
        this.loadingCaissiers = false;
      })
    ).subscribe({
      next: (data) => {
        const allCaissiers = data ?? [];
        if (this.isCaissier && this.currentUserId != null) {
          this.caissiers = allCaissiers.filter(c => c.id === this.currentUserId);
          this.form.patchValue({ caissierId: this.currentUserId });
          this.form.get('caissierId')?.disable();
          return;
        }

        this.caissiers = allCaissiers;
      },
      error: () => {
        // Le backend peut ne pas encore exposer ce endpoint dans certains environnements.
        // Dans ce cas on garde simplement une liste vide sans bloquer la création.
        this.caissiers = [];
      }
    });
  }

  submit(): void {
    if (this.isCaissier && !this.currentUserAgenceId) {
      this.error = 'Votre compte caissier n\'est rattaché à aucune agence. Contactez l\'administrateur.';
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';

    const rawValue = this.form.getRawValue();
    const payload: CaisseCreateRequest = {
      libelle: rawValue.libelle,
      agenceId: rawValue.agenceId,
      siteId: rawValue.siteId ?? undefined,
      caissierAffecteId: rawValue.caissierId ?? undefined,
      devise: rawValue.devise
    };

    if (this.isCaissier && this.currentUserId != null) {
      payload.caissierAffecteId = this.currentUserId;
    }

    this.caisseService.create(payload).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (created) => {
        this.generatedCode = created.codeCaisse || this.generatedCode;
        this.success = 'Caisse créée avec succès.';
        setTimeout(() => this.router.navigate(['/caisses']), 800);
      },
      error: (err) => {
        console.error(err);
        const validationErrors = err?.error?.validationErrors as Record<string, string> | undefined;
        if (validationErrors && typeof validationErrors === 'object') {
          const details = Object.entries(validationErrors)
            .map(([field, message]) => `${field}: ${message}`)
            .join(' | ');
          this.error = details || err?.error?.message || 'Impossible de créer la caisse.';
          return;
        }

        this.error = err?.error?.message || 'Impossible de créer la caisse.';
      }
    });
  }
}