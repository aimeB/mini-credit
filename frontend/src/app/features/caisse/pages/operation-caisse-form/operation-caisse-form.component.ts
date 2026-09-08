import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';

import { OperationCaisseService } from '../../services/operation-caisse.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import {
  SourceOperationCaisse,
  SOURCE_OPERATION_CAISSE_LABELS,
} from '../../models/source-operation-caisse';
import { CategorieOperationCaisse } from '../../models/categorie-operation-caisse';
import { TypeOperationCaisse } from '../../models/type-operation-caisse';
import { ModePaiement } from '../../../epargne/models/mode-paiement';
import { OperationCaisseRequest } from '../../models/operation-caisse-request';
import { NatureFinancementApprovisionnement, NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS } from '../../models/nature-financement-approvisionnement';

interface CategorieOption {
  value: CategorieOperationCaisse;
  label: string;
}

/**
 * PATCH 7 — Formulaire de saisie manuelle d'une opération caisse.
 *
 * Route : /caisses/session/:sessionId/operations/nouveau
 *
 * Sources autorisées depuis ce formulaire :
 *   - MANUEL (défaut) — entrée/sortie manuelle (frais, dépenses, cotisation, etc.)
 *   - APPROVISIONNEMENT — alimentation externe de la caisse
 *   - AJUSTEMENT — correction/contrepassation d'une opération erronée
 *   - AUTRE — cas non classés
 *
 * Sources gérées exclusivement par le backend (non sélectionnables ici) :
 *   RECETTE_JOURNALIERE, RETRAIT_EPARGNE, CREDIT_DECAISSEMENT, CREDIT_REMBOURSEMENT
 */
@Component({
  selector: 'app-operation-caisse-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './operation-caisse-form.component.html',
  styleUrls: ['./operation-caisse-form.component.css'],
})
export class OperationCaisseFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly operationCaisseService = inject(OperationCaisseService);
  private readonly sessionCaisseService = inject(SessionCaisseService);

  sessionId!: number;
  session: SessionCaisseResponse | null = null;

  loading = false;
  loadingSession = false;
  error = '';

  readonly typeOptions: TypeOperationCaisse[] = ['ENTREE', 'SORTIE'];

  readonly entreeCategories: CategorieOption[] = [
    { value: 'APPROVISIONNEMENT', label: 'Approvisionnement' },
    { value: 'ENTREE_DIVERSE', label: 'Recette diverse' },
    { value: 'COTISATION', label: 'Retour terrain manuel' },
    { value: 'EPARGNE', label: 'Autre entrée' },
  ];

  readonly sortieCategories: CategorieOption[] = [
    { value: 'DEPENSE', label: 'Dépense caisse' },
    { value: 'DECAISSEMENT_CREDIT', label: 'Décaissement' },
    { value: 'RETRAIT_EPARGNE', label: 'Retrait payé' },
    { value: 'SORTIE_DIVERSE', label: 'Autre sortie' },
  ];

  readonly modePaiementOptions: ModePaiement[] = [
    'ESPECES',
    'MOBILE_MONEY',
    'VIREMENT',
    'CARTE',
    'AUTRE',
  ];

  readonly SourceOperationCaisse = SourceOperationCaisse;
  readonly natureFinancementOptions: Array<{ value: NatureFinancementApprovisionnement; label: string }> = [
    { value: 'TRANSFERT_INTERNE', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.TRANSFERT_INTERNE },
    { value: 'APPORT_PROPRIETAIRE', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.APPORT_PROPRIETAIRE },
    { value: 'PRET_RECU', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.PRET_RECU },
    { value: 'REMBOURSEMENT_AVANCE', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.REMBOURSEMENT_AVANCE },
    { value: 'AUTRE_FINANCEMENT', label: NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS.AUTRE_FINANCEMENT },
  ];

  form = this.fb.group({
    typeOperation:    this.fb.control<TypeOperationCaisse | null>('ENTREE', Validators.required),
    categorieOperation: this.fb.control<CategorieOperationCaisse | null>('APPROVISIONNEMENT', Validators.required),
    natureFinancement: this.fb.control<NatureFinancementApprovisionnement | null>(null),
    montant:          this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    source:           this.fb.control<SourceOperationCaisse>(SourceOperationCaisse.MANUEL, { nonNullable: true }),
    referenceExterne: this.fb.control<string | null>(''),
    modePaiement:     this.fb.control<ModePaiement | null>('ESPECES', Validators.required),
    motif:            this.fb.control<string | null>('', Validators.required),
    observation:      this.fb.control<string | null>(''),
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('sessionId'));
    if (!id) {
      this.error = 'Identifiant de session invalide.';
      return;
    }
    this.sessionId = id;
    this.form.get('typeOperation')?.valueChanges.subscribe((type) => {
      const categorieControl = this.form.get('categorieOperation');
      const currentValue = categorieControl?.value;
      const isValid = this.currentCategorieOptions.some((opt) => opt.value === currentValue);
      if (!isValid) {
        categorieControl?.setValue(this.currentCategorieOptions[0]?.value ?? null);
      }
      this.updateApprovisionnementValidators();
    });
    this.form.get('categorieOperation')?.valueChanges.subscribe(() => this.updateApprovisionnementValidators());
    this.form.get('natureFinancement')?.valueChanges.subscribe(() => this.updateApprovisionnementValidators());
    this.updateApprovisionnementValidators();
    this.loadSession();
  }

  private loadSession(): void {
    this.loadingSession = true;
    this.sessionCaisseService.getById(this.sessionId).pipe(
      finalize(() => { this.loadingSession = false; })
    ).subscribe({
      next: (s) => {
        if (s.statut !== 'OUVERTE') {
          this.error = `La session #${this.sessionId} n'est pas ouverte (statut : ${s.statut}). Impossible de saisir une opération.`;
        } else if (!this.isSessionDuJour(s)) {
          this.error = `La session #${this.sessionId} a une date comptable ancienne (${s.dateComptable}). Aucune opération n'est autorisée hors session du jour.`;
        }
        this.session = s;
      },
      error: () => {
        this.error = 'Impossible de charger la session caisse.';
      }
    });
  }

  get sourceValue(): SourceOperationCaisse {
    return this.form.get('source')!.value as SourceOperationCaisse;
  }

  get sourceDisplayLabel(): string {
    return this.isApprovisionnement
      ? SOURCE_OPERATION_CAISSE_LABELS[SourceOperationCaisse.APPROVISIONNEMENT]
      : SOURCE_OPERATION_CAISSE_LABELS[SourceOperationCaisse.MANUEL];
  }

  get devise(): string {
    const value = this.session?.devise?.trim();
    return value || 'CDF';
  }

  get caisseDisplayName(): string {
    if (!this.session) {
      return 'Caisse';
    }
    const site = this.session.siteNom?.trim();
    return site ? `Caisse ${site}` : 'Caisse';
  }

  get agenceDisplayName(): string {
    return this.session?.antenneNom?.trim() || 'Agence non renseignée';
  }

  get currentCategorieOptions(): CategorieOption[] {
    return this.isSortie ? this.sortieCategories : this.entreeCategories;
  }

  get isSortie(): boolean {
    return this.form.get('typeOperation')?.value === 'SORTIE';
  }

  get isApprovisionnement(): boolean {
    return this.form.get('categorieOperation')?.value === 'APPROVISIONNEMENT';
  }

  get requiresFinancementComment(): boolean {
    const nature = this.form.get('natureFinancement')?.value;
    return this.isApprovisionnement && (nature === 'PRET_RECU' || nature === 'AUTRE_FINANCEMENT');
  }

  get soldeActuel(): number {
    return this.session?.soldeTheorique ?? 0;
  }

  get montantValue(): number {
    const raw = this.form.get('montant')?.value;
    return typeof raw === 'number' ? raw : Number(raw ?? 0);
  }

  get operationLabel(): string {
    return this.isSortie ? 'Sortie' : 'Entrée';
  }

  get soldeApresOperation(): number {
    const delta = this.isSortie ? -this.montantValue : this.montantValue;
    return this.soldeActuel + (Number.isFinite(delta) ? delta : 0);
  }

  get hasSoldeInsuffisant(): boolean {
    return this.isSortie && this.montantValue > 0 && this.montantValue > this.soldeActuel;
  }

  get canSubmit(): boolean {
    return !!this.session
      && this.session.statut === 'OUVERTE'
      && this.isSessionDuJour(this.session)
      && !this.loading
      && this.form.valid
      && !this.hasSoldeInsuffisant;
  }

  get disabledSubmitReason(): string {
    if (!this.session || this.session.statut !== 'OUVERTE') {
      return 'Session non ouverte: enregistrement impossible.';
    }
    if (!this.isSessionDuJour(this.session)) {
      return 'La session doit être celle du jour pour saisir une opération.';
    }
    if (!this.form.get('montant')?.value || this.form.get('montant')?.invalid) {
      return 'Renseignez le montant et le motif pour enregistrer l’opération.';
    }
    if (!this.form.get('categorieOperation')?.value) {
      return 'Sélectionnez une catégorie pour poursuivre.';
    }
    if (!this.form.get('motif')?.value?.trim()) {
      return 'Renseignez le montant et le motif pour enregistrer l’opération.';
    }
    if (this.isApprovisionnement && !this.form.get('natureFinancement')?.value) {
      return 'Sélectionnez la nature du financement pour cet approvisionnement.';
    }
    if (this.requiresFinancementComment && !this.form.get('observation')?.value?.trim()) {
      return 'Un commentaire est obligatoire pour un prêt reçu ou un autre financement.';
    }
    if (!this.form.get('modePaiement')?.value) {
      return 'Choisissez le mode de paiement.';
    }
    if (this.hasSoldeInsuffisant) {
      return 'Solde insuffisant pour cette sortie.';
    }
    return '';
  }

  formatMontant(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value || 0);
  }

  submit(): void {
    if (!this.canSubmit || !this.session) {
      this.form.markAllAsTouched();
      this.error = this.disabledSubmitReason;
      return;
    }

    const v = this.form.value;

    const payload: OperationCaisseRequest = {
      sessionCaisseId: this.sessionId,
      caisseId:        this.session.caisseId,
      dateOperation:   new Date().toISOString(),
      typeOperation:   v.typeOperation as TypeOperationCaisse,
      categorieOperation: v.categorieOperation as CategorieOperationCaisse,
      natureFinancement: this.isApprovisionnement ? v.natureFinancement as NatureFinancementApprovisionnement : undefined,
      montant:         v.montant as number,
      source: this.isApprovisionnement ? SourceOperationCaisse.APPROVISIONNEMENT : SourceOperationCaisse.MANUEL,
      referenceExterne: v.referenceExterne?.trim() || undefined,
      modePaiement:    v.modePaiement ?? undefined,
      description:     v.motif?.trim() || undefined,
      observation:     v.observation?.trim() || undefined,
    };

    this.loading = true;
    this.error = '';

    this.operationCaisseService.enregistrer(payload).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: () => {
        this.router.navigate(['/caisses/session', this.sessionId, 'operations']);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur lors de l\'enregistrement de l\'opération.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/caisses/session', this.sessionId]);
  }

  private isSessionDuJour(session: SessionCaisseResponse): boolean {
    return !!session.dateComptable && session.dateComptable === new Date().toISOString().slice(0, 10);
  }

  private updateApprovisionnementValidators(): void {
    const natureControl = this.form.get('natureFinancement');
    const observationControl = this.form.get('observation');

    if (this.isApprovisionnement) {
      natureControl?.setValidators(Validators.required);
    } else {
      natureControl?.clearValidators();
      natureControl?.setValue(null, { emitEvent: false });
    }

    if (this.requiresFinancementComment) {
      observationControl?.setValidators(Validators.required);
    } else {
      observationControl?.clearValidators();
    }

    natureControl?.updateValueAndValidity({ emitEvent: false });
    observationControl?.updateValueAndValidity({ emitEvent: false });
  }
}
