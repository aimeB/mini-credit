import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, catchError, of, takeUntil } from 'rxjs';
import { AgenceResponse } from '../../../organisation/models/agence-response';
import { AgenceService } from '../../../employes/services/agence.service';
import { RapportRevenusService } from '../../services/rapport-revenus.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ApportFinancementDetailDto, ChargeDetailDto, ComparaisonAgenceDto, ControleCoherenceDto, DetailMasseSalarialeDto, RapportRevenusFilters, RapportRevenusResponse, RevenuDetailDto, RevenuKpiDto } from '../../models/rapport-revenus.model';
import { NatureFinancementApprovisionnement, NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS } from '../../../caisse/models/nature-financement-approvisionnement';
import { DepenseCaisseBeneficiaireSalaire } from '../../../caisse/models/depense-caisse-beneficiaire-salaire';
import { PaieEmployePreview } from '../../../caisse/models/paie-employe-preview';
import { TYPE_PAIEMENT_PERSONNEL_OPTIONS, TypePaiementPersonnel } from '../../../caisse/models/type-paiement-personnel';

type QuickPeriod = 'today' | 'week' | 'month' | 'custom';
type RapportTab = 'decision' | 'comparaison' | 'revenus' | 'paie' | 'credit' | 'financement' | 'audit';

interface SelectOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-rapport-revenus',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="mc-page-wide">
      <section class="mc-page-hero flex flex-col gap-5 lg:flex-row lg:items-stretch lg:justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-wide text-amber-100">Rapports financiers</p>
          <h1 class="mc-page-title">Rapport revenus & resultat</h1>
          <p class="mc-page-subtitle">Suivi consultatif des revenus reels, des charges reelles, du resultat net estime et des flux financiers exclus du resultat.</p>
        </div>
        <div class="rounded-lg border border-white/20 bg-white/10 p-5 text-white lg:min-w-[260px]">
          <span>Total revenus</span>
          <strong class="mt-2 block text-2xl font-bold">{{ formatMoney(report?.totalRevenus || 0) }}</strong>
          <small class="mt-2 block text-white/80">{{ report?.dateDebut || filters.dateDebut }} - {{ report?.dateFin || filters.dateFin }}</small>
        </div>
      </section>

      <section class="mc-filter-panel space-y-4">
        <div class="mc-button-row" aria-label="Periode rapide">
          <button type="button" class="mc-btn" [class.mc-btn-primary]="quickPeriod === 'today'" (click)="setQuickPeriod('today')">Aujourd'hui</button>
          <button type="button" class="mc-btn" [class.mc-btn-primary]="quickPeriod === 'week'" (click)="setQuickPeriod('week')">Semaine</button>
          <button type="button" class="mc-btn" [class.mc-btn-primary]="quickPeriod === 'month'" (click)="setQuickPeriod('month')">Mois</button>
          <button type="button" class="mc-btn" [class.mc-btn-primary]="quickPeriod === 'custom'" (click)="quickPeriod = 'custom'">Personnalise</button>
        </div>

        <div class="mc-filter-grid">
          <label>
            <span class="mc-field-label">Date debut</span>
            <input class="mc-input" type="date" [(ngModel)]="filters.dateDebut" (ngModelChange)="quickPeriod = 'custom'">
          </label>
          <label>
            <span class="mc-field-label">Date fin</span>
            <input class="mc-input" type="date" [(ngModel)]="filters.dateFin" (ngModelChange)="quickPeriod = 'custom'">
          </label>
          <label>
            <span class="mc-field-label">Antenne / bureau</span>
            <select class="mc-select" [(ngModel)]="filters.agenceId">
              <option *ngIf="hasGlobalAgencyAccess()" [ngValue]="null">Toutes accessibles</option>
              <option *ngFor="let agence of agences" [ngValue]="agence.id">{{ agence.nomAgence }}</option>
            </select>
          </label>
          <label>
            <span class="mc-field-label">Categorie</span>
            <select class="mc-select" [(ngModel)]="filters.categorie">
              <option [ngValue]="null">Toutes</option>
              <option *ngFor="let option of categorieOptions" [ngValue]="option.value">{{ option.label }}</option>
            </select>
          </label>
          <label>
            <span class="mc-field-label">Source</span>
            <select class="mc-select" [(ngModel)]="filters.source">
              <option [ngValue]="null">Toutes</option>
              <option *ngFor="let option of sourceOptions" [ngValue]="option.value">{{ option.label }}</option>
            </select>
          </label>
          <button type="button" class="mc-btn mc-btn-primary" (click)="loadReport()" [disabled]="loading">
            {{ loading ? 'Chargement...' : 'Actualiser' }}
          </button>
        </div>

        <p class="mc-state mc-state-warning" *ngIf="agenceWarning">{{ agenceWarning }}</p>
      </section>

      <div class="mc-state mc-state-danger" *ngIf="error">{{ error }}</div>

      <section class="mc-kpi-grid-decision" *ngIf="report as data">
        <article class="mc-kpi-card mc-kpi-positive">
          <span class="mc-kpi-badge">Revenu</span>
          <span class="mc-kpi-label">Revenus reels</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.totalRevenus) }}</strong>
          <small class="mc-kpi-help">Frais, interets, penalites et carnets vendus sur {{ periodeLabel(data) }}.</small>
        </article>
        <article class="mc-kpi-card mc-kpi-warning">
          <span class="mc-kpi-badge">Charge</span>
          <span class="mc-kpi-label">Charges connues</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.totalCharges || 0) }}</strong>
          <small class="mc-kpi-help">Depenses deja identifiees, dont caisse payee et cout carnets.</small>
        </article>
        <article class="mc-kpi-card" [ngClass]="amountTone(data.beneficeNetEstime || 0) === 'positive' ? 'mc-kpi-positive' : 'mc-kpi-danger'">
          <span class="mc-kpi-badge">Resultat</span>
          <span class="mc-kpi-label">Resultat net estime</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.beneficeNetEstime || 0) }}</strong>
          <small class="mc-kpi-help">Revenus reels moins charges connues.</small>
        </article>
        <article class="mc-kpi-card" [ngClass]="amountTone(data.resultatPrevisionnelApresSalairesAPayer || 0) === 'positive' ? 'mc-kpi-positive' : 'mc-kpi-warning'">
          <span class="mc-kpi-badge">Projection</span>
          <span class="mc-kpi-label">Resultat apres salaires restant a payer</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.resultatPrevisionnelApresSalairesAPayer || 0) }}</strong>
          <small class="mc-kpi-help">Resultat net estime diminue des salaires encore dus.</small>
        </article>
        <article class="mc-kpi-card" [ngClass]="amountTone(data.resultatPrevisionnelApresChargesFixes || 0) === 'positive' ? 'mc-kpi-positive' : 'mc-kpi-warning'">
          <span class="mc-kpi-badge">Projection</span>
          <span class="mc-kpi-label">Resultat apres charges restantes</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.resultatPrevisionnelApresChargesFixes || 0) }}</strong>
          <small class="mc-kpi-help">Apres salaires et transport terrain restant a payer.</small>
        </article>
        <article class="mc-kpi-card mc-kpi-estimation">
          <span class="mc-kpi-badge">Estimation</span>
          <span class="mc-kpi-label">Estimation prudente recuperable</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.tresorerieDisponible?.tresorerieRecuperablePrudente || 0) }}</strong>
          <small class="mc-kpi-help">Apres protection des fonds membres et engagements connus. A valider avant tout remboursement.</small>
        </article>
        <article class="mc-kpi-card mc-kpi-protected">
          <span class="mc-kpi-badge">Protection</span>
          <span class="mc-kpi-label">Fonds membres proteges</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.fondsMembresProteges?.totalFondsMembres || 0) }}</strong>
          <small class="mc-kpi-help">Epargne, garanties et retraits valides non payes. Non recuperable par le proprietaire.</small>
        </article>
        <article class="mc-kpi-card mc-kpi-neutral">
          <span class="mc-kpi-badge">Financement</span>
          <span class="mc-kpi-label">Capital proprietaire non encore rembourse</span>
          <strong class="mc-kpi-value">{{ formatMoney(data.apportsFinancements?.capitalInjecteRestantARecuperer || 0) }}</strong>
          <small class="mc-kpi-help">Apports proprietaire cumules moins remboursements deja effectues.</small>
        </article>
        <article class="mc-kpi-card" [ngClass]="criticalAlerts(data).length > 0 ? 'mc-kpi-danger' : 'mc-kpi-positive'">
          <span class="mc-kpi-badge">Alerte</span>
          <span class="mc-kpi-label">Alertes critiques</span>
          <strong class="mc-kpi-value">{{ criticalAlerts(data).length }}</strong>
          <small class="mc-kpi-help">Controles ROUGE et ORANGE a traiter avant decision financiere.</small>
        </article>
      </section>

      <nav class="report-tabs" *ngIf="report">
        <button type="button" *ngFor="let tab of tabs" [class.active]="activeTab === tab.value" (click)="activeTab = tab.value">
          <span>{{ tab.label }}</span>
          <small>{{ tab.hint }}</small>
        </button>
      </nav>

      <section class="panel comparison-panel" *ngIf="activeTab === 'comparaison' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Comparaison des agences</p>
            <h2>Rentabilite par agence</h2>
          </div>
          <span>{{ comparisonAgencies(data).length }} agence(s)</span>
        </div>

        <div class="position-grid comparison-cards">
          <article class="position-card income">
            <span>Agence avec plus de revenus</span>
            <strong>{{ rankName(data.comparaisonAgences?.agencePlusRevenus) }}</strong>
            <small>{{ formatMoney(data.comparaisonAgences?.agencePlusRevenus?.revenusReels || 0) }}</small>
          </article>
          <article class="position-card highlight">
            <span>Agence plus rentable</span>
            <strong>{{ rankName(data.comparaisonAgences?.agencePlusRentable) }}</strong>
            <small>{{ formatMoney(data.comparaisonAgences?.agencePlusRentable?.resultatNetEstime || 0) }}</small>
          </article>
          <article class="position-card">
            <span>Agence avec plus de charges</span>
            <strong>{{ rankName(data.comparaisonAgences?.agencePlusCharges) }}</strong>
            <small>{{ formatMoney(data.comparaisonAgences?.agencePlusCharges?.chargesConnues || 0) }}</small>
          </article>
          <article class="position-card count">
            <span>Agences deficitaires</span>
            <strong>{{ data.comparaisonAgences?.agencesDeficitaires?.length || 0 }}</strong>
          </article>
        </div>

        <div class="result-list comparison-summary">
          <div><span>Total revenus agences</span><strong>{{ formatMoney(data.comparaisonAgences?.totalRevenusAgences || 0) }}</strong></div>
          <div><span>Total charges agences</span><strong>{{ formatMoney(data.comparaisonAgences?.totalChargesAgences || 0) }}</strong></div>
          <div><span>Total resultat agences</span><strong>{{ formatMoney(data.comparaisonAgences?.totalResultatAgences || 0) }}</strong></div>
          <div class="net"><span>Charges globales / siège</span><strong>{{ formatMoney(data.chargesGlobalesSiege || data.comparaisonAgences?.chargesGlobalesSiege || 0) }}</strong></div>
          <div class="net"><span>Resultat global apres charges siège</span><strong>{{ formatMoney(data.resultatApresChargesGlobalesSiege || data.comparaisonAgences?.resultatGlobalApresChargesSiege || 0) }}</strong></div>
        </div>

        <div class="insight-box">
          <strong>Charges globales / siège separees</strong>
          <p>Les salaires COO et RCI, ainsi que les charges non rattachables a une agence, restent separes de Delvaux, Masina ou toute autre agence.</p>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Agence</th>
                <th>Revenus reels</th>
                <th>Charges connues</th>
                <th>Resultat net estime</th>
                <th>Marge</th>
                <th>Commissions retrait</th>
                <th>Frais credit</th>
                <th>Interets</th>
                <th>Penalites</th>
                <th>Carnets vendus</th>
                <th>Autres revenus</th>
                <th>Salaires payes</th>
                <th>Salaires restant</th>
                <th>Primes</th>
                <th>Transport paye</th>
                <th>Transport restant</th>
                <th>Fonctionnement</th>
                <th>Achat carnets</th>
                <th>Autres charges</th>
                <th>Collectes validees</th>
                <th>Membres actifs</th>
                <th>Credits actifs</th>
                <th>Alertes</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let row of comparisonAgencies(data)">
                <td>{{ row.agenceNom || 'Non rattache' }}</td>
                <td class="amount strong">{{ formatMoney(row.revenusReels) }}</td>
                <td class="amount">{{ formatMoney(row.chargesConnues) }}</td>
                <td class="amount strong" [ngClass]="amountTone(row.resultatNetEstime)">{{ formatMoney(row.resultatNetEstime) }}</td>
                <td class="amount">{{ formatPercent(row.margePourcentage) }}</td>
                <td class="amount">{{ formatMoney(row.commissionsRetrait) }}</td>
                <td class="amount">{{ formatMoney(row.fraisCredit) }}</td>
                <td class="amount">{{ formatMoney(row.interetsCredit) }}</td>
                <td class="amount">{{ formatMoney(row.penalitesCredit) }}</td>
                <td class="amount">{{ formatMoney(row.carnetsVendus) }}</td>
                <td class="amount">{{ formatMoney(row.autresRevenus) }}</td>
                <td class="amount">{{ formatMoney(row.salairesPayes) }}</td>
                <td class="amount">{{ formatMoney(row.salairesRestantAPayer) }}</td>
                <td class="amount">{{ formatMoney(row.primes) }}</td>
                <td class="amount">{{ formatMoney(row.transportTerrain) }}</td>
                <td class="amount">{{ formatMoney(row.transportRestantAPayer) }}</td>
                <td class="amount">{{ formatMoney(row.fonctionnement) }}</td>
                <td class="amount">{{ formatMoney(row.achatCarnets) }}</td>
                <td class="amount">{{ formatMoney(row.autresCharges) }}</td>
                <td class="amount">{{ row.collectesValidees || 0 }}</td>
                <td class="amount">{{ row.membresActifs || 0 }}</td>
                <td class="amount">{{ row.creditsActifs || 0 }}</td>
                <td>
                  <span class="pill danger-pill" *ngIf="row.alerteDeficit">Deficit</span>
                  <span class="pill warning-pill" *ngIf="row.alerteChargesElevees">Charges elevees</span>
                  <span class="pill" *ngIf="!row.alerteDeficit && !row.alerteChargesElevees">OK</span>
                </td>
              </tr>
              <tr *ngIf="comparisonAgencies(data).length === 0">
                <td colspan="23" class="empty-state">Aucune agence dans le perimetre selectionne.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="decision-panel" *ngIf="activeTab === 'decision' && report as data">
        <article class="panel decision-summary">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Vue decisionnelle</p>
              <h2>Ce qu'il faut regarder maintenant</h2>
            </div>
            <span>{{ periodeLabel(data) }}</span>
          </div>
          <div class="decision-grid">
            <div><span>Activite</span><strong>{{ (data.beneficeNetEstime || 0) >= 0 ? 'Beneficiaire' : 'Deficitaire' }}</strong></div>
            <div><span>Charges restant a reserver</span><strong>{{ formatMoney((data.masseSalariale?.salairesRestantAPayer || 0) + (data.transportFixePrevu?.totalTransportRestant || 0)) }}</strong></div>
            <div><span>Fonds non recuperables</span><strong>{{ formatMoney(data.fondsMembresProteges?.totalFondsMembres || 0) }}</strong></div>
            <div><span>Montant indicatif a valider</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.montantRecuperableConseille || 0) }}</strong><small>Estimation interne, pas une autorisation automatique de retrait.</small></div>
          </div>
          <p class="agence-hint">La synthese ne modifie aucun calcul: elle reprend les montants deja produits par le rapport et masque les details d'audit dans les autres onglets.</p>
        </article>

        <article class="panel interpretation-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Aide a l'interpretation</p>
              <h2>Ce que cela signifie</h2>
            </div>
          </div>
          <div class="interpretation-list">
            <p>Les revenus reels representent les gains de l'institution.</p>
            <p>Les charges connues representent les depenses deja identifiees.</p>
            <p>Les fonds membres proteges ne sont pas recuperables par le proprietaire.</p>
            <p>Le capital proprietaire non encore rembourse correspond aux apports restants a rembourser.</p>
            <p>L'estimation prudente recuperable est une aide de gestion, pas une autorisation automatique.</p>
          </div>
        </article>

        <article class="panel owner-analysis-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Decision interne</p>
              <h2>Analyse proprietaire</h2>
            </div>
            <span>{{ decisionStatus(data) }}</span>
          </div>
          <div class="owner-analysis-grid">
            <div><span>Capital proprietaire non encore rembourse</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.capitalInjecteRestantARecuperer || data.apportsFinancements?.capitalInjecteRestantARecuperer || 0) }}</strong></div>
            <div><span>Estimation prudente recuperable</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.tresorerieRecuperablePrudente || data.tresorerieDisponible?.tresorerieRecuperablePrudente || 0) }}</strong></div>
            <div><span>Fonds membres proteges</span><strong>{{ formatMoney(data.fondsMembresProteges?.totalFondsMembres || 0) }}</strong></div>
            <div><span>Charges restantes a prevoir</span><strong>{{ formatMoney((data.masseSalariale?.salairesRestantAPayer || 0) + (data.transportFixePrevu?.totalTransportRestant || 0)) }}</strong></div>
          </div>
          <p class="owner-analysis-note">Cette section est une aide interne a la decision. Elle ne constitue pas une regle officielle 3N ni une autorisation automatique de retrait.</p>
        </article>
      </section>

      <section class="accounting-grid" *ngIf="activeTab === 'revenus' && report as data">
        <article class="panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Flux exclus</p>
              <h2>Flux financiers exclus du resultat</h2>
            </div>
            <span>{{ formatMoney(data.totalMouvementsNonRevenus || 0) }}</span>
          </div>
          <div class="category-list">
            <div class="category-row" *ngFor="let item of data.mouvementsNonRevenus || []">
              <span>{{ item.sousCategorie || categoryLabel(item.categorie) }}</span>
              <strong>{{ formatMoney(item.montant) }}</strong>
            </div>
            <p class="empty-state" *ngIf="(data.mouvementsNonRevenus || []).length === 0">Aucun flux financier exclu sur cette periode.</p>
          </div>
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Charges</p>
              <h2>Resultat net estime</h2>
            </div>
            <span>{{ formatMoney(data.beneficeNetEstime || 0) }}</span>
          </div>
          <div class="result-list">
            <div><span>Revenus reels</span><strong>{{ formatMoney(data.totalRevenus) }}</strong></div>
            <div><span>Charges caisse payees</span><strong>{{ formatMoney(chargesCaissePayees(data)) }}</strong></div>
            <div><span>Cout achat carnets inclus</span><strong>{{ formatMoney(coutCarnets(data)) }}</strong></div>
            <div><span>Charges totales connues</span><strong>{{ formatMoney(data.totalCharges || 0) }}</strong></div>
            <div class="formula"><span>Formule</span><strong>Resultat net estime = Revenus reels - Charges connues</strong></div>
            <div class="net"><span>Resultat net estime</span><strong>{{ formatMoney(data.beneficeNetEstime || 0) }}</strong></div>
            <div><span>Salaires restant a payer</span><strong>{{ formatMoney(data.masseSalariale?.salairesRestantAPayer || 0) }}</strong></div>
            <div class="net"><span>Resultat apres salaires restant a payer</span><strong>{{ formatMoney(data.resultatPrevisionnelApresSalairesAPayer || 0) }}</strong></div>
            <div><span>Transport terrain restant a payer</span><strong>{{ formatMoney(data.transportFixePrevu?.totalTransportRestant || 0) }}</strong></div>
            <div class="net"><span>Resultat previsionnel charges fixes</span><strong>{{ formatMoney(data.resultatPrevisionnelApresChargesFixes || 0) }}</strong></div>
          </div>
          <div class="category-list compact">
            <div class="category-row" *ngFor="let item of data.chargesParCategorie || []">
              <span>{{ item.categorie }}</span>
              <strong>{{ formatMoney(item.montant) }}</strong>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Carnets</p>
              <h2>Marge carnet</h2>
            </div>
            <span>{{ data.nombreCarnetsVendus || 0 }} carnet(s)</span>
          </div>
          <div class="result-list">
            <div><span>Ventes brutes</span><strong>{{ formatMoney(data.carnetMarge?.montantVentesCarnets || data.montantVentesCarnets || 0) }}</strong></div>
            <div><span>Cout achat unitaire</span><strong>{{ formatMoney(data.carnetMarge?.coutAchatUnitaireCarnet || 0) }}</strong></div>
            <div><span>Cout achat carnets</span><strong>{{ formatMoney(coutCarnets(data)) }}</strong></div>
            <div class="formula"><span>Traitement resultat</span><strong>Inclus dans les charges connues</strong></div>
            <div class="net"><span>Marge carnets</span><strong>{{ formatMoney(data.carnetMarge?.margeCarnets || data.margeCarnets || 0) }}</strong></div>
          </div>
          <p class="agence-hint" *ngIf="data.messageMargeCarnets">{{ data.messageMargeCarnets }}</p>
        </article>
      </section>

      <section class="panel position-credit-panel" *ngIf="activeTab === 'paie' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Charges fixes prevues</p>
            <h2>Transport terrain par site</h2>
          </div>
          <span>{{ data.transportFixePrevu?.periodeCharge || '-' }}</span>
        </div>

        <div class="insight-box">
          <strong>Regle interne configurable</strong>
          <p>Le transport terrain est calculé par jour, par Agent Terrain et par site. Calcul basé sur les jours calendaires de la période.</p>
        </div>

        <div class="position-grid funding-grid">
          <article class="position-card highlight">
            <span>Transport journalier total prévu</span>
            <strong>{{ formatMoney(data.transportFixePrevu?.totalTransportPrevu || 0) }}</strong>
            <small>Montant journalier × agents × jours</small>
          </article>
          <article class="position-card income">
            <span>Transport deja paye</span>
            <strong>{{ formatMoney(data.transportFixePrevu?.totalTransportPaye || 0) }}</strong>
          </article>
          <article class="position-card">
            <span>Transport restant a payer</span>
            <strong>{{ formatMoney(data.transportFixePrevu?.totalTransportRestant || 0) }}</strong>
          </article>
          <article class="position-card count">
            <span>Agents Terrain actifs</span>
            <strong>{{ data.transportFixePrevu?.nombreAgentsTerrain || 0 }}</strong>
          </article>
          <article class="position-card count">
            <span>Nombre de jours période</span>
            <strong>{{ data.transportFixePrevu?.nombreJoursPeriode || 0 }}</strong>
          </article>
          <article class="position-card count">
            <span>Sites configures</span>
            <strong>{{ data.transportFixePrevu?.nombreSitesConfigures || 0 }}</strong>
          </article>
          <article class="position-card count">
            <span>Sites sans montant</span>
            <strong>{{ data.transportFixePrevu?.nombreSitesSansMontant || 0 }}</strong>
          </article>
        </div>

        <div class="result-list">
          <div class="formula"><span>Formule</span><strong>Transport prévu site = montant journalier par agent × nombre agents × nombre jours</strong></div>
        </div>
        <p class="agence-hint" *ngIf="data.transportFixePrevu?.commentaireCalcul as commentaireTransport">{{ commentaireTransport }}</p>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Site</th>
                <th>Montant journalier / Agent Terrain</th>
                <th>Agents actifs</th>
                <th>Jours période</th>
                <th>Transport prévu site</th>
                <th>Transport déjà payé</th>
                <th>Transport restant à payer</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let site of data.transportFixePrevu?.detailsParSite || []">
                <td>{{ site.siteNom || '-' }}</td>
                <td class="amount strong">{{ formatMoney(site.montantJournalierParAgent) }}</td>
                <td class="amount">{{ site.nombreAgentsTerrainActifs }}</td>
                <td class="amount">{{ site.nombreJoursPeriode }}</td>
                <td class="amount strong">{{ formatMoney(site.transportPrevuSite) }}</td>
                <td class="amount">{{ formatMoney(site.transportPayeSite) }}</td>
                <td class="amount strong">{{ formatMoney(site.transportRestantSite) }}</td>
              </tr>
              <tr *ngIf="(data.transportFixePrevu?.detailsParSite || []).length === 0">
                <td colspan="7" class="empty-state">Aucun site avec Agent Terrain actif dans le périmètre sélectionné.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Matricule</th>
                <th>Agent Terrain</th>
                <th>Site</th>
                <th>Montant journalier</th>
                <th>Jours période</th>
                <th>Transport prévu</th>
                <th>Deja paye</th>
                <th>Reste a payer</th>
                <th>Statut</th>
                <th>References</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let agent of data.transportFixePrevu?.details || []">
                <td>{{ agent.matricule || '-' }}</td>
                <td>{{ agent.nomComplet || '-' }}</td>
                <td>{{ agent.siteNom || '-' }}</td>
                <td class="amount">{{ formatMoney(agent.montantJournalierParAgent || 0) }}</td>
                <td class="amount">{{ agent.nombreJoursPeriode || 0 }}</td>
                <td class="amount strong">{{ formatMoney(agent.montantPrevu) }}</td>
                <td class="amount">{{ formatMoney(agent.montantPaye) }}</td>
                <td class="amount strong">{{ formatMoney(agent.resteAPayer) }}</td>
                <td><span class="pill">{{ statutTransportLabel(agent.statut) }}</span></td>
                <td class="observation-cell">{{ agent.referencesPaiement || '-' }}</td>
              </tr>
              <tr *ngIf="(data.transportFixePrevu?.details || []).length === 0">
                <td colspan="10" class="empty-state">Aucun Agent Terrain actif dans le perimetre selectionne.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel position-credit-panel" *ngIf="activeTab === 'paie' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Charges fixes prevues</p>
            <h2>Masse salariale mensuelle</h2>
          </div>
          <span>{{ data.masseSalariale?.periodePaie || '-' }}</span>
        </div>

        <div class="insight-box">
          <strong>Les salaires non payes ne sont pas encore des charges realisees</strong>
          <p>Ils restent separes du benefice net realise, mais doivent etre reserves avant tout remboursement d'apport proprietaire.</p>
        </div>

        <div class="position-grid funding-grid">
          <article class="position-card highlight">
            <span>Masse salariale prevue</span>
            <strong>{{ formatMoney(data.masseSalariale?.masseSalarialeMensuellePrevue || 0) }}</strong>
          </article>
          <article class="position-card income">
            <span>Salaires deja payes</span>
            <strong>{{ formatMoney(data.masseSalariale?.salairesPayes || 0) }}</strong>
          </article>
          <article class="position-card">
            <span>Salaires restant a payer</span>
            <strong>{{ formatMoney(data.masseSalariale?.salairesRestantAPayer || 0) }}</strong>
          </article>
          <article class="position-card count">
            <span>Employes actifs</span>
            <strong>{{ data.masseSalariale?.nombreEmployesActifs || 0 }}</strong>
          </article>
          <article class="position-card highlight">
            <span>Resultat apres salaires restant a payer</span>
            <strong>{{ formatMoney(data.resultatPrevisionnelApresSalairesAPayer || 0) }}</strong>
          </article>
        </div>

        <div class="error-banner" *ngIf="data.masseSalariale?.alerte as alertePaie">{{ alertePaie }}</div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Matricule</th>
                <th>Employe</th>
                <th>Poste</th>
                <th>Salaire base</th>
                <th>Épargne validée</th>
                <th>Prime épargne 1%</th>
                <th>Remboursements validés</th>
                <th>Prime remboursement 2%</th>
                <th>Carnets</th>
                <th>Bonus carnets</th>
                <th>Total primes terrain</th>
                <th>Primes/bonus autres</th>
                <th>Remuneration attendue</th>
                <th>Partiels</th>
                <th>Avances</th>
                <th>Retenues</th>
                <th>Deja paye</th>
                <th>Ecart</th>
                <th>Reste a payer</th>
                <th>Statut paie</th>
                <th>Motif</th>
                <th>Reference paiement</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let employe of data.masseSalariale?.detailsEmployes || []">
                <td>{{ employe.matricule || '-' }}</td>
                <td>{{ employe.nomComplet || '-' }}</td>
                <td>{{ employe.poste || '-' }}</td>
                <td class="amount strong">{{ formatMoney(employe.salaireBase || employe.salairePrevu) }}</td>
                <td class="amount">{{ formatMoney(employe.totalEpargneCollecteeValidee || 0) }}</td>
                <td class="amount">{{ formatMoney(employe.primeEpargne || 0) }}</td>
                <td class="amount">{{ formatMoney(employe.totalRemboursementCollecteValide || 0) }}</td>
                <td class="amount">{{ formatMoney(employe.primeRemboursement || 0) }}</td>
                <td class="amount">{{ employe.nombreCarnetsVendus || 0 }}</td>
                <td class="amount">{{ formatMoney(employe.bonusCarnets || 0) }}</td>
                <td class="amount strong">{{ formatMoney(employe.totalPrimesAgentTerrain || 0) }}</td>
                <td class="amount">{{ formatMoney(otherPrimesBonus(employe)) }}</td>
                <td class="amount strong">{{ formatMoney(employe.remunerationAttendueTotale || employe.salairePrevu) }}</td>
                <td class="amount">{{ formatMoney(employe.salairesPartielsPayes) }}</td>
                <td class="amount">{{ formatMoney(employe.avancesPayees) }}</td>
                <td class="amount">{{ formatMoney(employe.retenues) }}</td>
                <td class="amount">{{ formatMoney(employe.montantPaye) }}</td>
                <td class="amount">{{ formatMoney(employe.ecartRemuneration || 0) }}</td>
                <td class="amount strong">{{ formatMoney(employe.resteAPayer) }}</td>
                <td><span class="pill">{{ statutPaieLabel(employe.statutPaie) }}</span></td>
                <td class="observation-cell">{{ employe.motifRemuneration || '-' }}</td>
                <td class="observation-cell">{{ employe.referenceDepenseCaisse || '-' }}</td>
              </tr>
              <tr *ngIf="(data.masseSalariale?.detailsEmployes || []).length === 0">
                <td colspan="22" class="empty-state">Aucun employe actif dans le perimetre selectionne.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel position-credit-panel" *ngIf="activeTab === 'credit' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Analyse credit</p>
            <h2>Position credit / capital immobilise</h2>
          </div>
          <span>{{ data.positionCredit?.capitalRestantEstime ? 'Estimation periode' : 'Encours principal' }}</span>
        </div>

        <div class="insight-box">
          <strong>Resultat net ≠ tresorerie disponible</strong>
          <p>Le resultat net estime la rentabilite de la periode. Le capital restant dehors represente l'argent prete aux membres qui n'est pas encore revenu. Ce montant n'est pas une charge, mais il reduit la tresorerie disponible tant qu'il n'est pas rembourse.</p>
        </div>

        <div class="position-grid">
          <article class="position-card">
            <span>Capital decaisse</span>
            <strong>{{ formatMoney(data.positionCredit?.capitalDecaisse || 0) }}</strong>
          </article>
          <article class="position-card">
            <span>Principal recupere</span>
            <strong>{{ formatMoney(data.positionCredit?.principalRecupere || 0) }}</strong>
          </article>
          <article class="position-card highlight">
            <span>Capital restant dehors</span>
            <strong>{{ formatMoney(data.positionCredit?.capitalRestantDehors || 0) }}</strong>
            <small *ngIf="data.positionCredit?.capitalRestantEstime">Estime : capital decaisse - principal recupere</small>
          </article>
          <article class="position-card income">
            <span>Interets encaisses</span>
            <strong>{{ formatMoney(data.positionCredit?.interetsEncaisses || 0) }}</strong>
          </article>
          <article class="position-card income">
            <span>Penalites encaissees</span>
            <strong>{{ formatMoney(data.positionCredit?.penalitesEncaisses || 0) }}</strong>
          </article>
          <article class="position-card count">
            <span>Credits actifs</span>
            <strong>{{ data.positionCredit?.nombreCreditsActifs || 0 }}</strong>
          </article>
          <article class="position-card count">
            <span>Credits rembourses / clotures</span>
            <strong>{{ data.positionCredit?.nombreCreditsRembourses || 0 }}</strong>
          </article>
        </div>

        <p class="agence-hint" *ngIf="data.positionCredit?.commentairePedagogique as commentaire">{{ commentaire }}</p>
      </section>

      <section class="panel position-credit-panel" *ngIf="activeTab === 'financement' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Financement</p>
            <h2>Apports / financements injectes</h2>
          </div>
          <span>{{ formatMoney(data.apportsFinancements?.totalApprovisionnements || 0) }}</span>
        </div>

        <div class="insight-box">
          <strong>Les approvisionnements ne sont pas des revenus</strong>
          <p>Un apport proprietaire augmente la caisse mais n'est pas un revenu. Il pourra etre recupere plus tard uniquement si l'activite genere des benefices distribuables ou si la tresorerie le permet.</p>
        </div>

        <div class="position-grid funding-grid">
          <article class="position-card">
            <span>Total approvisionnements</span>
            <strong>{{ formatMoney(data.apportsFinancements?.totalApprovisionnements || 0) }}</strong>
          </article>
          <article class="position-card highlight">
            <span>Apports proprietaire</span>
            <strong>{{ formatMoney(data.apportsFinancements?.apportsProprietaire || 0) }}</strong>
          </article>
          <article class="position-card">
            <span>Transferts internes</span>
            <strong>{{ formatMoney(data.apportsFinancements?.transfertsInternes || 0) }}</strong>
          </article>
          <article class="position-card">
            <span>Prets recus</span>
            <strong>{{ formatMoney(data.apportsFinancements?.pretsRecus || 0) }}</strong>
          </article>
          <article class="position-card">
            <span>Remboursements d'avance</span>
            <strong>{{ formatMoney(data.apportsFinancements?.remboursementsAvance || 0) }}</strong>
          </article>
          <article class="position-card">
            <span>Autres financements</span>
            <strong>{{ formatMoney(data.apportsFinancements?.autresFinancements || 0) }}</strong>
          </article>
          <article class="position-card count">
            <span>Approvisionnements non qualifies</span>
            <strong>{{ formatMoney(data.apportsFinancements?.approvisionnementsNonQualifies || 0) }}</strong>
          </article>
          <article class="position-card highlight">
            <span>Capital injecte a recuperer</span>
            <strong>{{ formatMoney(data.apportsFinancements?.capitalInjecteARecuperer || 0) }}</strong>
            <small *ngIf="data.apportsFinancements?.capitalInjecteIndicatif">Montant indicatif</small>
          </article>
          <article class="position-card income">
            <span>Remboursements d'apport payes</span>
            <strong>{{ formatMoney(data.apportsFinancements?.remboursementsApportPayes || 0) }}</strong>
          </article>
          <article class="position-card highlight">
            <span>Capital restant a recuperer</span>
            <strong>{{ formatMoney(data.apportsFinancements?.capitalInjecteRestantARecuperer || 0) }}</strong>
          </article>
        </div>

        <p class="agence-hint" *ngIf="data.apportsFinancements?.commentairePedagogique as commentaireFinancement">{{ commentaireFinancement }}</p>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Antenne</th>
                <th>Categorie</th>
                <th>Nature financement</th>
                <th>Reference</th>
                <th>Utilisateur</th>
                <th>Montant</th>
                <th>Observation</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let detail of data.apportsFinancementsDetails || []">
                <td>{{ formatDate(detail.date) }}</td>
                <td>{{ detail.antenne || 'Non rattache' }}</td>
                <td>{{ detail.categorie || 'APPROVISIONNEMENT' }}</td>
                <td>{{ detail.natureFinancement || 'Approvisionnement non qualifie' }}</td>
                <td>{{ detail.reference || '-' }}</td>
                <td>{{ detail.utilisateur || '-' }}</td>
                <td class="amount strong">{{ formatMoney(detail.montant) }}</td>
                <td class="observation-cell">{{ detail.observation || '-' }}</td>
                <td>
                  <button type="button" class="inline-action" *ngIf="canQualifyFinancement(detail)" (click)="openQualification(detail)">Qualifier</button>
                </td>
              </tr>
              <tr *ngIf="(data.apportsFinancementsDetails || []).length === 0">
                <td colspan="9" class="empty-state">Aucun approvisionnement sur cette periode.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="accounting-grid" *ngIf="activeTab === 'financement' && report as data">
        <article class="panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Proprietaire</p>
              <h2>Analyse proprietaire</h2>
            </div>
            <span>{{ formatMoney(data.capaciteRetraitProprietaire?.montantRecuperableConseille || 0) }}</span>
          </div>
          <div class="result-list">
            <div><span>Capital injecte cumule</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.capitalInjecteCumule || 0) }}</strong></div>
            <div><span>Remboursements deja payes</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.remboursementsApportPayes || 0) }}</strong></div>
            <div><span>Capital proprietaire non encore rembourse</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.capitalInjecteRestantARecuperer || 0) }}</strong></div>
            <div><span>Solde caisse theorique actif</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.soldeCaisseTheoriqueActif || 0) }}</strong></div>
            <div><span>Fonds membres proteges</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.fondsMembresAProteger || 0) }}</strong></div>
            <div><span>Tresorerie apres protection des membres</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.tresorerieApresProtectionMembres || 0) }}</strong></div>
            <div><span>Engagements court terme</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.engagementsCourtTerme || 0) }}</strong></div>
            <div><span>Dont salaires restant a payer</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.salairesRestantAPayer || 0) }}</strong></div>
            <div><span>Dont transport terrain restant à payer</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.transportRestantAPayer || 0) }}</strong></div>
            <div><span>Fonds minimum securite</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.fondsMinimumSecurite || 0) }}</strong></div>
            <div><span>Marge de prudence</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.margePrudence || 0) }}</strong></div>
            <div><span>Estimation prudente recuperable</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.tresorerieRecuperablePrudente || 0) }}</strong></div>
            <div class="net"><span>Montant indicatif a valider</span><strong>{{ formatMoney(data.capaciteRetraitProprietaire?.montantRecuperableConseille || 0) }}</strong></div>
          </div>
          <div class="error-banner" *ngIf="data.capaciteRetraitProprietaire?.alerteTresorerie as alerteTresorerie">{{ alerteTresorerie }}</div>
          <div class="error-banner" *ngIf="data.capaciteRetraitProprietaire?.alerte as alerteRetrait">{{ alerteRetrait }}</div>
          <div class="agence-hint" *ngIf="data.capaciteRetraitProprietaire?.alerteCredit as alerteCredit">{{ alerteCredit }}</div>
          <p class="agence-hint" *ngIf="data.capaciteRetraitProprietaire?.commentairePedagogique as commentaireRetrait">{{ commentaireRetrait }}</p>
        </article>
      </section>

      <section class="accounting-grid" *ngIf="activeTab === 'credit' && report as data">
        <article class="panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Tresorerie</p>
              <h2>Disponible / engagements</h2>
            </div>
            <span>{{ formatMoney(data.tresorerieDisponible?.tresorerieDisponibleApresEngagements || 0) }}</span>
          </div>
          <div class="result-list">
            <div><span>Solde caisse theorique actif</span><strong>{{ formatMoney(data.tresorerieDisponible?.soldeCaisseTheoriqueActif || 0) }}</strong></div>
            <div><span>Fonds membres proteges</span><strong>{{ formatMoney(data.tresorerieDisponible?.fondsMembresAProteger || 0) }}</strong></div>
            <div><span>Tresorerie apres protection membres</span><strong>{{ formatMoney(data.tresorerieDisponible?.tresorerieApresProtectionMembres || 0) }}</strong></div>
            <div><span>Retraits epargne valides non payes</span><strong>{{ formatMoney(data.tresorerieDisponible?.retraitsEpargneValidesNonPayes || 0) }}</strong></div>
            <div><span>Credits approuves non decaisses</span><strong>{{ formatMoney(data.tresorerieDisponible?.creditsApprouvesNonDecaisses || 0) }}</strong></div>
            <div><span>Depenses validees non payees</span><strong>{{ formatMoney(data.tresorerieDisponible?.depensesValideesNonPayees || 0) }}</strong></div>
            <div><span>Salaires restant a payer</span><strong>{{ formatMoney(data.tresorerieDisponible?.salairesRestantAPayer || 0) }}</strong></div>
            <div class="with-help">
              <span>Transport terrain restant à payer</span>
              <strong>{{ formatMoney(data.tresorerieDisponible?.transportRestantAPayer || 0) }}</strong>
              <small>Transport prévu des Agents Terrain selon montant journalier par site, diminué des paiements déjà effectués.</small>
            </div>
            <div><span>Fonds minimum securite</span><strong>{{ formatMoney(data.tresorerieDisponible?.fondsMinimumSecurite || 0) }}</strong></div>
            <div><span>Marge de prudence</span><strong>{{ formatMoney(data.tresorerieDisponible?.margePrudence || 0) }}</strong></div>
            <div><span>Total engagements court terme</span><strong>{{ formatMoney(data.tresorerieDisponible?.totalEngagementsCourtTerme || 0) }}</strong></div>
            <div class="net"><span>Estimation prudente recuperable</span><strong>{{ formatMoney(data.tresorerieDisponible?.tresorerieRecuperablePrudente || 0) }}</strong></div>
          </div>
          <p class="agence-hint" *ngIf="data.tresorerieDisponible?.commentairePedagogique as commentaireTresorerie">{{ commentaireTresorerie }}</p>
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Membres</p>
              <h2>Fonds membres proteges</h2>
            </div>
            <span>{{ formatMoney(data.fondsMembresProteges?.totalFondsMembres || 0) }}</span>
          </div>
          <div class="result-list">
            <div><span>Epargne disponible membres</span><strong>{{ formatMoney(data.fondsMembresProteges?.epargneDisponibleMembres || 0) }}</strong></div>
            <div><span>Epargne bloquee / garanties</span><strong>{{ formatMoney(data.fondsMembresProteges?.epargneBloqueeGaranties || 0) }}</strong></div>
            <div><span>Retraits valides non deja inclus</span><strong>{{ formatMoney(data.fondsMembresProteges?.retraitsEpargneValidesNonPayesNonInclus || 0) }}</strong></div>
            <div class="net"><span>Total fonds membres</span><strong>{{ formatMoney(data.fondsMembresProteges?.totalFondsMembres || 0) }}</strong></div>
          </div>
          <p class="agence-hint" *ngIf="data.fondsMembresProteges?.commentairePedagogique as commentaireFonds">{{ commentaireFonds }}</p>
        </article>
      </section>

      <div class="modal-backdrop" *ngIf="qualificationTarget">
        <form class="modal-card" (ngSubmit)="submitQualification()">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Correction audit</p>
              <h2>Qualifier l'approvisionnement</h2>
            </div>
            <button type="button" class="icon-action" (click)="cancelQualification()">×</button>
          </div>

          <div class="result-list">
            <div><span>Reference</span><strong>{{ qualificationTarget.reference || '-' }}</strong></div>
            <div><span>Montant</span><strong>{{ formatMoney(qualificationTarget.montant) }}</strong></div>
          </div>

          <label>
            <span>Nature du financement</span>
            <select name="qualificationNature" [(ngModel)]="qualificationNature" required>
              <option [ngValue]="null">Selectionner</option>
              <option *ngFor="let option of qualificationOptions" [ngValue]="option.value">{{ option.label }}</option>
            </select>
          </label>

          <label>
            <span>Commentaire de correction</span>
            <textarea name="qualificationCommentaire" [(ngModel)]="qualificationCommentaire" rows="3" required placeholder="Requalification historique : apport propriétaire"></textarea>
          </label>

          <div class="error-banner" *ngIf="qualificationError">{{ qualificationError }}</div>

          <div class="modal-actions">
            <button type="button" class="secondary-action" (click)="cancelQualification()">Annuler</button>
            <button type="submit" class="primary-action" [disabled]="qualificationSaving">{{ qualificationSaving ? 'Enregistrement...' : 'Valider' }}</button>
          </div>
        </form>
      </div>

      <section class="panel" *ngIf="activeTab === 'audit' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Flux exclus</p>
            <h2>Flux financiers exclus du resultat par antenne</h2>
          </div>
          <span>{{ (data.mouvementsNonRevenusParAntenne || []).length }} antenne(s)</span>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Antenne</th>
                <th>Epargne collectee</th>
                <th>Principal rembourse</th>
                <th>Garanties</th>
                <th>Approvisionnements</th>
                <th>Retraits epargne</th>
                <th>Decaissements credit</th>
                <th>Autres</th>
                <th>Total flux exclus</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let row of data.mouvementsNonRevenusParAntenne || []">
                <td>{{ row.antenneNom || 'Non rattache' }}</td>
                <td class="amount">{{ formatMoney(row.epargneCollectee) }}</td>
                <td class="amount">{{ formatMoney(row.principalCreditRembourse) }}</td>
                <td class="amount">{{ formatMoney(row.garantiesDepotGarantie) }}</td>
                <td class="amount">{{ formatMoney(row.approvisionnementsCaisse) }}</td>
                <td class="amount">{{ formatMoney(row.retraitsEpargne) }}</td>
                <td class="amount">{{ formatMoney(row.decaissementsCredit) }}</td>
                <td class="amount">{{ formatMoney(row.autresMouvementsNonRevenus) }}</td>
                <td class="amount strong">{{ formatMoney(row.totalHorsRevenus) }}</td>
              </tr>
              <tr *ngIf="(data.mouvementsNonRevenusParAntenne || []).length === 0">
                <td colspan="9" class="empty-state">Aucun flux financier exclu sur cette periode.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel" *ngIf="activeTab === 'audit' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Charges</p>
            <h2>Detail des charges</h2>
          </div>
          <span>{{ (data.chargeDetails || []).length }} charge(s)</span>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Antenne</th>
                <th>Categorie charge</th>
                <th>Reference</th>
                <th>Beneficiaire / utilisateur</th>
                <th>Detail paie</th>
                <th>Montant</th>
                <th>Statut</th>
                <th>Observation</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let charge of data.chargeDetails || []">
                <td>{{ formatDate(charge.date) }}</td>
                <td>{{ charge.antenne || 'Non rattache' }}</td>
                <td>{{ charge.categorie || '-' }}</td>
                <td>{{ charge.reference || '-' }}</td>
                <td>{{ charge.beneficiaire || '-' }}</td>
                <td class="observation-cell">
                  <ng-container *ngIf="charge.periodePaie; else noPayrollDetail">
                    <div><strong>{{ charge.employePoste || '-' }}</strong> · {{ charge.periodePaie }}</div>
                    <div>{{ paiementPaieSummary(charge) }}</div>
                    <div>Base: {{ formatMoney(charge.salaireBase || 0) }}</div>
                    <div *ngIf="charge.modeCalculPaie === 'AGENT_TERRAIN_AUTOMATIQUE'">
                      Épargne: {{ formatMoney(charge.epargneCollecteeReference || 0) }} · Prime: {{ formatMoney(charge.primeMobilisationEpargne || 0) }}<br>
                      Remb.: {{ formatMoney(charge.remboursementCollecteReference || 0) }} · Prime: {{ formatMoney(charge.primeMobilisationRemboursement || 0) }}<br>
                      Carnets: {{ charge.nombreCarnetsVendus || 0 }} · Bonus: {{ formatMoney(charge.bonusCarnets || 0) }}
                    </div>
                    <div *ngIf="charge.modeCalculPaie === 'PERSONNEL_BUREAU_MANUEL'">
                      Prime motivation: {{ formatMoney(charge.primeMotivationManuelle || 0) }}
                    </div>
                    <div>Mode: {{ charge.modeCalculPaie || '-' }}</div>
                  </ng-container>
                  <ng-template #noPayrollDetail>
                    <ng-container *ngIf="charge.periodeCharge || charge.siteChargeNom; else noChargeFixeDetail">
                      <div><strong>Transport site</strong> · {{ charge.periodeCharge || '-' }}</div>
                      <div>Site: {{ charge.siteChargeNom || '-' }}</div>
                      <div>Prévu: {{ formatMoney(charge.montantChargeFixeReference || 0) }} · Écart: {{ formatMoney(charge.montantEcartChargeFixe || 0) }}</div>
                      <div>{{ charge.commentaireRapprochement || '-' }}</div>
                    </ng-container>
                    <ng-template #noChargeFixeDetail>-</ng-template>
                  </ng-template>
                </td>
                <td class="amount strong">{{ formatMoney(charge.montant) }}</td>
                <td>{{ charge.statut || '-' }}</td>
                <td class="observation-cell">{{ charge.observation || '-' }}</td>
                <td>
                  <button type="button" class="inline-action" *ngIf="canRattacherPaie(charge)" (click)="openRattachementPaie(charge)">Rattacher paie</button>
                  <button type="button" class="inline-action" *ngIf="canRattacherTransport(charge)" (click)="openRattachementTransport(charge)">Rattacher transport</button>
                </td>
              </tr>
              <tr *ngIf="(data.chargeDetails || []).length === 0">
                <td colspan="10" class="empty-state">Aucune charge payee sur cette periode.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <div class="overlay-modal" *ngIf="rattachementTarget">
        <form class="modal-paie" (ngSubmit)="submitRattachementPaie()">
          <div class="panel-heading modal-paie-header">
            <div>
              <p class="eyebrow">Rattachement paie</p>
              <h2>Qualifier la dépense salaire</h2>
              <small class="modal-version">Modal paie compact V3</small>
            </div>
            <button type="button" class="icon-action" (click)="cancelRattachementPaie()">×</button>
          </div>

          <div class="modal-paie-body">
            <div class="paie-summary-grid">
              <div><span>Référence</span><strong>{{ rattachementTarget.reference || ('DEPENSE-' + rattachementTarget.depenseId) }}</strong></div>
              <div><span>Montant payé</span><strong>{{ formatMoney(rattachementTarget.montant) }}</strong></div>
              <ng-container *ngIf="rattachementPreview; else noRattachementPreview">
                <div><span>Salaire prévu</span><strong>{{ formatMoney(rattachementPreview.totalAPayer) }}</strong></div>
                <div><span>Différence</span><strong>{{ formatMoney(rattachementDifference()) }}</strong></div>
                <div><span>Reste estimé</span><strong>{{ formatMoney(rattachementReste()) }}</strong></div>
                <div><span>Employé</span><strong>{{ rattachementPreview.nomComplet || '-' }}</strong></div>
              </ng-container>
              <ng-template #noRattachementPreview>
                <div class="formula paie-summary-note"><span>Salaire prévu</span><strong>Sélectionnez un employé pour calculer le salaire prévu.</strong></div>
              </ng-template>
            </div>

            <div class="paie-fields-grid">
              <label>
                <span>Employé</span>
                <select name="rattachementEmploye" [(ngModel)]="rattachementForm.employeId" (ngModelChange)="refreshRattachementPreview()" required>
                  <option [ngValue]="null">Sélectionner</option>
                  <option *ngFor="let employe of rattachementBeneficiaires" [ngValue]="employe.employeId || employe.id">{{ employe.affichage }}</option>
                </select>
              </label>

              <label>
                <span>Période paie</span>
                <input name="rattachementPeriode" type="month" [(ngModel)]="rattachementForm.periodePaie" (ngModelChange)="refreshRattachementPreview()" required>
              </label>

              <label>
                <span>Nature du paiement</span>
                <select name="rattachementType" [(ngModel)]="rattachementForm.typePaiementPersonnel" required>
                  <option *ngFor="let option of typePaiementOptions" [ngValue]="option.value">{{ option.label }}</option>
                </select>
              </label>
            </div>

            <div class="agence-hint" *ngIf="rattachementPreview && rattachementDifference() < 0">
              Le montant payé est inférieur à la rémunération prévue. Précisez s’il s’agit d’un paiement partiel, d’une avance ou d’une retenue.
            </div>

            <label *ngIf="showMotifGeneralRattachement()">
              <span>{{ rattachementForm.typePaiementPersonnel === 'AVANCE_SALAIRE' ? 'Motif avance' : 'Motif général' }}</span>
              <textarea name="rattachementMotif" [(ngModel)]="rattachementForm.motif" rows="2"></textarea>
            </label>

            <label *ngIf="rattachementForm.typePaiementPersonnel === 'SALAIRE_PARTIEL'">
              <span>Motif paiement partiel</span>
              <textarea name="rattachementMotifPartiel" [(ngModel)]="rattachementForm.motifPaiementPartiel" rows="2" required></textarea>
            </label>

            <label *ngIf="rattachementForm.typePaiementPersonnel === 'RETENUE_SALAIRE'">
              <span>Motif retenue</span>
              <textarea name="rattachementMotifRetenue" [(ngModel)]="rattachementForm.motifRetenue" rows="2" required></textarea>
            </label>

            <label class="checkbox-line" *ngIf="rattachementForm.typePaiementPersonnel === 'RETENUE_SALAIRE'">
              <input type="checkbox" name="rattachementRetenueDefinitive" [(ngModel)]="rattachementForm.retenueDefinitive">
              <span>Retenue définitive</span>
            </label>

            <label>
              <span>Commentaire correction</span>
              <textarea name="rattachementCommentaire" [(ngModel)]="rattachementForm.commentaireCorrection" rows="2" required></textarea>
            </label>

            <div class="error-banner" *ngIf="rattachementError">{{ rattachementError }}</div>
          </div>

          <div class="modal-actions modal-paie-footer">
            <button type="button" class="secondary-action" (click)="cancelRattachementPaie()">Annuler</button>
            <button type="submit" class="primary-action" [disabled]="rattachementSaving || !rattachementForm.employeId">{{ rattachementSaving ? 'Enregistrement...' : 'Valider le rattachement' }}</button>
          </div>
        </form>
      </div>

      <div class="overlay-modal" *ngIf="transportTarget">
        <form class="modal-paie" (ngSubmit)="submitRattachementTransport()">
          <div class="panel-heading modal-paie-header">
            <div>
              <p class="eyebrow">Rattachement transport</p>
              <h2>Qualifier la dépense transport</h2>
            </div>
            <button type="button" class="icon-action" (click)="cancelRattachementTransport()">×</button>
          </div>

          <div class="modal-paie-body">
            <div class="paie-summary-grid">
              <div><span>Référence</span><strong>{{ transportTarget.reference || ('DEPENSE-' + transportTarget.depenseId) }}</strong></div>
              <div><span>Montant payé</span><strong>{{ formatMoney(transportTarget.montant) }}</strong></div>
              <div><span>Montant journalier</span><strong>{{ formatMoney(selectedTransportAgentDetail()?.montantJournalierParAgent || selectedTransportSiteDetail()?.montantJournalierParAgent || 0) }}</strong></div>
              <div><span>Jours période</span><strong>{{ report?.transportFixePrevu?.nombreJoursPeriode || nombreJoursRapport() }}</strong></div>
              <div><span>Montant prévu sur période</span><strong>{{ formatMoney(transportExpectedForModal()) }}</strong></div>
              <div><span>Différence</span><strong>{{ formatMoney(transportDifference()) }}</strong></div>
            </div>

            <div class="paie-fields-grid">
              <label>
                <span>Agent Terrain</span>
                <select name="transportEmploye" [(ngModel)]="transportForm.employeId" (ngModelChange)="syncTransportSiteFromAgent()" required>
                  <option [ngValue]="null">Sélectionner</option>
                  <option *ngFor="let employe of rattachementBeneficiaires" [ngValue]="employe.employeId || employe.id">{{ employe.affichage }}</option>
                </select>
              </label>

              <label>
                <span>Période charge</span>
                <input name="transportPeriode" type="month" [(ngModel)]="transportForm.periodeCharge" required>
              </label>

              <label>
                <span>Site</span>
                <select name="transportSite" [(ngModel)]="transportForm.siteId" required>
                  <option [ngValue]="null">Sélectionner</option>
                  <option *ngFor="let site of transportSiteOptions()" [ngValue]="site.id">{{ site.nom }}</option>
                </select>
              </label>
            </div>

            <label>
              <span>Commentaire correction</span>
              <textarea name="transportCommentaire" [(ngModel)]="transportForm.commentaireCorrection" rows="2" required></textarea>
            </label>

            <div class="error-banner" *ngIf="transportError">{{ transportError }}</div>
          </div>

          <div class="modal-actions modal-paie-footer">
            <button type="button" class="secondary-action" (click)="cancelRattachementTransport()">Annuler</button>
            <button type="submit" class="primary-action" [disabled]="transportSaving || !transportForm.employeId || !transportForm.siteId">{{ transportSaving ? 'Enregistrement...' : 'Valider le rattachement' }}</button>
          </div>
        </form>
      </div>

      <section class="panel" *ngIf="(activeTab === 'decision' || activeTab === 'paie' || activeTab === 'audit') && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Controle</p>
            <h2>{{ activeTab === 'decision' ? 'Alertes principales' : 'Controles de coherence' }}</h2>
          </div>
          <span>{{ visibleAlerts(data).length }} alerte(s)</span>
        </div>
        <div class="mc-alert-panel">
          <div class="mc-alert-row" *ngFor="let alert of visibleAlerts(data)" [ngClass]="alertPanelClass(alert.severite)">
            <div class="mc-alert-title">
              <strong>{{ alertSeverityLabel(alert.severite) }}</strong>
              <span>{{ alertTypeLabel(alert.type) }}</span>
            </div>
            <div class="mc-alert-message">
              <p>{{ alertTitle(alert.message) }}</p>
              <small>{{ alert.reference || '-' }} · {{ alert.antenne || 'Non rattache' }}</small>
            </div>
            <div class="mc-alert-action">
              <span>Action conseillee</span>
              <p>{{ alertActionHint(alert.severite) }}</p>
              <details>
                <summary>Voir detail</summary>
                <p>{{ alert.message }}</p>
              </details>
            </div>
          </div>
          <p class="empty-state" *ngIf="visibleAlerts(data).length === 0">Aucune alerte de coherence sur cette periode.</p>
        </div>
      </section>

      <section class="content-grid" *ngIf="activeTab === 'revenus' && report as data">
        <article class="panel summary-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Synthese</p>
              <h2>Revenus par antenne</h2>
            </div>
            <span>{{ data.parAntenne.length }} ligne(s)</span>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Antenne</th>
                  <th>Total</th>
                  <th>Analyse credit</th>
                  <th>Retrait epargne</th>
                  <th>Interets</th>
                  <th>Penalites</th>
                  <th>Carnets</th>
                  <th>Divers</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let row of data.parAntenne">
                  <td>{{ row.antenneNom || 'Non rattache' }}</td>
                  <td class="amount strong">{{ formatMoney(row.totalRevenus) }}</td>
                  <td class="amount">{{ formatMoney(row.fraisAnalyseCredit) }}</td>
                  <td class="amount">{{ formatMoney(row.fraisRetraitEpargne) }}</td>
                  <td class="amount">{{ formatMoney(row.interetsCredit) }}</td>
                  <td class="amount">{{ formatMoney(row.penalitesCredit) }}</td>
                  <td class="amount">{{ formatMoney(row.carnetsVendus) }}</td>
                  <td class="amount">{{ formatMoney(row.revenusDivers) }}</td>
                </tr>
                <tr *ngIf="data.parAntenne.length === 0">
                  <td colspan="8" class="empty-state">Aucun revenu trouve pour cette periode.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel categories-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Composition</p>
              <h2>Par categorie</h2>
            </div>
          </div>
          <div class="category-list">
            <div class="category-row" *ngFor="let item of data.parCategorie">
              <span>
                {{ categoryLabel(item.categorie) }}
                <small *ngIf="item.sousCategorie">{{ item.sousCategorie }}</small>
              </span>
              <strong>{{ formatMoney(item.montant) }}</strong>
            </div>
            <p class="empty-state" *ngIf="data.parCategorie.length === 0">Aucune categorie sur la periode.</p>
          </div>
        </article>
      </section>

      <section class="panel details-panel" *ngIf="activeTab === 'audit' && report as data">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Audit</p>
            <h2>Detail des revenus</h2>
          </div>
          <span>{{ data.details.length }} operation(s)</span>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Antenne</th>
                <th>Categorie</th>
                <th>Nature / Sous-categorie</th>
                <th>Source</th>
                <th>Reference</th>
                <th>Membre</th>
                <th>Utilisateur</th>
                <th>Observation</th>
                <th>Montant</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let detail of visibleDetails(data.details)">
                <td>{{ formatDate(detail.date) }}</td>
                <td>{{ detail.antenne || 'Non rattache' }}</td>
                <td><span class="pill">{{ categoryLabel(detail.categorie) }}</span></td>
                <td>{{ natureLabel(detail) }}</td>
                <td>{{ sourceLabel(detail.source) }}</td>
                <td>{{ detail.reference || '-' }}</td>
                <td>{{ detail.membre || '-' }}</td>
                <td>{{ detail.utilisateur || '-' }}</td>
                <td class="observation-cell">{{ detail.observation || '-' }}</td>
                <td class="amount strong">{{ formatMoney(detail.montant) }}</td>
              </tr>
              <tr *ngIf="data.details.length === 0">
                <td colspan="10" class="empty-state">Aucune operation de revenu pour cette selection.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  `,
  styles: [`
    :host {
      display: block;
      background: #f4f6f4;
      color: #17231e;
      min-height: 100vh;
    }

    .revenus-page {
      padding: 28px;
      display: grid;
      gap: 22px;
    }

    .revenus-hero {
      display: flex;
      align-items: stretch;
      justify-content: space-between;
      gap: 22px;
      padding: 30px;
      border-radius: 8px;
      background: linear-gradient(135deg, #143d2f 0%, #1d5d46 54%, #d5a441 100%);
      color: #fff;
      box-shadow: 0 18px 42px rgba(20, 61, 47, 0.22);
    }

    .eyebrow {
      margin: 0 0 8px;
      font-size: 0.74rem;
      font-weight: 800;
      letter-spacing: 0;
      text-transform: uppercase;
      color: #c9972b;
    }

    .revenus-hero .eyebrow {
      color: #ffe0a1;
    }

    h1, h2 {
      margin: 0;
      letter-spacing: 0;
    }

    h1 {
      font-size: clamp(2rem, 4vw, 3.4rem);
      line-height: 1;
      font-weight: 900;
    }

    h2 {
      font-size: 1.1rem;
      font-weight: 850;
    }

    .subtitle {
      max-width: 760px;
      margin: 12px 0 0;
      color: rgba(255, 255, 255, 0.86);
      font-size: 1rem;
    }

    .hero-total {
      min-width: 260px;
      padding: 20px;
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.13);
      border: 1px solid rgba(255, 255, 255, 0.22);
      display: grid;
      align-content: center;
      gap: 8px;
    }

    .hero-total span, .hero-total small {
      color: rgba(255, 255, 255, 0.78);
    }

    .hero-total strong {
      font-size: 1.8rem;
      line-height: 1;
    }

    .filter-panel, .panel, .kpi-card {
      background: #fff;
      border: 1px solid #dde5df;
      border-radius: 8px;
      box-shadow: 0 10px 28px rgba(23, 35, 30, 0.06);
    }

    .filter-panel {
      padding: 18px;
      display: grid;
      gap: 16px;
    }

    .quick-periods {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .quick-periods button, .primary-action {
      border: 0;
      border-radius: 6px;
      font-weight: 800;
      cursor: pointer;
      transition: transform 120ms ease, background 120ms ease, color 120ms ease;
    }

    .quick-periods button {
      padding: 9px 14px;
      color: #33453d;
      background: #edf2ef;
    }

    .quick-periods button.active {
      color: #fff;
      background: #1d5d46;
    }

    .filters-grid {
      display: grid;
      grid-template-columns: repeat(6, minmax(130px, 1fr));
      gap: 12px;
      align-items: end;
    }

    label {
      display: grid;
      gap: 6px;
      font-size: 0.82rem;
      font-weight: 800;
      color: #4d5b55;
    }

    input, select {
      width: 100%;
      min-height: 42px;
      border: 1px solid #cfd9d3;
      border-radius: 6px;
      background: #fbfcfb;
      padding: 0 12px;
      color: #17231e;
      font: inherit;
      font-weight: 650;
    }

    .primary-action {
      min-height: 42px;
      padding: 0 18px;
      background: #c9972b;
      color: #17231e;
    }

    .primary-action:disabled {
      opacity: 0.65;
      cursor: wait;
    }

    .secondary-action,
    .inline-action,
    .icon-action {
      border: 0;
      border-radius: 6px;
      font-weight: 850;
      cursor: pointer;
    }

    .secondary-action {
      min-height: 42px;
      padding: 0 18px;
      background: #edf2ef;
      color: #33453d;
    }

    .inline-action {
      min-height: 32px;
      padding: 0 12px;
      background: #1d5d46;
      color: #fff;
      white-space: nowrap;
    }

    .icon-action {
      width: 34px;
      height: 34px;
      background: #edf2ef;
      color: #33453d;
      font-size: 1.2rem;
      line-height: 1;
    }

    .agence-hint, .error-banner {
      margin: 0;
      padding: 12px 14px;
      border-radius: 6px;
      font-weight: 700;
    }

    .agence-hint {
      background: #fff6df;
      color: #76560e;
    }

    .error-banner {
      background: #ffe8e4;
      color: #8b1e10;
      border: 1px solid #ffc5ba;
    }

    .decision-strip {
      display: grid;
      grid-template-columns: repeat(4, minmax(170px, 1fr));
      gap: 12px;
    }

    .kpi-card {
      min-height: 104px;
      padding: 18px;
      display: grid;
      align-content: space-between;
      gap: 14px;
    }

    .kpi-card span {
      color: #66746e;
      font-weight: 800;
      font-size: 0.82rem;
    }

    .kpi-card strong {
      color: #143d2f;
      font-size: 1.22rem;
      line-height: 1.1;
    }

    .kpi-card small {
      color: #6f7c76;
      font-size: 0.72rem;
      font-weight: 800;
      line-height: 1.25;
    }

    .kpi-card.total {
      background: #143d2f;
      border-color: #143d2f;
    }

    .kpi-card.total span, .kpi-card.total strong {
      color: #fff;
    }

    .kpi-card.result {
      background: #e8f5ed;
      border-color: #b9dcc8;
    }

    .kpi-card.positive {
      background: #e8f5ed;
      border-color: #b9dcc8;
    }

    .kpi-card.negative,
    .kpi-card.danger {
      background: #fff0ed;
      border-color: #f3b4a8;
    }

    .kpi-card.warning {
      background: #fff8ea;
      border-color: #f2d8a7;
    }

    .kpi-card.info {
      background: #eff6ff;
      border-color: #bfdbfe;
    }

    .kpi-card.muted {
      background: #f8faf8;
    }

    .mc-kpi-grid-decision {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 18px;
    }

    .mc-kpi-grid-decision .mc-kpi-card {
      display: flex;
      min-height: 150px;
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
      padding: 18px;
      border-radius: 12px;
      border: 1px solid #dde5df;
      background: #fff;
      box-shadow: 0 10px 28px rgba(23, 35, 30, 0.06);
    }

    .mc-kpi-badge {
      display: inline-flex;
      align-items: center;
      min-height: 24px;
      padding: 0 10px;
      border-radius: 999px;
      background: rgba(20, 61, 47, 0.1);
      color: #143d2f;
      font-size: 0.72rem;
      font-weight: 950;
      text-transform: uppercase;
    }

    .mc-kpi-label {
      color: #33453d;
      font-size: 0.95rem;
      font-weight: 900;
      line-height: 1.25;
    }

    .mc-kpi-value {
      display: block;
      color: #143d2f;
      font-size: clamp(1.45rem, 2vw, 2rem);
      font-weight: 950;
      line-height: 1.1;
      white-space: normal;
    }

    .mc-kpi-help {
      color: #66746e;
      font-size: 0.84rem;
      font-weight: 750;
      line-height: 1.45;
    }

    .mc-kpi-positive {
      background: #eef9f1 !important;
      border-color: #bfe7cb !important;
    }

    .mc-kpi-warning {
      background: #fff8ea !important;
      border-color: #f2d8a7 !important;
    }

    .mc-kpi-danger {
      background: #fff0ed !important;
      border-color: #f3b4a8 !important;
    }

    .mc-kpi-neutral {
      background: #f8faf8 !important;
      border-color: #dde5df !important;
    }

    .mc-kpi-protected {
      background: #f1f7ff !important;
      border-color: #c8dff5 !important;
    }

    .mc-kpi-estimation {
      background: #f5f2ff !important;
      border-color: #d8cff8 !important;
    }

    .report-tabs {
      position: sticky;
      top: 0;
      z-index: 20;
      display: grid;
      grid-template-columns: repeat(7, minmax(0, 1fr));
      gap: 8px;
      padding: 8px;
      border: 1px solid #dde5df;
      border-radius: 8px;
      background: rgba(244, 246, 244, 0.94);
      backdrop-filter: blur(8px);
    }

    .report-tabs button {
      display: grid;
      gap: 4px;
      min-height: 58px;
      padding: 10px 12px;
      border: 1px solid #dde5df;
      border-radius: 6px;
      background: #fff;
      color: #33453d;
      text-align: left;
      cursor: pointer;
      font: inherit;
    }

    .report-tabs button.active {
      background: #143d2f;
      border-color: #143d2f;
      color: #fff;
    }

    .report-tabs span {
      font-weight: 900;
      font-size: 0.86rem;
    }

    .report-tabs small {
      color: inherit;
      opacity: 0.76;
      font-weight: 750;
      line-height: 1.2;
    }

    .decision-panel,
    .decision-grid {
      display: grid;
      gap: 14px;
    }

    .decision-grid {
      grid-template-columns: repeat(4, minmax(160px, 1fr));
    }

    .decision-grid div {
      display: grid;
      gap: 8px;
      padding: 14px;
      border-radius: 8px;
      background: #f8faf8;
      border: 1px solid #edf1ee;
    }

    .decision-grid span {
      color: #66746e;
      font-weight: 850;
      font-size: 0.78rem;
    }

    .decision-grid strong {
      color: #143d2f;
      font-size: 1.1rem;
    }

    .decision-grid small {
      color: #66746e;
      font-size: 0.78rem;
      font-weight: 750;
      line-height: 1.35;
    }

    .interpretation-panel,
    .owner-analysis-panel {
      border-color: #c8dff5;
      background: #f8fbff;
    }

    .interpretation-list {
      display: grid;
      gap: 10px;
    }

    .interpretation-list p {
      margin: 0;
      padding: 12px 14px;
      border-radius: 8px;
      background: #fff;
      color: #33453d;
      font-weight: 800;
      line-height: 1.45;
    }

    .owner-analysis-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
    }

    .owner-analysis-grid div {
      display: grid;
      gap: 8px;
      padding: 14px;
      border-radius: 10px;
      border: 1px solid #dde5df;
      background: #fff;
    }

    .owner-analysis-grid span {
      color: #66746e;
      font-size: 0.78rem;
      font-weight: 900;
      line-height: 1.25;
    }

    .owner-analysis-grid strong {
      color: #143d2f;
      font-size: 1.08rem;
      font-weight: 950;
    }

    .owner-analysis-note {
      margin: 14px 0 0;
      padding: 12px 14px;
      border-radius: 10px;
      background: #fff8ea;
      color: #5f430d;
      font-weight: 850;
      line-height: 1.45;
    }

    .accounting-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 18px;
    }

    .content-grid {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 340px;
      gap: 18px;
    }

    .comparison-panel {
      display: grid;
      gap: 16px;
    }

    .comparison-cards,
    .comparison-summary {
      margin-bottom: 0;
    }

    .panel {
      padding: 18px;
      min-width: 0;
    }

    .panel-heading {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 16px;
    }

    .panel-heading span {
      color: #66746e;
      font-size: 0.85rem;
      font-weight: 800;
    }

    .table-wrap {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      min-width: 920px;
    }

    th, td {
      padding: 12px 10px;
      border-bottom: 1px solid #edf1ee;
      text-align: left;
      white-space: nowrap;
    }

    th {
      color: #66746e;
      font-size: 0.75rem;
      text-transform: uppercase;
      font-weight: 900;
      letter-spacing: 0;
      background: #f8faf8;
    }

    td {
      color: #24312c;
      font-size: 0.88rem;
      font-weight: 650;
    }

    .amount {
      text-align: right;
      font-variant-numeric: tabular-nums;
    }

    .strong {
      color: #143d2f;
      font-weight: 900;
    }

    .positive {
      color: #116238;
    }

    .negative {
      color: #9f2415;
    }

    .category-list {
      display: grid;
      gap: 10px;
    }

    .category-list.compact {
      margin-top: 12px;
    }

    .category-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      padding: 12px;
      border-radius: 6px;
      background: #f8faf8;
      color: #33453d;
      font-weight: 800;
    }

    .category-row strong {
      color: #143d2f;
      white-space: nowrap;
    }

    .category-row span {
      display: grid;
      gap: 3px;
    }

    .category-row small {
      color: #6f7c76;
      font-size: 0.76rem;
      font-weight: 750;
    }

    .observation-cell {
      max-width: 280px;
      white-space: normal;
      color: #56635d;
    }

    .result-list {
      display: grid;
      gap: 10px;
    }

    .result-list div {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      color: #4d5b55;
      font-weight: 800;
    }

    .result-list strong {
      color: #143d2f;
      white-space: nowrap;
    }

    .result-list .net {
      padding-top: 10px;
      border-top: 1px solid #edf1ee;
      color: #17231e;
    }

    .result-list .formula {
      display: grid;
      gap: 4px;
      padding: 10px 12px;
      border-radius: 6px;
      background: #f8faf8;
    }

    .result-list .formula strong {
      white-space: normal;
      color: #4d5b55;
      font-size: 0.86rem;
    }

    .result-list .with-help {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      gap: 4px 16px;
    }

    .result-list .with-help small {
      grid-column: 1 / -1;
      color: #66746e;
      font-size: 0.78rem;
      font-weight: 750;
      line-height: 1.35;
    }

    .position-credit-panel {
      display: grid;
      gap: 16px;
    }

    .insight-box {
      display: grid;
      gap: 6px;
      padding: 16px;
      border-radius: 8px;
      background: #eef7f2;
      border: 1px solid #c7e4d3;
      color: #24312c;
    }

    .insight-box strong {
      color: #143d2f;
      font-size: 1rem;
      font-weight: 900;
    }

    .insight-box p {
      margin: 0;
      color: #4d5b55;
      font-weight: 750;
      line-height: 1.45;
    }

    .position-grid {
      display: grid;
      grid-template-columns: repeat(7, minmax(130px, 1fr));
      gap: 12px;
    }

    .position-card {
      min-height: 112px;
      display: grid;
      align-content: space-between;
      gap: 10px;
      padding: 16px;
      border-radius: 8px;
      border: 1px solid #dde5df;
      background: #f8faf8;
    }

    .position-card span,
    .position-card small {
      color: #66746e;
      font-weight: 850;
      line-height: 1.25;
    }

    .position-card span {
      font-size: 0.8rem;
    }

    .position-card small {
      font-size: 0.72rem;
    }

    .position-card strong {
      color: #143d2f;
      font-size: 1.18rem;
      line-height: 1.1;
      font-variant-numeric: tabular-nums;
    }

    .position-card.highlight {
      background: #fff8ea;
      border-color: #f2d8a7;
    }

    .position-card.income {
      background: #f1f7ff;
      border-color: #c8dff5;
    }

    .position-card.count {
      background: #fff;
    }

    .modal-backdrop,
    .overlay-modal {
      position: fixed;
      inset: 0;
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      overflow: hidden;
      background: rgba(23, 35, 30, 0.42);
    }

    .modal-card {
      width: min(560px, 100%);
      display: grid;
      gap: 16px;
      padding: 20px;
      border-radius: 8px;
      background: #fff;
      border: 1px solid #dde5df;
      box-shadow: 0 24px 80px rgba(23, 35, 30, 0.26);
    }

    .modal-card.wide {
      width: min(760px, 100%);
    }

    .modal-paie {
      width: min(760px, calc(100vw - 32px));
      max-height: calc(100dvh - 32px);
      height: auto;
      display: flex;
      flex-direction: column;
      gap: 0;
      overflow: hidden;
      box-sizing: border-box;
      border-radius: 8px;
      background: #fff;
      border: 1px solid #dde5df;
      box-shadow: 0 24px 80px rgba(23, 35, 30, 0.26);
    }

    .modal-paie-header {
      flex: 0 0 auto;
      margin: 0;
      padding: 12px 16px 10px;
      border-bottom: 1px solid #edf1ee;
    }

    .modal-version {
      display: block;
      margin-top: 4px;
      color: #7b8782;
      font-size: 0.72rem;
      font-weight: 800;
    }

    .modal-paie-body {
      flex: 1 1 auto;
      min-height: 0;
      max-height: calc(100dvh - 180px);
      overflow-y: auto;
      display: grid;
      gap: 10px;
      padding: 12px 16px;
    }

    .paie-summary-grid,
    .paie-fields-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 10px;
    }

    .paie-summary-grid > div {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 10px;
      border-radius: 6px;
      background: #f8faf8;
      color: #4d5b55;
      font-weight: 800;
    }

    .paie-summary-grid strong {
      color: #143d2f;
      text-align: right;
    }

    .paie-summary-note {
      grid-column: 1 / -1;
    }

    .modal-paie-footer {
      flex: 0 0 auto;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 12px 16px;
      border-top: 1px solid #e5e7eb;
      background: #fff;
    }

    .modal-card textarea,
    .modal-paie textarea {
      width: 100%;
      min-height: 54px;
      max-height: 80px;
      border: 1px solid #cfd9d3;
      border-radius: 6px;
      background: #fbfcfb;
      padding: 10px 12px;
      color: #17231e;
      font: inherit;
      font-weight: 650;
      resize: vertical;
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }

    .checkbox-line {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    @media (max-width: 720px) {
      .paie-summary-grid,
      .paie-fields-grid {
        grid-template-columns: 1fr;
      }
    }

    .alert-list {
      display: grid;
      gap: 10px;
    }

    .mc-alert-panel {
      display: grid;
      gap: 12px;
    }

    .mc-alert-row {
      display: grid;
      grid-template-columns: 180px minmax(0, 1fr) 300px;
      gap: 16px;
      align-items: start;
      padding: 16px;
      border-radius: 12px;
      border: 1px solid #f2d8a7;
      background: #fff8ea;
      color: #5f430d;
    }

    .mc-alert-critical {
      border-color: #f3b4a8;
      background: #fff0ed;
      color: #8b1e10;
    }

    .mc-alert-warning {
      border-color: #f2d8a7;
      background: #fff8ea;
      color: #5f430d;
    }

    .mc-alert-info {
      border-color: #bfdbfe;
      background: #eff6ff;
      color: #1e3a8a;
    }

    .mc-alert-title,
    .mc-alert-message,
    .mc-alert-action {
      display: grid;
      gap: 6px;
    }

    .mc-alert-title strong {
      font-size: 0.82rem;
      font-weight: 950;
      text-transform: uppercase;
    }

    .mc-alert-title span,
    .mc-alert-action span {
      color: inherit;
      opacity: 0.75;
      font-size: 0.78rem;
      font-weight: 900;
    }

    .mc-alert-message p,
    .mc-alert-action p {
      margin: 0;
      font-weight: 850;
      line-height: 1.45;
    }

    .mc-alert-message small {
      color: #7b8782;
      font-weight: 800;
    }

    .mc-alert-action details {
      margin-top: 4px;
    }

    .mc-alert-action summary {
      cursor: pointer;
      font-weight: 950;
    }

    .alert-row {
      display: grid;
      grid-template-columns: 80px 150px 180px minmax(0, 1fr);
      gap: 12px;
      align-items: center;
      padding: 12px;
      border-radius: 6px;
      border: 1px solid #f2d8a7;
      background: #fff8ea;
      color: #5f430d;
      font-weight: 800;
    }

    .alert-row.compact {
      grid-template-columns: 74px 150px minmax(0, 1fr) 118px;
      align-items: start;
    }

    .alert-row details {
      justify-self: end;
      width: 100%;
      max-width: 260px;
    }

    .alert-row summary {
      cursor: pointer;
      font-weight: 900;
      color: inherit;
    }

    .alert-row details p {
      margin-top: 8px;
      max-height: 160px;
      overflow: auto;
      font-size: 0.82rem;
      line-height: 1.45;
    }

    .alert-row.red {
      border-color: #f3b4a8;
      background: #fff0ed;
      color: #8b1e10;
    }

    .alert-row.info {
      border-color: #bfdbfe;
      background: #eff6ff;
      color: #1e3a8a;
    }

    .alert-row p {
      margin: 0;
      font-weight: 750;
    }

    .alert-row small {
      display: block;
      margin-top: 3px;
      color: #7b8782;
    }

    .pill {
      display: inline-flex;
      align-items: center;
      min-height: 26px;
      padding: 0 10px;
      border-radius: 999px;
      background: #eef5f1;
      color: #1d5d46;
      font-size: 0.78rem;
      font-weight: 900;
    }

    .danger-pill {
      background: #fff0ed;
      color: #9f2415;
    }

    .warning-pill {
      background: #fff8ea;
      color: #76560e;
    }

    .empty-state {
      padding: 20px 10px;
      color: #7b8782;
      text-align: center;
      font-weight: 800;
    }

    @media (max-width: 1200px) {
      .filters-grid, .decision-strip {
        grid-template-columns: repeat(3, minmax(160px, 1fr));
      }

      .report-tabs {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      .mc-kpi-grid-decision,
      .decision-grid,
      .owner-analysis-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .mc-alert-row {
        grid-template-columns: 1fr;
      }

      .accounting-grid,
      .content-grid,
      .position-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 720px) {
      .revenus-page {
        padding: 16px;
      }

      .revenus-hero {
        flex-direction: column;
        padding: 22px;
      }

      .hero-total {
        min-width: 0;
      }

      .filters-grid, .decision-strip, .report-tabs, .mc-kpi-grid-decision, .decision-grid, .owner-analysis-grid {
        grid-template-columns: 1fr;
      }

      .mc-kpi-grid-decision .mc-kpi-card {
        min-height: 0;
      }

      .alert-row.compact {
        grid-template-columns: 1fr;
      }

      .alert-row details {
        justify-self: stretch;
        max-width: none;
      }
    }
  `]
})
export class RapportRevenusComponent implements OnInit, OnDestroy {
  private readonly rapportRevenusService = inject(RapportRevenusService);
  private readonly agenceService = inject(AgenceService);
  private readonly authService = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  loading = false;
  error: string | null = null;
  agenceWarning: string | null = null;
  report: RapportRevenusResponse | null = null;
  activeTab: RapportTab = 'decision';
  qualificationTarget: ApportFinancementDetailDto | null = null;
  qualificationNature: NatureFinancementApprovisionnement | null = null;
  qualificationCommentaire = '';
  qualificationSaving = false;
  qualificationError: string | null = null;
  rattachementTarget: ChargeDetailDto | null = null;
  rattachementBeneficiaires: DepenseCaisseBeneficiaireSalaire[] = [];
  rattachementPreview?: PaieEmployePreview;
  rattachementSaving = false;
  rattachementError: string | null = null;
  transportTarget: ChargeDetailDto | null = null;
  transportSaving = false;
  transportError: string | null = null;
  agences: AgenceResponse[] = [];
  quickPeriod: QuickPeriod = 'month';

  readonly tabs: Array<{ value: RapportTab; label: string; hint: string }> = [
    { value: 'decision', label: 'Vue decisionnelle', hint: 'KPI et alertes' },
    { value: 'comparaison', label: 'Comparaison', hint: 'Agences, siège' },
    { value: 'revenus', label: 'Revenus & charges', hint: 'Categories, antennes' },
    { value: 'paie', label: 'Paie & transport', hint: 'Salaires, terrain' },
    { value: 'credit', label: 'Credit & tresorerie', hint: 'Capital, fonds' },
    { value: 'financement', label: 'Financement', hint: 'Apports proprietaire' },
    { value: 'audit', label: 'Audit', hint: 'Details complets' }
  ];

  rattachementForm = {
    employeId: null as number | null,
    periodePaie: this.currentPayrollPeriod(),
    typePaiementPersonnel: 'SALAIRE_PARTIEL' as TypePaiementPersonnel,
    motif: 'Paiement partiel du salaire de juillet',
    motifRetenue: '',
    motifPaiementPartiel: 'Paiement partiel du salaire de juillet',
    retenueDefinitive: false,
    commentaireCorrection: 'Régularisation historique paie juillet'
  };

  transportForm = {
    employeId: null as number | null,
    periodeCharge: this.currentPayrollPeriod(),
    siteId: null as number | null,
    commentaireCorrection: 'Régularisation historique transport terrain'
  };

  readonly typePaiementOptions = TYPE_PAIEMENT_PERSONNEL_OPTIONS;

  readonly qualificationOptions: Array<{ value: NatureFinancementApprovisionnement; label: string }> = [
    { value: 'TRANSFERT_INTERNE', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.TRANSFERT_INTERNE },
    { value: 'APPORT_PROPRIETAIRE', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.APPORT_PROPRIETAIRE },
    { value: 'PRET_RECU', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.PRET_RECU },
    { value: 'REMBOURSEMENT_AVANCE', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.REMBOURSEMENT_AVANCE },
    { value: 'AUTRE_FINANCEMENT', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.AUTRE_FINANCEMENT },
  ];

  filters: RapportRevenusFilters = {
    dateDebut: this.firstDayOfMonth(),
    dateFin: this.todayIso(),
    agenceId: null,
    categorie: null,
    source: null
  };

  readonly categorieOptions: SelectOption[] = [
    { value: 'FRAIS_ANALYSE_CREDIT', label: 'Frais analyse credit' },
    { value: 'FRAIS_RETRAIT_EPARGNE', label: 'Frais retrait epargne' },
    { value: 'INTERETS_CREDIT', label: 'Interets credit' },
    { value: 'PENALITES_CREDIT', label: 'Penalites credit' },
    { value: 'CARNETS_VENDUS', label: 'Carnets vendus' },
    { value: 'REVENUS_DIVERS', label: 'Revenus divers' }
  ];

  readonly sourceOptions: SelectOption[] = [
    { value: 'CAISSE', label: 'Caisse' },
    { value: 'CREDIT', label: 'Credit' },
    { value: 'COLLECTE', label: 'Collecte' }
  ];

  ngOnInit(): void {
    this.loadAgences();
    this.loadReport();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadReport(): void {
    this.loading = true;
    this.error = null;

    this.rapportRevenusService.getRapport(this.filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (report) => {
          this.report = report;
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur chargement rapport revenus', error);
          this.error = 'Impossible de charger le rapport des revenus pour cette selection.';
          this.loading = false;
        }
      });
  }

  setQuickPeriod(period: QuickPeriod): void {
    this.quickPeriod = period;
    const today = new Date();

    if (period === 'today') {
      this.filters.dateDebut = this.toIsoDate(today);
      this.filters.dateFin = this.toIsoDate(today);
    }

    if (period === 'week') {
      const start = new Date(today);
      const day = start.getDay() || 7;
      start.setDate(start.getDate() - day + 1);
      this.filters.dateDebut = this.toIsoDate(start);
      this.filters.dateFin = this.toIsoDate(today);
    }

    if (period === 'month') {
      this.filters.dateDebut = this.firstDayOfMonth();
      this.filters.dateFin = this.toIsoDate(today);
    }

    if (period !== 'custom') {
      this.loadReport();
    }
  }

  periodeLabel(data: RapportRevenusResponse | null | undefined): string {
    const debut = data?.dateDebut || this.filters.dateDebut;
    const fin = data?.dateFin || this.filters.dateFin;
    return `${debut} - ${fin}`;
  }

  amountTone(value: number): 'positive' | 'negative' {
    return Number(value || 0) >= 0 ? 'positive' : 'negative';
  }

  comparisonAgencies(data: RapportRevenusResponse): ComparaisonAgenceDto[] {
    return data.comparaisonAgences?.agences || [];
  }

  rankName(row?: ComparaisonAgenceDto | null): string {
    return row?.agenceNom || '-';
  }

  formatPercent(value?: number | null): string {
    return `${Number(value || 0).toLocaleString('fr-FR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} %`;
  }

  currentRole(): string {
    return (this.authService.getCurrentUser()?.role || '').replace(/^ROLE_/, '').toUpperCase();
  }

  isRci(): boolean {
    return this.currentRole() === 'RCI';
  }

  hasGlobalAgencyAccess(): boolean {
    return ['ADMIN', 'GERANT_GENERAL', 'COO', 'RCI'].includes(this.currentRole());
  }

  criticalAlerts(data: RapportRevenusResponse): ControleCoherenceDto[] {
    return this.sortAlerts(data.controlesCoherence || [])
      .filter((alert) => alert.severite === 'ROUGE' || alert.severite === 'ORANGE');
  }

  visibleAlerts(data: RapportRevenusResponse): ControleCoherenceDto[] {
    const alerts = this.sortAlerts(data.controlesCoherence || []);
    if (this.activeTab === 'decision') {
      return alerts.filter((alert) => alert.severite === 'ROUGE' || alert.severite === 'ORANGE');
    }
    if (this.activeTab === 'paie') {
      return alerts.filter((alert) => alert.type === 'MASSE_SALARIALE' || alert.type === 'TRANSPORT_TERRAIN');
    }
    return alerts;
  }

  decisionStatus(data: RapportRevenusResponse): string {
    if (data.capaciteRetraitProprietaire?.retraitDeconseille) {
      return 'Retrait deconseille';
    }
    if (this.criticalAlerts(data).length > 0) {
      return 'Analyse complementaire necessaire';
    }
    return 'A valider par gestion';
  }

  alertSeverityLabel(severite?: string | null): string {
    if (severite === 'ROUGE') {
      return 'Critique';
    }
    if (severite === 'INFO') {
      return 'Information';
    }
    return 'Attention';
  }

  alertPanelClass(severite?: string | null): string {
    if (severite === 'ROUGE') {
      return 'mc-alert-critical';
    }
    if (severite === 'INFO') {
      return 'mc-alert-info';
    }
    return 'mc-alert-warning';
  }

  alertActionHint(severite?: string | null): string {
    if (severite === 'ROUGE') {
      return 'A regulariser avant validation finale.';
    }
    return 'A verifier avant cloture ou decision financiere.';
  }

  private sortAlerts(alerts: ControleCoherenceDto[]): ControleCoherenceDto[] {
    const rank: Record<string, number> = { ROUGE: 0, ORANGE: 1, INFO: 2 };
    return [...alerts].sort((a, b) => (rank[a.severite || 'INFO'] ?? 3) - (rank[b.severite || 'INFO'] ?? 3));
  }

  alertTypeLabel(type?: string | null): string {
    switch (type) {
      case 'MASSE_SALARIALE': return 'Masse salariale';
      case 'TRANSPORT_TERRAIN': return 'Transport';
      case 'POSITION_CREDIT': return 'Credit';
      case 'FRAIS_ANALYSE_CREDIT': return 'Frais credit';
      default: return type || '-';
    }
  }

  alertTitle(message?: string | null): string {
    if (!message) {
      return '-';
    }
    const firstSentence = message.split('.')[0]?.trim();
    return firstSentence ? `${firstSentence}.` : message;
  }

  kpiItems(kpis: RevenuKpiDto): Array<{ label: string; value: number }> {
    return [
      { label: 'Frais analyse credit', value: kpis.fraisAnalyseCredit },
      { label: 'Frais retrait epargne', value: kpis.fraisRetraitEpargne },
      { label: 'Interets credit', value: kpis.interetsCredit },
      { label: 'Penalites credit', value: kpis.penalitesCredit },
      { label: 'Carnets vendus', value: kpis.carnetsVendus },
      { label: 'Revenus divers', value: kpis.revenusDivers }
    ];
  }

  visibleDetails(details: RevenuDetailDto[]): RevenuDetailDto[] {
    return details.slice(0, 300);
  }

  canQualifyFinancement(detail: ApportFinancementDetailDto): boolean {
    return !this.isRci() && !!detail.operationId && (detail.natureFinancement || '').toLowerCase().includes('non qualifi');
  }

  openQualification(detail: ApportFinancementDetailDto): void {
    this.qualificationTarget = detail;
    this.qualificationNature = null;
    this.qualificationCommentaire = '';
    this.qualificationError = null;
  }

  cancelQualification(): void {
    this.qualificationTarget = null;
    this.qualificationNature = null;
    this.qualificationCommentaire = '';
    this.qualificationSaving = false;
    this.qualificationError = null;
  }

  submitQualification(): void {
    if (!this.qualificationTarget?.operationId || !this.qualificationNature || !this.qualificationCommentaire.trim()) {
      this.qualificationError = 'Nature et commentaire de correction sont obligatoires.';
      return;
    }

    this.qualificationSaving = true;
    this.qualificationError = null;
    this.rapportRevenusService.requalifierNatureFinancement(this.qualificationTarget.operationId, {
      natureFinancement: this.qualificationNature,
      commentaireCorrection: this.qualificationCommentaire.trim()
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.cancelQualification();
        this.loadReport();
      },
      error: (error) => {
        this.qualificationError = error?.error?.message || 'Impossible de qualifier cet approvisionnement.';
        this.qualificationSaving = false;
      }
    });
  }

  canRattacherPaie(charge: ChargeDetailDto): boolean {
    const isSalaire = charge.categorieTechnique === 'SALAIRE' || charge.categorie === 'Salaire / prime / commission';
    const isEligibleStatus = charge.statut === 'PAYEE' || charge.statut === 'VALIDEE';
    return !!charge.depenseId
      && !this.isRci()
      && isSalaire
      && isEligibleStatus
      && (!charge.employeId || !charge.periodePaie || !charge.typePaiementPersonnel || charge.canRattacherPaie === true);
  }

  canRattacherTransport(charge: ChargeDetailDto): boolean {
    const isTransport = charge.categorieTechnique === 'TRANSPORT' || charge.categorie === 'Transport';
    const isEligibleStatus = charge.statut === 'PAYEE' || charge.statut === 'VALIDEE';
    return !!charge.depenseId
      && !this.isRci()
      && isTransport
      && isEligibleStatus
      && (!charge.employeId || !charge.periodeCharge || !charge.siteChargeId || !charge.typeChargeFixe || charge.canRattacherTransport === true);
  }

  openRattachementPaie(charge: ChargeDetailDto): void {
    this.rattachementTarget = charge;
    this.rattachementPreview = undefined;
    this.rattachementError = null;
    this.rattachementForm = {
      employeId: charge.employeId || null,
      periodePaie: charge.periodePaie || this.currentPayrollPeriod(),
      typePaiementPersonnel: 'SALAIRE_PARTIEL',
      motif: 'Paiement partiel du salaire de juillet',
      motifRetenue: '',
      motifPaiementPartiel: 'Paiement partiel du salaire de juillet',
      retenueDefinitive: false,
      commentaireCorrection: 'Régularisation historique paie juillet'
    };
    if (!charge.caisseId) {
      this.rattachementError = 'Caisse introuvable pour charger les employés.';
      return;
    }
    this.rapportRevenusService.getBeneficiairesSalaire(charge.caisseId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (beneficiaires) => {
          this.rattachementBeneficiaires = beneficiaires ?? [];
          this.refreshRattachementPreview();
        },
        error: (error) => this.rattachementError = error?.error?.message || 'Impossible de charger les employés.'
      });
  }

  cancelRattachementPaie(): void {
    this.rattachementTarget = null;
    this.rattachementPreview = undefined;
    this.rattachementError = null;
    this.rattachementSaving = false;
  }

  openRattachementTransport(charge: ChargeDetailDto): void {
    this.transportTarget = charge;
    this.transportError = null;
    this.transportForm = {
      employeId: charge.employeId || null,
      periodeCharge: charge.periodeCharge || this.report?.transportFixePrevu?.periodeCharge || this.currentPayrollPeriod(),
      siteId: charge.siteChargeId || null,
      commentaireCorrection: 'Régularisation historique transport terrain'
    };
    if (!charge.caisseId) {
      this.transportError = 'Caisse introuvable pour charger les employés.';
      return;
    }
    this.rapportRevenusService.getBeneficiairesSalaire(charge.caisseId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (beneficiaires) => this.rattachementBeneficiaires = beneficiaires ?? [],
        error: (error) => this.transportError = error?.error?.message || 'Impossible de charger les employés.'
      });
  }

  cancelRattachementTransport(): void {
    this.transportTarget = null;
    this.transportError = null;
    this.transportSaving = false;
  }

  submitRattachementTransport(): void {
    if (!this.transportTarget?.depenseId || !this.transportForm.employeId || !this.transportForm.siteId || !this.transportForm.periodeCharge || !this.transportForm.commentaireCorrection.trim()) {
      this.transportError = 'Agent Terrain, site, période et commentaire correction sont obligatoires.';
      return;
    }
    this.transportSaving = true;
    this.transportError = null;
    this.rapportRevenusService.rattacherTransport(this.transportTarget.depenseId, {
      employeId: this.transportForm.employeId,
      periodeCharge: this.transportForm.periodeCharge,
      dateDebutPeriode: this.filters.dateDebut,
      dateFinPeriode: this.filters.dateFin,
      siteId: this.transportForm.siteId,
      typeChargeFixe: 'TRANSPORT_SITE',
      commentaireCorrection: this.transportForm.commentaireCorrection.trim()
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.cancelRattachementTransport();
        this.loadReport();
      },
      error: (error) => {
        this.transportSaving = false;
        this.transportError = error?.error?.message || 'Impossible de rattacher cette dépense au transport terrain.';
      }
    });
  }

  transportSiteOptions(): Array<{ id: number; nom: string }> {
    const details = this.report?.transportFixePrevu?.detailsParSite || [];
    const byId = new Map<number, string>();
    for (const detail of details) {
      if (detail.siteId) byId.set(detail.siteId, detail.siteNom || `Site #${detail.siteId}`);
    }
    if (this.transportTarget?.siteChargeId) {
      byId.set(this.transportTarget.siteChargeId, this.transportTarget.siteChargeNom || `Site #${this.transportTarget.siteChargeId}`);
    }
    return Array.from(byId.entries()).map(([id, nom]) => ({ id, nom }));
  }

  selectedTransportAgentDetail() {
    const employeId = this.transportForm.employeId;
    return (this.report?.transportFixePrevu?.details || []).find(detail => detail.employeId === employeId);
  }

  syncTransportSiteFromAgent(): void {
    const detail = this.selectedTransportAgentDetail();
    if (detail?.siteId) {
      this.transportForm.siteId = detail.siteId;
    }
  }

  selectedTransportSiteDetail() {
    const siteId = this.transportForm.siteId;
    return (this.report?.transportFixePrevu?.detailsParSite || []).find(detail => detail.siteId === siteId);
  }

  nombreJoursRapport(): number {
    const debut = new Date(this.filters.dateDebut);
    const fin = new Date(this.filters.dateFin);
    const diff = Math.floor((fin.getTime() - debut.getTime()) / 86400000) + 1;
    return Number.isFinite(diff) && diff > 0 ? diff : 0;
  }

  transportExpectedForModal(): number {
    const detail = this.selectedTransportAgentDetail();
    if (detail) return detail.montantPrevu || 0;
    const site = this.selectedTransportSiteDetail();
    return (site?.montantJournalierParAgent || 0) * this.nombreJoursRapport();
  }

  transportDifference(): number {
    return (this.transportTarget?.montant || 0) - this.transportExpectedForModal();
  }

  refreshRattachementPreview(): void {
    if (!this.rattachementForm.employeId || !/^\d{4}-\d{2}$/.test(this.rattachementForm.periodePaie)) {
      this.rattachementPreview = undefined;
      return;
    }
    this.rapportRevenusService.getPaiePreview(this.rattachementForm.employeId, this.rattachementForm.periodePaie)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (preview) => this.rattachementPreview = preview,
        error: (error) => this.rattachementError = error?.error?.message || 'Impossible de prévisualiser la paie.'
      });
  }

  showMotifGeneralRattachement(): boolean {
    return this.rattachementForm.typePaiementPersonnel === 'AVANCE_SALAIRE'
      || this.rattachementForm.typePaiementPersonnel === 'PRIME'
      || this.rattachementForm.typePaiementPersonnel === 'COMMISSION'
      || this.rattachementForm.typePaiementPersonnel === 'REGULARISATION'
      || this.rattachementForm.typePaiementPersonnel === 'AUTRE';
  }

  submitRattachementPaie(): void {
    if (!this.rattachementTarget?.depenseId || !this.rattachementForm.employeId || !this.rattachementForm.periodePaie || !this.rattachementForm.commentaireCorrection.trim()) {
      this.rattachementError = 'Employé, période et commentaire correction sont obligatoires.';
      return;
    }
    if (this.rattachementForm.typePaiementPersonnel === 'SALAIRE_PARTIEL' && !this.rattachementForm.motifPaiementPartiel.trim()) {
      this.rattachementError = 'Le motif du paiement partiel est obligatoire.';
      return;
    }
    if (this.rattachementForm.typePaiementPersonnel === 'RETENUE_SALAIRE' && !this.rattachementForm.motifRetenue.trim()) {
      this.rattachementError = 'Le motif de retenue est obligatoire.';
      return;
    }
    if (this.rattachementForm.typePaiementPersonnel === 'AVANCE_SALAIRE' && !this.rattachementForm.motif.trim()) {
      this.rattachementError = 'Le motif de l’avance est obligatoire.';
      return;
    }
    this.rattachementSaving = true;
    this.rattachementError = null;
    this.rapportRevenusService.rattacherPaie(this.rattachementTarget.depenseId, {
      employeId: this.rattachementForm.employeId,
      periodePaie: this.rattachementForm.periodePaie,
      typePaiementPersonnel: this.rattachementForm.typePaiementPersonnel,
      motif: this.rattachementForm.motif || undefined,
      motifRetenue: this.rattachementForm.motifRetenue || undefined,
      motifPaiementPartiel: this.rattachementForm.motifPaiementPartiel || undefined,
      retenueDefinitive: this.rattachementForm.retenueDefinitive,
      commentaireCorrection: this.rattachementForm.commentaireCorrection
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.cancelRattachementPaie();
        this.loadReport();
      },
      error: (error) => {
        this.rattachementSaving = false;
        this.rattachementError = error?.error?.message || 'Impossible de rattacher cette dépense à une paie.';
      }
    });
  }

  rattachementDifference(): number {
    return (this.rattachementTarget?.montant || 0) - (this.rattachementPreview?.totalAPayer || 0);
  }

  rattachementReste(): number {
    if (this.rattachementForm.typePaiementPersonnel === 'RETENUE_SALAIRE' && this.rattachementForm.retenueDefinitive) {
      return 0;
    }
    return Math.max((this.rattachementPreview?.totalAPayer || 0) - (this.rattachementTarget?.montant || 0), 0);
  }

  categoryLabel(value: string): string {
    return this.categorieOptions.find(option => option.value === value)?.label || value;
  }

  sourceLabel(value: string): string {
    return this.sourceOptions.find(option => option.value === value)?.label || value;
  }

  natureLabel(detail: RevenuDetailDto): string {
    return detail.nature || detail.sousCategorie || 'Revenu non categorise';
  }

  severityClass(severite?: string | null): string {
    if (severite === 'ROUGE') {
      return 'red';
    }
    if (severite === 'INFO') {
      return 'info';
    }
    return 'orange';
  }

  statutPaieLabel(statut?: string | null): string {
    switch (statut) {
      case 'PAYE': return 'Payé';
      case 'PAYE_AVEC_PRIME_BONUS': return 'Payé avec prime/bonus';
      case 'PARTIEL': return 'Partiel';
      case 'PAYE_AVEC_RETENUE': return 'Payé avec retenue';
      case 'AVANCE_A_REGULARISER': return 'Avance à régulariser';
      case 'SURPAYE': return 'Surpayé';
      case 'A_VERIFIER': return 'À vérifier';
      case 'NON_PAYE': return 'Non payé';
      default: return statut || '-';
    }
  }

  otherPrimesBonus(employe: DetailMasseSalarialeDto): number {
    const fixedComplements = (employe.primeFixe || 0) + (employe.bonusVariable || 0);
    const justifiedComplements = Math.max((employe.primesBonusJustifies || employe.primes || 0) - (employe.totalPrimesAgentTerrain || 0), 0);
    return fixedComplements + justifiedComplements + (employe.commissions || 0) + (employe.regularisations || 0);
  }

  statutTransportLabel(statut?: string | null): string {
    switch (statut) {
      case 'PAYE': return 'Payé';
      case 'PARTIEL': return 'Partiel';
      case 'SURPAYE': return 'Surpayé';
      case 'A_VERIFIER': return 'À vérifier';
      case 'NON_CONFIGURE': return 'Non configuré';
      case 'NON_PAYE': return 'Non payé';
      default: return statut || '-';
    }
  }

  paiementPaieSummary(charge: { typePaiementPersonnel?: string | null; montant?: number | null; montantRemunerationReference?: number | null; montantRestantApresPaiement?: number | null; montantRetenue?: number | null; motifRetenue?: string | null; motifPaiementPartiel?: string | null; commentairePaie?: string | null }): string {
    const montant = this.formatMoney(charge.montant || 0);
    const prevu = this.formatMoney(charge.montantRemunerationReference || 0);
    const reste = this.formatMoney(charge.montantRestantApresPaiement || 0);
    switch (charge.typePaiementPersonnel) {
      case 'SALAIRE_PARTIEL': return `Salaire partiel ${montant} / prévu ${prevu}, reste ${reste}`;
      case 'AVANCE_SALAIRE': return `Avance salaire ${montant} à régulariser`;
      case 'RETENUE_SALAIRE': return `Retenue salaire ${this.formatMoney(charge.montantRetenue || 0)}, motif : ${charge.motifRetenue || '-'}`;
      case 'PRIME': return `Prime ${montant}, motif : ${charge.commentairePaie || '-'}`;
      case 'COMMISSION': return `Commission ${montant}, motif : ${charge.commentairePaie || '-'}`;
      case 'REGULARISATION': return `Régularisation ${montant}, motif : ${charge.commentairePaie || '-'}`;
      case 'SALAIRE_COMPLET':
      case 'SALAIRE': return `Salaire complet ${montant} / prévu ${prevu}`;
      default: return charge.typePaiementPersonnel ? `${charge.typePaiementPersonnel} ${montant}` : '-';
    }
  }

  coutCarnets(data: RapportRevenusResponse): number {
    return data.carnetMarge?.coutTotalCarnets || data.coutEstimeCarnets || 0;
  }

  chargesCaissePayees(data: RapportRevenusResponse): number {
    return Math.max((data.totalCharges || 0) - this.coutCarnets(data), 0);
  }

  formatMoney(value: number | null | undefined): string {
    return new Intl.NumberFormat('fr-CD', {
      style: 'currency',
      currency: 'CDF',
      maximumFractionDigits: 0
    }).format(Number(value || 0));
  }

  formatDate(value?: string | null): string {
    if (!value) return '-';
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value));
  }

  private loadAgences(): void {
    this.agenceService.getAll()
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          console.warn('Liste des agences indisponible pour ce role', error);
          this.agenceWarning = 'Liste des antennes indisponible pour votre role. Le rapport reste limite a votre perimetre autorise.';
          return of([] as AgenceResponse[]);
        })
      )
      .subscribe((agences) => {
        this.agences = agences.filter(agence => agence.actif !== false);
      });
  }

  private todayIso(): string {
    return this.toIsoDate(new Date());
  }

  private firstDayOfMonth(): string {
    const today = new Date();
    return this.toIsoDate(new Date(today.getFullYear(), today.getMonth(), 1));
  }

  private currentPayrollPeriod(): string {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
