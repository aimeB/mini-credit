import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DemandeRetraitEpargneService } from '../../services/demande-retrait-epargne.service';
import { DemandeRetraitEpargneResponse } from '../../models/demande-retrait-epargne';
import { StatutDemandeRetrait, STATUT_LABELS, STATUT_COLORS } from '../../models/statut-demande-retrait.enum';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

/**
 * Composant pour afficher la liste des demandes de retrait épargne (PHASE 6B.3)
 * 
 * Features:
 * - Affiche liste toutes les demandes
 * - Filtrage par statut
 * - Pagination
 * - Actions: Voir détail, Annuler
 */
@Component({
  selector: 'app-demande-retrait-epargne-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, WorkflowGuidanceBannerComponent],
  template: `
    <div class="mc-page-wide">
      <header class="mc-page-hero">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h1 class="mc-page-title">{{ getPageTitle() }}</h1>
            <p class="mc-page-subtitle">Suivi des demandes de retrait épargne: demande, contrôle, validation contrôleur et paiement caissier.</p>
          </div>
        <button *ngIf="canCreateDemande()" class="mc-btn mc-btn-success" (click)="navigateToCreate()">
          Nouvelle Demande
        </button>
        </div>
      </header>

      <div class="space-y-6">
        <app-workflow-guidance-banner [guidance]="guidance"></app-workflow-guidance-banner>

        <section class="mc-filter-panel">
          <div class="mc-filter-grid">
            <label class="mc-field-label">
              Filtre par Statut
              <select class="mc-select" [(ngModel)]="filterStatut" (change)="onFilterStatutChange()">
                <option value="">Tous les statuts</option>
                <option *ngFor="let statut of visibleStatuts" [value]="statut">
                  {{ getStatutLabel(statut) }}
                </option>
              </select>
            </label>
            <label class="mc-field-label">
              Recherche
              <input type="text" class="mc-input" placeholder="Compte ou Membre" [(ngModel)]="filterText" (change)="applyFilters()">
            </label>
            <div class="flex items-end">
              <button class="mc-btn mc-btn-dark w-full" (click)="resetFilters()">
                Réinitialiser
              </button>
            </div>
          </div>
        </section>

        <section class="space-y-4">
          <div class="mc-panel">
            <h2 class="mb-1 text-lg font-bold text-slate-900">Demandes de retrait</h2>
            <p class="text-sm leading-6 text-slate-600">Référence, compte, membre, montant, commission, total débité, statut et date restent visibles pour la traçabilité.</p>
          </div>

          <div class="mc-table-wrap">
            <table class="mc-table min-w-[1120px]">
              <thead>
                <tr>
                  <th>Référence</th>
                  <th>Compte</th>
                  <th>Membre</th>
                  <th class="text-right">Montant</th>
                  <th class="text-right">Commission</th>
                  <th class="text-right">Total débité</th>
                  <th>Statut</th>
                  <th>Date Demande</th>
                  <th class="text-center">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let demande of demandes">
                  <td class="font-semibold text-slate-900">{{ getReferenceRetrait(demande) }}</td>
                  <td>{{ demande.compteEpargneNumero || '-' }}</td>
                  <td>{{ demande.membreNom || '-' }}</td>
                  <td class="text-right font-bold text-slate-900">{{ demande.montantDemande | currency: 'CDF' }}</td>
                  <td class="text-right font-semibold text-emerald-700">{{ (demande.fraisRetrait || 0) | currency: 'CDF' }}</td>
                  <td class="text-right font-semibold text-red-700">{{ getMontantTotalDebite(demande) | currency: 'CDF' }}</td>
                  <td>
                    <span class="mc-badge" [ngClass]="'bg-' + getStatusColor(demande.statut) + '-100 text-' + getStatusColor(demande.statut) + '-800'">
                      {{ getStatutLabel(demande.statut) }}
                    </span>
                  </td>
                  <td>{{ demande.dateDemande | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td class="text-center">
                    <div class="mc-button-row justify-center">
                    <button class="mc-btn mc-btn-primary" (click)="viewDetail(demande.id)">
                      Voir
                    </button>
                    <button *ngIf="canCancel(demande)" class="mc-btn bg-rose-600 text-white hover:bg-rose-700" (click)="annulerDemande(demande.id, demande.montantDemande)">
                      Annuler
                    </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div *ngIf="demandes.length === 0" class="mc-state mc-state-info text-center">
            Aucune demande de retrait trouvée
          </div>
        </section>

        <nav aria-label="Pagination">
          <div class="mc-button-row justify-center">
            <button class="mc-btn mc-btn-dark" [disabled]="currentPage === 1" (click)="previousPage()">Précédent</button>
            <span class="mc-badge bg-blue-100 text-blue-800">Page {{ currentPage }}</span>
            <button class="mc-btn mc-btn-dark" [disabled]="demandes.length < pageSize" (click)="nextPage()">Suivant</button>
          </div>
        </nav>
      </div>
    </div>
  `,
  styles: []
})
export class DemandeRetraitEpargneListComponent implements OnInit {

  private service = inject(DemandeRetraitEpargneService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private workflowMessageService = inject(WorkflowMessageService);

  allDemandes: DemandeRetraitEpargneResponse[] = [];
  demandes: DemandeRetraitEpargneResponse[] = [];
  guidance: WorkflowGuidance | null = null;
  filterStatut: string = '';
  filterText: string = '';
  filterDateDebut = '';
  filterDateFin = '';
  isCaissier = false;
  isControleur = false;
  isAdmin = false;
  
  allStatuts = Object.values(StatutDemandeRetrait);
  visibleStatuts = this.allStatuts;
  currentPage = 1;
  pageSize = 10;

  ngOnInit(): void {
    const role = this.normalizeRole(this.authService.getCurrentUser()?.role);
    this.isCaissier = role.includes('CAISSIER');
    this.isControleur = role.includes('CONTROLEUR');
    this.isAdmin = role.includes('ADMIN');
    this.visibleStatuts = this.resolveVisibleStatuts();
    this.applyQueryFilters();
    this.loadDemandes();
  }

  loadDemandes(): void {
    this.service.getAll(this.filterStatut || undefined).subscribe({
      next: (data: DemandeRetraitEpargneResponse[]) => {
        this.allDemandes = data ?? [];
        this.applyFilters();
      },
      error: (err: any) => {
        console.error('Erreur lors du chargement des demandes', err);
        alert('Erreur lors du chargement des demandes');
      }
    });
  }

  applyFilters(): void {
    // Filtrage côté client pour v1 (optionnel côté serveur pour v2)
    let filtered = [...this.allDemandes];

    if (this.filterStatut) {
      filtered = filtered.filter(d => d.statut === this.filterStatut);
    }

    if (this.filterText) {
      const text = this.filterText.toLowerCase();
      filtered = filtered.filter(d => 
        (d.compteEpargneNumero?.toLowerCase().includes(text)) ||
        (d.membreNom?.toLowerCase().includes(text))
      );
    }

    if (this.filterDateDebut) {
      const dateDebut = new Date(`${this.filterDateDebut}T00:00:00`);
      filtered = filtered.filter(d => !!d.dateDemande && new Date(d.dateDemande) >= dateDebut);
    }

    if (this.filterDateFin) {
      const dateFin = new Date(`${this.filterDateFin}T23:59:59.999`);
      filtered = filtered.filter(d => !!d.dateDemande && new Date(d.dateDemande) <= dateFin);
    }

    this.demandes = filtered;
    this.refreshGuidance();
  }

  resetFilters(): void {
    this.filterStatut = this.getDefaultStatutFilter();
    this.filterText = '';
    this.filterDateDebut = '';
    this.filterDateFin = '';
    this.loadDemandes();
  }

  viewDetail(id: number): void {
    this.router.navigate(['/epargne/demandes-retrait', id]);
  }

  navigateToCreate(): void {
    this.router.navigate(['/epargne/demandes-retrait/nouveau']);
  }

  canCreateDemande(): boolean {
    return this.isCaissier || this.isAdmin;
  }

  getPageTitle(): string {
    if (this.isCaissier) return 'Retraits à payer';
    if (this.isControleur) return 'Retraits à contrôler';
    return 'Demandes de Retrait Épargne';
  }

  canCancel(demande: DemandeRetraitEpargneResponse): boolean {
    if (this.isCaissier) {
      return false;
    }

    // Ne peut annuler que si pas décaissée et pas rejetée
    return demande.statut !== StatutDemandeRetrait.DECAISSEE && 
           demande.statut !== StatutDemandeRetrait.REJETEE;
  }

  annulerDemande(id: number, montant: number): void {
    if (confirm(`Êtes-vous sûr d'annuler cette demande de ${montant} CDF?`)) {
      this.service.annulerDemande(id).subscribe({
        next: () => {
          alert('Demande annulée avec succès');
          this.loadDemandes();
        },
        error: (err: any) => {
          console.error('Erreur lors de l\'annulation', err);
          alert('Erreur lors de l\'annulation: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  getReferenceRetrait(demande: DemandeRetraitEpargneResponse): string {
    if (demande.referenceRetrait) {
      return demande.referenceRetrait;
    }
    const date = demande.dateDemande || demande.createdAt;
    const year = date ? new Date(date).getFullYear() : new Date().getFullYear();
    return `RET-${year}-${String(demande.id).padStart(4, '0')}`;
  }

  getMontantTotalDebite(demande: DemandeRetraitEpargneResponse): number {
    return Number(demande.montantTotalDebite ?? ((demande.montantDemande || 0) + (demande.fraisRetrait || 0)));
  }

  getStatutLabel(statut: StatutDemandeRetrait): string {
    return STATUT_LABELS[statut];
  }

  getStatusColor(statut: StatutDemandeRetrait): string {
    return STATUT_COLORS[statut];
  }

  previousPage(): void {
    if (this.currentPage > 1) this.currentPage--;
  }

  nextPage(): void {
    this.currentPage++;
  }

  private refreshGuidance(): void {
    const currentRole = this.authService.getCurrentUser()?.role;
    const status = this.resolveGuidanceStatus();

    this.guidance = this.workflowMessageService.getGuidance({
      module: 'RETRAIT_EPARGNE',
      status,
      currentRole,
      permissions: this.authService.getCurrentUser()?.permissions
    });
  }

  private resolveGuidanceStatus(): string {
    if (this.filterStatut) {
      return this.filterStatut;
    }

    if (this.demandes.length > 0) {
      const uniqueStatuses = Array.from(new Set(this.demandes.map(d => d.statut).filter(Boolean)));
      if (uniqueStatuses.length === 1) {
        return uniqueStatuses[0] as string;
      }
    }

    return 'DEMANDE';
  }

  private applyQueryFilters(): void {
    const params = this.route.snapshot.queryParamMap;
    this.filterStatut = params.get('statut') || this.getDefaultStatutFilter();
    this.filterDateDebut = params.get('dateDebut') || this.filterDateDebut;
    this.filterDateFin = params.get('dateFin') || this.filterDateFin;
  }

  onFilterStatutChange(): void {
    this.loadDemandes();
  }

  private normalizeRole(role: string | null | undefined): string {
    let normalized = (role ?? '').trim().toUpperCase();
    if (normalized.startsWith('ROLE_')) {
      normalized = normalized.slice(5);
    }
    return normalized;
  }

  private getDefaultStatutFilter(): string {
    if (this.isCaissier) return StatutDemandeRetrait.VALIDEE;
    if (this.isControleur) return StatutDemandeRetrait.CREEE;
    return '';
  }

  private resolveVisibleStatuts(): StatutDemandeRetrait[] {
    if (this.isCaissier) {
      return [StatutDemandeRetrait.VALIDEE];
    }
    if (this.isControleur) {
      return [
        StatutDemandeRetrait.CREEE,
        StatutDemandeRetrait.EN_ATTENTE_VALIDATION,
        StatutDemandeRetrait.VALIDEE,
        StatutDemandeRetrait.REJETEE,
        StatutDemandeRetrait.DECAISSEE
      ];
    }
    return this.allStatuts;
  }
}
