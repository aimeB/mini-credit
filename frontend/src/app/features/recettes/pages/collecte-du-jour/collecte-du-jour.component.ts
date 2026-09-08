import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { MembreService } from '../../../membres/services/membre.service';
import { MembreResponse } from '../../../membres/models/membre-response';
import { AuthService } from '../../../../core/services/auth.service';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { CompteEpargneService } from '../../../epargne/services/compte-epargne.service';
import { CreditService } from '../../../credit/services/credit.service';
import { ParametresMetierService } from '../../../../core/services/parametres-metier.service';
import { DUREE_UNITE_OPTIONS, DureeUnite } from '../../../../shared/enums/duree-unite.enum';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  CollecteRecapResponse,
  CollecteTerrainResponse,
  TypeLigneCollecte,
  ModaliteRemboursementCollecte,
  CreateCollecteMembreLigneRequest,
} from '../../models/collecte-terrain.model';
import { StatutCredit } from '../../../credit/models/enums/statut-credit.enum';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-collecte-du-jour',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, WorkflowGuidanceBannerComponent],
  templateUrl: './collecte-du-jour.component.html',
  styleUrls: ['./collecte-du-jour.component.css'],
})
export class CollecteDuJourComponent implements OnInit {
  private collecteService = inject(CollecteTerrainService);
  private route = inject(ActivatedRoute);
  private membreService = inject(MembreService);
  private authService = inject(AuthService);
  private compteEpargneService = inject(CompteEpargneService);
  private creditService = inject(CreditService);
  private parametresMetierService = inject(ParametresMetierService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  loading = false;
  addingLine = false;
  submittingCollecte = false;
  savingDraft = false;

  mode: 'today' | 'edition' = 'today';
  editionCollecteId?: number;

  uiError = '';
  uiSuccess = '';
  operationHint = '';

  membres: MembreResponse[] = [];
  filteredMembres: MembreResponse[] = [];
  selectedMember?: MembreResponse;
  collecte?: CollecteTerrainResponse;
  recap?: CollecteRecapResponse;
  editingLineId?: number;

  hasActiveCompteEpargne = false;
  hasActiveCredit = false;
  activeCreditCount = 0;
  blockingCreditCount = 0;
  globalGuidance: WorkflowGuidance = {
    title: 'Collecte journalière terrain',
    message: 'Cette page sert à saisir la collecte terrain du jour: membres visités, épargne, remboursements, carnets et demandes de crédit, puis les espèces remises et les observations. La saisie doit rester cohérente avec le billetage et le contrôle avant toute validation de la collecte.',
    currentStep: 'Saisie terrain',
    nextStep: 'Soumission de la collecte',
    expectedRole: 'Agent Terrain',
    expectedAction: 'Compléter la collecte du jour',
    severity: 'info',
    canCurrentUserAct: true
  };

  readonly dureeUniteOptions = DUREE_UNITE_OPTIONS;
  readonly modaliteOptions: ModaliteRemboursementCollecte[] = ['JOURNALIERE', 'HEBDOMADAIRE', 'MENSUELLE'];

  carnetPrix = 1000;

  typeOptions: TypeLigneCollecte[] = [
    'EPARGNE',
    'REMBOURSEMENT_CREDIT',
    'CARNET',
    'DEMANDE_CREDIT',
  ];

  get availableTypeOptions(): TypeLigneCollecte[] {
    return this.typeOptions.filter((type) => type !== 'DEMANDE_CREDIT' || this.blockingCreditCount === 0);
  }

  searchControl = this.fb.control('');

  ligneForm = this.fb.group({
    membreId: [null as number | null, Validators.required],
    typeLigne: ['EPARGNE' as TypeLigneCollecte, Validators.required],
    montant: [null as number | null],
    reference: [''],
    commentaire: [''],
    montantSouhaite: [null as number | null],
    objetCredit: [''],
    gagePropose: [''],
    dureeValeur: [1 as number | null],
    dureeUnite: ['MOIS' as DureeUnite],
    modaliteRemboursement: ['MENSUELLE' as ModaliteRemboursementCollecte],
  });

  remiseForm = this.fb.group({
    especesRemises: [0, [Validators.required, Validators.min(0)]],
    observations: [''],
  });

  get currentUser() {
    return this.authService.getCurrentUser();
  }

  get backendError(): string {
    return this.uiError;
  }

  get selectedMembre(): MembreResponse | undefined {
    return this.selectedMember;
  }

  get lineForm() {
    return this.ligneForm;
  }

  get selectedType(): TypeLigneCollecte {
    return this.ligneForm.get('typeLigne')?.value as TypeLigneCollecte;
  }

  get isBrouillon(): boolean {
    return this.collecte?.statut === 'BROUILLON';
  }

  get isEditionMode(): boolean {
    return this.mode === 'edition';
  }

  get isTodayMode(): boolean {
    return this.mode === 'today';
  }

  get showEditionPastDateWarning(): boolean {
    if (!this.isEditionMode || !this.collecte?.dateCollecte) {
      return false;
    }

    const collecteDate = new Date(this.collecte.dateCollecte);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const currentCollecteDate = new Date(collecteDate.getFullYear(), collecteDate.getMonth(), collecteDate.getDate());
    return currentCollecteDate.getTime() < today.getTime();
  }

  get ecartCourant(): number {
    const total = Number(this.collecte?.totalGeneralCalcule || 0);
    const remise = Number(this.remiseForm.get('especesRemises')?.value || 0);
    return remise - total;
  }

  get isObservationRequired(): boolean {
    return this.ecartCourant !== 0;
  }

  get canAddLine(): boolean {
    return !!this.collecte
      && !!this.selectedMember
      && this.isBrouillon
      && this.isSelectedTypeAllowed()
      && this.ligneForm.valid
      && !this.addingLine;
  }

  get canSubmit(): boolean {
    return !!this.collecte && this.isBrouillon && !this.submittingCollecte;
  }

  get submitting(): boolean {
    return this.submittingCollecte;
  }

  get isDraft(): boolean {
    return this.isBrouillon;
  }

  get ecartTresorerieCalcule(): number {
    return this.ecartCourant;
  }

  get membresVisitesCount(): number {
    if (!this.collecte?.lignes?.length) {
      return 0;
    }

    return new Set(this.collecte.lignes.map((ligne) => ligne.membreId)).size;
  }

  get totalCarnetsFraisResume(): number {
    const collecte = this.collecte;
    if (!collecte) {
      return 0;
    }

    if (collecte.totalFraisCalcule !== undefined && collecte.totalFraisCalcule !== null) {
      return Number(collecte.totalFraisCalcule);
    }

    return (collecte.lignes || [])
      .filter((ligne) => ligne.typeLigne === 'CARNET' || ligne.typeLigne === 'FRAIS_ANALYSE')
      .reduce((sum, ligne) => sum + this.lineTotal(ligne), 0);
  }

  get totalGeneralAttenduResume(): number {
    const collecte = this.collecte;
    if (!collecte) {
      return 0;
    }

    if (collecte.totalGeneralCalcule !== undefined && collecte.totalGeneralCalcule !== null) {
      return Number(collecte.totalGeneralCalcule);
    }

    return Number(collecte.totalEpargneCalcule || 0)
      + Number(collecte.totalRemboursementsCalcule || 0)
      + this.totalCarnetsFraisResume;
  }

  ngOnInit(): void {
    const routeId = Number(this.route.snapshot.paramMap.get('id'));
    if (routeId > 0) {
      this.mode = 'edition';
      this.editionCollecteId = routeId;
    }

    this.carnetPrix = this.parametresMetierService.getDecimal('PRIX_CARNET') || 1000;
    this.configureLineTypeRules(this.selectedType);

    this.ligneForm.get('typeLigne')?.valueChanges.subscribe((type) => {
      this.configureLineTypeRules((type as TypeLigneCollecte) || 'EPARGNE');
    });

    this.loadInitial();

    this.searchControl.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged())
      .subscribe((q) => this.searchMembres(q || ''));
  }

  private loadInitial(): void {
    this.loading = true;
    this.uiError = '';

    if (this.isEditionMode && this.editionCollecteId) {
      this.collecteService.getById(this.editionCollecteId).subscribe({
        next: (existingCollecte) => {
          this.collecte = existingCollecte;
          this.remiseForm.patchValue({
            especesRemises: existingCollecte.especesDeclareesAgent ?? existingCollecte.especesRemises,
            observations: existingCollecte.observations || '',
          });
          this.searchMembres('');
          this.loadRecap(existingCollecte.id);
        },
        error: (error) => {
          this.loading = false;
          this.showError(error, 'Impossible de charger cette collecte en édition.');
        },
      });
      return;
    }

    this.collecteService.getToday().subscribe({
      next: (today) => {
        if (today) {
          this.collecte = today;
          this.remiseForm.patchValue({
            especesRemises: today.especesDeclareesAgent ?? today.especesRemises,
            observations: today.observations || '',
          });
          this.searchMembres('');
          this.loadRecap(today.id);
          return;
        }

        this.collecteService.create({}).subscribe({
          next: (created) => {
            this.collecte = created;
            this.searchMembres('');
            this.loadRecap(created.id);
          },
          error: (error) => {
            this.loading = false;
            this.showError(error, 'Impossible de préparer Ma collecte du jour.');
          },
        });
      },
      error: (error) => {
        this.loading = false;
        this.showError(error, 'Impossible de charger Ma collecte du jour.');
      },
    });
  }

  private searchMembres(query: string): void {
    this.membreService.searchPaginated(query, undefined, 0, 20).subscribe({
      next: (page) => {
        this.membres = page?.content || [];
        this.filteredMembres = [...this.membres];
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.showError(error, 'Impossible de charger les membres.');
      },
    });
  }

  selectMember(membre: MembreResponse): void {
    this.selectedMember = membre;
    this.ligneForm.patchValue({ membreId: membre.id });
    this.clearOperationFields();
    this.loadMemberAutomaticInfos(membre.id);
    this.uiError = '';
    this.uiSuccess = '';
  }

  private loadMemberAutomaticInfos(membreId: number): void {
    this.hasActiveCompteEpargne = false;
    this.hasActiveCredit = false;
    this.activeCreditCount = 0;
    this.blockingCreditCount = 0;

    forkJoin({
      comptes: this.compteEpargneService.getByMembre(membreId),
      credits: this.creditService.getByMembre(membreId),
    }).subscribe({
      next: ({ comptes, credits }) => {
        this.hasActiveCompteEpargne = (comptes || []).some((value) => value.statut === 'ACTIF');
        const activeCredits = (credits || []).filter((value) =>
          value.statut === 'DECAISSE' || value.statut === 'EN_COURS' || value.statut === 'EN_RETARD'
        );
        const blockingCredits = (credits || []).filter((value) =>
          value.statut === 'APPROUVE'
          || value.statut === 'DECAISSE'
          || value.statut === 'EN_COURS'
          || value.statut === 'EN_RETARD'
          || value.statut === 'CONTENTIEUX'
        );
        this.activeCreditCount = activeCredits.length;
        this.hasActiveCredit = this.activeCreditCount === 1;
        this.blockingCreditCount = blockingCredits.length;

        if (this.selectedType === 'REMBOURSEMENT_CREDIT' && !this.isSelectedTypeAllowed()) {
          this.ligneForm.patchValue({ typeLigne: 'EPARGNE' });
          this.configureLineTypeRules('EPARGNE');
          this.uiError = this.activeCreditCount > 1
            ? 'Plusieurs crédits actifs trouvés pour ce membre. Contrôle requis.'
            : "Ce membre n'a aucun crédit en cours.";
        }

        if (this.selectedType === 'DEMANDE_CREDIT' && !this.isSelectedTypeAllowed()) {
          this.ligneForm.patchValue({ typeLigne: 'EPARGNE' });
          this.configureLineTypeRules('EPARGNE');
          this.uiError = 'Ce membre a déjà un crédit actif ou approuvé. Nouvelle demande interdite.';
        }
      },
      error: () => {
        this.uiError = 'Impossible de charger les informations automatiques du membre.';
      },
    });
  }

  private isSelectedTypeAllowed(): boolean {
    if (this.selectedType === 'REMBOURSEMENT_CREDIT') {
      return this.activeCreditCount === 1;
    }

    if (this.selectedType === 'DEMANDE_CREDIT') {
      return this.blockingCreditCount === 0;
    }

    return true;
  }

  chooseMembre(membre: MembreResponse): void {
    this.selectMember(membre);
  }

  addLigne(): void {
    this.addOrUpdateLine();
  }

  addOrUpdateLine(): void {
    this.uiError = '';
    this.uiSuccess = '';

    if (!this.collecte) {
      this.uiError = 'Collecte introuvable. Veuillez recharger la page.';
      return;
    }

    if (!this.isBrouillon) {
      this.uiError = 'Cette collecte ne peut plus être modifiée.';
      return;
    }

    if (!this.selectedMember) {
      this.uiError = 'Veuillez sélectionner un membre visité.';
      return;
    }

    if (this.ligneForm.invalid) {
      this.ligneForm.markAllAsTouched();
      this.uiError = 'Veuillez corriger les champs indiqués.';
      return;
    }

    if (this.selectedType === 'EPARGNE' && !this.hasActiveCompteEpargne) {
      this.uiError = "Ce membre n'a pas de compte épargne actif.";
      return;
    }

    if (this.selectedType === 'REMBOURSEMENT_CREDIT') {
      if (this.activeCreditCount === 0) {
        this.uiError = "Ce membre n'a aucun crédit en cours.";
        return;
      }
      if (this.activeCreditCount > 1) {
        this.uiError = 'Plusieurs crédits actifs trouvés pour ce membre. Contrôle requis.';
        return;
      }
    }

    if (this.selectedType === 'DEMANDE_CREDIT' && this.blockingCreditCount > 0) {
      this.uiError = 'Ce membre a déjà un crédit actif ou approuvé. Nouvelle demande interdite.';
      return;
    }

    const payload = this.buildLinePayload();
    if (!payload) {
      return;
    }

    this.addingLine = true;

    const request$ = this.editingLineId
      ? this.collecteService.updateLigne(this.collecte.id, this.editingLineId, payload)
      : this.collecteService.addLigne(this.collecte.id, payload);

    request$.subscribe({
      next: () => {
        this.uiSuccess = this.editingLineId
          ? 'Ligne mise à jour avec succès.'
          : 'Ligne ajoutée avec succès.';

        this.editingLineId = undefined;
        this.clearOperationFields();
        this.refreshCollecteAndRecap();
      },
      error: (error) => {
        this.addingLine = false;
        this.showError(error, 'Impossible d\'ajouter la ligne.');
      }
    });
  }

  editLigne(ligneId: number): void {
    if (!this.collecte) {
      return;
    }

    const line = this.collecte.lignes.find((value) => value.id === ligneId);
    if (!line) {
      return;
    }

    const member = this.filteredMembres.find((value) => value.id === line.membreId)
      || this.membres.find((value) => value.id === line.membreId)
      || ({
        id: line.membreId,
        codeMembre: line.membreCode || '-',
        nom: line.membreNom || '-',
        nomComplet: line.membreNom || '-',
        siteId: this.collecte.siteId,
        siteNom: this.collecte.siteNom || '',
        statut: 'ACTIF',
        dateAdhesion: this.collecte.dateCollecte,
        createdAt: this.collecte.dateCollecte,
        updatedAt: this.collecte.dateCollecte,
      } as MembreResponse);

    this.selectedMember = member;
    this.editingLineId = line.id;

    this.ligneForm.patchValue({
      membreId: line.membreId,
      typeLigne: line.typeLigne,
      montant: line.montant,
      reference: line.reference || '',
      commentaire: line.commentaire || '',
      montantSouhaite: line.montantSouhaite || null,
      objetCredit: line.objetCredit || '',
      gagePropose: line.gagePropose || '',
      dureeValeur: line.dureeValeur || 1,
      dureeUnite: line.dureeUnite || 'MOIS',
      modaliteRemboursement: line.modaliteRemboursement || 'MENSUELLE',
    });
  }

  removeLigne(ligneId: number): void {
    if (!this.collecte || !this.isBrouillon) {
      return;
    }

    this.collecteService.deleteLigne(this.collecte.id, ligneId).subscribe({
      next: () => {
        this.uiSuccess = 'Ligne supprimée avec succès.';
        this.refreshCollecteAndRecap();
      },
      error: (error) => {
        this.showError(error, 'Impossible de supprimer la ligne.');
      }
    });
  }

  saveBrouillon(): void {
    if (!this.collecte) {
      return;
    }

    if (this.isEditionMode) {
      this.uiSuccess = 'Les modifications sont enregistrées au fil des lignes. Soumettez pour terminer.';
      return;
    }

    this.savingDraft = true;
    this.uiError = '';
    this.uiSuccess = '';

    this.collecteService.create({
      especesRemises: Number(this.remiseForm.get('especesRemises')?.value || 0),
      especesDeclareesAgent: Number(this.remiseForm.get('especesRemises')?.value || 0),
      observations: (this.remiseForm.get('observations')?.value || '').trim() || undefined,
    }).subscribe({
      next: () => {
        this.uiSuccess = 'Brouillon enregistré.';
        this.refreshCollecteAndRecap();
      },
      error: (error) => {
        this.savingDraft = false;
        this.showError(error, 'Impossible d\'enregistrer le brouillon.');
      }
    });
  }

  soumettre(): void {
    if (!this.collecte) {
      return;
    }

    const remise = Number(this.remiseForm.get('especesRemises')?.value || 0);
    const observations = (this.remiseForm.get('observations')?.value || '').trim();

    if (this.ecartCourant !== 0 && !observations) {
      this.remiseForm.get('observations')?.markAsTouched();
      this.uiError = 'Observation obligatoire en cas d\'écart.';
      return;
    }

    this.submittingCollecte = true;
    this.uiError = '';
    this.uiSuccess = '';

    this.collecteService.soumettre(this.collecte.id, {
      especesRemises: remise,
      especesDeclareesAgent: remise,
      observations: observations || undefined,
    }).subscribe({
      next: (response) => {
        this.collecte = response;
        this.submittingCollecte = false;
        this.uiSuccess = 'Collecte soumise pour contrôle.';
        this.refreshCollecteAndRecap();
      },
      error: (error) => {
        this.submittingCollecte = false;
        this.showError(error, 'Impossible de soumettre la collecte.');
      }
    });
  }

  annuler(): void {
    if (this.isEditionMode) {
      this.router.navigate(['/collectes/mes-collectes']);
      return;
    }

    this.router.navigate(['/recettes']);
  }

  formatDate(dateValue: string): string {
    if (!dateValue) {
      return '';
    }

    return new Intl.DateTimeFormat('fr-FR').format(new Date(dateValue));
  }

  getTypeLabel(type: TypeLigneCollecte): string {
    switch (type) {
      case 'EPARGNE':
        return 'Epargne';
      case 'REMBOURSEMENT_CREDIT':
        return 'Remboursement credit';
      case 'CARNET':
        return 'Carnet';
      case 'FRAIS_ANALYSE':
        return 'Frais analyse';
      case 'DEMANDE_CREDIT':
        return 'Demande credit';
      default:
        return type;
    }
  }

  formatCdf(value: number | null | undefined): string {
    return `${new Intl.NumberFormat('fr-CD').format(Number(value || 0))} CDF`;
  }

  getDisplayedMontant(value: { typeLigne: TypeLigneCollecte; montant?: number; montantSouhaite?: number }): number {
    return value.typeLigne === 'DEMANDE_CREDIT'
      ? Number(value.montantSouhaite || 0)
      : Number(value.montant || 0);
  }

  getImpactCaisse(value: { typeLigne: TypeLigneCollecte; totalLigne?: number; montant?: number; quantite?: number }): number {
    return value.typeLigne === 'DEMANDE_CREDIT' ? 0 : this.lineTotal(value);
  }

  lineTotal(value: { totalLigne?: number; montant?: number; quantite?: number }): number {
    if (value.totalLigne !== undefined && value.totalLigne !== null) {
      return Number(value.totalLigne);
    }

    const montant = Number(value.montant || 0);
    const quantite = Number(value.quantite || 0);
    return quantite > 0 ? montant * quantite : montant;
  }

  requiresMontant(): boolean {
    return this.selectedType === 'EPARGNE' || this.selectedType === 'REMBOURSEMENT_CREDIT';
  }

  clearSelection(): void {
    this.selectedMember = undefined;
    this.editingLineId = undefined;
    this.ligneForm.patchValue({ membreId: null });
    this.clearOperationFields();
  }

  private clearOperationFields(): void {
    this.ligneForm.patchValue({
      typeLigne: 'EPARGNE',
      montant: null,
      reference: '',
      commentaire: '',
      montantSouhaite: null,
      objetCredit: '',
      gagePropose: '',
      dureeValeur: 1,
      dureeUnite: 'MOIS',
      modaliteRemboursement: 'MENSUELLE',
    });
    this.configureLineTypeRules('EPARGNE');
  }

  private configureLineTypeRules(type: TypeLigneCollecte): void {
    const montant = this.ligneForm.get('montant');
    const montantSouhaite = this.ligneForm.get('montantSouhaite');
    const objetCredit = this.ligneForm.get('objetCredit');
    const gagePropose = this.ligneForm.get('gagePropose');
    const dureeValeur = this.ligneForm.get('dureeValeur');
    const dureeUnite = this.ligneForm.get('dureeUnite');
    const modaliteRemboursement = this.ligneForm.get('modaliteRemboursement');

    montant?.clearValidators();
    montantSouhaite?.clearValidators();
    objetCredit?.clearValidators();
    gagePropose?.clearValidators();
    dureeValeur?.clearValidators();
    dureeUnite?.clearValidators();
    modaliteRemboursement?.clearValidators();
    this.operationHint = '';

    if (type === 'EPARGNE' || type === 'REMBOURSEMENT_CREDIT') {
      montant?.setValidators([Validators.required, Validators.min(1)]);
    } else if (type === 'DEMANDE_CREDIT') {
      montant?.setValidators([Validators.min(0)]);
    } else {
      montant?.setValue(null, { emitEvent: false });
    }

    if (type === 'CARNET') {
      this.operationHint = `Carnet: 1 unite, ${this.formatCdf(this.carnetPrix)} (parametre metier).`;
    }

    if (type === 'DEMANDE_CREDIT') {
      montantSouhaite?.setValidators([Validators.required, Validators.min(1)]);
      objetCredit?.setValidators([Validators.required]);
      gagePropose?.setValidators([Validators.required]);
      dureeValeur?.setValidators([Validators.required, Validators.min(1), Validators.max(60)]);
      dureeUnite?.setValidators([Validators.required]);
      modaliteRemboursement?.setValidators([Validators.required]);
      this.operationHint = 'Cette ligne enregistre une demande de crédit à pré-analyser. Les frais saisis ici sont un montant prévu; l’encaissement officiel reste au bureau par le caissier.';
    } else {
      montantSouhaite?.setValue(null, { emitEvent: false });
      objetCredit?.setValue('', { emitEvent: false });
      gagePropose?.setValue('', { emitEvent: false });
      dureeValeur?.setValue(1, { emitEvent: false });
      dureeUnite?.setValue('MOIS', { emitEvent: false });
      modaliteRemboursement?.setValue('MENSUELLE', { emitEvent: false });
    }

    montant?.updateValueAndValidity({ emitEvent: false });
    montantSouhaite?.updateValueAndValidity({ emitEvent: false });
    objetCredit?.updateValueAndValidity({ emitEvent: false });
    gagePropose?.updateValueAndValidity({ emitEvent: false });
    dureeValeur?.updateValueAndValidity({ emitEvent: false });
    dureeUnite?.updateValueAndValidity({ emitEvent: false });
    modaliteRemboursement?.updateValueAndValidity({ emitEvent: false });
  }

  private buildLinePayload(): CreateCollecteMembreLigneRequest | null {
    const values = this.ligneForm.getRawValue();
    const membreId = Number(values.membreId || this.selectedMember?.id);

    if (!membreId || membreId <= 0) {
      this.uiError = 'Le membre sélectionné est invalide.';
      return null;
    }

    const payload: CreateCollecteMembreLigneRequest = {
      membreId,
      typeLigne: values.typeLigne as TypeLigneCollecte,
      montant: values.montant !== null && values.montant !== undefined ? Number(values.montant) : undefined,
      reference: values.reference?.trim() || undefined,
      commentaire: values.commentaire?.trim() || undefined,
      montantSouhaite: values.montantSouhaite !== null && values.montantSouhaite !== undefined
        ? Number(values.montantSouhaite)
        : undefined,
      objetCredit: values.objetCredit?.trim() || undefined,
      gagePropose: values.gagePropose?.trim() || undefined,
      dureeValeur: values.dureeValeur !== null && values.dureeValeur !== undefined
        ? Number(values.dureeValeur)
        : undefined,
      dureeUnite: values.dureeUnite || undefined,
      modaliteRemboursement: values.modaliteRemboursement || undefined,
    };

    if (this.requiresMontant() && (!payload.montant || payload.montant <= 0)) {
      this.uiError = 'Le montant doit être supérieur à 0.';
      return null;
    }

    if (payload.typeLigne === 'DEMANDE_CREDIT') {
      if (payload.montant !== undefined && payload.montant < 0) {
        this.uiError = 'Les frais de demande doivent être positifs ou nuls.';
        return null;
      }
      if (!payload.montantSouhaite || payload.montantSouhaite <= 0) {
        this.uiError = 'Le montant demandé du crédit est obligatoire.';
        return null;
      }
      if (!payload.objetCredit) {
        this.uiError = "L'objet du credit est obligatoire.";
        return null;
      }
      if (!payload.gagePropose) {
        this.uiError = 'Le gage propose est obligatoire.';
        return null;
      }
      if (!payload.dureeValeur || payload.dureeValeur < 1 || payload.dureeValeur > 60) {
        this.uiError = 'La duree du credit doit etre comprise entre 1 et 60.';
        return null;
      }
      if (!payload.dureeUnite) {
        this.uiError = "L'unite de duree est obligatoire.";
        return null;
      }
      if (!payload.modaliteRemboursement) {
        this.uiError = 'La modalite de remboursement est obligatoire.';
        return null;
      }
    }

    return payload;
  }

  private refreshCollecteAndRecap(): void {
    const source$ = this.isEditionMode && this.editionCollecteId
      ? this.collecteService.getById(this.editionCollecteId)
      : this.collecteService.getToday();

    source$.subscribe({
      next: (today) => {
        if (!today) {
          return;
        }

        this.collecte = today;
        this.remiseForm.patchValue({
          especesRemises: today.especesDeclareesAgent ?? today.especesRemises,
          observations: today.observations || '',
        });

        this.loadRecap(today.id);
        this.addingLine = false;
        this.submittingCollecte = false;
        this.savingDraft = false;
      },
      error: (error) => {
        this.addingLine = false;
        this.submittingCollecte = false;
        this.savingDraft = false;
        this.showError(error, 'Impossible de rafraîchir la collecte.');
      },
    });
  }

  private loadRecap(collecteId: number): void {
    this.collecteService.recap(collecteId).subscribe({
      next: (response) => {
        this.recap = response;
      },
      error: () => {
        this.recap = undefined;
      }
    });
  }

  private showError(error: unknown, fallbackMessage: string): void {
    const backendMessage = (error as any)?.error?.message || (error as any)?.message;
    this.uiError = backendMessage || fallbackMessage;
  }

}
