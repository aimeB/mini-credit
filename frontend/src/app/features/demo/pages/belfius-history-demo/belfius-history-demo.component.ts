import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface OperationDemo {
  date: string;
  montant: string;
  badge?: string;
  transaction: string;
  contrepartie: string;
}

@Component({
  selector: 'app-belfius-history-demo',
  imports: [CommonModule],
  templateUrl: './belfius-history-demo.component.html',
  styleUrl: './belfius-history-demo.component.scss'
})
export class BelfiusHistoryDemoComponent {
  operations: OperationDemo[] = [
    {
      date: '16/05/2026',
      montant: '-9,99 EUR',
      badge: 'en traitement',
      transaction: 'PAIEMENT DEBIT MASTERCARD GOOGLE*GOOGLE ONE DUBLIN : 5169 2001 8115 9239 - 745756 - MCC5968',
      contrepartie: 'GOOGLE*GOOGLE ONE DUBLIN'
    },
    {
      date: '19/05/2026',
      montant: '-25,00 EUR',
      transaction: 'VIREMENT INSTANTANE BELFIUS MOBILE VERS BE36 1030 1694 7281 M. Emmanuel Desirotte REF. : 090541405I957 VAL. 18-05',
      contrepartie: 'BE36 1030 1694 7281\nM. Emmanuel Desirotte'
    },
    {
      date: '18/05/2026',
      montant: '-450,00 EUR',
      transaction: 'VIREMENT INSTANTANE BELFIUS MOBILE VERS BE36 1030 1694 7281 M. Emmanuel Desirotte REF. : 090541525I826 VAL. 18-05',
      contrepartie: 'BE36 1030 1694 7281\nM. Emmanuel Desirotte'
    },
    {
      date: '18/05/2026',
      montant: '-29,99 EUR',
      transaction: 'PAIEMENT MASTERCARD DEBIT VIA eCommerce 16/05 MEETIC PARIS FR 29,99 EUR CARTE N° 5169 2001 8115 9239',
      contrepartie: ''
    }
  ];
}
