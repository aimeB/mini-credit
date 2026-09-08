import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { SessionCaisseCloseRequest } from '../../models/session-caisse-close-request';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { SessionCaisseService } from '../../services/session-caisse.service';

@Component({
  selector: 'app-session-caisse-cloture',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './session-caisse-cloture.component.html'
})
export class SessionCaisseClotureComponent implements OnInit {
  readonly transmissionNotice = 'Cette action ne clôture pas définitivement la session. Elle transmet la session au contrôle du Contrôleur. La clôture finale ne peut intervenir qu\'après validation du contrôle.';

  session?: SessionCaisseResponse;
  loading = false;
  saving = false;
  error = '';
  prepared = false;
  closingGuidance: WorkflowGuidance | null = null;

  form!: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly workflowMessageService: WorkflowMessageService,
    private readonly sessionService: SessionCaisseService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      soldePhysique: [null as number | null, [Validators.required, Validators.min(0)]],
      observation: ['']
    });

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error = 'Identifiant de session invalide.';
      return;
    }
    this.loadSession(id);
  }

  get currentUserName(): string {
    return this.authService.getCurrentUser()?.nomComplet || this.authService.getCurrentUser()?.username || '-';
  }

  get soldeTheoriqueCalcule(): number {
    if (!this.session) return 0;
    return Number(this.session.soldeOuverture || 0) + Number(this.session.totalEntrees || 0) - Number(this.session.totalSorties || 0);
  }

  get ecartCalcule(): number {
    const soldePhysique = Number(this.form.value.soldePhysique ?? 0);
    return soldePhysique - this.soldeTheoriqueCalcule;
  }

  get hasEcart(): boolean {
    return this.form.value.soldePhysique != null && this.ecartCalcule !== 0;
  }

  get isSessionCloturable(): boolean {
    return !!this.session && this.session.statut === 'OUVERTE' && !this.session.dateCloture;
  }

  private loadSession(id: number): void {
    this.loading = true;
    this.error = '';

    this.sessionService.getById(id).pipe(
      finalize(() => (this.loading = false))
    ).subscribe({
      next: (data) => {
        this.session = data;
        this.updateGuidance(data);
        const soldeTheorique = Number(data.soldeOuverture || 0) + Number(data.totalEntrees || 0) - Number(data.totalSorties || 0);
        this.form.patchValue({ soldePhysique: soldeTheorique });
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de charger la session à clôturer.';
      }
    });
  }

  preparerCloture(): void {
    this.prepared = true;
    if (this.hasEcart && !(this.form.value.observation || '').trim()) {
      this.form.get('observation')?.setErrors({ requiredOnEcart: true });
    }
  }

  cloturerSession(): void {
    if (!this.session) return;

    const observation = (this.form.value.observation || '').trim();
    if (this.hasEcart && !observation) {
      this.form.get('observation')?.setErrors({ requiredOnEcart: true });
    }

    if (this.form.invalid || !this.isSessionCloturable || (this.hasEcart && !observation)) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: SessionCaisseCloseRequest = {
      dateCloture: new Date().toISOString(),
      soldePhysique: Number(this.form.value.soldePhysique),
      observation: observation || undefined
    };

    this.saving = true;
    this.error = '';

    this.sessionService.cloturer(this.session.id, payload).pipe(
      finalize(() => (this.saving = false))
    ).subscribe({
      next: (closed) => {
        this.router.navigate(['/caisses/sessions', closed.id], {
          state: { successMessage: 'Session transmise au contrôle du Contrôleur.' }
        });
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur lors de la soumission au contrôle.';
      }
    });
  }

  annuler(): void {
    if (!this.session) {
      this.router.navigate(['/caisses']);
      return;
    }
    this.router.navigate(['/caisses/sessions', this.session.id]);
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) return '0 CDF';
    return `${new Intl.NumberFormat('fr-CD').format(value)} CDF`;
  }

  private updateGuidance(session: SessionCaisseResponse): void {
    const ecart = Number(session.ecartCaisse || 0);
    const status = session.statut === 'OUVERTE' ? 'PRE_CLOTUREE' : session.statut || 'PRE_CLOTUREE';

    this.closingGuidance = this.workflowMessageService.getGuidance({
      module: 'CAISSE',
      status,
      currentRole: this.authService.getCurrentUser()?.role,
      expectedRole: status === 'PRE_CLOTUREE' ? 'CONTROLEUR' : undefined,
      metadata: { ecart }
    });
  }
}
