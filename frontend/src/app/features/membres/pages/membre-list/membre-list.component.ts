import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MembreService } from '../../services/membre.service';
import { MembreResponse } from '../../models/membre-response';
import { Page } from '../../../../shared/models/page.model';
import { StatutMembre } from '../../enum/statut-membre';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-membre-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, WorkflowGuidanceBannerComponent],
  templateUrl: './membre-list.component.html'
})
export class MembreListComponent implements OnInit {

  private membreService = inject(MembreService);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  membres: MembreResponse[] = [];
  totalElements = 0;
  totalPages = 0;
  loading = false;
  error = '';

  // Filtres
  searchTerm = '';
  statutFilter: '' | StatutMembre = '';
  siteFilter: string = '';
  agentFilter: string = '';

  // Pagination backend (0-based)
  currentPage = 0;
  itemsPerPage = 10;

  readonly statutOptions: Array<{ label: string; value: '' | StatutMembre }> = [
    { label: 'Tous les statuts', value: '' },
    { label: 'En attente', value: 'EN_ATTENTE' },
    { label: 'Actif', value: 'ACTIF' },
    { label: 'Suspendu', value: 'SUSPENDU' },
    { label: 'Bloque', value: 'BLOQUE' },
    { label: 'Cloture', value: 'CLOTURE' },
  ];

  readonly globalGuidance: WorkflowGuidance = {
    title: 'Gestion des membres',
    message: 'Cette page permet de gerer les membres de l institution. Un membre doit avoir un code unique, un site et un agent responsable. La creation d un membre entraine la creation de son compte epargne. Les informations du membre sont utilisees dans les credits, l epargne, les retraits, les garanties, les recettes journalieres, les rapports et le controle interne. Toute modification importante doit rester tracable.',
    currentStep: 'Donnee membre consultable ou modifiable',
    nextStep: 'Validation des informations / utilisation dans les workflows metier',
    expectedRole: 'Agent Terrain / Gestionnaire / Controleur / Chef de Bureau selon les droits existants',
    expectedAction: 'Creer, verifier ou mettre a jour les informations du membre selon les droits existants',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'Un membre ne doit pas etre cree sans site ni agent responsable. Le membre ne doit pas etre duplique.'
  };

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(() => {
      this.loadMembres();
    });
  }

  loadMembres(): void {
    this.loading = true;
    this.error = '';

    const siteId = this.siteFilter.trim() ? Number(this.siteFilter) : undefined;
    if (this.siteFilter.trim() && (!siteId || siteId <= 0)) {
      this.loading = false;
      this.error = 'Le filtre site doit être un identifiant numérique valide.';
      return;
    }

    this.membreService.searchPaginated(this.searchTerm, siteId, this.currentPage, this.itemsPerPage).subscribe({
      next: (page: Page<MembreResponse>) => {
        this.totalElements = page?.totalElements ?? 0;
        this.totalPages = page?.totalPages ?? 0;
        this.membres = this.applyClientFilters(page?.content ?? []);
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les membres.';
        this.membres = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadMembres();
  }

  onFiltersChanged(): void {
    this.currentPage = 0;
    this.loadMembres();
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.statutFilter = '';
    this.siteFilter = '';
    this.agentFilter = '';
    this.currentPage = 0;
    this.loadMembres();
  }

  private applyClientFilters(source: MembreResponse[]): MembreResponse[] {
    let filtered = [...source];

    if (this.statutFilter) {
      filtered = filtered.filter((m) => m.statut === this.statutFilter);
    }

    const agentId = this.agentFilter.trim() ? Number(this.agentFilter) : undefined;
    if (this.agentFilter.trim() && (!agentId || agentId <= 0)) {
      this.error = 'Le filtre agent doit être un identifiant numérique valide.';
      return [];
    }

    if (agentId) {
      filtered = filtered.filter((m) => m.agentId === agentId);
    }

    return filtered;
  }

  goToPage(page: number): void {
    const target = page - 1;
    if (target < 0 || target >= this.totalPages) {
      return;
    }

    this.currentPage = target;
    this.loadMembres();
  }

  getTotalPages(): number {
    return this.totalPages;
  }

  getPageNumbers(): number[] {
    const totalPages = this.getTotalPages();
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  get hasActiveFilters(): boolean {
    return !!(
      this.searchTerm.trim()
      || this.statutFilter
      || this.siteFilter.trim()
      || this.agentFilter.trim()
    );
  }

  get currentPageDisplay(): number {
    return this.currentPage + 1;
  }

  get canEditMembers(): boolean {
    return !this.authService.hasRole('AGENT_TERRAIN');
  }

  get canCloseMembers(): boolean {
    return !this.authService.hasRole('GESTIONNAIRE');
  }

  get isGestionnaire(): boolean {
    return this.authService.hasRole('GESTIONNAIRE');
  }

  supprimer(id: number): void {
    const confirmation = confirm('Êtes-vous sûr de vouloir clôturer ce membre ?');
    if (!confirmation) {
      return;
    }

    this.membreService.delete(id).subscribe({
      next: () => {
        this.loadMembres();
      },
      error: () => {
        this.error = 'Impossible de clôturer ce membre.';
      }
    });
  }
}