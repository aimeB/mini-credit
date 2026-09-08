import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EmployeService } from '../../services/employe.service';
import { EmployeResponse } from '../../models/employe-response';
import { FONCTION_LABELS, FonctionEmploye } from '../../models/fonction-employe';
import { PaiementModalComponent } from '../../components/paiement-modal/paiement-modal.component';

@Component({
  selector: 'app-employe-list',
  standalone: true,
  imports: [CommonModule, RouterLink, PaiementModalComponent],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.css']
})
export class EmployeListComponent implements OnInit, OnDestroy {
  employes: EmployeResponse[] = [];
  loading: boolean = false;
  error: string | null = null;
  
  // Modal properties
  paymentModalOpen = false;
  selectedEmploye: EmployeResponse | null = null;

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(private employeService: EmployeService) {}

  ngOnInit(): void {
    this.loadEmployes();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }

  loadEmployes(): void {
    this.loading = true;
    this.error = null;

    this.subscriptions.add(
      this.employeService.getAll()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (data) => {
            this.employes = data;
            this.loading = false;
          },
          error: (err) => {
            this.error = 'Erreur lors du chargement des employés';
            console.error(err);
            this.loading = false;
          }
        })
    );
  }

  deleteEmploye(id: number, nom: string): void {
    if (confirm(`Êtes-vous sûr de vouloir supprimer ${nom}?`)) {
      this.subscriptions.add(
        this.employeService.delete(id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.employes = this.employes.filter(e => e.id !== id);
            },
            error: (err) => {
              alert('Erreur lors de la suppression');
              console.error(err);
            }
          })
      );
    }
  }

  getFonctionLabel(fonction: string): string {
    return FONCTION_LABELS[fonction as FonctionEmploye] ?? fonction ?? '—';
  }

  isAgentTerrain(employe: EmployeResponse): boolean {
    return employe.fonction === 'AGENT_TERRAIN';
  }

  isTransverseFunction(employe: EmployeResponse): boolean {
    return employe.fonction === 'COO' || employe.fonction === 'RCI';
  }

  getAffectationLabel(employe: EmployeResponse): string {
    return this.isTransverseFunction(employe) ? 'Toutes les agences' : (employe.nomAgence || '—');
  }

  getAffectationTitle(employe: EmployeResponse): string {
    if (this.isTransverseFunction(employe)) {
      return 'Toutes les agences / supervision transverse';
    }

    return this.isAgentTerrain(employe)
      ? `${employe.nomAgence || '—'} / Site : ${employe.nomSite || '—'}`
      : (employe.nomAgence || '—');
  }

  getFonctionBadgeColor(fonction: string): string {
    const colors: { [key: string]: string } = {
      'GESTIONNAIRE':          'bg-purple-100 text-purple-800',
      'AGENT_TERRAIN':         'bg-yellow-100 text-yellow-800',
      'CONTROLEUR':            'bg-blue-100 text-blue-800',
      'CAISSIER':              'bg-green-100 text-green-800',
      'CHEF_BUREAU':           'bg-indigo-100 text-indigo-800',
      'COO':                   'bg-red-100 text-red-800',
      'RCI':                   'bg-orange-100 text-orange-800',
      'GERANT_GENERAL':        'bg-red-100 text-red-800',
      'CHARGE_OPERATIONS':     'bg-teal-100 text-teal-800',
      'RESPONSABLE_CONTROLES': 'bg-cyan-100 text-cyan-800',
      'ADMINISTRATIF':         'bg-gray-100 text-gray-800',
      'AUTRE':                 'bg-gray-100 text-gray-600',
    };
    return colors[fonction] || 'bg-gray-100 text-gray-800';
  }

  getStatusColor(actif: boolean): string {
    return actif ? 'text-green-600' : 'text-red-600';
  }

  getStatusLabel(actif: boolean): string {
    return actif ? '● Actif' : '● Inactif';
  }

  openPaymentModal(employe: EmployeResponse): void {
    this.selectedEmploye = employe;
    this.paymentModalOpen = true;
  }

  closePaymentModal(): void {
    this.paymentModalOpen = false;
    this.selectedEmploye = null;
  }

  onPaymentSuccess(response: any): void {
    alert(`✓ Paiement confirmé pour ${this.selectedEmploye?.prenom} ${this.selectedEmploye?.nom}\nMontant: ${response.montant}$\nN° Opération Caisse: ${response.numeroOperationCaisse}`);
    this.closePaymentModal();
    this.loadEmployes(); // Refresh list
  }
}
