import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin, switchMap } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { DemandeCreditResponse } from '../../models/demande-credit-response';
import {
  AjouterGarantieMaterielleRequest,
  GarantieCreditResponse,
  GarantieMaterielleResponse,
  StatutGarantieMaterielle
} from '../../models/garantie-credit.model';
import { DemandeCreditService } from '../../services/demande-credit.service';
import { GarantieCreditService } from '../../services/garantie-credit.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

@Component({
  selector: 'app-garantie-credit-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './garantie-credit-page.component.html'
})
export class GarantieCreditPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly demandeCreditService = inject(DemandeCreditService);
  private readonly garantieCreditService = inject(GarantieCreditService);
  private readonly workflowMessageService = inject(WorkflowMessageService);

  readonly demandeId = Number(this.route.snapshot.paramMap.get('id'));

  readonly demande = signal<DemandeCreditResponse | null>(null);
  readonly garantie = signal<GarantieCreditResponse | null>(null);
  readonly materielles = signal<GarantieMaterielleResponse[]>([]);

  loading = false;
  actionLoading = false;
  errorMessage = '';
  successMessage = '';
  guidance: WorkflowGuidance | null = null;

  readonly materielleForm = this.fb.nonNullable.group({
    typeBien: ['', [Validators.required]],
    description: ['', [Validators.required]],
    valeurEstimee: [0, [Validators.required, Validators.min(1)]],
    devise: ['CDF', [Validators.required]],
    proprietaireDeclare: [''],
    localisation: [''],
    referenceDocument: ['']
  });

  readonly decisionForm = this.fb.nonNullable.group({
    commentaire: ['', [Validators.required]]
  });

  readonly isControleur = computed(() => this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR']));
  readonly isChefBureau = computed(() => this.authService.hasAnyRole(['CHEF_BUREAU']));

  readonly montantGarantieRequis = computed(() => {
    const garantieValue = this.garantie()?.montantGarantieRequis;
    if (typeof garantieValue === 'number' && !Number.isNaN(garantieValue) && garantieValue > 0) {
      return garantieValue;
    }

    const montantDemande = Number(this.demande()?.montantDemande ?? 0);
    return Number(((montantDemande * 20) / 100).toFixed(2));
  });

  readonly montantManquant = computed(() => {
    const garantieValue = this.garantie();
    if (!garantieValue) {
      return 0;
    }

    const manquant = Number(garantieValue.montantGarantieManquant ?? 0);
    return manquant > 0 ? manquant : 0;
  });

  readonly epargneBloquee = computed(() => {
    const g = this.garantie();
    if (!g) {
      return false;
    }

    const statut = g.statutGarantieEpargne;
    if (statut === 'BLOQUEE' || statut === 'VALIDEE') {
      return true;
    }

    return Number(g.montantGarantieBloque ?? 0) >= Number(g.montantGarantieRequis ?? 0);
  });

  readonly canBloquer = computed(() => {
    if (!this.isControleur()) {
      return false;
    }

    const g = this.garantie();
    if (!g) {
      return false;
    }

    if (g.statutGarantieEpargne === 'BLOQUEE' || g.statutGarantieEpargne === 'VALIDEE') {
      return false;
    }

    return Number(g.soldeDisponible ?? 0) >= this.montantGarantieRequis();
  });

  readonly canVerifier = computed(() => this.isControleur());

  readonly totalMateriel = computed(() => {
    const g = this.garantie();
    if (g) {
      return Number(g.montantMaterielTotal ?? 0);
    }

    return this.materielles()
      .map((item) => Number(item.valeurEstimee ?? 0))
      .reduce((sum, value) => sum + value, 0);
  });

  readonly valeurMinimaleGageMateriel = computed(() => {
    const g = this.garantie();
    const valeurBackend = Number(g?.valeurMinimaleGageMateriel ?? 0);
    if (valeurBackend > 0) {
      return valeurBackend;
    }
    return Number(((this.demande()?.montantDemande ?? 0) * 2).toFixed(2));
  });

  readonly totalMaterielAccepte = computed(() => {
    const g = this.garantie();
    const valeurBackend = Number(g?.valeurTotaleGarantiesMateriellesAcceptees ?? 0);
    if (valeurBackend > 0) {
      return valeurBackend;
    }
    return this.materielles()
      .filter((item) => item.statut === 'ACCEPTEE')
      .map((item) => Number(item.valeurEstimee ?? 0))
      .reduce((sum, value) => sum + value, 0);
  });

  readonly couvertureMaterielle = computed(() => {
    const backendRatio = Number(this.garantie()?.ratioCouvertureMaterielle ?? 0);
    if (backendRatio > 0) {
      return backendRatio;
    }
    const requis = this.valeurMinimaleGageMateriel();
    if (requis <= 0) {
      return 0;
    }

    return Number(((this.totalMaterielAccepte() / requis) * 100).toFixed(2));
  });

  readonly garantieMaterielleSuffisante = computed(() => {
    const backendValue = this.garantie()?.garantieMaterielleSuffisante;
    if (typeof backendValue === 'boolean') {
      return backendValue;
    }
    return this.totalMaterielAccepte() >= this.valeurMinimaleGageMateriel();
  });

  readonly statutGlobalAffiche = computed(() => this.garantie()?.statutGlobal ?? 'EN_ATTENTE');

  ngOnInit(): void {
    if (!this.demandeId || Number.isNaN(this.demandeId)) {
      this.errorMessage = 'Identifiant de demande invalide.';
      return;
    }

    this.chargerEcran();
  }

  chargerEcran(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    forkJoin({
      demande: this.demandeCreditService.getById(this.demandeId),
      garantie: this.garantieCreditService.getGarantie(this.demandeId)
    }).subscribe({
      next: ({ demande, garantie }) => {
        this.demande.set(demande);
        this.garantie.set(garantie);
        this.materielles.set(garantie.garantiesMaterielles ?? []);
        this.guidance = this.workflowMessageService.getGuidance({
          module: 'CREDIT',
          status: demande.statut,
          currentRole: this.authService.getCurrentUser()?.role,
          permissions: this.authService.getCurrentUser()?.permissions,
          metadata: {
            garantieStatus: garantie.statutGlobal,
            garantieBloquee: this.epargneBloquee()
          }
        });
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Impossible de charger la garantie du crédit.';
      }
    });
  }

  verifierGarantie(): void {
    if (!this.canVerifier()) {
      this.errorMessage = 'Vérification réservée au Contrôleur.';
      return;
    }

    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.garantieCreditService.verifierGarantie(this.demandeId, {}).subscribe({
      next: (garantie) => {
        this.actionLoading = false;
        this.garantie.set(garantie);
        this.materielles.set(garantie.garantiesMaterielles ?? []);
        this.successMessage = this.epargneBloquee()
          ? 'Vérification effectuée: la garantie épargne est déjà bloquée.'
          : 'Vérification de garantie effectuée.';
      },
      error: (error) => {
        this.actionLoading = false;
        this.errorMessage = error?.error?.message || 'Erreur pendant la vérification.';
      }
    });
  }

  bloquerEpargne(): void {
    if (!this.canBloquer()) {
      const manquant = this.montantManquant();
      const devise = this.garantie()?.devise || this.demande()?.devise || 'CDF';
      if (manquant > 0) {
        this.errorMessage = `Blocage impossible: Dépôt complémentaire requis: ${manquant.toFixed(2)} ${devise}.`;
      } else {
        this.errorMessage = 'Blocage impossible: garantie déjà bloquée ou validée.';
      }
      return;
    }

    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.garantieCreditService.bloquerEpargne(this.demandeId, {}).subscribe({
      next: (garantie) => {
        this.actionLoading = false;
        this.garantie.set(garantie);
        this.materielles.set(garantie.garantiesMaterielles ?? []);
        this.successMessage = 'Garantie épargne bloquée avec succès.';
      },
      error: (error) => {
        this.actionLoading = false;
        this.errorMessage = error?.error?.message || 'Erreur pendant le blocage épargne.';
      }
    });
  }

  ajouterMaterielle(): void {
    if (!this.isControleur()) {
      this.errorMessage = 'Ajout de garantie matérielle réservé au Contrôleur.';
      return;
    }

    if (this.materielleForm.invalid) {
      this.materielleForm.markAllAsTouched();
      this.errorMessage = 'Veuillez compléter le type de bien, la description, une valeur supérieure à 0 et la devise.';
      return;
    }

    const formValue = this.materielleForm.getRawValue();
    const request: AjouterGarantieMaterielleRequest = {
      typeBien: formValue.typeBien.trim(),
      description: formValue.description.trim(),
      valeurEstimee: Number(formValue.valeurEstimee),
      devise: formValue.devise.trim().toUpperCase(),
      proprietaireDeclare: formValue.proprietaireDeclare.trim() || undefined,
      localisation: formValue.localisation.trim() || undefined,
      referenceDocument: formValue.referenceDocument.trim() || undefined
    };

    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.garantieCreditService.ajouterGarantieMaterielle(this.demandeId, request).subscribe({
      next: () => {
        this.actionLoading = false;
        this.successMessage = 'Garantie matérielle ajoutée.';
        this.materielleForm.reset({
          typeBien: '',
          description: '',
          valeurEstimee: 0,
          devise: 'CDF',
          proprietaireDeclare: '',
          localisation: '',
          referenceDocument: ''
        });
        this.rechargerGarantiesMaterielles();
      },
      error: (error) => {
        this.actionLoading = false;
        this.errorMessage = error?.error?.message || 'Erreur lors de l’ajout du bien.';
      }
    });
  }

  utiliserGagePropose(): void {
    const gagePropose = this.garantie()?.gagePropose || this.demande()?.gagePropose || '';
    if (!gagePropose.trim()) {
      this.errorMessage = 'Aucun gage proposé à reprendre.';
      return;
    }

    this.materielleForm.patchValue({
      typeBien: 'GAGE_PROPOSE_DEMANDE',
      description: gagePropose.trim(),
      devise: (this.garantie()?.devise || this.demande()?.devise || 'CDF').toUpperCase()
    });
    this.successMessage = 'Gage proposé repris dans la saisie. Renseignez la valeur estimée avant ajout.';
  }

  accepterMaterielle(item: GarantieMaterielleResponse): void {
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.garantieCreditService.accepterGarantieMaterielle(this.demandeId, item.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.successMessage = 'Garantie matérielle acceptée.';
        this.rechargerGarantiesMaterielles();
      },
      error: (error) => {
        this.actionLoading = false;
        this.errorMessage = error?.error?.message || 'Erreur lors de l’acceptation.';
      }
    });
  }

  refuserMaterielle(item: GarantieMaterielleResponse): void {
    const commentaire = window.prompt('Motif de refus (obligatoire) :')?.trim();
    if (!commentaire) {
      this.errorMessage = 'Le commentaire est obligatoire pour refuser une garantie matérielle.';
      return;
    }

    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.garantieCreditService.refuserGarantieMaterielle(this.demandeId, item.id, commentaire).subscribe({
      next: () => {
        this.actionLoading = false;
        this.successMessage = 'Garantie matérielle refusée.';
        this.rechargerGarantiesMaterielles();
      },
      error: (error) => {
        this.actionLoading = false;
        this.errorMessage = error?.error?.message || 'Erreur lors du refus.';
      }
    });
  }

  validerGarantie(): void {
    if (!this.epargneBloquee()) {
      this.errorMessage = 'Validation impossible: la garantie épargne n’est pas encore bloquée.';
      return;
    }

    if (this.decisionForm.invalid) {
      this.decisionForm.markAllAsTouched();
      return;
    }

    const commentaire = this.decisionForm.getRawValue().commentaire.trim();
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.garantieCreditService.validerGarantie(this.demandeId, { commentaire }).pipe(
      switchMap((garantie) => {
        this.garantie.set(garantie);
        this.materielles.set(garantie.garantiesMaterielles ?? []);
        return this.demandeCreditService.controlerGarantie(this.demandeId, commentaire);
      })
    ).subscribe({
      next: (demande) => {
        this.actionLoading = false;
        this.demande.set(demande);
        this.successMessage = 'Garantie globale validée. Dossier transmis au Chef de Bureau pour approbation.';
      },
      error: (error) => {
        this.actionLoading = false;
        this.errorMessage = error?.error?.message || 'Erreur lors de la validation globale ou de la transmission au Chef de Bureau.';
      }
    });
  }

  rejeterGarantie(): void {
    if (this.decisionForm.invalid) {
      this.decisionForm.markAllAsTouched();
      return;
    }

    const commentaire = this.decisionForm.getRawValue().commentaire.trim();
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.garantieCreditService.rejeterGarantie(this.demandeId, { commentaire }).subscribe({
      next: (garantie) => {
        this.actionLoading = false;
        this.garantie.set(garantie);
        this.materielles.set(garantie.garantiesMaterielles ?? []);
        this.successMessage = 'Garantie rejetée.';
      },
      error: (error) => {
        this.actionLoading = false;
        this.errorMessage = error?.error?.message || 'Erreur lors du rejet de garantie.';
      }
    });
  }

  retourListe(): void {
    this.router.navigate(['/credits/demandes']);
  }

  isRefusable(item: GarantieMaterielleResponse): boolean {
    return item.statut !== 'REFUSEE';
  }

  isAcceptable(item: GarantieMaterielleResponse): boolean {
    return item.statut !== 'ACCEPTEE';
  }

  protected readonly asMaterielleStatut = (status: StatutGarantieMaterielle): string => status;

  private rechargerGarantiesMaterielles(): void {
    this.garantieCreditService.getGarantiesMaterielles(this.demandeId).subscribe({
      next: (materielles) => {
        this.materielles.set(materielles);
      }
    });

    this.garantieCreditService.getGarantie(this.demandeId).subscribe({
      next: (garantie) => {
        this.garantie.set(garantie);
      }
    });
  }
}
