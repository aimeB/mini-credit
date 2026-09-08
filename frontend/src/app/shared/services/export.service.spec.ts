import { TestBed } from '@angular/core/testing';

import { ExportService } from './export.service';

describe('ExportService', () => {
  let service: ExportService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ExportService] });
    service = TestBed.inject(ExportService);
    spyOn<any>(service, 'writeWorkbook').and.stub();
  });

  it('exporte le détail collecte par membre', () => {
    service.exportCollectesDetailByMembreExcel([
      {
        id: 1,
        agentTerrainId: 10,
        agentTerrainNom: 'Agent A',
        siteId: 3,
        siteNom: 'Site A',
        antenneId: 5,
        dateCollecte: '2026-06-15',
        statut: 'SOUMISE',
        especesRemises: 120,
        totalEpargneCalcule: 100,
        totalRemboursementsCalcule: 10,
        totalFraisCalcule: 10,
        totalCarnetsCalcule: 0,
        totalGeneralCalcule: 120,
        ecartTresorerie: 0,
        lignes: [{ id: 1, membreId: 99, membreNom: 'Marie', typeLigne: 'EPARGNE', montant: 100, quantite: 1 }]
      }
    ] as any);

    expect((service as any).writeWorkbook).toHaveBeenCalled();
    const workbook = ((service as any).writeWorkbook as jasmine.Spy).calls.mostRecent().args[0] as any;
    expect(workbook.SheetNames).toContain('CollectesDetailMembres');
  });

  it('exporte le résumé journalier par agent', () => {
    service.exportCollectesResumeJournalierParAgentExcel([
      {
        id: 1,
        agentTerrainId: 10,
        agentTerrainNom: 'Agent A',
        siteId: 3,
        siteNom: 'Site A',
        antenneId: 5,
        dateCollecte: '2026-06-15',
        statut: 'SOUMISE',
        especesRemises: 120,
        totalEpargneCalcule: 100,
        totalRemboursementsCalcule: 10,
        totalFraisCalcule: 10,
        totalCarnetsCalcule: 0,
        totalGeneralCalcule: 120,
        ecartTresorerie: 0,
        lignes: [{ id: 1, membreId: 99, typeLigne: 'CARNET', montant: 1, quantite: 2 }]
      }
    ] as any);

    expect((service as any).writeWorkbook).toHaveBeenCalled();
    const workbook = ((service as any).writeWorkbook as jasmine.Spy).calls.mostRecent().args[0] as any;
    expect(workbook.SheetNames).toContain('CollectesResumeJournalier');
  });

  it('exporte les écarts de trésorerie', () => {
    service.exportCollectesEcartsTresorerieExcel([
      {
        id: 1,
        agentTerrainId: 10,
        agentTerrainNom: 'Agent A',
        siteId: 3,
        siteNom: 'Site A',
        antenneId: 5,
        dateCollecte: '2026-06-15',
        statut: 'SOUMISE',
        especesRemises: 120,
        totalEpargneCalcule: 100,
        totalRemboursementsCalcule: 10,
        totalFraisCalcule: 10,
        totalCarnetsCalcule: 0,
        totalGeneralCalcule: 120,
        ecartTresorerie: -5,
        lignes: []
      }
    ] as any);

    expect((service as any).writeWorkbook).toHaveBeenCalled();
    const workbook = ((service as any).writeWorkbook as jasmine.Spy).calls.mostRecent().args[0] as any;
    expect(workbook.SheetNames).toContain('EcartsTresorerieCollecte');
  });
});
