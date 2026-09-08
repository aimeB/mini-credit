import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { catchError, forkJoin, map, of } from 'rxjs';
import { OperationEpargneService } from '../../services/operation-epargne.service';
import { OperationEpargneResponse } from '../../models/operation-epargne-response';
import { TicketRecuResponse } from '../../models/ticket-recu.model';
import { TicketRecuService } from '../../services/ticket-recu.service';

@Component({
  selector: 'app-operation-epargne-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './operation-epargne-list.component.html'
})
export class OperationEpargneListComponent implements OnChanges {
  @Input() compteId!: number;

  private operationEpargneService = inject(OperationEpargneService);
  private ticketRecuService = inject(TicketRecuService);

  operations: OperationEpargneResponse[] = [];
  retraitTicketsByOperation: Record<number, TicketRecuResponse | null> = {};
  loading = false;
  errorMessage = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['compteId'] && this.compteId) {
      this.loadOperations();
    }
  }

  loadOperations(): void {
    this.loading = true;
    this.errorMessage = '';

    this.operationEpargneService.getByCompte(this.compteId).subscribe({
      next: (data) => {
        this.operations = data;
        this.loadRetraitTicketDetails(data);
        this.loading = false;
      },
      error: (err) => {
        this.operations = [];
        this.loading = false;
        this.errorMessage =
          err?.error?.message || 'Erreur lors du chargement des opérations épargne';
      }
    });
  }

  hasCommissionDetails(operation: OperationEpargneResponse): boolean {
    const ticket = this.retraitTicketsByOperation[operation.id];
    return operation.typeOperation === 'RETRAIT' && Number(ticket?.montantCommission ?? 0) > 0;
  }

  private loadRetraitTicketDetails(operations: OperationEpargneResponse[]): void {
    this.retraitTicketsByOperation = {};
    const retraits = operations.filter(operation => operation.typeOperation === 'RETRAIT');
    if (retraits.length === 0) {
      return;
    }

    const requests = retraits.map(operation =>
      this.ticketRecuService.getByOperationEpargne(operation.id).pipe(
        map(tickets => tickets.find(ticket => ticket.typeTicket === 'RETRAIT_EPARGNE') ?? null),
        catchError(() => of(null))
      )
    );

    forkJoin(requests).subscribe(tickets => {
      retraits.forEach((operation, index) => {
        this.retraitTicketsByOperation[operation.id] = tickets[index];
      });
    });
  }
}