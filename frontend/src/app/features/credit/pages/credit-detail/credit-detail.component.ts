import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../../core/services/auth.service';
import { CreditDetailResponse, CreditResponsables } from '../../models/credit-detail-response';
import { CreditService } from '../../services/credit.service';

@Component({
  selector: 'app-credit-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './credit-detail.component.html'
})
export class CreditDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly creditService = inject(CreditService);
  private readonly authService = inject(AuthService);

  readonly creditId = Number(this.route.snapshot.paramMap.get('creditId'));

  dossier: CreditDetailResponse | null = null;
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadDossier();
  }

  loadDossier(): void {
    if (!this.creditId) {
      this.errorMessage = 'Crédit introuvable.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.creditService.getDetail(this.creditId).pipe(
      finalize(() => {
        this.loading = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (dossier) => {
        this.dossier = dossier;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = err?.error?.message || 'Impossible de charger le dossier crédit.';
      }
    });
  }

  canRembourser(): boolean {
    const statut = this.dossier?.resume?.statut;
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE'])
      && ['DECAISSE', 'EN_COURS', 'EN_RETARD'].includes(statut || '');
  }

  formatRoleValue(value: string | null | undefined): string {
    return value && value.trim() ? value : 'Non renseigné';
  }

  getGestionnaireNom(dossier: CreditDetailResponse): string {
    return this.formatRoleValue(this.getResponsableValue(dossier.responsables, 'gestionnaireNom', 'gestionnaireNomComplet'));
  }

  getCaissierNom(dossier: CreditDetailResponse): string {
    return this.formatRoleValue(this.getResponsableValue(dossier.responsables, 'caissierNom', 'caissierNomComplet'));
  }

  private getResponsableValue(responsables: CreditResponsables, ...keys: Array<keyof CreditResponsables>): string | null | undefined {
    for (const key of keys) {
      const value = responsables[key];
      if (typeof value === 'string' && value.trim()) {
        return value;
      }
    }

    return undefined;
  }

  trackById(_: number, item: { id: number }): number {
    return item.id;
  }

  trackByHistory(index: number): number {
    return index;
  }
}
