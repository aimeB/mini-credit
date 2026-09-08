import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { CaisseResponse } from '../../models/caisse-response';
import { SessionCaisseOpeningContextResponse } from '../../models/session-caisse-opening-context-response';
import { SessionCaisseOpenRequest } from '../../models/session-caisse-open-request';
import { CaisseService } from '../../services/caisse.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { AuthService, User } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-session-caisse-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './session-caisse-form.component.html'
})
export class SessionCaisseFormComponent implements OnInit {
  caisses: CaisseResponse[] = [];
  selectedCaisse: CaisseResponse | null = null;
  currentUser: User | null = null;
  loading = false;
  loadingCaisses = false;
  loadingOpeningContext = false;
  error = '';
  openingContext: SessionCaisseOpeningContextResponse | null = null;
  existingSessionMessage = '';
  showOverrideWarning = false;
  openingGuidance: WorkflowGuidance | null = null;
  private automaticOpeningBalance: number | null = null;

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private caisseService: CaisseService,
    private sessionCaisseService: SessionCaisseService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.openingGuidance = {
      title: 'Préparation de l\'ouverture de session',
      message: 'Le caissier connecté sera responsable des opérations de la journée. Une nouvelle session ne peut être créée que s\'il n\'existe aucune session active sur cette caisse.',
      currentStep: 'Préparation de l\'ouverture',
      nextStep: 'Ouverture effective de la session',
      expectedRole: 'Caissier',
      expectedAction: 'Vérifier la caisse, le solde d\'ouverture et confirmer la création de session',
      severity: 'info',
      canCurrentUserAct: true
    };

    this.currentUser = this.authService.getCurrentUser();
    this.form = this.fb.group({
      caisseId: [null, Validators.required],
      dateOuverture: ['', Validators.required],
      soldeOuverture: [null as number | null, [Validators.min(0)]],
      observation: ['']
    });

    this.loadCaisses();

    this.form.patchValue({
      dateOuverture: this.nowForDatetimeLocal()
    });

    this.form.get('caisseId')?.valueChanges.subscribe((caisseId) => {
      this.selectedCaisse = this.caisses.find(c => c.id === Number(caisseId)) || null;
      this.loadOpeningContext();
    });

    this.form.get('dateOuverture')?.valueChanges.subscribe(() => {
      this.loadOpeningContext();
    });

    this.form.get('soldeOuverture')?.valueChanges.subscribe(() => {
      this.updateOverrideWarning();
    });
  }

  private nowForDatetimeLocal(): string {
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');

    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
  }

  loadCaisses(): void {
    this.loadingCaisses = true;
    this.error = '';

    this.caisseService.getAccessibles().pipe(
      finalize(() => {
        this.loadingCaisses = false;
      })
    ).subscribe({
      next: (data) => {
        this.caisses = data ?? [];
        if (this.caisses.length === 0) {
          this.error = 'Aucune caisse active disponible.';
        }

        const caisseIdParam = Number(this.route.snapshot.queryParamMap.get('caisseId'));
        if (caisseIdParam) {
          this.form.patchValue({ caisseId: caisseIdParam });
          this.selectedCaisse = this.caisses.find(c => c.id === caisseIdParam) || null;
          this.loadOpeningContext();
        }
      },
      error: (err) => {
        console.error(err);
        const backendMessage = err?.error?.message || err?.error?.error || err?.message;
        const status = err?.status ? ` (HTTP ${err.status})` : '';
        this.error = backendMessage ? `${backendMessage}${status}` : `Impossible de charger les caisses.${status}`;
      }
    });
  }

  private loadOpeningContext(): void {
    const caisseId = Number(this.form.get('caisseId')?.value);
    const dateOuverture = this.form.get('dateOuverture')?.value as string | null;
    const dateComptable = this.extractDateComptable(dateOuverture ?? '');

    if (!caisseId || !dateComptable) {
      this.openingContext = null;
      this.existingSessionMessage = '';
      this.automaticOpeningBalance = null;
      this.showOverrideWarning = false;
      this.form.get('soldeOuverture')?.enable({ emitEvent: false });
      return;
    }

    this.loadingOpeningContext = true;
    this.sessionCaisseService.getOuvertureContext(caisseId, dateComptable).pipe(
      finalize(() => {
        this.loadingOpeningContext = false;
      })
    ).subscribe({
      next: (context) => {
        this.openingContext = context;
        this.existingSessionMessage = this.buildExistingSessionMessage(context);
        this.automaticOpeningBalance = context.soldeOuvertureAutomatique ?? 0;
        this.applyOpeningContext(context);
      },
      error: (err) => {
        console.error(err);
        this.openingContext = null;
        this.existingSessionMessage = '';
        this.automaticOpeningBalance = null;
        this.showOverrideWarning = false;
      }
    });
  }

  private applyOpeningContext(context: SessionCaisseOpeningContextResponse): void {
    const soldeControl = this.form.get('soldeOuverture');
    if (!soldeControl) {
      return;
    }

    if (context.premiereSession) {
      if (soldeControl.disabled) {
        soldeControl.enable({ emitEvent: false });
      }
      this.showOverrideWarning = false;
      return;
    }

    const autoValue = context.soldeOuvertureAutomatique ?? 0;
    soldeControl.setValue(autoValue, { emitEvent: false });
    soldeControl.disable({ emitEvent: false });
    this.showOverrideWarning = false;
  }

  private updateOverrideWarning(): void {
    const soldeControl = this.form.get('soldeOuverture');
    if (!soldeControl || soldeControl.disabled || !this.openingContext || this.openingContext.premiereSession) {
      this.showOverrideWarning = false;
      return;
    }

    const currentValue = this.toNumber(soldeControl.value);
    this.showOverrideWarning = currentValue !== null
      && this.automaticOpeningBalance !== null
      && currentValue !== this.automaticOpeningBalance;
  }

  get hasExistingSession(): boolean {
    return this.openingContext?.sessionExistante === true && !!this.openingContext.sessionExistanteId;
  }

  get existingSessionUrl(): string | null {
    return this.openingContext?.sessionExistanteId ? `/caisses/session/${this.openingContext.sessionExistanteId}` : null;
  }

  private buildExistingSessionMessage(context: SessionCaisseOpeningContextResponse): string {
    if (!context.sessionExistante || !context.sessionExistanteId) {
      return '';
    }

    const dateOuverture = context.sessionExistanteDateOuverture ? this.formatDateTime(context.sessionExistanteDateOuverture) : 'date inconnue';
    const auteur = context.sessionExistanteUtilisateurNom || 'utilisateur inconnu';
    const statut = context.sessionExistanteStatut || 'statut inconnu';

    return `Une session active existe déjà pour cette caisse : SESSION-${context.sessionExistanteId}, statut ${statut}, ouverte par ${auteur} à ${dateOuverture}.`;
  }

  private formatDateTime(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  private toNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    const observation = `${rawValue.observation ?? ''}`.trim()
      || (this.authService.hasRole('CHEF_BUREAU') ? 'Substitution caissier absent' : '');

    if (this.showOverrideWarning && !observation) {
      this.error = 'Une justification est obligatoire pour forcer un solde différent du solde calculé.';
      return;
    }

    this.loading = true;
    this.error = '';

    const payload: SessionCaisseOpenRequest = {
      caisseId: rawValue.caisseId,
      dateComptable: this.extractDateComptable(rawValue.dateOuverture),
      dateOuverture: rawValue.dateOuverture,
      soldeOuverture: rawValue.soldeOuverture ?? undefined,
      observation
    };

    this.sessionCaisseService.ouvrir(payload).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (session) => {
        this.router.navigate(['/caisses/sessions', session.id]);
      },
      error: (err) => {
        console.error(err);
        this.error = err?.error?.message || 'Erreur lors de l\'ouverture de la session.';
      }
    });
  }

  get caissierConnecteLabel(): string {
    if (!this.currentUser) {
      return 'Utilisateur non identifié';
    }

    return this.currentUser.nomComplet || this.currentUser.username;
  }

  get antenneDisplayName(): string {
    const antenneNom = this.selectedCaisse?.antenneNom;
    if (typeof antenneNom === 'string' && antenneNom.trim().length > 0) {
      return antenneNom;
    }
    return 'Non renseignée';
  }

  get sessionDevise(): string {
    return this.selectedCaisse?.devise || this.openingContext?.devise || 'CDF';
  }

  get isPremiereSession(): boolean {
    return this.openingContext?.premiereSession === true;
  }

  get hasReportedOpeningBalance(): boolean {
    return !!this.openingContext && !this.openingContext.premiereSession;
  }

  get reportedOpeningBalanceLabel(): string {
    return this.formatAmount(this.openingContext?.soldeOuvertureAutomatique ?? 0, this.sessionDevise);
  }

  private formatAmount(value: number | null | undefined, devise: string): string {
    const amount = value ?? 0;
    return `${new Intl.NumberFormat('fr-CD').format(amount)} ${devise}`;
  }

  private extractDateComptable(dateOuverture: string): string {
    return (dateOuverture || '').split('T')[0];
  }
}