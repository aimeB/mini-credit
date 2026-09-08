import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

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
 * PATCH 11 — Liste des écarts de caisse.
 *
 * Contexte d'affichage :
 *   - /caisses/ecarts            : tous les écarts (CONTROLEUR, CHEF_BUREAU, RCI, ADMIN)
 *   - /caisses/session/:id/ecarts : filtrés par session (CAISSIER inclus)
 *
 * RBAC : les actions visibles dépendent du rôle de l'utilisateur connecté.
 * RCI accède en lecture seule dans le cadre de sa mission d'audit.
 * Aucun seuil fixe n'est codé ici — le flag seuilDepassé vient du backend.
 */
@Component({
  selector: 'app-ecart-caisse-list',
  standalone: true,
  imports: [CommonModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './ecart-caisse-list.component.html',
})
export class EcartCaisseListComponent implements OnInit {
  private readonly ecartService = inject(EcartCaisseService);
  private readonly authService = inject(AuthService);
  private readonly workflowMessageService = inject(WorkflowMessageService);
  private readonly route = inject(ActivatedRoute);

  readonly ecarts = signal<EcartCaisseResponse[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  guidance: WorkflowGuidance | null = null;

  /** ID de session si la vue est contextualisée par une session. */
  sessionId: number | null = null;

  // Labels et classes CSS pour les templates
  readonly statutLabels = STATUT_ECART_CAISSE_LABELS;
  readonly statutCss = STATUT_ECART_CAISSE_CSS;
  readonly typeLabels = TYPE_ECART_CAISSE_LABELS;

  // ===== RBAC — visibilité selon rôle =====
  /** Liste globale : ADMIN, CONTROLEUR, CHEF_BUREAU, RCI (audit) */
  readonly canViewAll = computed(() =>
    this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI'])
  );
  /** Justifier : ADMIN, CONTROLEUR, CAISSIER uniquement — RCI ne justifie pas */
  readonly canJustifier = computed(() =>
    this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR', 'CAISSIER'])
  );

  ngOnInit(): void {
    const sessionParam = this.route.snapshot.paramMap.get('sessionId');
    if (sessionParam) {
      this.sessionId = Number(sessionParam);
      this.loadBySession(this.sessionId);
    } else {
      this.loadAll();
    }

    this.guidance = this.workflowMessageService.getGuidance({
      module: 'CAISSE',
      status: 'ECART_CONSTATE',
      currentRole: this.authService.getCurrentUser()?.role,
      expectedRole: 'CONTROLEUR',
      nextStep: this.sessionId ? 'Justification et traitement des écarts de la session' : 'Suivi et traitement des écarts'
    });
  }

  private loadAll(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.ecartService.getAll().subscribe({
      next: (data) => { this.ecarts.set(data ?? []); this.loading.set(false); },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Impossible de charger les écarts.');
        this.loading.set(false);
      },
    });
  }

  private loadBySession(sessionId: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.ecartService.getBySession(sessionId).subscribe({
      next: (data) => { this.ecarts.set(data ?? []); this.loading.set(false); },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Impossible de charger les écarts de la session.');
        this.loading.set(false);
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

  get backLink(): string {
    return this.sessionId ? `/caisses/session/${this.sessionId}` : '/caisses';
  }
}
