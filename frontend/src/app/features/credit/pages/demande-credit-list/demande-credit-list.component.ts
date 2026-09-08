import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { DemandeCreditService } from '../../services/demande-credit.service';
import { CreditService } from '../../services/credit.service';
import { DemandeCreditResponse } from '../../models/demande-credit-response';
import { ApprobationCreditRequest } from '../../models/approbation-credit-request';
import { Page } from '../../../../shared/models/page.model';
import { AuthService } from '../../../../core/services/auth.service';
import { PreAnalyseAction, PreAnalyseRequest } from '../../models/pre-analyse-request';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

@Component({
  selector: 'app-demande-credit-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './demande-credit-list.component.html'
})
export class DemandeCreditListComponent implements OnInit {
  private readonly demandeCreditService = inject(DemandeCreditService);
  private readonly creditService = inject(CreditService);
  private readonly authService = inject(AuthService);
  private readonly workflowMessageService = inject(WorkflowMessageService);
  private readonly route = inject(ActivatedRoute);

  demandes: DemandeCreditResponse[] = [];
  paginationData: Page<DemandeCreditResponse> | null = null;
  globalGuidance: WorkflowGuidance = {
    title: 'Suivi des demandes de crédit',
    message: 'Cette page présente les demandes de crédit en attente de traitement. Chaque demande doit suivre le workflow 3N : soumission, pré-analyse par le Gestionnaire, analyse par le Contrôleur, vérification de la garantie, approbation par le Chef de Bureau, puis décaissement par le Caissier.',
    currentStep: 'Suivi des demandes',
    nextStep: 'Consulter le statut de chaque dossier',
    expectedRole: 'Selon le statut du dossier',
    expectedAction: 'Identifier le référent de l’étape selon le statut de chaque dossier',
    severity: 'info',
    canCurrentUserAct: true
  };
  guidanceByDemandeId = new Map<number, WorkflowGuidance>();
  loading = false;
  errorMessage = '';
  currentPage = 0;
  pageSize = 10;
  preAnalyseModalOpen = false;
  demandePreAnalyse: DemandeCreditResponse | null = null;
  preAnalyseAction: PreAnalyseAction = 'TRANSMETTRE_ANALYSE';
  preAnalyseCommentaire = '';
  preAnalyseDossierComplet = true;
  focusedDemandeId: number | null = null;

  ngOnInit(): void {
    const demandeId = Number(this.route.snapshot.queryParamMap.get('demandeId'));
    this.focusedDemandeId = Number.isFinite(demandeId) && demandeId > 0 ? demandeId : null;
    this.chargerDemandes();
  }

  chargerDemandes(page: number = 0): void {
    this.loading = true;
    this.errorMessage = '';
    this.currentPage = page;

    if (this.focusedDemandeId) {
      this.demandeCreditService.getById(this.focusedDemandeId).subscribe({
        next: (demande) => {
          this.paginationData = null;
          this.demandes = demande ? [demande] : [];
          this.rebuildGuidance();
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur chargement demande de crédit ciblée :', error);
          this.errorMessage = error?.error?.message || 'Impossible de charger la demande de crédit à approuver.';
          this.loading = false;
        }
      });
      return;
    }

    this.demandeCreditService.getAll(page, this.pageSize).subscribe({
      next: (pageData: Page<DemandeCreditResponse>) => {
        this.paginationData = pageData;
        this.demandes = pageData.content || [];
        this.rebuildGuidance();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement demandes de crédit :', error);
        this.errorMessage = 'Impossible de charger les demandes de crédit.';
        this.loading = false;
      }
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < (this.paginationData?.totalPages || 1)) {
      this.chargerDemandes(page);
    }
  }

  ouvrirApprobation(demandeId: number): void {
    const currentUser = this.authService.getCurrentUser();
    const decidedByNumber = Number(currentUser?.id);

    if (Number.isNaN(decidedByNumber) || decidedByNumber <= 0) {
      alert('Utilisateur connecté invalide pour approuver.');
      return;
    }

    const request: ApprobationCreditRequest = {
      decidedBy: decidedByNumber,
      genererEcheancier: true
    };

    this.creditService.approuverDemande(demandeId, request).subscribe({
      next: () => {
        alert('Crédit approuvé avec succès.');
        this.chargerDemandes();
      },
      error: (err) => {
        console.error('Erreur approbation crédit :', err);
        alert(err?.error?.message || 'Erreur lors de l’approbation du crédit.');
      }
    });
  }

  ouvrirPreAnalyse(demande: DemandeCreditResponse): void {
    this.demandePreAnalyse = demande;
    this.preAnalyseAction = 'TRANSMETTRE_ANALYSE';
    this.preAnalyseCommentaire = '';
    this.preAnalyseDossierComplet = true;
    this.preAnalyseModalOpen = true;
  }

  fermerPreAnalyse(): void {
    this.preAnalyseModalOpen = false;
    this.demandePreAnalyse = null;
  }

  soumettrePreAnalyse(): void {
    if (!this.demandePreAnalyse) {
      return;
    }

    const commentaire = this.preAnalyseCommentaire.trim();
    if (!commentaire) {
      alert('Observation de pré-analyse obligatoire.');
      return;
    }

    const request: PreAnalyseRequest = {
      action: this.preAnalyseAction,
      commentaire,
      dossierComplet: this.preAnalyseAction === 'TRANSMETTRE_ANALYSE' ? this.preAnalyseDossierComplet : false
    };

    this.demandeCreditService.preAnalyserDecision(this.demandePreAnalyse.id, request).subscribe({
      next: () => {
        alert(this.preAnalyseAction === 'TRANSMETTRE_ANALYSE'
          ? 'Pré-analyse validée et garantie 20% bloquée. Dossier transmis à l\'analyse.'
          : 'Dossier renvoyé à l\'agent terrain pour complément.');
        this.fermerPreAnalyse();
        this.chargerDemandes(this.currentPage);
      },
      error: (err) => {
        alert(err?.error?.message || 'Erreur lors de la pré-analyse.');
      }
    });
  }

  enregistrerObservationRisque(demande: DemandeCreditResponse): void {
    const commentaire = window.prompt('Observation de contrôle risque (obligatoire) :');
    if (!commentaire?.trim()) {
      alert('Observation obligatoire: aucun changement de statut effectué.');
      return;
    }

    this.demandeCreditService.enregistrerObservationRisque(demande.id, commentaire.trim()).subscribe({
      next: () => {
        alert('Observation de contrôle enregistrée.');
        this.chargerDemandes(this.currentPage);
      },
      error: (err) => {
        alert(err?.error?.message || 'Erreur lors de l\'enregistrement de l\'observation risque.');
      }
    });
  }

  validerAnalyseRisque(demande: DemandeCreditResponse): void {
    const commentaire = window.prompt('Commentaire de validation analyse risque (obligatoire) :');
    if (!commentaire?.trim()) {
      alert('Validation refusée: commentaire obligatoire.');
      return;
    }

    this.demandeCreditService.validerAnalyseRisque(demande.id, commentaire.trim()).subscribe({
      next: () => {
        alert('Analyse risque validée.');
        this.chargerDemandes(this.currentPage);
      },
      error: (err) => {
        alert(err?.error?.message || 'Erreur lors de la validation de l\'analyse risque.');
      }
    });
  }

  controlerGarantie(demande: DemandeCreditResponse): void {
    const commentaire = window.prompt('Commentaire de contrôle garantie (optionnel) :') || undefined;
    this.demandeCreditService.controlerGarantie(demande.id, commentaire).subscribe({
      next: () => {
        alert('Garantie validée.');
        this.chargerDemandes(this.currentPage);
      },
      error: (err) => {
        alert(err?.error?.message || 'Erreur lors du contrôle garantie.');
      }
    });
  }

  rejeter(demande: DemandeCreditResponse): void {
    const commentaire = window.prompt('Motif de rejet (obligatoire) :');
    if (!commentaire?.trim()) {
      return;
    }

    this.demandeCreditService.rejeter(demande.id, commentaire.trim()).subscribe({
      next: () => {
        alert('Demande rejetée.');
        this.chargerDemandes(this.currentPage);
      },
      error: (err) => {
        alert(err?.error?.message || 'Erreur lors du rejet.');
      }
    });
  }

  isPaiementInitialSolde(demande: DemandeCreditResponse): boolean {
    return this.isFraisDemandeIntegralementPayes(demande) && this.isDepotGarantieSolde(demande);
  }

  peutPayerInitial(demande: DemandeCreditResponse): boolean {
    if (!this.canSeePaiementAction) {
      return false;
    }

    const statutsPaiementFrais = [
      'SOUMISE',
      'EN_ANALYSE',
      'ANALYSE_TERRAIN_VALIDEE',
      'VALIDATION_CHEF',
      'VALIDATION_CONTROLEUR',
      'APPROUVEE'
    ];

    return (statutsPaiementFrais.includes(demande.statut) && !this.isFraisDemandeIntegralementPayes(demande))
      || (demande.statut === 'APPROUVEE' && !this.isDepotGarantieSolde(demande));
  }

  peutPreAnalyser(demande: DemandeCreditResponse): boolean {
    return this.canSeePreAnalyseActions && demande.statut === 'SOUMISE';
  }

  peutAnalyser(demande: DemandeCreditResponse): boolean {
    return this.canSeeAnalyseActions && demande.statut === 'EN_ANALYSE' && this.isGarantieBloquee(demande);
  }

  peutControlerRisque(demande: DemandeCreditResponse): boolean {
    return this.canSeeControleurActions && demande.statut === 'EN_ANALYSE' && this.isGarantieBloquee(demande);
  }

  peutValiderAnalyseRisque(demande: DemandeCreditResponse): boolean {
    return this.canSeeControleurActions
      && demande.statut === 'EN_ANALYSE'
      && this.isGarantieBloquee(demande)
      && this.isFraisDemandeIntegralementPayes(demande);
  }

  isFraisDemandeIntegralementPayes(demande: DemandeCreditResponse): boolean {
    return Number(demande.fraisDemandePayes ?? 0) >= Number(demande.fraisDemande ?? 0);
  }

  fraisDemandeRestant(demande: DemandeCreditResponse): number {
    return Math.max(Number(demande.fraisDemande ?? 0) - Number(demande.fraisDemandePayes ?? 0), 0);
  }

  getStatutFraisBadge(demande: DemandeCreditResponse): 'PAYES' | 'INCOMPLETS' {
    return this.isFraisDemandeIntegralementPayes(demande) ? 'PAYES' : 'INCOMPLETS';
  }

  peutAfficherMessagePaiementCaissier(demande: DemandeCreditResponse): boolean {
    return !this.canSeePaiementAction
      && !this.isFraisDemandeIntegralementPayes(demande)
      && !['REJETEE', 'ANNULEE'].includes(demande.statut);
  }

  private isDepotGarantieSolde(demande: DemandeCreditResponse): boolean {
    return Number(demande.depotGarantiePaye ?? 0) >= Number(demande.depotGarantieRequis ?? 0);
  }

  isGarantieBloquee(demande: DemandeCreditResponse): boolean {
    if (demande.garantieBloquee === true) {
      return true;
    }

    if (demande.statutGarantie === 'BLOQUEE') {
      return true;
    }

    const bloque = Number(demande.montantGarantieBloque ?? 0);
    const requis = Number(demande.depotGarantieRequis ?? 0);
    return requis > 0 && bloque >= requis;
  }

  getStatutGarantieBadge(demande: DemandeCreditResponse): 'NON_BLOQUEE' | 'BLOQUEE' | 'INSUFFISANTE' {
    if (demande.statutGarantie === 'BLOQUEE' || this.isGarantieBloquee(demande)) {
      return 'BLOQUEE';
    }

    const requis = Number(demande.depotGarantieRequis ?? 0);
    const bloque = Number(demande.montantGarantieBloque ?? 0);
    if (requis > 0 && bloque > 0 && bloque < requis) {
      return 'INSUFFISANTE';
    }

    return demande.statutGarantie === 'INSUFFISANTE' ? 'INSUFFISANTE' : 'NON_BLOQUEE';
  }

  peutAfficherMessageGarantieBloquee(demande: DemandeCreditResponse): boolean {
    return this.canSeeControleurActions
      && demande.statut === 'EN_ANALYSE'
      && !this.isGarantieBloquee(demande);
  }

  peutControlerGarantie(demande: DemandeCreditResponse): boolean {
    return this.canSeeControleurActions
      && ['ANALYSE_TERRAIN_VALIDEE', 'VALIDATION_CHEF'].includes(demande.statut)
      && this.isFraisDemandeIntegralementPayes(demande);
  }

  peutVoirGarantie(demande: DemandeCreditResponse): boolean {
    return this.canSeeGarantieReadOnlyActions
      && ['ANALYSE_TERRAIN_VALIDEE', 'VALIDATION_CHEF', 'APPROUVEE'].includes(demande.statut);
  }

  peutApprouver(demande: DemandeCreditResponse): boolean {
    return this.canSeeApprobationActions
      && demande.statut === 'VALIDATION_CHEF'
      && this.isFraisDemandeIntegralementPayes(demande);
  }

  peutRejeter(demande: DemandeCreditResponse): boolean {
    if (demande.statut === 'SOUMISE' && this.authService.hasAnyRole(['CONTROLEUR'])) {
      return false;
    }

    return this.canSeeRejetActions && !['APPROUVEE', 'REJETEE', 'ANNULEE'].includes(demande.statut);
  }

  get canSeeAnalyseActions(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR']);
  }

  get canSeePreAnalyseActions(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'GESTIONNAIRE']);
  }

  get canSeeControleurActions(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR']);
  }

  get canSeePaiementAction(): boolean {
    return this.authService.hasAnyRole(['CAISSIER']);
  }

  get canSeeApprobationActions(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU']);
  }

  get canSeeRejetActions(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR']);
  }

  get canSeeGarantieReadOnlyActions(): boolean {
    return this.authService.hasAnyRole(['CHEF_BUREAU']);
  }

  trackByDemandeId(_: number, demande: DemandeCreditResponse): number {
    return demande.id;
  }

  getDemandeGuidance(demande: DemandeCreditResponse): WorkflowGuidance {
    return this.guidanceByDemandeId.get(demande.id) || this.buildDemandeGuidance(demande);
  }

  private rebuildGuidance(): void {
    this.guidanceByDemandeId = new Map(this.demandes.map(demande => [demande.id, this.buildDemandeGuidance(demande)]));
  }

  private buildDemandeGuidance(demande: DemandeCreditResponse): WorkflowGuidance {
    const guidance = this.workflowMessageService.getGuidance({
      module: 'CREDIT',
      status: demande.statut,
      currentRole: this.authService.getCurrentUser()?.role,
      permissions: this.authService.getCurrentUser()?.permissions,
      metadata: {
        garantieStatus: demande.statutGarantie,
        garantieBloquee: this.isGarantieBloquee(demande)
      }
    });
    const currentRole = (this.authService.getCurrentUser()?.role || '').toUpperCase();

    if (demande.statut === 'SOUMISE') {
      return {
        ...guidance,
        title: 'Demande de crédit soumise',
        message: 'La demande de crédit est soumise. Le Gestionnaire doit maintenant effectuer la pré-analyse : vérifier les informations du membre, analyser la cohérence de la demande et préparer le dossier avant l’analyse du Contrôleur.',
        currentStep: 'Demande soumise',
        nextStep: 'Pré-analyse',
        expectedRole: 'Gestionnaire',
        expectedAction: 'Effectuer la pré-analyse du dossier',
        blockedReason: currentRole === 'CONTROLEUR'
          ? 'Vous ne pouvez pas encore traiter cette demande à cette étape. Le Gestionnaire doit d’abord effectuer la pré-analyse. Le Contrôleur interviendra ensuite pour l’analyse de risque.'
          : guidance.blockedReason
      };
    }

    if (demande.statut === 'EN_ANALYSE') {
      return {
        ...guidance,
        title: 'Analyse Contrôleur',
        message: 'La pré-analyse est validée et la garantie 20% est bloquée. Le Contrôleur doit maintenant analyser le dossier, enregistrer l’analyse de risque puis valider l’analyse avant le contrôle de garantie.',
        currentStep: 'Analyse Contrôleur',
        nextStep: 'Contrôle garantie puis approbation',
        expectedRole: 'Contrôleur',
        expectedAction: 'Analyser le dossier et valider l’analyse risque',
        blockedReason: currentRole !== 'CONTROLEUR' && currentRole !== 'ADMIN'
          ? 'Cette étape est réservée au Contrôleur. Le dossier apparaîtra dans les actions du Contrôleur tant que l’analyse risque n’est pas validée.'
          : guidance.blockedReason
      };
    }

    return guidance;
  }
}