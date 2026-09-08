import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { OperationEpargneService } from '../../services/operation-epargne.service';
import { ModePaiement } from '../../models/mode-paiement';
import { TypeOperationEpargne } from '../../models/operation-epargne-response';
import { SessionCaisseService } from '../../../caisse/services/session-caisse.service';
import { SessionCaisseResponse } from '../../../caisse/models/session-caisse-response';
import { AuthService } from '../../../../core/services/auth.service';
import { TicketRecuService } from '../../services/ticket-recu.service';
import { TicketRecuResponse } from '../../models/ticket-recu.model';

@Component({
  selector: 'app-operation-epargne-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './operation-epargne-form.component.html'
})
export class OperationEpargneFormComponent implements OnInit {
  @Input() membreId!: number;
  @Input() compteId!: number;
  @Output() saved = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private operationService = inject(OperationEpargneService);
  private ticketRecuService = inject(TicketRecuService);
  private sessionCaisseService = inject(SessionCaisseService);
  private authService = inject(AuthService);

  loading = false;
  errorMessage = '';
  successMessage = '';
  ticketRecu: TicketRecuResponse | null = null;
  ticketLoading = false;

  sessionCaisseActive: SessionCaisseResponse | null = null;
  erreurSession = '';

  private readonly allOperations: TypeOperationEpargne[] = [
    'COTISATION',
    'EPARGNE',
    'RETRAIT',
    'BLOCAGE_GARANTIE',
    'DEBLOCAGE_GARANTIE',
    'AJUSTEMENT'
  ];

  operations: TypeOperationEpargne[] = [...this.allOperations];

  modesPaiement: ModePaiement[] = [
    'ESPECES',
    'MOBILE_MONEY',
    'VIREMENT',
    'CARTE',
    'AUTRE'
  ];

  sensOptions: ('ENTREE' | 'SORTIE')[] = ['ENTREE', 'SORTIE'];

  form = this.fb.group({
    typeOperation: this.fb.control<TypeOperationEpargne | null>(null, Validators.required),
    montant: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    sens: this.fb.control<'ENTREE' | 'SORTIE' | null>(null),
    modePaiement: this.fb.control<ModePaiement | null>(null),
    referenceExterne: this.fb.control<string | null>(''),
    observation: this.fb.control<string | null>('')
  });

  ngOnInit(): void {
    this.refreshOperations();

    this.chargerSessionActive();

    this.form.get('typeOperation')?.valueChanges.subscribe(type => {
      const sensControl = this.form.get('sens');

      if (type === 'AJUSTEMENT') {
        sensControl?.setValidators([Validators.required]);
      } else {
        sensControl?.clearValidators();
        sensControl?.setValue(null);
      }

      sensControl?.updateValueAndValidity();
    });
  }

  chargerSessionActive(): void {
    this.sessionCaisseService.getSessionActive().subscribe({
      next: (session) => {
        this.sessionCaisseActive = session;
      },
      error: (err) => {
        console.error(err);
        this.sessionCaisseActive = null;
        this.erreurSession = "Aucune session de caisse ouverte.";
      }
    });
  }

  submit(): void {
    if (this.form.invalid || !this.compteId || !this.membreId) {
      this.form.markAllAsTouched();
      return;
    }

    const type = this.form.value.typeOperation;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.ticketRecu = null;

    this.operationService.enregistrer({
      compteEpargneId: this.compteId,
      membreId: this.membreId,
      dateOperation: new Date().toISOString(),
      typeOperation: type!,
      montant: Number(this.form.value.montant),
      sens: this.form.value.sens ?? undefined,
      modePaiement: this.form.value.modePaiement ?? undefined,
      referenceExterne: this.form.value.referenceExterne || undefined,
      observation: this.form.value.observation || undefined,
      agentId: undefined,
      sessionCaisseId: this.sessionCaisseActive?.id ?? undefined,
      createdBy: undefined
    }).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (operation) => {
        this.form.reset();
        this.successMessage = 'Opération enregistrée. Reçu généré si l’opération est éligible.';
        this.chargerTicketOperation(operation.id);
        this.saved.emit();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Erreur lors de l’enregistrement';
      }
    });
  }

  canPrintTicket(): boolean {
    return this.authService.hasPermission('TICKET_RECU_PRINT');
  }

  imprimerTicket(): void {
    if (!this.ticketRecu) {
      return;
    }

    this.ticketRecuService.getPrintableHtml(this.ticketRecu.id).subscribe({
      next: html => {
        this.openPrintableHtml(html);
        this.ticketRecuService.marquerImpression(this.ticketRecu!.id).subscribe({
          next: ticket => this.ticketRecu = ticket,
          error: err => this.errorMessage = err?.error?.message || err?.message || 'Erreur lors du marquage impression'
        });
      },
      error: err => {
        this.errorMessage = err?.error?.message || err?.message || 'Erreur lors de la préparation du ticket';
      }
    });
  }

  private openPrintableHtml(html: string): void {
    const printWindow = window.open('', '_blank', 'noopener,noreferrer');
    if (!printWindow) {
      this.errorMessage = 'Impossible d’ouvrir la fenêtre d’impression.';
      return;
    }
    printWindow.document.open();
    printWindow.document.write(html);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
  }

  private chargerTicketOperation(operationId: number): void {
    if (!operationId) {
      return;
    }

    this.ticketLoading = true;
    this.ticketRecuService.getByOperationEpargne(operationId).pipe(
      finalize(() => this.ticketLoading = false)
    ).subscribe({
      next: tickets => {
        this.ticketRecu = tickets.find(ticket => ticket.typeTicket !== 'DUPLICATA') ?? tickets[0] ?? null;
      },
      error: () => {
        this.ticketRecu = null;
      }
    });
  }

  private refreshOperations(): void {
    if (this.authService.hasRole('AGENT_TERRAIN')) {
      this.operations = ['COTISATION', 'EPARGNE'];
    } else {
      this.operations = [...this.allOperations];
    }

    const selectedOperation = this.form.value.typeOperation;
    if (selectedOperation && !this.operations.includes(selectedOperation)) {
      this.form.get('typeOperation')?.setValue(null);
    }
  }
}