import { Component, Input, OnChanges, OnInit, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecetteTerrainService } from '../../services/recette-terrain.service';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { AuthService } from '../../../../core/services/auth.service';

export interface KpiStats {
  totalRecettes: number;
  brouillon: number;
  soumises: number;
  validees: number;
  rejetees: number;
  totalEpargne: number;
  totalRemboursements: number;
  totalFrais: number;
  totalGeneral: number;
  totalEspecesRemises: number;
  totalEcartTresorerie: number;
  totalMembresVisites: number;
  totalCarnets: number;
  totalDemandesCredit: number;
  montantMoyen: number;
}

@Component({
  selector: 'app-recette-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="supervision-panel">
      <div>
        <p class="eyebrow">Supervision des collectes terrain</p>
        <h2>{{ mode === 'COLLECTE' ? 'Circuit actif CollecteJournaliereTerrain' : 'Historique RecetteJournaliereTerrain legacy' }}</h2>
        <p>{{ supervisionMessage }}</p>
      </div>
      <div class="supervision-steps">
        <span>État actuel : supervision terrain</span>
        <span>Étape suivante : {{ mode === 'COLLECTE' ? 'contrôle ou validation selon statut' : 'consultation / migration progressive' }}</span>
        <span>Rôle attendu : {{ roleLabel }}</span>
        <span>Action attendue : {{ expectedAction }}</span>
      </div>
    </section>

    <section class="kpi-grid">
      <article class="kpi-card emphasis">
        <span>{{ mode === 'COLLECTE' ? 'Nombre de collectes' : 'Nombre de recettes legacy' }}</span>
        <strong>{{ stats.totalRecettes }}</strong>
        <small>Tous statuts confondus</small>
      </article>
      <article class="kpi-card warning">
        <span>Brouillon</span>
        <strong>{{ stats.brouillon }}</strong>
        <small>En cours de rédaction</small>
      </article>
      <article class="kpi-card info">
        <span>Soumises</span>
        <strong>{{ stats.soumises }}</strong>
        <small>En attente de contrôle</small>
      </article>
      <article class="kpi-card success">
        <span>Validées</span>
        <strong>{{ stats.validees }}</strong>
        <small>Contrôlées / approuvées</small>
      </article>
      <article class="kpi-card danger">
        <span>Rejetées</span>
        <strong>{{ stats.rejetees }}</strong>
        <small>À corriger ou historisées</small>
      </article>
      <article class="kpi-card">
        <span>Épargne collectée</span>
        <strong>{{ stats.totalEpargne | currency: 'CDF' }}</strong>
        <small>Flux membre, pas revenu institutionnel</small>
      </article>
      <article class="kpi-card">
        <span>Remboursements crédit collectés</span>
        <strong>{{ stats.totalRemboursements | currency: 'CDF' }}</strong>
        <small>Principal/intérêts selon lignes collectées</small>
      </article>
      <article class="kpi-card">
        <span>Frais / carnets collectés</span>
        <strong>{{ stats.totalFrais | currency: 'CDF' }}</strong>
        <small>À analyser dans le rapport revenus</small>
      </article>
      <article class="kpi-card emphasis">
        <span>Total espèces attendues</span>
        <strong>{{ stats.totalGeneral | currency: 'CDF' }}</strong>
        <small>Montant opérationnel attendu</small>
      </article>
      <article class="kpi-card info">
        <span>Espèces réellement remises</span>
        <strong>{{ stats.totalEspecesRemises | currency: 'CDF' }}</strong>
        <small>Total remis caisse</small>
      </article>
      <article class="kpi-card warning">
        <span>Écart attendu/remis</span>
        <strong>{{ stats.totalEcartTresorerie | currency: 'CDF' }}</strong>
        <small>Somme des écarts</small>
      </article>
      <article class="kpi-card">
        <span>Membres visités</span>
        <strong>{{ stats.totalMembresVisites }}</strong>
        <small>{{ mode === 'COLLECTE' ? 'Dérivé des lignes collectées' : 'Déclaré dans les recettes legacy' }}</small>
      </article>
      <article class="kpi-card">
        <span>Carnets vendus / demandes crédit</span>
        <strong>{{ stats.totalCarnets }} / {{ stats.totalDemandesCredit }}</strong>
        <small>Suivi opérationnel terrain</small>
      </article>
    </section>
  `,
  styles: [`
    :host {
      display: block;
      margin-top: 18px;
    }

    .supervision-panel,
    .kpi-card {
      background: #ffffff;
      border: 1px solid #dde5f0;
      border-radius: 8px;
      box-shadow: 0 10px 26px rgba(24, 33, 47, 0.06);
    }

    .supervision-panel {
      display: grid;
      grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
      gap: 18px;
      padding: 22px;
      margin-bottom: 18px;
    }

    .eyebrow {
      margin: 0 0 6px;
      color: #5f6f84;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    h2 {
      margin: 0;
      color: #111827;
      font-size: 1.15rem;
      font-weight: 760;
    }

    p {
      color: #526173;
      margin: 8px 0 0;
    }

    .supervision-steps {
      display: grid;
      gap: 8px;
    }

    .supervision-steps span {
      border: 1px solid #d5deea;
      border-radius: 8px;
      background: #f8fafc;
      color: #334155;
      padding: 8px 10px;
      font-weight: 700;
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 14px;
    }

    .kpi-card {
      padding: 16px;
      min-height: 124px;
    }

    .kpi-card span,
    .kpi-card small {
      display: block;
    }

    .kpi-card span {
      color: #5f6f84;
      font-weight: 800;
    }

    .kpi-card strong {
      display: block;
      margin: 10px 0 6px;
      color: #18212f;
      font-size: 1.35rem;
      font-variant-numeric: tabular-nums;
    }

    .kpi-card small {
      color: #65758a;
      line-height: 1.35;
    }

    .emphasis {
      border-color: #b9cef0;
    }

    .info {
      border-color: #bae6fd;
    }

    .success {
      border-color: #bbf7d0;
    }

    .warning {
      border-color: #fde68a;
    }

    .danger {
      border-color: #fecaca;
    }

    @media (max-width: 1100px) {
      .kpi-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 760px) {
      .supervision-panel,
      .kpi-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class RecetteDashboardComponent implements OnInit, OnChanges {
  private recetteService = inject(RecetteTerrainService);
  private collecteService = inject(CollecteTerrainService);
  private authService = inject(AuthService);

  @Input() mode: 'COLLECTE' | 'LEGACY' = 'COLLECTE';

  stats: KpiStats = {
    totalRecettes: 0,
    brouillon: 0,
    soumises: 0,
    validees: 0,
    rejetees: 0,
    totalEpargne: 0,
    totalRemboursements: 0,
    totalFrais: 0,
    totalGeneral: 0,
    totalEspecesRemises: 0,
    totalEcartTresorerie: 0,
    totalMembresVisites: 0,
    totalCarnets: 0,
    totalDemandesCredit: 0,
    montantMoyen: 0
  };

  get supervisionMessage(): string {
    return this.mode === 'COLLECTE'
      ? 'Le circuit actif suit la saisie terrain, la soumission, le billetage, le contrôle des écarts et la validation.'
      : 'Le mode legacy sert à consulter les anciennes recettes journalières et à accompagner leur migration progressive.';
  }

  get roleLabel(): string {
    const role = (this.authService.getCurrentUser()?.role || '').toUpperCase();
    if (role === 'AGENT_TERRAIN') return 'Agent Terrain';
    if (role === 'GESTIONNAIRE') return 'Gestionnaire';
    if (role === 'CONTROLEUR') return 'Contrôleur';
    if (role === 'CHEF_BUREAU') return 'Chef de Bureau';
    if (role === 'COO') return 'COO';
    if (role === 'RCI') return 'RCI';
    if (role === 'GERANT_GENERAL') return 'Gérant Général';
    if (role === 'CAISSIER') return 'Caissier';
    return role || 'Supervision autorisée';
  }

  get expectedAction(): string {
    if (this.mode === 'LEGACY') {
      return 'Consulter l’historique legacy sans le mélanger avec le circuit actif';
    }
    const role = (this.authService.getCurrentUser()?.role || '').toUpperCase();
    if (role === 'AGENT_TERRAIN') return 'Suivre ses propres collectes et compléter la saisie terrain';
    if (role === 'GESTIONNAIRE') return 'Superviser les collectes et pré-contrôler les anomalies';
    if (role === 'CONTROLEUR') return 'Contrôler le billetage, les écarts et la validation';
    if (role === 'CHEF_BUREAU') return 'Superviser les collectes de l’antenne';
    if (['COO', 'RCI', 'GERANT_GENERAL'].includes(role)) return 'Auditer la vue globale et les écarts';
    if (role === 'CAISSIER') return 'Consulter les éléments qui impactent la caisse';
    return 'Suivre les collectes, contrôler les écarts et valider selon le périmètre autorisé';
  }

  ngOnInit(): void {
    this.loadStats();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['mode'] && !changes['mode'].firstChange) {
      this.loadStats();
    }
  }

  private loadStats(): void {
    if (this.mode === 'COLLECTE') {
      this.loadCollecteStats();
      return;
    }
    this.loadLegacyStats();
  }

  private loadLegacyStats(): void {
    this.recetteService.searchAndFilterPaginated(0, 10000).subscribe({
      next: (page) => {
        const recettes = page.content;
        this.stats.totalRecettes = page.totalElements;
        this.stats.brouillon = recettes.filter(r => r.statut === 'BROUILLON').length;
        this.stats.soumises = recettes.filter(r => r.statut === 'SOUMISE').length;
        this.stats.validees = recettes.filter(r => r.statut === 'VALIDEE').length;
        this.stats.rejetees = recettes.filter(r => r.statut === 'REJETEE').length;
        this.stats.totalEpargne = recettes.reduce((sum, r) => sum + (r.epargneCollectee || 0), 0);
        this.stats.totalRemboursements = recettes.reduce((sum, r) => sum + (r.remboursementsCreditCollectes || 0), 0);
        this.stats.totalFrais = recettes.reduce((sum, r) => sum + (r.fraisCollectes || 0), 0);
        this.stats.totalGeneral = recettes.reduce((sum, r) => sum + (r.totalCollecte || 0), 0);
        this.stats.totalEspecesRemises = recettes.reduce((sum, r) => sum + (r.especesRemises || 0), 0);
        this.stats.totalEcartTresorerie = recettes.reduce((sum, r) => sum + ((r.excedent || 0) - (r.manquant || 0)), 0);
        this.stats.totalMembresVisites = recettes.reduce((sum, r) => sum + (r.membresVisites || 0), 0);
        this.stats.totalCarnets = recettes.reduce((sum, r) => sum + (r.carnetDistribues || 0), 0);
        this.stats.totalDemandesCredit = recettes.reduce((sum, r) => sum + (r.demandesCreditRecueillies || 0), 0);
        this.stats.montantMoyen = recettes.length > 0 ? this.stats.totalGeneral / recettes.length : 0;
      },
      error: (err) => console.error('Erreur chargement stats', err)
    });
  }

  private loadCollecteStats(): void {
    this.collecteService.list({ page: 0, size: 10000 }).subscribe({
      next: (page) => {
        const collectes = page.content || [];
        this.stats.totalRecettes = page.totalElements || collectes.length;
        this.stats.brouillon = collectes.filter(c => c.statut === 'BROUILLON').length;
        this.stats.soumises = collectes.filter(c => c.statut === 'SOUMISE').length;
        this.stats.validees = collectes.filter(c => c.statut === 'VALIDEE').length;
        this.stats.rejetees = collectes.filter(c => c.statut === 'REJETEE').length;
        this.stats.totalEpargne = collectes.reduce((sum, c) => sum + (c.totalEpargneCalcule || 0), 0);
        this.stats.totalRemboursements = collectes.reduce((sum, c) => sum + (c.totalRemboursementsCalcule || 0), 0);
        this.stats.totalFrais = collectes.reduce((sum, c) => sum + (c.totalFraisCalcule || 0), 0);
        this.stats.totalGeneral = collectes.reduce((sum, c) => sum + (c.totalGeneralCalcule || 0), 0);
        this.stats.totalEspecesRemises = collectes.reduce((sum, c) => sum + (c.especesRemises || 0), 0);
        this.stats.totalEcartTresorerie = collectes.reduce((sum, c) => sum + (c.ecartTresorerie || 0), 0);
        this.stats.totalMembresVisites = collectes.reduce((sum, c) => {
          const lignes = c.lignes || [];
          return sum + new Set(lignes.map(l => l.membreId)).size;
        }, 0);
        this.stats.totalCarnets = collectes.reduce((sum, c) => {
          const lignes = c.lignes || [];
          return sum + lignes
            .filter(l => l.typeLigne === 'CARNET')
            .reduce((s, l) => s + (l.quantite || 0), 0);
        }, 0);
        this.stats.totalDemandesCredit = collectes.reduce((sum, c) => {
          const lignes = c.lignes || [];
          return sum + lignes.filter(l => l.typeLigne === 'DEMANDE_CREDIT').length;
        }, 0);
        this.stats.montantMoyen = collectes.length > 0 ? this.stats.totalGeneral / collectes.length : 0;
      },
      error: (err) => console.error('Erreur chargement stats collecte', err)
    });
  }
}
