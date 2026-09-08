import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RecetteTerrainService, PageResponse } from '../../services/recette-terrain.service';
import { RecetteTerrainResponse } from '../../models';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { CollecteTerrainResponse } from '../../models/collecte-terrain.model';
import { AuthService } from '../../../../core/services/auth.service';
import { RecetteDashboardComponent } from '../recette-dashboard/recette-dashboard.component';
import { ExportService } from '../../../../shared/services/export.service';
import { Page } from '../../../../shared/models/page.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

type ReportingMode = 'COLLECTE' | 'LEGACY';

@Component({
  selector: 'app-recette-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, RecetteDashboardComponent, WorkflowGuidanceBannerComponent],
  templateUrl: './recette-list.component.html',
  styleUrls: ['./recette-list.component.css']
})
export class RecetteListComponent implements OnInit {
  private recetteService = inject(RecetteTerrainService);
  private collecteService = inject(CollecteTerrainService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private exportService = inject(ExportService);

  activeMode: ReportingMode = 'COLLECTE';
  recettes: RecetteTerrainResponse[] = [];
  collectes: CollecteTerrainResponse[] = [];
  isLoading = false;
  currentUserRole: string = '';
  currentUserId: number = 0;
  Math = Math;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  // Filtres
  filterStatut: string = '';
  filterDateDebut: string = '';
  filterDateFin: string = '';
  filterSiteId: number | null = null;
  filterAgentId: number | null = null;
  globalGuidance: WorkflowGuidance = {
    title: 'Supervision des collectes terrain',
    message: 'Suivi opérationnel des collectes terrain, des remises caisse et des écarts. Le circuit actif reste séparé des anciennes recettes historiques legacy.',
    currentStep: 'Supervision terrain',
    nextStep: 'Contrôle ou validation selon le statut',
    expectedRole: 'Gestionnaire / Contrôleur / Chef de Bureau',
    expectedAction: 'Suivre les collectes, contrôler les écarts et valider les remises selon le périmètre autorisé',
    severity: 'info',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    this.currentUserRole = currentUser?.role || '';
    this.currentUserId = currentUser?.id || 0;
    this.applyQueryFilters();
    this.loadData();
  }

  private applyQueryFilters(): void {
    const params = this.route.snapshot.queryParamMap;
    const mode = params.get('mode');
    if (mode === 'COLLECTE' || mode === 'LEGACY') {
      this.activeMode = mode;
    }

    this.filterStatut = params.get('statut') || this.filterStatut;
    this.filterDateDebut = params.get('dateDebut') || this.filterDateDebut;
    this.filterDateFin = params.get('dateFin') || this.filterDateFin;
    this.filterSiteId = params.get('siteId') ? Number(params.get('siteId')) : this.filterSiteId;
    this.filterAgentId = params.get('agentId') ? Number(params.get('agentId')) : this.filterAgentId;
  }

  private loadData(): void {
    if (this.activeMode === 'COLLECTE') {
      this.loadCollectes();
      return;
    }
    this.loadRecettes();
  }

  private loadCollectes(): void {
    this.isLoading = true;
    this.collecteService.list({
      statut: this.filterStatut || undefined,
      dateDebut: this.filterDateDebut || undefined,
      dateFin: this.filterDateFin || undefined,
      agentId: this.filterAgentId || undefined,
      siteId: this.filterSiteId || undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).subscribe({
      next: (page: Page<CollecteTerrainResponse>) => {
        this.collectes = page.content || [];
        this.recettes = [];
        this.totalElements = page.totalElements || 0;
        this.totalPages = page.totalPages || 0;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement collectes', err);
        this.isLoading = false;
      }
    });
  }

  private loadRecettes(): void {
    this.isLoading = true;
    this.recetteService.searchAndFilterPaginated(
      this.currentPage,
      this.pageSize,
      'dateRecette',
      'desc',
      this.filterStatut || undefined,
      this.filterSiteId || undefined,
      this.filterAgentId || undefined,
      this.filterDateDebut || undefined,
      this.filterDateFin || undefined
    ).subscribe({
      next: (page: PageResponse<RecetteTerrainResponse>) => {
        this.recettes = page.content;
        this.collectes = [];
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement', err);
        this.isLoading = false;
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0; // Reset to first page when filtering
    this.loadData();
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadData();
    }
  }

  setMode(mode: ReportingMode): void {
    if (this.activeMode === mode) {
      return;
    }
    this.activeMode = mode;
    this.currentPage = 0;
    this.loadData();
  }

  get modeTitle(): string {
    return this.activeMode === 'COLLECTE' ? 'Collectes terrain actives' : 'Recettes historiques legacy';
  }

  get modeDescription(): string {
    return this.activeMode === 'COLLECTE'
      ? 'Circuit actuel : saisie, soumission, billetage, contrôle et validation.'
      : 'Anciennes recettes conservées pour consultation et migration progressive. Ne pas utiliser pour le nouveau circuit terrain.';
  }

  get canCreateLegacy(): boolean {
    return this.activeMode === 'LEGACY' && this.currentUserRole === 'ADMIN';
  }

  goToFirstPage(): void {
    this.onPageChange(0);
  }

  goToLastPage(): void {
    this.onPageChange(this.totalPages - 1);
  }

  goToPreviousPage(): void {
    this.onPageChange(this.currentPage - 1);
  }

  goToNextPage(): void {
    this.onPageChange(this.currentPage + 1);
  }

  onCreate(): void {
    if (this.activeMode !== 'LEGACY') {
      alert('La saisie terrain active se fait dans le module Collecte.');
      return;
    }
    if (this.currentUserRole !== 'ADMIN') {
      alert('Accès non autorisé');
      return;
    }
    if (!confirm('Vous créez une recette historique. Pour une collecte terrain normale, utilisez le module Ma collecte. Continuer ?')) {
      return;
    }
    this.router.navigate(['/recettes/nouveau']);
  }

  onView(id: number): void {
    if (this.activeMode !== 'LEGACY') {
      return;
    }
    this.router.navigate(['/recettes', id]);
  }

  onEdit(recette: RecetteTerrainResponse): void {
    if (this.activeMode !== 'LEGACY') {
      return;
    }
    if (recette.statut !== 'BROUILLON') {
      alert('Impossible de modifier une recette non-brouillon');
      return;
    }
    this.router.navigate(['/recettes', recette.id, 'edit']);
  }

  onDelete(id: number): void {
    if (this.activeMode !== 'LEGACY') {
      return;
    }
    if (!confirm('Êtes-vous sûr?')) return;

    this.recetteService.delete(id).subscribe({
      next: () => {
        alert('Recette supprimée');
        this.loadData();
      },
      error: (err) => alert('Erreur: ' + err.message)
    });
  }

  getStatusColor(statut: string): string {
    const colors: Record<string, string> = {
      'BROUILLON': 'warning',
      'SOUMISE': 'info',
      'VALIDEE': 'success',
      'REJETEE': 'danger'
    };
    return colors[statut] || 'secondary';
  }

  getStatusLabel(statut: string): string {
    const labels: Record<string, string> = {
      'BROUILLON': 'Brouillon',
      'SOUMISE': 'Soumise',
      'VALIDEE': 'Validée',
      'REJETEE': 'Rejetée'
    };
    return labels[statut] || statut;
  }

  canEdit(recette: RecetteTerrainResponse): boolean {
    return this.activeMode === 'LEGACY' && recette.statut === 'BROUILLON' && this.currentUserRole === 'ADMIN';
  }

  canValidate(recette: RecetteTerrainResponse): boolean {
    return false;
  }

  legacyEcart(recette: RecetteTerrainResponse): number {
    return (recette.excedent || 0) - (recette.manquant || 0);
  }

  getCollecteStatusLabel(statut: string): string {
    return this.getStatusLabel(statut);
  }

  // Pagination helpers
  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible);
    
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }
    
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }

  // === EXPORTS ===

  onExportExcel(): void {
    if (this.activeMode === 'COLLECTE') {
      this.onExportCollecteDetail();
      return;
    }
    if (this.recettes.length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }
    this.exportService.exportToExcel(this.recettes, 'recettes-terrain.xlsx');
  }

  onExportPdf(): void {
    if (this.activeMode === 'COLLECTE') {
      alert('Export PDF legacy uniquement. Utilisez les exports Collecte détaillés (Excel).');
      return;
    }
    if (this.recettes.length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }

    const stats = {
      totalRecettes: this.totalElements,
      brouillon: this.recettes.filter(r => r.statut === 'BROUILLON').length,
      soumises: this.recettes.filter(r => r.statut === 'SOUMISE').length,
      validees: this.recettes.filter(r => r.statut === 'VALIDEE').length,
      rejetees: this.recettes.filter(r => r.statut === 'REJETEE').length,
      totalCollecte: this.recettes.reduce((sum, r) => sum + (r.totalCollecte || 0), 0),
      montantMoyen: this.recettes.length > 0 
        ? this.recettes.reduce((sum, r) => sum + (r.totalCollecte || 0), 0) / this.recettes.length 
        : 0
    };

    const filtres = {
      statut: this.filterStatut || null,
      dateDebut: this.filterDateDebut || null,
      dateFin: this.filterDateFin || null
    };

    this.exportService.exportToPdf(this.recettes, stats, filtres, 'recettes-terrain.pdf');
  }

  onExportCollecteDetail(): void {
    if (this.collectes.length === 0) {
      alert('Aucune collecte à exporter');
      return;
    }
    this.exportService.exportCollectesDetailByMembreExcel(this.collectes, 'collectes-detail-par-membre.xlsx');
  }

  onExportCollecteResume(): void {
    if (this.collectes.length === 0) {
      alert('Aucune collecte à exporter');
      return;
    }
    this.exportService.exportCollectesResumeJournalierParAgentExcel(this.collectes, 'collectes-resume-journalier-agent.xlsx');
  }

  onExportCollecteEcarts(): void {
    if (this.collectes.length === 0) {
      alert('Aucune collecte à exporter');
      return;
    }
    this.exportService.exportCollectesEcartsTresorerieExcel(this.collectes, 'collectes-ecarts-tresorerie.xlsx');
  }
}
