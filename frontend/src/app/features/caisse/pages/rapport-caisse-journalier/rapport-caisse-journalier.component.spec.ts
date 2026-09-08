import { of } from 'rxjs';

import { RapportCaisseJournalierComponent } from './rapport-caisse-journalier.component';
import { RapportCaisseService } from '../../services/rapport-caisse.service';
import { OperationCaisseService } from '../../services/operation-caisse.service';
import { JournalCaisseResponse } from '../../models/journal-caisse-response';
import { RapportCaisseJournalier } from '../../models/rapport-caisse.model';
import { SourceOperationCaisse } from '../../models/source-operation-caisse';

describe('RapportCaisseJournalierComponent', () => {
  const rapportService = {} as RapportCaisseService;
  const operationService = {
    getJournalCaisse: jasmine.createSpy('getJournalCaisse').and.returnValue(of({ content: [] }))
  } as unknown as OperationCaisseService;

  function createComponent(): RapportCaisseJournalierComponent {
    return new RapportCaisseJournalierComponent(rapportService, operationService);
  }

  function rapport(partial: Partial<RapportCaisseJournalier> = {}): RapportCaisseJournalier {
    return {
      date: '2026-07-28',
      nombreSessions: 1,
      nombreOperations: 2,
      nombreSessionsNonCloturees: 0,
      totalEntrees: 1000,
      totalSorties: 250,
      soldeTheoriqueTotal: 750,
      soldePhysiqueTotal: 750,
      ecartTotal: 0,
      nombreDepenses: 0,
      montantDepenses: 0,
      nombreEcarts: 0,
      montantEcarts: 0,
      ...partial
    };
  }

  function transaction(partial: Partial<JournalCaisseResponse>): JournalCaisseResponse {
    return {
      operationId: partial.operationId ?? 1,
      sessionCaisseId: 7,
      caisseId: 3,
      caisseLibelle: 'Caisse principale',
      siteId: 2,
      siteLibelle: 'Site Gombe',
      dateOperation: '2026-07-28T08:00:00',
      utilisateurId: 12,
      utilisateurNom: 'Marie K.',
      roleUtilisateur: 'CAISSIER',
      typeOperation: 'ENTREE',
      categorie: 'APPROVISIONNEMENT',
      source: SourceOperationCaisse.APPROVISIONNEMENT,
      montant: 1000,
      devise: 'CDF',
      soldeApresOperation: 1000,
      referenceMetier: 'REC-1',
      ...partial
    };
  }

  it('confirme continuite, tracabilite, controle interne et exactitude du solde quand les donnees concordent', () => {
    const component = createComponent();
    component.hasSearched = true;
    component.loadingTransactions = false;
    component.rapport = rapport();
    component.transactions = [
      transaction({ operationId: 1, typeOperation: 'ENTREE', montant: 1000, soldeApresOperation: 1000 }),
      transaction({ operationId: 2, typeOperation: 'SORTIE', montant: 250, soldeApresOperation: 750 })
    ];

    expect(component.continuiteCaisseAssuree).toBeTrue();
    expect(component.tracabiliteComplete).toBeTrue();
    expect(component.soldeExact).toBeTrue();
    expect(component.controleInterneSousAlerte).toBeFalse();
    expect(component.soldeDisponibleControle).toBe(750);
    expect(component.caissiersResponsables).toBe('Marie K.');
  });

  it('signale une alerte quand une session reste ouverte ou que le detail ne correspond pas a la synthese', () => {
    const component = createComponent();
    component.hasSearched = true;
    component.loadingTransactions = false;
    component.rapport = rapport({ nombreSessionsNonCloturees: 1, ecartTotal: 25 });
    component.transactions = [
      transaction({ operationId: 1, typeOperation: 'ENTREE', montant: 900, source: undefined })
    ];

    expect(component.continuiteCaisseAssuree).toBeFalse();
    expect(component.tracabiliteComplete).toBeFalse();
    expect(component.soldeExact).toBeFalse();
    expect(component.controleInterneSousAlerte).toBeTrue();
  });
});