import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { EcartCaisseService } from '../../services/ecart-caisse.service';
import { EcartCaisseResponse } from '../../models/ecart-caisse-response';
import {
  STATUT_ECART_CAISSE_LABELS,
  STATUT_ECART_CAISSE_CSS,
  StatutEcartCaisse,
} from '../../models/statut-ecart-caisse';
import { TYPE_ECART_CAISSE_LABELS } from '../../models/type-ecart-caisse';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

/**
 * PATCH 11 — Détail d'un écart de caisse avec actions selon rôle et statut.
 *
 * Workflow (règles métier 3N) :
 *   DETECTE         → CAISSIER/CONTROLEUR peut justifier (statut inchangé)
 *   DETECTE         → CONTROLEUR ou RCI peut ouvrir enquête → EN_INVESTIGATION
 *   EN_INVESTIGATION → CONTROLEUR peut résoudre → RESOLU
 *   RESOLU          → CHEF_BUREAU peut accepter → ACCEPTE
 *   tout statut     → ADMIN peut rejeter → REJETE
 *
 * RBAC :
 *   RCI = Responsable du Contrôle Interne (PATCH 11)
 *         Rôle officiel distinct du Chef de Bureau et du Contrôleur.
 *         Peut : consulter, ouvrir enquête dans le cadre de l'audit.
 *         Ne peut PAS : justifier, résoudre, accepter, rejeter.
 *   Chef de Bureau = CHEF_BUREAU.
 *
 * Aucun seuil fixe côté frontend : le flag seuilDepassé est fourni par le backend.
 *
 * NOTE : La validation 10 caractères pour la justification est provisoire (technique).
 */
@Component({
  selector: 'app-ecart-caisse-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './ecart-caisse-detail.component.html',
})
export class EcartCaisseDetailComponent implements OnInit {
  private readonly ecartService = inject(EcartCaisseService);
  private readonly authService = inject(AuthService);
  private readonly workflowMessageService = inject(WorkflowMessageService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly ecart = signal<EcartCaisseResponse | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionError = signal('');
  readonly actionLoading = signal(false);
  readonly successMessage = signal('');
  readonly guidance = signal<WorkflowGuidance | null>(null);

  // Labels
  readonly statutLabels = STATUT_ECART_CAISSE_LABELS;
  readonly statutCss = STATUT_ECART_CAISSE_CSS;
  readonly typeLabels = TYPE_ECART_CAISSE_LABELS;

  // Formulaires inline
  activePanel: 'justifier' | 'enquete' | 'resoudre' | null = null;
  justificationInput = '';
  enqueteInput = '';
  raisonInput = '';

  // ===== RBAC calculé =====
  readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));
  readonly isControleur = computed(() => this.authService.hasRole('CONTROLEUR'));
  readonly isResponsable = computed(() => this.authService.hasRole('CHEF_BUREAU'));
  readonly isCaissier = computed(() => this.authService.hasRole('CAISSIER'));
  /** PATCH 11 — RCI : rôle officiel distinct du Chef de Bureau et du Contrôleur. */
  readonly isRci = computed(() => this.authService.hasRole('RCI'));

  /** Peut justifier : ADMIN, CONTROLEUR, CAISSIER — RCI ne justifie pas */
  readonly canJustifier = computed(() =>
    this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR', 'CAISSIER'])
  );
  /** Peut ouvrir enquête : ADMIN, CONTROLEUR, RCI (mission contrôle interne) */
  readonly canOuvrirEnquete = computed(() =>
    this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR', 'RCI'])
  );
  /** Peut résoudre : ADMIN, CONTROLEUR — RCI ne résout pas (rôle opérationnel) */
  readonly canResoudre = computed(() =>
    this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR'])
  );
  /** Peut accepter : ADMIN, CHEF_BUREAU — RCI ne peut pas accepter */
  readonly canAccepter = computed(() =>
    this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU'])
  );
  /** Peut rejeter : ADMIN uniquement */
  readonly canRejeter = computed(() => this.authService.hasRole('ADMIN'));

  // ===== Disponibilité actions selon statut =====

  readonly showJustifierBtn = computed(() => {
    const e = this.ecart();
    if (!e || !this.canJustifier()) return false;
    return !['RESOLU', 'ACCEPTE', 'REJETE'].includes(e.statut);
  });

  readonly showOuvrirEnqueteBtn = computed(() => {
    const e = this.ecart();
    if (!e || !this.canOuvrirEnquete()) return false;
    return e.statut === 'DETECTE';
  });

  readonly showResoudreBtn = computed(() => {
    const e = this.ecart();
    if (!e || !this.canResoudre()) return false;
    return ['DETECTE', 'EN_INVESTIGATION'].includes(e.statut);
  });

  readonly showAccepterBtn = computed(() => {
    const e = this.ecart();
    if (!e || !this.canAccepter()) return false;
    return e.statut === 'RESOLU';
  });

  readonly showRejeterBtn = computed(() => {
    const e = this.ecart();
    if (!e || !this.canRejeter()) return false;
    return !['ACCEPTE', 'REJETE'].includes(e.statut);
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMessage.set('Identifiant d\'écart invalide.');
      return;
    }
    this.loadEcart(id);
  }

  private loadEcart(id: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.ecartService.getById(id).subscribe({
      next: (data) => {
        this.ecart.set(data);
        this.updateGuidance(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Impossible de charger l\'écart.');
        this.loading.set(false);
      },
    });
  }

  openPanel(panel: 'justifier' | 'enquete' | 'resoudre'): void {
    this.activePanel = panel;
    this.actionError.set('');
    this.successMessage.set('');
  }

  closePanel(): void {
    this.activePanel = null;
    this.justificationInput = '';
    this.enqueteInput = '';
    this.raisonInput = '';
    this.actionError.set('');
  }

  submitJustifier(): void {
    const text = this.justificationInput.trim();
    if (text.length < 10) {
      this.actionError.set('La justification doit comporter au moins 10 caractères.');
      return;
    }
    this.runAction(() =>
      this.ecartService.justifier(this.ecart()!.id, { justification: text })
    );
  }

  submitEnquete(): void {
    const text = this.enqueteInput.trim();
    if (text.length < 10) {
      this.actionError.set('Le motif d\'enquête doit comporter au moins 10 caractères.');
      return;
    }
    this.runAction(() =>
      this.ecartService.ouvrirEnquete(this.ecart()!.id, { justification: text })
    );
  }

  submitResoudre(): void {
    const text = this.raisonInput.trim();
    if (!text) {
      this.actionError.set('La raison de résolution est obligatoire.');
      return;
    }
    this.runAction(() =>
      this.ecartService.resoudre(this.ecart()!.id, { raison: text })
    );
  }

  doAccepter(): void {
    if (!confirm('Confirmer l\'acceptation de cet écart comme variance normale ?')) return;
    this.runAction(() => this.ecartService.accepter(this.ecart()!.id));
  }

  doRejeter(): void {
    if (!confirm('Confirmer le rejet de cet écart (erreur système présumée) ?')) return;
    this.runAction(() => this.ecartService.rejeter(this.ecart()!.id));
  }

  private runAction(action: () => import('rxjs').Observable<EcartCaisseResponse>): void {
    this.actionLoading.set(true);
    this.actionError.set('');
    this.successMessage.set('');

    action().subscribe({
      next: (updated) => {
        this.ecart.set(updated);
        this.closePanel();
        this.successMessage.set('Action effectuée avec succès.');
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.actionError.set(err?.error?.message || 'Une erreur est survenue.');
        this.actionLoading.set(false);
      },
    });
  }

  getStatutLabel(statut: StatutEcartCaisse): string {
    return this.statutLabels[statut] ?? statut;
  }

  getStatutCss(statut: StatutEcartCaisse): string {
    return this.statutCss[statut] ?? '';
  }

  getTypeLabel(typeEcart: string): string {
    return (this.typeLabels as Record<string, string>)[typeEcart] ?? typeEcart;
  }

  private updateGuidance(ecart: EcartCaisseResponse): void {
    const status = ecart.statut === 'DETECTE' || ecart.statut === 'EN_INVESTIGATION'
      ? 'ECART_CONSTATE'
      : ecart.statut === 'RESOLU'
        ? 'VALIDEE_CONTROLE'
        : 'CLOTUREE';

    this.guidance.set(this.workflowMessageService.getGuidance({
      module: 'CAISSE',
      status,
      currentRole: this.authService.getCurrentUser()?.role,
      expectedRole: status === 'ECART_CONSTATE' ? 'CONTROLEUR' : undefined,
      metadata: { ecart: Number(ecart.montantEcart || 0) }
    }));
  }
}
