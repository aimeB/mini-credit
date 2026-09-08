import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { OperationCaisseResponse } from '../../models/operation-caisse-response';
import { OperationCaisseService } from '../../services/operation-caisse.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { SOURCE_OPERATION_CAISSE_LABELS } from '../../models/source-operation-caisse';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-operation-caisse-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './operation-caisse-list.component.html',
  styleUrls: ['./operation-caisse-list.component.css']
})
export class OperationCaisseListComponent implements OnInit {
  operations: OperationCaisseResponse[] = [];
  session: SessionCaisseResponse | null = null;
  loading = false;
  loadingSession = false;
  error = '';
  sessionId!: number;

  constructor(
    private route: ActivatedRoute,
    private operationCaisseService: OperationCaisseService,
    private sessionCaisseService: SessionCaisseService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
    if (!sessionId) {
      this.error = 'Session invalide.';
      return;
    }

    this.sessionId = sessionId;
    this.loadSession();
    this.loadOperations();
  }

  private loadSession(): void {
    this.loadingSession = true;
    this.sessionCaisseService.getById(this.sessionId).subscribe({
      next: (session) => {
        this.session = session;
        this.loadingSession = false;
      },
      error: () => {
        this.loadingSession = false;
      }
    });
  }

  loadOperations(): void {
    this.loading = true;
    this.error = '';

    this.operationCaisseService.getBySession(this.sessionId).subscribe({
      next: (data) => {
        this.operations = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = err?.error?.message || 'Erreur lors du chargement des opérations.';
        this.loading = false;
      }
    });
  }

  getSourceLabel(operation: OperationCaisseResponse): string {
    if (!operation.source) {
      return 'Non renseignée';
    }
    return SOURCE_OPERATION_CAISSE_LABELS[operation.source] ?? operation.source;
  }

  formatMoney(value: number | null | undefined, signed = false, type?: 'ENTREE' | 'SORTIE'): string {
    if (value == null) {
      return `0 ${this.devise}`;
    }
    const absolute = Math.abs(value);
    const amount = `${new Intl.NumberFormat('fr-FR').format(absolute)} ${this.devise}`;
    if (!signed || !type) {
      return amount;
    }
    return `${type === 'SORTIE' ? '-' : '+'}${amount}`;
  }

  formatDate(value: string | undefined): string {
    if (!value) {
      return '-';
    }
    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  formatLabel(value: string | undefined): string {
    if (!value) {
      return '-';
    }
    return value
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  get typeBadgeClass(): string {
    return this.session?.statut === 'OUVERTE' ? 'status-open' : this.session?.statut === 'PRE_CLOTUREE' ? 'status-pending' : 'status-closed';
  }

  get devise(): string {
    return this.session?.devise?.trim() || 'CDF';
  }

  get operationCount(): number {
    return this.operations.length;
  }

  get totalEntrees(): number {
    return this.session?.totalEntrees ?? this.operations
      .filter((operation) => operation.typeOperation === 'ENTREE')
      .reduce((sum, operation) => sum + (operation.montant || 0), 0);
  }

  get totalSorties(): number {
    return this.session?.totalSorties ?? this.operations
      .filter((operation) => operation.typeOperation === 'SORTIE')
      .reduce((sum, operation) => sum + (operation.montant || 0), 0);
  }

  get soldeTheorique(): number {
    return this.session?.soldeTheorique ?? 0;
  }

  get sessionSubtitle(): string {
    const caisseCode = this.session?.caisseCode || '-';
    return `Session #${this.sessionId} — Caisse ${caisseCode}`;
  }

  get canCreateOperation(): boolean {
    return !!this.session
      && this.session.statut === 'OUVERTE'
      && this.authService.hasAnyRole(['ADMIN', 'CAISSIER', 'CHEF_BUREAU']);
  }

  getAuthorLabel(operation: OperationCaisseResponse): string {
    return operation.createdById ? `#${operation.createdById}` : '-';
  }

  getOperationNarrative(operation: OperationCaisseResponse): string {
    return operation.description?.trim() || 'Aucune description disponible';
  }
}