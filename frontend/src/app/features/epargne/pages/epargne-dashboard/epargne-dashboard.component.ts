import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OperationEpargneListComponent } from '../operation-epargne-list/operation-epargne-list.component';
import { OperationEpargneFormComponent } from '../operation-epargne-form/operation-epargne-form.component';
import { CompteEpargneResponse } from '../../models/compte-epargne-response';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { Page } from '../../../../shared/models/page.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { PermissionCode } from '../../../../shared/enums/permission-code.enum';

@Component({
  selector: 'app-epargne-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    WorkflowGuidanceBannerComponent,
    OperationEpargneListComponent,
    OperationEpargneFormComponent
  ],
  templateUrl: './epargne-dashboard.component.html'
})
export class EpargneDashboardComponent implements OnInit {
  private compteEpargneService = inject(CompteEpargneService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private workflowMessageService = inject(WorkflowMessageService);

  comptes: CompteEpargneResponse[] = [];
  filteredComptes: CompteEpargneResponse[] = [];
  compteSelectionne: CompteEpargneResponse | null = null;
  loading = false;
  afficherFormOperation = false;
  errorMessage = '';
  guidance: WorkflowGuidance | null = null;

  searchTerm = '';
  repairMemberId: number | null = null;
  repairLoading = false;
  repairMessage = '';
  readonly isAgentTerrain = this.authService.hasRole('AGENT_TERRAIN');
  private membreIdFilter: number | null = null;

  ngOnInit(): void {
    const membreParam = this.route.snapshot.paramMap.get('membreId');
    this.membreIdFilter = membreParam ? Number(membreParam) : null;
    this.refreshGuidance();
    this.chargerTousLesComptes();
  }

  chargerTousLesComptes(): void {
    this.loading = true;
    this.errorMessage = '';

    this.resolveComptesRequest().subscribe({
      next: (data) => {
        this.comptes = this.normalizeComptesResponse(data);
        this.applySearch();
        this.refreshGuidance();
        this.loading = false;
      },
      error: (err) => {
        const status = Number(err?.status ?? 0);
        console.warn('[EpargneDashboard] Echec chargement comptes', {
          status,
          url: err?.url,
          message: err?.error?.message || err?.message
        });

        if (status === 401) {
          this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
        } else if (status === 403) {
          this.errorMessage = 'Accès refusé: vous n\'êtes pas autorisé à consulter ces comptes d\'épargne.';
        } else if (status === 404) {
          this.errorMessage = 'Service comptes épargne introuvable (404).';
        } else if (status >= 500) {
          this.errorMessage = 'Erreur serveur lors du chargement des comptes d\'épargne.';
        } else {
          this.errorMessage = 'Impossible de charger les comptes d\'épargne.';
        }
        this.refreshGuidance();
        this.loading = false;
      }
    });
  }

  private resolveComptesRequest() {
    if (this.membreIdFilter && this.membreIdFilter > 0) {
      return this.compteEpargneService.getByMembre(this.membreIdFilter);
    }

    if (this.isAgentTerrain) {
      return this.compteEpargneService.getMesMembresComptes();
    }

    if (this.authService.hasRole('MEMBER')) {
      return this.compteEpargneService.getMesComptes();
    }

    return this.compteEpargneService.getAll();
  }

  private normalizeComptesResponse(
    data: CompteEpargneResponse[] | Page<CompteEpargneResponse> | null | undefined
  ): CompteEpargneResponse[] {
    if (Array.isArray(data)) {
      return data;
    }

    if (data && Array.isArray(data.content)) {
      return data.content;
    }

    return [];
  }

  applySearch(): void {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredComptes = [...this.comptes];
      return;
    }

    this.filteredComptes = this.comptes.filter((compte) =>
      (compte.membreNomComplet || '').toLowerCase().includes(term)
      || (compte.numeroCompte || '').toLowerCase().includes(term)
      || String(compte.membreId).includes(term)
    );
  }

  repairMissingAccount(): void {
    if (!this.canRepairMissingAccount || !this.repairMemberId || this.repairMemberId <= 0) {
      this.repairMessage = 'Veuillez saisir un identifiant membre valide.';
      return;
    }

    this.repairLoading = true;
    this.repairMessage = '';
    this.compteEpargneService.repairMissingForMember(this.repairMemberId).subscribe({
      next: () => {
        this.repairLoading = false;
        this.repairMessage = 'Compte manquant créé avec succès.';
        this.chargerTousLesComptes();
      },
      error: (err) => {
        this.repairLoading = false;
        this.repairMessage = err?.error?.message || 'Impossible de créer le compte manquant.';
      }
    });
  }

  get canRepairMissingAccount(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  get canCreateOperation(): boolean {
    if (this.isAgentTerrain) {
      return false;
    }

    if (this.authService.hasRole('GESTIONNAIRE')) {
      return false;
    }

    return this.authService.hasAnyPermission([PermissionCode.EPARGNE_OPERATION_CREATE]);
  }

  get canViewOnlySelectedAccount(): boolean {
    return !!this.compteSelectionne && !this.canCreateOperation;
  }

  onCompteSelected(compte: CompteEpargneResponse): void {
    this.compteSelectionne = compte;
    this.afficherFormOperation = false;
    this.refreshGuidance();
  }

  toggleFormOperation(): void {
    if (!this.compteSelectionne || !this.canCreateOperation) {
      return;
    }
    this.afficherFormOperation = !this.afficherFormOperation;
    this.refreshGuidance();
  }

  onOperationCreated(): void {
    this.afficherFormOperation = false;
    this.refreshGuidance();
    this.chargerTousLesComptes();
  }

  trackByCompteId(_: number, compte: CompteEpargneResponse): number {
    return compte.id;
  }

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.split(' ');
    return (parts[0]?.charAt(0) + (parts[1]?.charAt(0) || '')).toUpperCase();
  }

  private refreshGuidance(): void {
    const currentRole = this.resolveCurrentRole();
    const status = this.resolveGuidanceStatus();
    const expectedRole = this.resolveExpectedRole(status, currentRole);

    this.guidance = this.workflowMessageService.getGuidance({
      module: 'EPARGNE_OPERATION',
      status,
      currentRole,
      expectedRole,
      metadata: {
        compteSelectionne: !!this.compteSelectionne,
        operationFormVisible: this.afficherFormOperation,
        expectedRole
      }
    });
  }

  private resolveCurrentRole(): string {
    if (this.authService.hasRole('AGENT_TERRAIN')) return 'AGENT_TERRAIN';
    if (this.authService.hasRole('CAISSIER')) return 'CAISSIER';
    if (this.authService.hasRole('GESTIONNAIRE')) return 'GESTIONNAIRE';
    if (this.authService.hasRole('CHEF_BUREAU')) return 'CHEF_BUREAU';
    if (this.authService.hasRole('CONTROLEUR')) return 'CONTROLEUR';
    if (this.authService.hasRole('ADMIN')) return 'ADMIN';
    if (this.authService.hasRole('MEMBER')) return 'MEMBER';
    return 'MEMBER';
  }

  private resolveGuidanceStatus(): string {
    if (!this.canCreateOperation) {
      return 'OPERATION_BLOQUEE';
    }

    if (this.compteSelectionne) {
      return 'OPERATION_AUTORISEE';
    }

    return 'CONSULTATION';
  }

  private resolveExpectedRole(status: string, currentRole: string): string {
    if (status === 'OPERATION_AUTORISEE' || status === 'CONSULTATION') {
      return currentRole;
    }

    return 'GESTIONNAIRE';
  }
}