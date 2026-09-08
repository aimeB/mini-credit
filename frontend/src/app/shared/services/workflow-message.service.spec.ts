import { TestBed } from '@angular/core/testing';

import { WorkflowMessageService } from './workflow-message.service';

describe('WorkflowMessageService', () => {
  let service: WorkflowMessageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WorkflowMessageService);
  });

  describe('CREDIT', () => {
    it('demande créée: prochain acteur Gestionnaire', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'SOUMISE',
        currentRole: 'CAISSIER'
      });

      expect(guidance.message).toContain('La demande de crédit est enregistrée');
      expect(guidance.message).toContain('cohérence générale du dossier');
      expect(guidance.nextStep).toContain('Pré-analyse');
      expect(guidance.expectedRole).toBe('Gestionnaire');
      expect(guidance.canCurrentUserAct).toBeFalse();
      expect(guidance.blockedReason).toContain('Gestionnaire');
    });

    it('pré-analyse terminée: prochain acteur Contrôleur', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'EN_ANALYSE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.message).toContain('Vous devez maintenant analyser le dossier');
      expect(guidance.message).toContain('garanties et les risques');
      expect(guidance.expectedRole).toBe('Contrôleur');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('garantie validée: prochain acteur Chef de Bureau', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'VALIDATION_CHEF',
        currentRole: 'CHEF_BUREAU',
        metadata: {
          garantieStatus: 'VALIDEE',
          garantieBloquee: true
        }
      });

      expect(guidance.expectedRole).toBe('Chef de Bureau');
      expect(guidance.nextStep).toContain('Décaissement');
      expect(guidance.message).toContain('examiner la demande, l’analyse, la garantie');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('crédit approuvé: prochain acteur Caissier', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'APPROUVE',
        currentRole: 'CAISSIER'
      });

      expect(guidance.expectedRole).toBe('Caissier');
      expect(guidance.message).toContain('Aucun décaissement n’est autorisé sans ces conditions');
      expect(guidance.message).toContain('garantie est validée');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('crédit décaissé: phase remboursement', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'DECAISSE',
        currentRole: 'GESTIONNAIRE'
      });

      expect(guidance.currentStep).toBe('Remboursement');
      expect(guidance.message).toContain('phase de remboursement');
      expect(guidance.message).toContain('échéances doivent être suivies');
    });

    it('crédit clôturé: dossier clôturé', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'CLOTUREE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.title).toContain('Crédit clôturé');
      expect(guidance.message).toContain('dossier doit rester consultable');
    });
  });

  describe('RECETTE_JOURNALIERE', () => {
    it('brouillon: Agent Terrain complète', () => {
      const guidance = service.getGuidance({
        module: 'RECETTE_JOURNALIERE',
        status: 'BROUILLON',
        currentRole: 'AGENT_TERRAIN'
      });

      expect(guidance.expectedRole).toBe('Agent Terrain');
      expect(guidance.message).toContain('membres visités');
      expect(guidance.message).toContain('espèces remises');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('soumise: billetage Agent Terrain + Caissier', () => {
      const guidance = service.getGuidance({
        module: 'RECETTE_JOURNALIERE',
        status: 'SOUMISE',
        currentRole: 'CAISSIER'
      });

      expect(guidance.currentStep).toContain('Billetage');
      expect(guidance.expectedRole).toBe('Caissier');
      expect(guidance.message).toContain('confirmer les espèces remises');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('billetage confirmé: Contrôleur contrôle', () => {
      const guidance = service.getGuidance({
        module: 'RECETTE_JOURNALIERE',
        status: 'BILLETAGE_CONFIRME',
        currentRole: 'AGENT_TERRAIN'
      });

      expect(guidance.expectedRole).toBe('Contrôleur');
      expect(guidance.message).toContain('comparer les montants collectés');
      expect(guidance.message).toContain('écarts éventuels');
      expect(guidance.canCurrentUserAct).toBeFalse();
      expect((guidance.blockedReason || '').toLowerCase()).toContain('contrôleur');
    });

    it('écart constaté: justification obligatoire', () => {
      const guidance = service.getGuidance({
        module: 'RECETTE_JOURNALIERE',
        status: 'ECART_CONSTATE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.message).toContain('justification est obligatoire');
      expect(guidance.message).toContain('demander des explications');
      expect(guidance.severity).toBe('danger');
    });

    it('validée: génération opérations officielles', () => {
      const guidance = service.getGuidance({
        module: 'RECETTE_JOURNALIERE',
        status: 'VALIDEE',
        currentRole: 'CONTROLEUR',
        metadata: { operationsGenerated: false }
      });

      expect(guidance.message).toContain('opérations d’épargne');
      expect(guidance.message).toContain('mouvements caisse');
      expect(guidance.expectedRole).toBe('Contrôleur');
    });
  });

  describe('RETRAIT_EPARGNE', () => {
    it('demande créée: Contrôleur vérifie', () => {
      const guidance = service.getGuidance({
        module: 'RETRAIT_EPARGNE',
        status: 'DEMANDE',
        currentRole: 'MEMBER'
      });

      expect(guidance.expectedRole).toBe('Contrôleur');
      expect(guidance.message).toContain('vérifier l’identité du membre');
      expect(guidance.message).toContain('Le membre ne peut retirer que le solde disponible');
      expect(guidance.canCurrentUserAct).toBeFalse();
    });

    it('approuvée: Caissier paie', () => {
      const guidance = service.getGuidance({
        module: 'RETRAIT_EPARGNE',
        status: 'APPROUVEE',
        currentRole: 'CAISSIER'
      });

      expect(guidance.expectedRole).toBe('Caissier');
      expect(guidance.message).toContain('effectuer le paiement au membre');
      expect(guidance.message).toContain('Après paiement');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('rejetée: motif visible', () => {
      const guidance = service.getGuidance({
        module: 'RETRAIT_EPARGNE',
        status: 'REJETEE',
        currentRole: 'MEMBER',
        metadata: { motifRejet: 'Solde insuffisant' }
      });

      expect(guidance.message).toContain('Aucun paiement ne doit être effectué');
      expect(guidance.expectedAction).toContain('Solde insuffisant');
    });

    it('payée: opération historisée', () => {
      const guidance = service.getGuidance({
        module: 'RETRAIT_EPARGNE',
        status: 'PAYEE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.message).toContain('solde du compte épargne doit refléter le paiement');
      expect(guidance.currentStep).toBe('Historisation');
    });
  });

  describe('CAISSE', () => {
    it('session ouverte: Caissier opère', () => {
      const guidance = service.getGuidance({
        module: 'CAISSE',
        status: 'OUVERTE',
        currentRole: 'CAISSIER'
      });

      expect(guidance.expectedRole).toBe('Caissier');
      expect(guidance.message).toContain('Le Caissier est responsable des opérations de la journée');
      expect(guidance.message).toContain('soumettre la session au contrôle');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('session ouverte vue par un Contrôleur: message détaillé de blocage', () => {
      const guidance = service.getGuidance({
        module: 'CAISSE',
        status: 'OUVERTE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.expectedRole).toBe('Caissier');
      expect(guidance.nextStep).toBe('Soumission au contrôle');
      expect(guidance.canCurrentUserAct).toBeFalse();
      expect(guidance.message).toContain('Le Caissier est responsable des opérations de la journée');
      expect(guidance.message).toContain('Le Contrôleur interviendra seulement après cette soumission');
      expect(guidance.message).toContain('solde théorique');
      expect(guidance.message).toContain('solde physique');
      expect(guidance.message).toContain('écarts éventuels');
      expect(guidance.blockedReason).toContain('Cette session est encore ouverte');
      expect(guidance.blockedReason).toContain('Caissier doit d’abord');
      expect(guidance.blockedReason).toContain('soumettre la session au contrôle');
      expect(guidance.blockedReason).toContain('Contrôleur pourra effectuer le contrôle physique');
    });

    it('session en contrôle: Contrôleur compare les soldes', () => {
      const guidance = service.getGuidance({
        module: 'CAISSE',
        status: 'PRE_CLOTUREE',
        currentRole: 'CAISSIER'
      });

      expect(guidance.expectedRole).toBe('Contrôleur');
      expect(guidance.nextStep).toBe('Contrôle du Contrôleur');
      expect(guidance.message).toContain('comparer le solde théorique, le solde physique');
      expect(guidance.message).toContain('valider la conformité de la caisse');
      expect(guidance.canCurrentUserAct).toBeFalse();
      expect((guidance.blockedReason || '').toLowerCase()).toContain('contrôleur');
    });

    it('écart constaté: justification obligatoire', () => {
      const guidance = service.getGuidance({
        module: 'CAISSE',
        status: 'ECART_CONSTATE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.message).toContain('justification est obligatoire');
      expect(guidance.message).toContain('validation hiérarchique');
      expect(guidance.severity).toBe('danger');
    });

    it('session validée: clôture possible selon droits', () => {
      const guidance = service.getGuidance({
        module: 'CAISSE',
        status: 'VALIDEE_CONTROLE',
        currentRole: 'CHEF_BUREAU'
      });

      expect(guidance.expectedRole).toBe('Chef de Bureau');
      expect(guidance.nextStep).toBe('Clôture');
      expect(guidance.message).toContain('doivent rester historisés pour l’audit');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('session clôturée: audit/consultation', () => {
      const guidance = service.getGuidance({
        module: 'CAISSE',
        status: 'CLOTUREE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.message).toContain('consultables pour le contrôle interne');
      expect(guidance.currentStep).toBe('Clôture');
    });
  });

  describe('DEPENSE_CAISSE', () => {
    it('en attente de validation: rôle validateur attendu', () => {
      const guidance = service.getGuidance({
        module: 'DEPENSE_CAISSE',
        status: 'EN_ATTENTE_VALIDATION',
        currentRole: 'CHEF_BUREAU'
      });

      expect(guidance.title).toContain('Dépense en attente de validation');
      expect(guidance.expectedRole).toBe('Chef de Bureau');
      expect(guidance.expectedAction).toContain('valider ou rejeter');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('en attente de validation vue par Caissier: message de blocage explicite', () => {
      const guidance = service.getGuidance({
        module: 'DEPENSE_CAISSE',
        status: 'EN_ATTENTE_VALIDATION',
        currentRole: 'CAISSIER'
      });

      expect(guidance.canCurrentUserAct).toBeFalse();
      expect(guidance.blockedReason).toContain('Vous ne pouvez pas encore payer cette dépense');
      expect(guidance.blockedReason).toContain('Elle doit d’abord être validée');
    });

    it('dépense validée: paiement réservé au Caissier', () => {
      const guidance = service.getGuidance({
        module: 'DEPENSE_CAISSE',
        status: 'VALIDEE',
        currentRole: 'ADMIN'
      });

      expect(guidance.expectedRole).toBe('Caissier');
      expect(guidance.nextStep).toBe('Paiement');
      expect(guidance.canCurrentUserAct).toBeFalse();
      expect(guidance.blockedReason).toContain('Le paiement appartient au Caissier après validation');
    });

    it('dépense payée: étape historisation/contrôle', () => {
      const guidance = service.getGuidance({
        module: 'DEPENSE_CAISSE',
        status: 'PAYEE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.currentStep).toBe('Dépense payée');
      expect(guidance.nextStep).toBe('Historisation / contrôle');
      expect(guidance.message).toContain('historisée');
    });
  });

  describe('403 contextualisé', () => {
    it('action crédit interdite pour mauvais rôle: message métier clair', () => {
      const forbidden = service.buildForbiddenMessage({
        requestUrl: '/api/credits/123/approve',
        currentRole: 'CAISSIER',
        metadata: { expectedRole: 'CHEF_BUREAU' }
      });

      expect(forbidden.title).toContain('Vous ne pouvez pas effectuer cette action');
      expect(forbidden.detail).toContain('Vous ne pouvez pas approuver ce crédit');
      expect(forbidden.detail).toContain('Chef de Bureau');
    });

    it('action caisse interdite au Contrôleur sur opération caissier: message métier clair', () => {
      const forbidden = service.buildForbiddenMessage({
        requestUrl: '/api/caisses/operations',
        currentRole: 'CONTROLEUR',
        metadata: { expectedRole: 'CAISSIER' }
      });

      expect(forbidden.detail).toContain('réservées au Caissier');
      expect(forbidden.detail).toContain('après soumission au contrôle');
    });

    it('action décaissement interdite hors Caissier: message métier clair', () => {
      const forbidden = service.buildForbiddenMessage({
        requestUrl: '/api/credits/decaissement',
        currentRole: 'CONTROLEUR',
        status: 'APPROUVEE'
      });

      expect(forbidden.detail).toContain('réservé au Caissier');
      expect(forbidden.detail).toContain('validation de la garantie');
    });

    it('fallback générique seulement sans contexte métier identifiable', () => {
      const forbidden = service.buildForbiddenMessage({
        requestUrl: '/api/autre/endpoint-inconnu',
        currentRole: 'ADMIN'
      });

      expect(forbidden.title).toContain('Vous ne pouvez pas effectuer cette action');
      expect(forbidden.detail).toContain('rôle attendu');
      expect(forbidden.detail).not.toContain('rôle responsable');
    });
  });

  describe('logique rôle courant', () => {
    it('canCurrentUserAct=true quand le rôle courant est le rôle attendu', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'APPROUVE',
        currentRole: 'CAISSIER'
      });

      expect(guidance.canCurrentUserAct).toBeTrue();
      expect(guidance.blockedReason).toBeUndefined();
    });

    it('canCurrentUserAct=false quand le rôle courant est différent', () => {
      const guidance = service.getGuidance({
        module: 'CREDIT',
        status: 'APPROUVE',
        currentRole: 'CONTROLEUR'
      });

      expect(guidance.canCurrentUserAct).toBeFalse();
      expect((guidance.blockedReason || '').toLowerCase()).toContain('caissier');
    });

    it('canCurrentUserAct=true quand le rôle courant est hiérarchiquement supérieur', () => {
      const guidance = service.getGuidance({
        module: 'CAISSE',
        status: 'PRE_CLOTUREE',
        currentRole: 'CHEF_BUREAU'
      });

      expect(guidance.canCurrentUserAct).toBeTrue();
      expect(guidance.blockedReason).toBeUndefined();
    });
  });

  describe('DASHBOARD_ROLE', () => {
    it('Gestionnaire reste un rôle métier officiel', () => {
      const guidance = service.getGuidance({
        module: 'DASHBOARD_ROLE',
        status: 'GESTIONNAIRE',
        currentRole: 'GESTIONNAIRE'
      });

      expect(guidance.title).toContain('Gestionnaire');
      expect(guidance.expectedRole).toBe('Gestionnaire');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('rôle inconnu affiche la guidance générique sans action utilisateur', () => {
      const guidance = service.getGuidance({
        module: 'DASHBOARD_ROLE',
        status: 'ROLE_INCONNU',
        currentRole: 'ROLE_INCONNU'
      });

      expect(guidance.title).toContain('Tableau de bord opérationnel 3N');
      expect(guidance.expectedRole).toBe('Gestionnaire');
      expect(guidance.canCurrentUserAct).toBeFalse();
    });

    it('Agent Terrain: guidance dédiée avec limite métier', () => {
      const guidance = service.getGuidance({
        module: 'DASHBOARD_ROLE',
        status: 'AGENT_TERRAIN',
        currentRole: 'AGENT_TERRAIN'
      });

      expect(guidance.title).toContain('Agent Terrain');
      expect(guidance.message).toContain('membres recrutés');
      expect(guidance.expectedAction).toContain('Compléter les données terrain');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('Caissier: guidance dédiée avec limite de paiement', () => {
      const guidance = service.getGuidance({
        module: 'DASHBOARD_ROLE',
        status: 'CAISSIER',
        currentRole: 'CAISSIER'
      });

      expect(guidance.title).toContain('Caissier');
      expect(guidance.message).toContain('tenir la caisse');
      expect(guidance.expectedAction).toContain('effectuer les paiements autorisés');
      expect(guidance.canCurrentUserAct).toBeTrue();
    });

    it('Transverse: guidance globale 3N', () => {
      const guidance = service.getGuidance({
        module: 'DASHBOARD_ROLE',
        status: 'TRANSVERSE',
        currentRole: 'MEMBER'
      });

      expect(guidance.title).toContain('Tableau de bord opérationnel 3N');
      expect(guidance.message).toContain('workflows 3N restent organisés par rôle');
      expect(guidance.canCurrentUserAct).toBeFalse();
      expect(guidance.blockedReason).toContain('Cette action est réservée');
    });
  });
});
