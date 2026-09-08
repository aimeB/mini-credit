import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PaiementSalaireService } from '../../services/paiement-salaire.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-paiement-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './paiement-modal.component.html',
  styleUrls: ['./paiement-modal.component.css']
})
export class PaiementModalComponent implements OnInit, OnDestroy {
  @Input() isOpen = false;
  @Input() employee: any = null;
  @Output() close = new EventEmitter<void>();
  @Output() success = new EventEmitter<any>();

  form!: FormGroup;
  loading = false;
  error: string | null = null;
  submitted = false;

  modePaiementOptions = [
    { label: 'Espèces', value: 'ESPECES' },
    { label: 'Mobile Money', value: 'MOBILE_MONEY' },
    { label: 'Carte Bancaire', value: 'CARTE' },
    { label: 'Virement', value: 'VIREMENT' },
    { label: 'Autre', value: 'AUTRE' }
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private paiementService: PaiementSalaireService
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {}

  private initializeForm(): void {
    this.form = this.fb.group({
      employeId: [0, Validators.required],
      employeNom: [{ value: '', disabled: true }],
      employeMatricule: [{ value: '', disabled: true }],
      montantTotal: [{ value: 0, disabled: true }],
      datePaiement: [this.getTodayDate(), Validators.required],
      montant: [0, [Validators.required, Validators.min(0.01)]],
      modePaiement: ['ESPECES', Validators.required],
      notes: [''],
      referenceExterne: ['']
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setEmployeeData(employee: any): void {
    if (!employee) return;
    
    const totalRemuneration = employee.salaireBase + (employee.primeFixe || 0) + (employee.bonusVariable || 0);
    
    this.form.patchValue({
      employeId: employee.id,
      employeNom: employee.prenom + ' ' + employee.nom,
      employeMatricule: employee.matricule,
      montantTotal: totalRemuneration,
      montant: totalRemuneration
    });
    
    this.employee = employee;
  }

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = null;

    if (this.form.invalid) {
      return;
    }

    this.loading = true;

    const payload = {
      employeId: this.f['employeId'].value,
      datePaiement: this.f['datePaiement'].value,
      montant: this.f['montant'].value,
      modePaiement: this.f['modePaiement'].value,
      notes: this.f['notes'].value || undefined,
      referenceExterne: this.f['referenceExterne'].value || undefined
    };

    this.paiementService.create(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.success.emit(response);
          this.closeModal();
        },
        error: (error) => {
          this.loading = false;
          this.error = error.error?.message || 'Une erreur est survenue lors du paiement';
          console.error('Payment error:', error);
        }
      });
  }

  private getTodayDate(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  closeModal(): void {
    this.close.emit();
    this.submitted = false;
    this.error = null;
    this.initializeForm();
  }
}
