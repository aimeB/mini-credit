import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Transaction, Compte } from '../../models/transaction.model';

@Component({
  selector: 'app-historique-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './historique-transactions.component.html',
  styleUrls: ['./historique-transactions.component.css']
})
export class HistoriqueTransactionsComponent implements OnInit {
  activeTab: 'operations' | 'annexes' | 'extraits' = 'operations';
  searchExpanded: boolean = false;
  
  selectedCompte: Compte | null = null;
  comptes: Compte[] = [];
  transactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];
  
  // Critères de recherche
  searchCriteria = {
    montantMin: null as number | null,
    montantMax: null as number | null,
    dateDebut: null as string | null,
    dateFin: null as string | null,
    transaction: '' as string,
    beneficiaire: '' as string
  };

  ngOnInit(): void {
    this.initializeData();
    if (this.comptes.length > 0) {
      this.selectCompte(this.comptes[0]);
    }
  }

  /**
   * Initialise les données statiques
   */
  private initializeData(): void {
    // Comptes statiques
    this.comptes = [
      {
        id: '1',
        numero: 'BE87 0636 9677 6394',
        solde: 416.49,
        disponible: 174.68,
        titulaire: 'Jean Dupont',
        type: 'PAIEMENT',
        devise: 'EUR'
      },
      {
        id: '2',
        numero: 'BE91 0829 2666 5676',
        solde: 0.00,
        disponible: 0.00,
        titulaire: 'Jean Dupont',
        type: 'EPARGNE',
        devise: 'EUR'
      },
      {
        id: '3',
        numero: 'BE98 7506 7637 4593',
        solde: 0.00,
        disponible: 0.00,
        titulaire: 'Jean Dupont',
        type: 'PAIEMENT',
        devise: 'EUR'
      }
    ];

    // Transactions statiques
    this.transactions = [
      {
        id: 'T001',
        date: new Date('2026-05-16'),
        montant: -9.99,
        type: 'DEBIT',
        description: 'PAIEMENT DEBIT MASTERCARD',
        status: 'PENDING',
        compteContrepartie: 'GOOGLE*GOOGLE ONE DUBLIN',
        nomBeneficiaire: 'GOOGLE*GOOGLE ONE DUBLIN : 5169 2001 8115 9239 - 745756 - MCC5968',
        reference: 'MEETIC PARIS'
      },
      {
        id: 'T002',
        date: new Date('2026-05-19'),
        montant: -25.00,
        type: 'DEBIT',
        description: 'VIREMENT INSTANTANE BELFIUS MOBILE',
        status: 'COMPLETED',
        compteContrepartie: 'BE36 1030 1694 7281',
        nomBeneficiaire: 'M. Emmanuel Desirotte',
        reference: 'VERS BE36 1030 1694 7281 - 090541405i957 VAL. 18-05'
      },
      {
        id: 'T003',
        date: new Date('2026-05-18'),
        montant: -450.00,
        type: 'DEBIT',
        description: 'VIREMENT INSTANTANE BELFIUS MOBILE',
        status: 'COMPLETED',
        compteContrepartie: 'BE36 1030 1694 7281',
        nomBeneficiaire: 'M. Emmanuel Desirotte',
        reference: 'VERS BE36 1030 1694 7281 - 090541525i826 VAL. 18-05'
      },
      {
        id: 'T004',
        date: new Date('2026-05-18'),
        montant: -29.99,
        type: 'DEBIT',
        description: 'PAIEMENT MASTERCARD DEBIT VIA',
        status: 'COMPLETED',
        compteContrepartie: 'eCommerce 16/05',
        nomBeneficiaire: 'MEETIC PARIS FR 29,99 EUR CARTE N° 5169 2001 8115 9239',
        reference: ''
      },
      {
        id: 'T005',
        date: new Date('2026-05-17'),
        montant: 1500.00,
        type: 'CREDIT',
        description: 'VIREMENT RECU',
        status: 'COMPLETED',
        compteContrepartie: 'BE12 3456 7890 1234',
        nomBeneficiaire: 'Employeur SARL',
        reference: 'Salaire mai 2026'
      },
      {
        id: 'T006',
        date: new Date('2026-05-15'),
        montant: -85.50,
        type: 'DEBIT',
        description: 'PAIEMENT PAR CARTE',
        status: 'COMPLETED',
        compteContrepartie: 'CARREFOUR',
        nomBeneficiaire: 'Carrefour Hypermarché',
        reference: 'Shopping'
      }
    ];
  }

  /**
   * Sélectionne un compte
   */
  selectCompte(compte: Compte): void {
    this.selectedCompte = compte;
    this.applyFilters();
  }

  /**
   * Change l'onglet actif
   */
  changeTab(tab: 'operations' | 'annexes' | 'extraits'): void {
    this.activeTab = tab;
  }

  /**
   * Applique les filtres aux transactions
   */
  applyFilters(): void {
    let filtered = this.transactions;

    if (this.searchCriteria.montantMin !== null) {
      filtered = filtered.filter(t => Math.abs(t.montant) >= this.searchCriteria.montantMin!);
    }

    if (this.searchCriteria.montantMax !== null) {
      filtered = filtered.filter(t => Math.abs(t.montant) <= this.searchCriteria.montantMax!);
    }

    if (this.searchCriteria.dateDebut) {
      const dateDebut = new Date(this.searchCriteria.dateDebut);
      filtered = filtered.filter(t => t.date >= dateDebut);
    }

    if (this.searchCriteria.dateFin) {
      const dateFin = new Date(this.searchCriteria.dateFin);
      filtered = filtered.filter(t => t.date <= dateFin);
    }

    this.filteredTransactions = filtered.sort((a, b) => b.date.getTime() - a.date.getTime());
  }

  /**
   * Réinitialise les filtres
   */
  resetFilters(): void {
    this.searchCriteria = {
      montantMin: null,
      montantMax: null,
      dateDebut: null,
      dateFin: null,
      transaction: '',
      beneficiaire: ''
    };
    this.applyFilters();
  }

  /**
   * Formate la date au format DD/MM/YYYY
   */
  formatDate(date: Date): string {
    const d = new Date(date);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  }

  /**
   * Retourne la classe CSS pour le statut
   */
  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'status-completed';
      case 'PENDING':
        return 'status-pending';
      case 'FAILED':
        return 'status-failed';
      default:
        return '';
    }
  }

  /**
   * Formate le montant avec devise
   */
  formatMontant(montant: number): string {
    const sign = montant >= 0 ? '+' : '';
    return `${sign}${montant.toFixed(2)}`;
  }
}
