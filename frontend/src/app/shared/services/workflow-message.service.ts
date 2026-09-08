import { Injectable } from '@angular/core';

import {
  WorkflowGuidance,
  WorkflowGuidanceContext,
  WorkflowModule,
  WorkflowSeverity
} from '../models/workflow-guidance.model';

interface WorkflowDefinition {
  title: string;
  message: string;
  currentStep: string;
  nextStep?: string;
  expectedRole?: string;
  expectedAction?: string;
  severity: WorkflowSeverity;
}

@Injectable({
  providedIn: 'root'
})
export class WorkflowMessageService {
  getGuidance(context: WorkflowGuidanceContext): WorkflowGuidance {
    const definition = this.resolveDefinition(context);
    const currentRole = this.normalizeRole(context.currentRole);
    const expectedRole = this.normalizeRole(context.expectedRole || definition.expectedRole);
    const canCurrentUserAct = this.canAct(context, currentRole, expectedRole);

    let message = definition.message;
    if (canCurrentUserAct && expectedRole) {
      message = this.adaptForCurrentActor(definition.message, expectedRole);
    }

    const blockedReason = context.blockingReason || this.buildBlockedReason({
      module: context.module,
      currentRole,
      expectedRole,
      expectedAction: definition.expectedAction,
      status: context.status,
      metadata: context.metadata
    });

    return {
      title: definition.title,
      message,
      currentStep: definition.currentStep,
      nextStep: context.nextStep || definition.nextStep,
      expectedRole: this.labelForRole(context.expectedRole || definition.expectedRole),
      expectedAction: definition.expectedAction,
      severity: definition.severity,
      canCurrentUserAct,
      blockedReason: canCurrentUserAct ? undefined : blockedReason,
      action: definition.expectedAction
        ? {
            label: definition.expectedAction,
            visible: canCurrentUserAct
          }
        : undefined
    };
  }

  buildForbiddenMessage(input: {
    requestUrl: string;
    currentRole?: string;
    status?: string;
    metadata?: Record<string, unknown>;
  }): { title: string; detail?: string } {
    const lowerUrl = (input.requestUrl || '').toLowerCase();
    const role = (input.currentRole || '').toUpperCase();

    if (role === 'AGENT_TERRAIN' && this.includesAny(lowerUrl, [
      '/operations-epargne',
      '/credits',
      '/recettes-journalieres',
      '/recettes-terrain',
      '/operations-caisse',
      '/sessions-caisse',
      '/caisses'
    ])) {
      return {
        title: "Vous ne pouvez pas effectuer cette action.",
        detail: 'Cette opération doit être enregistrée depuis Ma collecte du jour.'
      };
    }

    if (this.includesAny(lowerUrl, ['/caisses/sessions', '/sessions-caisse', '/caisses'])) {
      return {
        title: "Vous ne pouvez pas effectuer cette action.",
        detail: 'Les opérations de caisse restent réservées au Caissier tant que la session est ouverte. Le Contrôleur intervient après soumission au contrôle.'
      };
    }

    if (this.includesAny(lowerUrl, ['/paiement-initial', '/decaissement', '/credits'])) {
      if (this.includesAny(lowerUrl, ['/approve', '/approbation'])) {
        return {
          title: "Vous ne pouvez pas effectuer cette action.",
          detail: 'Vous ne pouvez pas approuver ce crédit. L’approbation est réservée au Chef de Bureau après analyse et validation de la garantie.'
        };
      }

      if (this.includesAny(lowerUrl, ['/paiement-initial', '/decaissement'])) {
        return {
          title: "Vous ne pouvez pas effectuer cette action.",
          detail: 'Vous ne pouvez pas décaisser ce crédit. Le décaissement est réservé au Caissier, uniquement après approbation du crédit, validation de la garantie et vérification du dossier.'
        };
      }

      return {
        title: "Vous ne pouvez pas effectuer cette action.",
        detail: 'Vous ne pouvez pas effectuer cette action sur ce crédit. Cette étape appartient au référent de l’étape indiqué dans le workflow. Vérifiez l’état du dossier et laissez le rôle attendu réaliser l’action.'
      };
    }

    if (this.includesAny(lowerUrl, ['/operations-epargne', '/comptes-epargne'])) {
      return {
        title: "Vous ne pouvez pas effectuer cette action.",
        detail: 'Vous ne pouvez pas effectuer cette action sur les opérations épargne. La consultation reste séparée de la création d’opérations financières.'
      };
    }

    if (this.includesAny(lowerUrl, ['/retraits', '/demandes-retrait'])) {
      return {
        title: "Vous ne pouvez pas effectuer cette action.",
        detail: 'Vous ne pouvez pas effectuer cette action sur le retrait. La validation appartient au Contrôleur et le paiement appartient au Caissier selon l’étape du dossier.'
      };
    }

    return {
      title: "Vous ne pouvez pas effectuer cette action.",
      detail: 'Cette étape est réservée au rôle attendu dans le workflow 3N.'
    };
  }

  private resolveDefinition(context: WorkflowGuidanceContext): WorkflowDefinition {
    const status = (context.status || '').toUpperCase();
    switch (context.module) {
      case 'CREDIT':
        return this.creditDefinition(status, context.metadata);
      case 'RECETTE_JOURNALIERE':
        return this.recetteDefinition(status, context.metadata);
      case 'RETRAIT_EPARGNE':
        return this.retraitDefinition(status, context.metadata);
      case 'CAISSE':
        return this.caisseDefinition(status, context.metadata);
      case 'DEPENSE_CAISSE':
        return this.depenseDefinition(status, context.metadata);
      case 'EPARGNE_OPERATION':
        return this.epargneOperationDefinition(status, context.metadata);
      case 'DASHBOARD_ROLE':
        return this.dashboardRoleDefinition(status);
      default:
        return this.defaultDefinition(status);
    }
  }

  private normalizeRole(role?: string): string {
    const normalizedRole = (role || '').toUpperCase();
    return normalizedRole;
  }

  private roleRank(role?: string): number {
    switch (this.normalizeRole(role)) {
      case 'ADMIN':
        return 7;
      case 'CHEF_BUREAU':
        return 6;
      case 'RCI':
        return 5;
      case 'CONTROLEUR':
        return 4;
      case 'GESTIONNAIRE':
        return 3;
      case 'AGENT_TERRAIN':
        return 2;
      case 'CAISSIER':
        return 1;
      case 'MEMBER':
        return 0;
      default:
        return -1;
    }
  }

  private canAct(context: WorkflowGuidanceContext, currentRole?: string, expectedRole?: string): boolean {
    if (!currentRole || !expectedRole) {
      return false;
    }

    const normalizedCurrentRole = this.normalizeRole(currentRole);
    const normalizedExpectedRole = this.normalizeRole(expectedRole);

    if (normalizedCurrentRole === normalizedExpectedRole) {
      return true;
    }

    if (context.module === 'CAISSE' && context.status === 'PRE_CLOTUREE') {
      return this.roleRank(normalizedCurrentRole) > this.roleRank(normalizedExpectedRole);
    }

    return false;
  }

  private dashboardRoleDefinition(status: string): WorkflowDefinition {
    switch (this.normalizeRole(status)) {
      case 'AGENT_TERRAIN':
        return {
          title: 'Tableau de bord Agent Terrain',
          message: 'Ce tableau de bord vous aide à suivre vos activités terrain : membres recrutés, épargnes collectées, remboursements collectés, demandes de crédit recueillies et recettes journalières à transmettre. Votre rôle est de collecter, documenter et remettre les fonds au bureau. Les validations financières appartiennent aux rôles de contrôle et de caisse selon le workflow.',
          currentStep: 'Collecte et transmission terrain',
          nextStep: 'Billetage, contrôle et validation',
          expectedRole: 'AGENT_TERRAIN',
          expectedAction: 'Compléter les données terrain, transmettre les recettes, remettre les fonds collectés et suivre les corrections demandées',
          severity: 'info'
        };
      case 'GESTIONNAIRE':
        return {
          title: 'Tableau de bord Gestionnaire',
          message: 'Ce tableau de bord vous aide à superviser les agents terrain, suivre les sites, analyser la qualité des dossiers et effectuer les pré-analyses de crédit. Le Gestionnaire prépare et recommande, mais ne valide pas les opérations financières.',
          currentStep: 'Pré-analyse et supervision',
          nextStep: 'Transmission au Contrôleur',
          expectedRole: 'GESTIONNAIRE',
          expectedAction: 'Pré-analyser les crédits, suivre les agents terrain, documenter les observations et transmettre les dossiers au Contrôleur',
          severity: 'info'
        };
      case 'CONTROLEUR':
        return {
          title: 'Tableau de bord Contrôleur',
          message: 'Ce tableau de bord vous aide à contrôler les opérations quotidiennes : recettes journalières, caisse, retraits épargne, garanties, crédits, écarts et irrégularités. Le Contrôleur vérifie, documente, valide ou rejette selon le workflow prévu.',
          currentStep: 'Contrôle opérationnel',
          nextStep: 'Paiement ou décaissement par le rôle habilité',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Contrôler les dossiers, valider ou rejeter les opérations autorisées, documenter les écarts et garantir la traçabilité',
          severity: 'warning'
        };
      case 'CAISSIER':
        return {
          title: 'Tableau de bord Caissier',
          message: 'Ce tableau de bord vous aide à tenir la caisse, recevoir les fonds, participer au billetage, payer les dépenses validées, payer les retraits validés et décaisser les crédits approuvés lorsque la garantie et le dossier sont conformes.',
          currentStep: 'Exécution des paiements autorisés',
          nextStep: 'Soumission et contrôle de session',
          expectedRole: 'CAISSIER',
          expectedAction: 'Tenir la caisse, effectuer les paiements autorisés, enregistrer les mouvements et préparer la session pour contrôle',
          severity: 'info'
        };
      case 'CHEF_BUREAU':
        return {
          title: 'Tableau de bord Chef de Bureau',
          message: 'Ce tableau de bord vous aide à superviser l’antenne, suivre le personnel, contrôler les priorités opérationnelles et approuver les crédits lorsque le dossier est complet. Le Chef de Bureau intervient comme responsable d’antenne et décide sur les étapes qui relèvent de son autorité.',
          currentStep: 'Supervision d’antenne',
          nextStep: 'Décision sur dossiers prêts',
          expectedRole: 'CHEF_BUREAU',
          expectedAction: 'Superviser l’antenne, examiner les dossiers prêts pour décision et traiter les alertes opérationnelles',
          severity: 'warning'
        };
      case 'COO':
        return {
          title: 'Tableau de bord COO',
          message: 'Ce tableau de bord vous aide à superviser les activités opérationnelles des antennes, suivre les gestionnaires, identifier les blocages et surveiller les performances globales. Le COO pilote les opérations sans remplacer les validations métier déjà définies.',
          currentStep: 'Supervision opérationnelle',
          nextStep: 'Suivi des blocages',
          expectedRole: 'COO',
          expectedAction: 'Superviser les opérations, identifier les blocages, demander des corrections et suivre les indicateurs',
          severity: 'info'
        };
      case 'RCI':
        return {
          title: 'Tableau de bord RCI',
          message: 'Ce tableau de bord vous aide à suivre le contrôle interne, superviser les contrôleurs et caissiers, identifier les irrégularités, surveiller les écarts et préparer les missions d’audit.',
          currentStep: 'Contrôle interne et audit',
          nextStep: 'Missions de contrôle',
          expectedRole: 'RCI',
          expectedAction: 'Analyser les alertes, vérifier les contrôles, documenter les constats et préparer les missions d’audit',
          severity: 'warning'
        };
      case 'GERANT_GENERAL':
      case 'ADMIN':
        return {
          title: 'Tableau de bord Gérant Général',
          message: 'Ce tableau de bord vous donne une vision globale de l’institution : antennes, opérations, crédits, épargne, caisse, recettes, risques et alertes stratégiques. Le Gérant Général supervise, arbitre les cas exceptionnels et suit les décisions importantes.',
          currentStep: 'Supervision stratégique',
          nextStep: 'Arbitrage des cas exceptionnels',
          expectedRole: 'GERANT_GENERAL',
          expectedAction: 'Superviser l’institution, arbitrer les cas exceptionnels et suivre les alertes majeures',
          severity: 'info'
        };
      default:
        return {
          title: 'Tableau de bord opérationnel 3N',
          message: 'Ce tableau de bord présente les indicateurs, alertes et actions disponibles selon vos droits. Les workflows 3N restent organisés par rôle : collecte, pré-analyse, contrôle, caisse, approbation, supervision et audit. Les actions sensibles doivent être réalisées uniquement par le référent de l’étape prévu par le processus.',
          currentStep: 'Pilotage opérationnel',
          nextStep: 'Orientation vers les écrans métier',
          expectedRole: 'GESTIONNAIRE',
          expectedAction: 'Consulter les priorités disponibles et accéder aux écrans métier correspondant à votre rôle',
          severity: 'info'
        };
    }
  }

  private depenseDefinition(status: string, metadata?: Record<string, unknown>): WorkflowDefinition {
    const rejectReason = String(metadata?.['motifRejet'] || '').trim();

    switch (status) {
      case 'BROUILLON':
      case 'SOUMISE':
        return {
          title: 'Demande de dépense enregistrée',
          message: 'La demande de dépense est enregistrée. Le rôle validateur doit maintenant vérifier le motif, la catégorie, le montant et la conformité de la demande avant autorisation.',
          currentStep: 'Demande dépense',
          nextStep: 'Validation / autorisation',
          expectedRole: 'CHEF_BUREAU',
          expectedAction: 'Vérifier puis autoriser la dépense',
          severity: 'info'
        };
      case 'EN_ATTENTE_VALIDATION':
        return {
          title: 'Dépense en attente de validation',
          message: 'La dépense a été enregistrée mais n’est pas encore autorisée. Le rôle validateur doit vérifier le motif, la catégorie, le montant et la conformité de la dépense avant paiement. Le Caissier ne doit pas payer cette dépense tant qu’elle n’est pas validée.',
          currentStep: 'En attente de validation',
          nextStep: 'Validation de la dépense',
          expectedRole: 'CHEF_BUREAU',
          expectedAction: 'Vérifier, valider ou rejeter la dépense',
          severity: 'warning'
        };
      case 'VALIDEE':
      case 'APPROUVEE':
        return {
          title: 'Dépense validée',
          message: 'La dépense est validée. Le Caissier peut maintenant effectuer le paiement, enregistrer la sortie de caisse et laisser une trace complète de l’opération.',
          currentStep: 'Dépense validée',
          nextStep: 'Paiement',
          expectedRole: 'CAISSIER',
          expectedAction: 'Payer la dépense et enregistrer la sortie de caisse',
          severity: 'success'
        };
      case 'PAYEE':
        return {
          title: 'Dépense payée',
          message: 'La dépense a été payée par le Caissier. Elle est historisée et doit rester consultable pour le contrôle de caisse, les rapports et l’audit.',
          currentStep: 'Dépense payée',
          nextStep: 'Historisation / contrôle',
          expectedRole: 'MEMBER',
          expectedAction: 'Consulter l’historique si nécessaire',
          severity: 'success'
        };
      case 'REJETEE':
        return {
          title: 'Dépense rejetée',
          message: 'La dépense a été rejetée. Aucun paiement ne doit être effectué. Le motif de rejet doit être visible afin que le demandeur comprenne la décision.',
          currentStep: 'Dépense rejetée',
          nextStep: 'Aucune, sauf nouvelle demande corrigée',
          expectedRole: 'MEMBER',
          expectedAction: rejectReason ? `Consulter le motif: ${rejectReason}` : 'Consulter le motif ou créer une nouvelle demande si nécessaire',
          severity: 'danger'
        };
      case 'ANNULEE':
        return {
          title: 'Dépense annulée',
          message: 'La dépense est annulée. Aucun paiement ne doit être effectué. Les informations restent disponibles pour la traçabilité et le contrôle.',
          currentStep: 'Historisation',
          severity: 'danger'
        };
      default:
        return this.defaultDefinition(status);
    }
  }

  private creditDefinition(status: string, metadata?: Record<string, unknown>): WorkflowDefinition {
    const garantieStatus = String(metadata?.['garantieStatus'] || '').toUpperCase();
    const garantieBloquee = metadata?.['garantieBloquee'] === true;

    switch (status) {
      case 'BROUILLON':
      case 'SOUMISE':
        return {
          title: 'Demande de crédit enregistrée',
          message: 'La demande de crédit est enregistrée. Le Gestionnaire doit maintenant vérifier les informations du membre, le montant demandé, l’activité déclarée et la cohérence générale du dossier avant transmission au Contrôleur.',
          currentStep: 'Demande enregistrée',
          nextStep: 'Pré-analyse Gestionnaire',
          expectedRole: 'GESTIONNAIRE',
          expectedAction: 'Effectuer la pré-analyse du dossier',
          severity: 'info'
        };
      case 'EN_ANALYSE':
        return {
          title: 'Dossier en attente d’analyse',
          message: 'La pré-analyse est terminée. Le Contrôleur doit maintenant analyser le dossier, vérifier les informations du membre, l’activité, l’adresse, les garanties et les risques avant la phase d’approbation.',
          currentStep: 'Analyse Contrôleur',
          nextStep: 'Garantie',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Analyser le dossier',
          severity: 'warning'
        };
      case 'ANALYSE_TERRAIN_VALIDEE':
      case 'VALIDATION_CONTROLEUR':
        return {
          title: 'Garantie à vérifier',
          message: 'La phase de garantie est en cours. Le Contrôleur doit vérifier que la garantie obligatoire représente 20 % du montant demandé, qu’elle provient de l’épargne du membre et que le blocage est effectif avant toute approbation.',
          currentStep: 'Garantie',
          nextStep: 'Approbation Chef de Bureau',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Vérifier et bloquer la garantie',
          severity: garantieStatus === 'INSUFFISANTE' ? 'danger' : 'warning'
        };
      case 'VALIDATION_CHEF':
        if (garantieStatus === 'INSUFFISANTE' || !garantieBloquee) {
          return {
            title: 'Garantie insuffisante',
            message: 'La garantie disponible ne couvre pas les 20 % requis du montant demandé. Le crédit ne peut pas être approuvé ni décaissé tant que la garantie n’est pas complétée et validée.',
            currentStep: 'Garantie',
            nextStep: 'Approbation Chef de Bureau',
            expectedRole: 'CONTROLEUR',
            expectedAction: 'Valider la garantie',
            severity: 'danger'
          };
        }

        return {
          title: 'Crédit en attente d’approbation',
          message: 'La garantie est validée. Le Chef de Bureau doit maintenant examiner la demande, l’analyse, la garantie et les observations avant d’approuver ou de rejeter le crédit.',
          currentStep: 'Approbation Chef de Bureau',
          nextStep: 'Décaissement Caissier',
          expectedRole: 'CHEF_BUREAU',
          expectedAction: 'Approuver ou refuser le crédit',
          severity: 'info'
        };
      case 'APPROUVEE':
      case 'APPROUVE':
        return {
          title: 'Crédit approuvé',
          message: 'Le crédit est approuvé. Le Caissier peut procéder au décaissement uniquement si la garantie est validée, le dossier complet et les fonds disponibles. Aucun décaissement n’est autorisé sans ces conditions.',
          currentStep: 'Décaissement Caissier',
          nextStep: 'Remboursement',
          expectedRole: 'CAISSIER',
          expectedAction: 'Procéder au décaissement',
          severity: 'success'
        };
      case 'DECAISSE':
        return {
          title: 'Crédit décaissé',
          message: 'Le crédit a été décaissé par le Caissier. Le dossier est maintenant en phase de remboursement. Les échéances doivent être suivies jusqu’au remboursement complet puis à la clôture.',
          currentStep: 'Remboursement',
          nextStep: 'Clôture',
          expectedRole: 'GESTIONNAIRE',
          expectedAction: 'Suivre et enregistrer les remboursements',
          severity: 'success'
        };
      case 'EN_COURS':
        return {
          title: 'Crédit en remboursement',
          message: 'Le crédit est en cours de remboursement. Les versements doivent être suivis et tracés jusqu’au solde complet du dossier.',
          currentStep: 'Remboursement',
          nextStep: 'Clôture',
          expectedRole: 'GESTIONNAIRE',
          expectedAction: 'Suivre les remboursements',
          severity: 'warning'
        };
      case 'CLOTURE':
      case 'CLOTUREE':
        return {
          title: 'Crédit clôturé',
          message: 'Le crédit est clôturé. Le dossier doit rester consultable pour l’historique, les rapports, le contrôle interne et l’audit.',
          currentStep: 'Clôture',
          severity: 'success'
        };
      case 'EN_RETARD':
        return {
          title: 'Retard de remboursement',
          message: 'Une échéance est en retard. Les pénalités prévues sont de 2500 FC par jour de retard. Le dossier doit être suivi jusqu’à régularisation complète avant toute clôture.',
          currentStep: 'Remboursement',
          nextStep: 'Clôture',
          expectedRole: 'GESTIONNAIRE',
          expectedAction: 'Suivre le dossier en retard',
          severity: 'danger'
        };
      case 'REMBOURSE':
        return {
          title: 'Crédit remboursé',
          message: 'Le crédit est remboursé. Le dossier peut être préparé pour clôture après vérification du solde, des éventuelles pénalités et de la garantie bloquée.',
          currentStep: 'Clôture',
          nextStep: 'Clôture',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Préparer la clôture du dossier',
          severity: 'success'
        };
      case 'CONTENTIEUX':
        return {
          title: 'Crédit en contentieux',
          message: 'Le dossier est en contentieux suite à un défaut de remboursement. Les actions de suivi et de régularisation doivent rester traçables avant toute clôture.',
          currentStep: 'Remboursement',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Documenter le suivi contentieux',
          severity: 'danger'
        };
      case 'ANNULE':
      case 'ANNULEE':
      case 'REJETEE':
        return {
          title: 'Crédit rejeté',
          message: 'Le crédit est rejeté ou annulé. Aucun décaissement ne doit être effectué. Le motif doit rester visible pour la traçabilité.',
          currentStep: 'Clôture',
          severity: 'danger'
        };
      default:
        return this.defaultDefinition(status);
    }
  }

  private recetteDefinition(status: string, metadata?: Record<string, unknown>): WorkflowDefinition {
    const alreadyGenerated = metadata?.['operationsGenerated'] === true;
    const ecart = Number(metadata?.['ecart'] || metadata?.['manquant'] || 0);
    const billetageConfirme = metadata?.['billetageConfirme'] === true;
    const generationBlocked = metadata?.['generationBlocked'] === true;

    switch (status) {
      case 'BROUILLON':
        return {
          title: 'Recette journalière en préparation',
          message: 'La recette journalière est en brouillon. L’Agent Terrain doit compléter les informations de collecte : membres visités, épargnes collectées, remboursements, frais, demandes de crédit, espèces remises, carnets distribués, nouveaux membres et observations.',
          currentStep: 'Activité terrain',
          nextStep: 'Retour bureau',
          expectedRole: 'AGENT_TERRAIN',
          expectedAction: 'Compléter la fiche de recette journalière',
          severity: 'info'
        };
      case 'SOUMISE':
        if (!billetageConfirme) {
          return {
            title: 'Billetage à effectuer',
            message: 'La recette journalière est soumise. Le Caissier doit effectuer le billetage avec l’Agent Terrain afin de confirmer les espèces remises. Le billetage sert de preuve opérationnelle et ne crée pas encore d’opération financière officielle.',
            currentStep: 'Billetage Agent Terrain + Caissier',
            nextStep: 'Contrôle Contrôleur',
            expectedRole: 'CAISSIER',
            expectedAction: 'Confirmer le billetage',
            severity: 'warning'
          };
        }

        return {
          title: 'Recette en attente de contrôle',
          message: 'Le billetage est confirmé. Le Contrôleur doit maintenant vérifier la recette : comparer les montants déclarés, les espèces remises, les remboursements, les demandes de crédit et les écarts éventuels avant validation.',
          currentStep: 'En attente de contrôle',
          nextStep: 'Validation Contrôleur',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Contrôler la recette',
          severity: 'warning'
        };
      case 'BILLETAGE_CONFIRME':
        return {
          title: 'Contrôle de la recette journalière',
          message: 'Le billetage est confirmé. Le Contrôleur doit maintenant contrôler la recette journalière : comparer les montants collectés, les espèces remises, les remboursements, les frais, les demandes de crédit et les écarts éventuels.',
          currentStep: 'Contrôle Contrôleur',
          nextStep: 'Validation Contrôleur',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Contrôler la recette',
          severity: 'warning'
        };
      case 'ECART_CONSTATE':
        return {
          title: 'Écart constaté',
          message: 'Un écart a été constaté dans la recette journalière. Une justification est obligatoire avant validation. Le Contrôleur doit analyser l’écart, demander des explications si nécessaire et décider de la suite selon les règles de contrôle interne.',
          currentStep: 'Contrôle Contrôleur',
          nextStep: 'Validation Contrôleur',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Justifier puis valider la recette',
          severity: 'danger'
        };
      case 'VALIDEE':
        if (generationBlocked) {
          return {
            title: 'Génération des opérations bloquée',
            message: 'La recette est validée mais la génération des opérations officielles est bloquée à cause d’une incohérence ou d’un risque de doublon. Le Contrôleur doit vérifier les montants et les opérations existantes avant correction.',
            currentStep: 'Génération en attente',
            nextStep: 'Vérification des incohérences',
            expectedRole: 'CONTROLEUR',
            expectedAction: 'Vérifier puis corriger l’incohérence',
            severity: 'danger'
          };
        }

        if (alreadyGenerated) {
          return {
            title: 'Opérations générées',
            message: 'Les opérations officielles ont été générées à partir de la recette journalière validée. Aucune double génération ne doit être possible. La recette devient une référence historisée du retour terrain.',
            currentStep: 'Génération automatique des opérations officielles',
            severity: 'success'
          };
        }

        if (ecart > 0) {
          return {
            title: 'Écart constaté',
            message: 'Écart constaté. Une justification est obligatoire avant validation.',
            currentStep: 'Contrôle Contrôleur',
            nextStep: 'Génération automatique des opérations officielles',
            expectedRole: 'CONTROLEUR',
            expectedAction: 'Justifier puis valider la recette',
            severity: 'danger'
          };
        }

        return {
          title: 'Recette validée',
          message: 'La recette journalière est validée par le Contrôleur. Le système peut maintenant générer les opérations officielles liées à cette recette : opérations d’épargne, remboursements, mouvements caisse, nouveaux membres ou demandes de crédit selon les données validées. Aucun double encodage ne doit être possible.',
          currentStep: 'Validation Contrôleur',
          nextStep: 'Génération automatique des opérations officielles',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Générer ou vérifier les opérations officielles',
          severity: 'success'
        };
      case 'REJETEE':
        return {
          title: 'Recette rejetée',
          message: 'La recette journalière est rejetée. Le motif de rejet doit être corrigé ou traité avant toute nouvelle validation. Les opérations officielles ne doivent pas être générées tant que la recette n’est pas validée.',
          currentStep: 'Contrôle Contrôleur',
          expectedRole: 'AGENT_TERRAIN',
          expectedAction: 'Consulter le motif et corriger la recette',
          severity: 'danger'
        };
      default:
        return this.defaultDefinition(status);
    }
  }

  private retraitDefinition(status: string, metadata?: Record<string, unknown>): WorkflowDefinition {
    const rejectReason = String(metadata?.['motifRejet'] || '').trim();

    switch (status) {
      case 'CREEE':
      case 'DEMANDE':
      case 'EN_ATTENTE_VALIDATION':
        return {
          title: 'Validation du retrait requise',
          message: 'La demande de retrait est créée. Le Contrôleur doit vérifier l’identité du membre, le compte épargne, le solde disponible et les garanties bloquées. Le membre ne peut retirer que le solde disponible.',
          currentStep: 'Contrôle du compte',
          nextStep: 'Paiement Caissier',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Valider ou rejeter la demande',
          severity: 'warning'
        };
      case 'VALIDEE':
      case 'APPROUVEE':
        return {
          title: 'Paiement du retrait attendu',
          message: 'La demande de retrait est approuvée par le Contrôleur. Le Caissier doit maintenant effectuer le paiement au membre et enregistrer l’opération correspondante. Après paiement, le retrait sera historisé.',
          currentStep: 'Paiement Caissier',
          nextStep: 'Historisation',
          expectedRole: 'CAISSIER',
          expectedAction: 'Effectuer le paiement',
          severity: 'success'
        };
      case 'REJETEE':
        return {
          title: 'Retrait rejeté',
          message: 'La demande de retrait est rejetée. Le motif de rejet doit être affiché clairement. Aucun paiement ne doit être effectué tant que la demande est rejetée.',
          currentStep: 'Contrôle du compte',
          severity: 'danger',
          expectedAction: rejectReason ? `Consulter le motif: ${rejectReason}` : 'Consulter le motif de rejet',
          expectedRole: 'MEMBER'
        };
      case 'DECAISSEE':
      case 'PAYEE':
        return {
          title: 'Retrait payé',
          message: 'Le retrait a été payé par le Caissier. L’opération est historisée et le solde du compte épargne doit refléter le paiement effectué.',
          currentStep: 'Historisation',
          severity: 'success'
        };
      case 'ANNULEE':
        return {
          title: 'Retrait annulé',
          message: 'La demande de retrait a été annulée et ne poursuit plus le workflow.',
          currentStep: 'Historisation',
          severity: 'danger'
        };
      default:
        return this.defaultDefinition(status);
    }
  }

  private caisseDefinition(status: string, metadata?: Record<string, unknown>): WorkflowDefinition {
    const ecart = Number(metadata?.['ecart'] || 0);

    switch (status) {
      case 'OUVERTE':
        if (metadata?.['isAncienneOuverte'] === true) {
          return {
            title: 'Session de caisse ancienne encore ouverte',
            message: 'Cette session appartient à une date comptable antérieure et bloque le travail du jour. Le Caissier doit vérifier les mouvements déjà enregistrés, régulariser les anomalies si nécessaire, puis soumettre la session au contrôle. Après cette soumission, le Contrôleur pourra effectuer le contrôle de caisse.',
            currentStep: 'Session ouverte ancienne',
            nextStep: 'Soumission au contrôle',
            expectedRole: 'CAISSIER',
            expectedAction: 'Vérifier les mouvements, régulariser si nécessaire et soumettre la session au contrôle',
            severity: 'danger'
          };
        }

        return {
          title: 'Session de caisse ouverte',
          message: 'La session de caisse est ouverte. Le Caissier est responsable des opérations de la journée : il doit enregistrer les entrées et sorties autorisées, vérifier les mouvements saisis, puis soumettre la session au contrôle. Le Contrôleur interviendra seulement après cette soumission pour comparer le solde théorique, le solde physique et traiter les écarts éventuels.',
          currentStep: 'Opérations journée',
          nextStep: 'Soumission au contrôle',
          expectedRole: 'CAISSIER',
          expectedAction: 'Enregistrer les opérations autorisées, vérifier les mouvements et soumettre la session au contrôle',
          severity: 'info'
        };
      case 'PRE_CLOTUREE':
        return {
          title: 'Session en contrôle',
          message: 'La session est soumise au contrôle. Le Contrôleur doit comparer le solde théorique, le solde physique, les entrées, les sorties et les écarts éventuels. Cette étape permet de valider la conformité de la caisse avant clôture.',
          currentStep: 'Contrôle physique',
          nextStep: 'Contrôle du Contrôleur',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Valider le contrôle de caisse',
          severity: ecart !== 0 ? 'danger' : 'warning'
        };
      case 'ECART_CONSTATE':
        return {
          title: 'Écart de caisse constaté',
          message: 'Un écart de caisse est constaté. Une justification est obligatoire. Le Contrôleur doit analyser l’écart, documenter l’observation et déclencher une régularisation ou une validation hiérarchique si nécessaire.',
          currentStep: 'Comparaison solde théorique / solde physique',
          nextStep: 'Validation',
          expectedRole: 'CONTROLEUR',
          expectedAction: 'Justifier l’écart et valider le contrôle',
          severity: 'danger'
        };
      case 'VALIDEE_CONTROLE':
        return {
          title: 'Session validée',
          message: 'La session de caisse est validée par le Contrôleur. La clôture peut être finalisée selon les droits autorisés. Les mouvements, écarts et validations doivent rester historisés pour l’audit.',
          currentStep: 'Validation',
          nextStep: 'Clôture',
          expectedRole: 'CHEF_BUREAU',
          expectedAction: 'Clôturer définitivement la session',
          severity: 'success'
        };
      case 'CLOTUREE':
        return {
          title: 'Session clôturée',
          message: 'La session de caisse est clôturée. Les mouvements sont historisés et consultables pour le contrôle interne, l’audit et les rapports de caisse.',
          currentStep: 'Clôture',
          severity: 'success'
        };
      case 'ANNULEE':
      case 'ANNULEE_ADMINISTRATIVEMENT':
        return {
          title: 'Session annulée',
          message: 'Cette session présente une anomalie ou une annulation contrôlée. Les actions doivent rester tracées avec l’utilisateur, la date, l’heure, l’antenne et le commentaire éventuel. Les droits d’annulation ou de régularisation doivent rester strictement limités aux rôles autorisés.',
          currentStep: 'Clôture',
          severity: 'danger'
        };
      default:
        return this.defaultDefinition(status);
    }
  }

  private epargneOperationDefinition(status: string, metadata?: Record<string, unknown>): WorkflowDefinition {
    const compteSelectionne = metadata?.['compteSelectionne'] === true;

    switch (status) {
      case 'OPERATION_AUTORISEE':
        return {
          title: 'Opérations épargne autorisées',
          message: compteSelectionne
            ? 'Le compte épargne est sélectionné. Vous pouvez enregistrer une opération autorisée, vérifier les montants et compléter les références nécessaires avant validation.'
            : 'Vous pouvez enregistrer des opérations épargne autorisées. Sélectionnez un compte, vérifiez les montants puis validez l’opération selon les règles métier.',
          currentStep: compteSelectionne ? 'Compte sélectionné' : 'Consultation des comptes',
          nextStep: 'Enregistrement de l’opération',
          expectedRole: this.readRoleFromMetadata(metadata) || 'GESTIONNAIRE',
          expectedAction: compteSelectionne ? 'Enregistrer l’opération sur le compte sélectionné' : 'Sélectionner un compte puis enregistrer une opération',
          severity: 'success'
        };
      case 'OPERATION_BLOQUEE':
        return {
          title: 'Opération épargne non autorisée',
          message: 'Vous êtes en mode consultation. Les opérations terrain doivent être encodées dans Ma collecte du jour et les autres opérations doivent être réalisées par le rôle habilité.',
          currentStep: 'Consultation des comptes',
          nextStep: 'Traitement par le rôle habilité',
          expectedRole: this.readRoleFromMetadata(metadata) || 'GESTIONNAIRE',
          expectedAction: 'Utiliser le canal autorisé selon le rôle',
          severity: 'warning'
        };
      default:
        return {
          title: 'Consultation des comptes épargne',
          message: 'Consultez les comptes épargne, vérifiez les soldes disponibles et les soldes bloqués, puis préparez l’étape suivante selon votre rôle.',
          currentStep: 'Consultation des comptes',
          nextStep: 'Sélection du compte',
          expectedRole: this.readRoleFromMetadata(metadata) || 'GESTIONNAIRE',
          expectedAction: 'Sélectionner un compte pour consulter le détail des opérations',
          severity: 'info'
        };
    }
  }

  private defaultDefinition(status: string): WorkflowDefinition {
    return {
      title: 'Étape métier en cours',
      message: `Le dossier est actuellement au statut ${status || 'inconnu'}.`,
      currentStep: 'Traitement en cours',
      severity: 'info'
    };
  }

  private adaptForCurrentActor(message: string, expectedRole: string): string {
    const replacements: Record<string, Array<[RegExp, string]>> = {
      CONTROLEUR: [[/Le Contrôleur doit maintenant/gi, 'Vous devez maintenant']],
      CAISSIER: [[/Le Caissier doit maintenant/gi, 'Vous devez maintenant']],
      CHEF_BUREAU: [[/Le Chef de Bureau peut maintenant/gi, 'Vous pouvez maintenant']],
      GESTIONNAIRE: [[/Le Gestionnaire doit maintenant/gi, 'Vous devez maintenant']],
      AGENT_TERRAIN: [[/L’Agent Terrain doit/gi, 'Vous devez']]
    };

    const replacers = replacements[expectedRole] || [];
    return replacers.reduce((current, [pattern, replacement]) => current.replace(pattern, replacement), message);
  }

  private buildBlockedReason(input: {
    module: WorkflowModule;
    currentRole?: string;
    expectedRole?: string;
    expectedAction?: string;
    status?: string;
    metadata?: Record<string, unknown>;
  }): string {
    const normalizedStatus = this.normalizeRole(input.status);
    if (input.module === 'CREDIT' && input.status === 'VALIDATION_CHEF') {
      const garantieStatus = String(input.metadata?.['garantieStatus'] || '').toUpperCase();
      const garantieBloquee = input.metadata?.['garantieBloquee'] === true;
      if (garantieStatus === 'INSUFFISANTE' || !garantieBloquee) {
        return 'Vous ne pouvez pas approuver ce crédit. La garantie doit d’abord être validée par le Contrôleur.';
      }
    }

    if (input.module === 'CREDIT' && (input.status === 'SOUMISE' || input.status === 'BROUILLON')) {
      return 'Vous ne pouvez pas agir à cette étape. Le Gestionnaire doit d’abord vérifier les informations du membre, effectuer la pré-analyse et préparer le dossier. Ensuite, le Contrôleur pourra reprendre le dossier pour l’analyse de risque.';
    }

    if (input.module === 'CREDIT' && input.status === 'EN_ANALYSE') {
      return 'Vous ne pouvez pas agir à cette étape. Le Contrôleur doit d’abord analyser le dossier, vérifier les risques et formuler son avis. Ensuite, le dossier pourra avancer vers la garantie puis l’approbation.';
    }

    if (input.module === 'CREDIT' && (input.status === 'ANALYSE_TERRAIN_VALIDEE' || input.status === 'VALIDATION_CONTROLEUR')) {
      return 'Vous ne pouvez pas agir à cette étape. Le Contrôleur doit d’abord vérifier la garantie, confirmer que les 20 % requis sont disponibles et bloquer les fonds nécessaires. Ensuite, le Chef de Bureau pourra examiner le dossier pour l’approbation.';
    }

    if (input.module === 'CREDIT' && (input.status === 'APPROUVEE' || input.status === 'APPROUVE')) {
      return 'Vous ne pouvez pas décaisser ce crédit. Cette action est réservée au Caissier après approbation et validation de la garantie.';
    }

    if (input.module === 'RETRAIT_EPARGNE' && (input.status === 'DEMANDE' || input.status === 'CREEE' || input.status === 'EN_ATTENTE_VALIDATION')) {
      return 'Vous ne pouvez pas agir à cette étape. Le Contrôleur doit d’abord vérifier le compte épargne, le solde disponible et les garanties bloquées avant toute décision. Ensuite, le Caissier pourra intervenir uniquement si le retrait est approuvé.';
    }

    if (input.module === 'RETRAIT_EPARGNE' && (input.status === 'APPROUVEE' || input.status === 'VALIDEE')) {
      return 'Vous ne pouvez pas agir à cette étape. Le Caissier doit maintenant effectuer le paiement et enregistrer l’opération. Après paiement, le retrait sera historisé.';
    }

    if (input.module === 'RECETTE_JOURNALIERE' && input.status === 'SOUMISE') {
      return 'Vous ne pouvez pas agir à cette étape. Le Caissier doit d’abord effectuer le billetage avec l’Agent Terrain pour confirmer les espèces remises. Ensuite, le Contrôleur pourra vérifier les montants, les écarts et les informations déclarées.';
    }

    if (input.module === 'RECETTE_JOURNALIERE' && (input.status === 'BILLETAGE_CONFIRME' || input.status === 'ECART_CONSTATE' || input.status === 'VALIDEE')) {
      return 'Vous ne pouvez pas agir à cette étape. Le Contrôleur doit d’abord contrôler la recette, analyser les écarts éventuels et valider les données. Ensuite, le système pourra générer les opérations officielles.';
    }

    if (input.module === 'CAISSE' && input.status === 'OUVERTE') {
      if (input.metadata?.['isAncienneOuverte'] === true) {
        return 'Vous ne pouvez pas encore contrôler cette session. Elle est toujours ouverte : le Caissier doit d’abord vérifier les mouvements, régulariser si nécessaire et la soumettre au contrôle. Ensuite, le Contrôleur pourra comparer le solde physique, le solde théorique et traiter les écarts.';
      }

      return 'Vous ne pouvez pas agir à cette étape. Cette session est encore ouverte : le Caissier doit d’abord enregistrer ou vérifier les mouvements autorisés, puis soumettre la session au contrôle. Ensuite, le Contrôleur pourra effectuer le contrôle physique, comparer le solde théorique et le solde physique, puis traiter les écarts éventuels.';
    }

    if (input.module === 'CAISSE' && input.status === 'PRE_CLOTUREE') {
      return 'Vous ne pouvez pas agir à cette étape. Le Contrôleur doit d’abord comparer le solde théorique, le solde physique, les entrées, les sorties et les écarts éventuels. Ensuite, la session pourra être validée puis clôturée selon les droits autorisés.';
    }

    if (input.module === 'CAISSE' && input.status === 'ECART_CONSTATE') {
      return 'Vous ne pouvez pas agir à cette étape. Le Contrôleur doit d’abord documenter l’écart, analyser l’observation et décider si une régularisation ou une validation hiérarchique est nécessaire avant la clôture.';
    }

    if (input.module === 'EPARGNE_OPERATION' && input.status === 'OPERATION_BLOQUEE') {
      return 'Vous ne pouvez pas enregistrer cette opération depuis cet écran. Les opérations terrain doivent être encodées dans Ma collecte du jour et les autres opérations restent réservées au rôle habilité.';
    }

    if (input.module === 'DASHBOARD_ROLE' && input.status === 'AGENT_TERRAIN') {
      return 'L’Agent Terrain ne valide pas les opérations financières. Après transmission, les étapes de billetage, contrôle et validation relèvent des rôles autorisés.';
    }

    if (input.module === 'DASHBOARD_ROLE' && input.status === 'GESTIONNAIRE') {
      return 'Le Gestionnaire ne valide pas les opérations financières, ne décaisse pas et n’approuve pas les crédits.';
    }

    if (input.module === 'DASHBOARD_ROLE' && input.status === 'CONTROLEUR') {
      return 'Le Contrôleur contrôle et valide certaines étapes, mais le paiement ou décaissement appartient au Caissier lorsque le workflow l’exige.';
    }

    if (input.module === 'DASHBOARD_ROLE' && input.status === 'CAISSIER') {
      return 'Le Caissier ne doit pas payer une dépense ou un retrait non validé, ni décaisser un crédit sans approbation, garantie validée et dossier complet.';
    }

    if (input.module === 'DASHBOARD_ROLE' && normalizedStatus === 'CHEF_BUREAU') {
      return 'L’approbation ne remplace pas les contrôles préalables : un crédit ne doit pas être approuvé ou décaissé sans analyse, garantie et dossier complet.';
    }

    if (input.module === 'DASHBOARD_ROLE' && input.status === 'COO') {
      return 'Le COO supervise et peut intervenir selon les droits existants, mais ne doit pas contourner les étapes de contrôle, caisse ou approbation prévues.';
    }

    if (input.module === 'DASHBOARD_ROLE' && input.status === 'RCI') {
      return 'Le RCI audite et contrôle ; il ne doit pas remplacer les opérations quotidiennes des rôles terrain, caisse ou contrôle sauf droit existant clairement prévu.';
    }

    if (input.module === 'DASHBOARD_ROLE' && (input.status === 'GERANT_GENERAL' || input.status === 'ADMIN')) {
      return 'Le Gérant Général arbitre et supervise ; les validations opérationnelles doivent rester traçables et respecter les workflows métier définis.';
    }

    if (input.module === 'DEPENSE_CAISSE' && input.status === 'EN_ATTENTE_VALIDATION' && input.currentRole === 'CAISSIER') {
      return 'Vous ne pouvez pas encore payer cette dépense. Elle doit d’abord être validée par le rôle autorisé. Après validation, le Caissier pourra effectuer le paiement et enregistrer la sortie de caisse.';
    }

    if (input.module === 'DEPENSE_CAISSE' && (input.status === 'VALIDEE' || input.status === 'APPROUVEE') && input.currentRole !== 'CAISSIER') {
      return 'Vous ne pouvez pas payer cette dépense. Le paiement appartient au Caissier après validation.';
    }

    const expectedRoleLabel = this.labelForRole(input.expectedRole);
    const expectedAction = input.expectedAction || 'agir sur ce dossier';
    if (expectedRoleLabel) {
      return `Vous ne pouvez pas agir à cette étape. Cette action est réservée au ${expectedRoleLabel.toLowerCase()} pour ${expectedAction.toLowerCase()}.`;
    }

    return 'Vous ne pouvez pas agir à cette étape du workflow 3N.';
  }

  private labelForRole(role?: string): string | undefined {
    const normalizedRole = this.normalizeRole(role);
    const labels: Record<string, string> = {
      ADMIN: 'Administrateur',
      GESTIONNAIRE: 'Gestionnaire',
      CONTROLEUR: 'Contrôleur',
      CHEF_BUREAU: 'Chef de Bureau',
      CAISSIER: 'Caissier',
      AGENT_TERRAIN: 'Agent Terrain',
      MEMBER: 'Demandeur',
      RCI: 'RCI',
      COO: 'COO',
      GERANT_GENERAL: 'Gérant Général'
    };

    return normalizedRole ? labels[normalizedRole] || normalizedRole : undefined;
  }

  private readRoleFromMetadata(metadata?: Record<string, unknown>): string | undefined {
    const value = metadata?.['expectedRole'];
    return typeof value === 'string' ? value : undefined;
  }

  private includesAny(value: string, segments: string[]): boolean {
    return segments.some(segment => value.includes(segment));
  }
}