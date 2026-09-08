import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { CollecteDuJourComponent } from './collecte-du-jour.component';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { MembreService } from '../../../membres/services/membre.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Page } from '../../../../shared/models/page.model';
import { CompteEpargneService } from '../../../epargne/services/compte-epargne.service';
import { CreditService } from '../../../credit/services/credit.service';
import { ParametresMetierService } from '../../../../core/services/parametres-metier.service';

describe('CollecteDuJourComponent', () => {
  let component: CollecteDuJourComponent;
  let fixture: ComponentFixture<CollecteDuJourComponent>;
  let collecteServiceSpy: jasmine.SpyObj<CollecteTerrainService>;
  let membreServiceSpy: jasmine.SpyObj<MembreService>;
  let compteEpargneServiceSpy: jasmine.SpyObj<CompteEpargneService>;
  let creditServiceSpy: jasmine.SpyObj<CreditService>;
  let routeStub: { snapshot: { paramMap: ReturnType<typeof convertToParamMap> } };

  const collecte = {
    id: 1,
    agentTerrainId: 10,
    agentTerrainNom: 'Vita Son',
    siteId: 20,
    siteNom: 'Site de Sakombi',
    antenneId: 30,
    dateCollecte: '2026-06-15',
    statut: 'BROUILLON',
    lignes: [],
    totalCarnetsCalcule: 0,
    totalEpargneCalcule: 50000,
    totalRemboursementsCalcule: 0,
    totalFraisCalcule: 0,
    totalGeneralCalcule: 50000,
    ecartTresorerie: 0,
    especesRemises: 50000,
  } as any;

  const recap = {
    collecteId: 1,
    membresVisites: 1,
    nouveauxMembres: 0,
    carnetsVendusDistribues: 0,
    totalEpargne: 50000,
    totalRemboursements: 0,
    totalFrais: 0,
    totalGeneralAttendu: 50000,
    especesRemises: 50000,
    ecartTresorerie: 0,
  } as any;

  beforeEach(async () => {
    collecteServiceSpy = jasmine.createSpyObj<CollecteTerrainService>(
      'CollecteTerrainService',
      ['getToday', 'getById', 'create', 'addLigne', 'deleteLigne', 'soumettre', 'recap']
    );

    membreServiceSpy = jasmine.createSpyObj<MembreService>('MembreService', ['searchPaginated']);
    compteEpargneServiceSpy = jasmine.createSpyObj<CompteEpargneService>('CompteEpargneService', ['getByMembre']);
    creditServiceSpy = jasmine.createSpyObj<CreditService>('CreditService', ['getByMembre']);

    const membresPage: Page<any> = {
      content: [
        { id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A', telephonePrincipal: '099001' },
        { id: 102, codeMembre: 'MBR-B', nomComplet: 'Jean B', telephonePrincipal: '099002' },
      ],
      totalElements: 2,
      totalPages: 1,
      currentPage: 0,
      pageSize: 20,
      hasNext: false,
      hasPrevious: false,
    };

    routeStub = {
      snapshot: {
        paramMap: convertToParamMap({}),
      },
    };

    collecteServiceSpy.getToday.and.returnValue(of(collecte));
    collecteServiceSpy.getById.and.returnValue(of(collecte));
    collecteServiceSpy.create.and.returnValue(of(collecte));
    collecteServiceSpy.addLigne.and.returnValue(of({ id: 77 } as any));
    collecteServiceSpy.deleteLigne.and.returnValue(of(undefined));
    collecteServiceSpy.soumettre.and.returnValue(of({ ...collecte, statut: 'SOUMISE' } as any));
    collecteServiceSpy.recap.and.returnValue(of(recap));
    membreServiceSpy.searchPaginated.and.returnValue(of(membresPage));
    compteEpargneServiceSpy.getByMembre.and.returnValue(of([{ id: 44, statut: 'ACTIF' } as any]));
    creditServiceSpy.getByMembre.and.returnValue(of([{ id: 88, statut: 'EN_COURS' } as any]));

    await TestBed.configureTestingModule({
      imports: [CollecteDuJourComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: CollecteTerrainService, useValue: collecteServiceSpy },
        { provide: MembreService, useValue: membreServiceSpy },
        { provide: CompteEpargneService, useValue: compteEpargneServiceSpy },
        { provide: CreditService, useValue: creditServiceSpy },
        {
          provide: ParametresMetierService,
          useValue: {
            getDecimal: () => 1000,
          }
        },
        {
          provide: AuthService,
          useValue: {
            getCurrentUser: () => ({ nomComplet: 'Vita Son' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CollecteDuJourComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('selection membre A et ajout EPARGNE envoie le bon payload', () => {
    const membreA = { id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any;
    component.chooseMembre(membreA);

    component.lineForm.patchValue({
      typeLigne: 'EPARGNE',
      montant: 50000,
      reference: 'COL-001',
      commentaire: 'Collecte terrain'
    });

    component.addLigne();

    expect(collecteServiceSpy.addLigne).toHaveBeenCalledWith(1, jasmine.objectContaining({
      membreId: 101,
      typeLigne: 'EPARGNE',
      montant: 50000,
      reference: 'COL-001'
    }));
  });

  it('changer vers membre B ne copie pas le montant de membre A', () => {
    const membreA = { id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any;
    const membreB = { id: 102, codeMembre: 'MBR-B', nomComplet: 'Jean B' } as any;

    component.chooseMembre(membreA);
    component.lineForm.patchValue({ montant: 50000 });

    component.chooseMembre(membreB);

    expect(component.lineForm.get('membreId')?.value).toBe(102);
    expect(component.lineForm.get('montant')?.value).not.toBe(50000);
  });

  it('apres ajout, rafraichit tableau et recap', () => {
    const refreshed = {
      ...collecte,
      lignes: [{ id: 77, membreId: 101, membreNom: 'Marie A', typeLigne: 'EPARGNE', montant: 50000, quantite: 0, montantSouhaite: null }]
    } as any;

    collecteServiceSpy.getToday.and.returnValues(of(collecte), of(refreshed));

    component.chooseMembre({ id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any);
    component.lineForm.patchValue({ typeLigne: 'EPARGNE', montant: 50000 });
    component.addLigne();

    expect(collecteServiceSpy.getToday).toHaveBeenCalledTimes(2);
    expect(collecteServiceSpy.recap).toHaveBeenCalledWith(1);
  });

  it('formatCdf affiche le format lisible', () => {
    expect(component.formatCdf(50000)).toContain('50');
    expect(component.formatCdf(50000)).toContain('CDF');
  });

  it('bouton ajouter desactive sans membre selectionne', () => {
    component.lineForm.patchValue({ typeLigne: 'EPARGNE', montant: 1000 });
    expect(component.canAddLine).toBeFalse();
  });

  it('observation obligatoire si ecart != 0', () => {
    component.remiseForm.patchValue({ especesRemises: 40000, observations: '' });

    component.soumettre();

    expect(component.backendError).toContain('Observation obligatoire');
    expect(collecteServiceSpy.soumettre).not.toHaveBeenCalled();
  });

  it('affiche message erreur backend si ajout refuse', () => {
    collecteServiceSpy.addLigne.and.returnValue(throwError(() => ({ error: { message: 'Membre hors site' } })));

    component.chooseMembre({ id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any);
    component.lineForm.patchValue({ typeLigne: 'EPARGNE', montant: 1000 });
    component.addLigne();

    expect(component.backendError).toContain('Membre hors site');
  });

  it('recherche membre appelle API paginee', fakeAsync(() => {
    component.searchControl.setValue('marie');
    tick(300);

    expect(membreServiceSpy.searchPaginated).toHaveBeenCalledWith('marie', undefined, 0, 20);
  }));

  it('type CARNET n affiche pas quantite editable ni montant editable', () => {
    component.chooseMembre({ id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any);
    component.lineForm.patchValue({ typeLigne: 'CARNET' });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Quantite');
    expect(text).toContain('Montant carnet (FC)');
  });

  it('retire FRAIS_ANALYSE des types pour Agent Terrain', () => {
    expect(component.typeOptions).not.toContain('FRAIS_ANALYSE' as any);
  });

  it('type DEMANDE_CREDIT affiche champs obligatoires et bloque ajout si incomplet', () => {
    creditServiceSpy.getByMembre.and.returnValue(of([]));
    component.chooseMembre({ id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any);
    component.lineForm.patchValue({ typeLigne: 'DEMANDE_CREDIT' });
    fixture.detectChanges();

    expect(component.canAddLine).toBeFalse();

    component.lineForm.patchValue({
      montantSouhaite: 250000,
      objetCredit: 'Stock commerce',
      gagePropose: 'Materiel',
      modaliteRemboursement: 'MENSUELLE'
    });

    expect(component.canAddLine).toBeTrue();
  });

  it('desactive REMBOURSEMENT_CREDIT si membre sans credit actif', () => {
    creditServiceSpy.getByMembre.and.returnValue(of([]));
    component.chooseMembre({ id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any);
    component.lineForm.patchValue({ typeLigne: 'REMBOURSEMENT_CREDIT', montant: 1000 });
    component.addLigne();

    expect(component.backendError).toContain("aucun crédit en cours");
    expect(collecteServiceSpy.addLigne).not.toHaveBeenCalled();
  });

  it('mode edition charge la collecte par id', () => {
    routeStub.snapshot.paramMap = convertToParamMap({ id: '15' });
    collecteServiceSpy.getById.and.returnValue(of({ ...collecte, id: 15, dateCollecte: '2026-06-15' } as any));

    const editionFixture = TestBed.createComponent(CollecteDuJourComponent);
    const editionComponent = editionFixture.componentInstance;
    editionFixture.detectChanges();

    expect(collecteServiceSpy.getById).toHaveBeenCalledWith(15);
    expect(editionComponent.isEditionMode).toBeTrue();
    expect(editionComponent.collecte?.id).toBe(15);
  });

  it('mode edition affiche la date de la collecte', () => {
    routeStub.snapshot.paramMap = convertToParamMap({ id: '15' });
    collecteServiceSpy.getById.and.returnValue(of({ ...collecte, id: 15, dateCollecte: '2026-06-15' } as any));

    const editionFixture = TestBed.createComponent(CollecteDuJourComponent);
    editionFixture.detectChanges();

    expect(editionFixture.nativeElement.textContent).toContain('15/06/2026');
  });

  it('bloque la modification si statut != BROUILLON', () => {
    component.collecte = { ...collecte, statut: 'SOUMISE' } as any;
    component.chooseMembre({ id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any);
    component.lineForm.patchValue({ typeLigne: 'EPARGNE', montant: 2000 });

    component.addLigne();

    expect(component.backendError).toContain('ne peut plus être modifiée');
    expect(collecteServiceSpy.addLigne).not.toHaveBeenCalled();
  });

  it('calcule total carnets/frais a 1000 pour carnet 1000 x 1', () => {
    component.collecte = {
      ...collecte,
      totalFraisCalcule: 1000,
      totalCarnetsCalcule: 1,
      lignes: [
        { typeLigne: 'CARNET', montant: 1000, quantite: 1, totalLigne: 1000 }
      ]
    } as any;

    expect(component.totalCarnetsFraisResume).toBe(1000);
  });

  it('lineTotal multiplie montant * quantite pour carnet 1000 x 2', () => {
    expect(component.lineTotal({ montant: 1000, quantite: 2 })).toBe(2000);
  });

  it('calcule le total general attendu a 51000 pour epargne 50000 + carnet 1000 x 1', () => {
    component.collecte = {
      ...collecte,
      totalEpargneCalcule: 50000,
      totalRemboursementsCalcule: 0,
      totalFraisCalcule: 1000,
      totalGeneralCalcule: 51000,
      totalCarnetsCalcule: 1,
      lignes: [
        { typeLigne: 'EPARGNE', montant: 50000, quantite: 0, totalLigne: 50000 },
        { typeLigne: 'CARNET', montant: 1000, quantite: 1, totalLigne: 1000 },
        { typeLigne: 'DEMANDE_CREDIT', montant: 0, quantite: 0, totalLigne: 0 }
      ]
    } as any;

    expect(component.totalCarnetsFraisResume).toBe(1000);
    expect(component.totalGeneralAttenduResume).toBe(51000);
  });

  it('une ligne demande credit a 0 ne change pas total carnets/frais', () => {
    component.collecte = {
      ...collecte,
      totalFraisCalcule: null,
      totalGeneralCalcule: null,
      lignes: [
        { typeLigne: 'DEMANDE_CREDIT', montant: 0, quantite: 0, totalLigne: 0 }
      ]
    } as any;

    expect(component.totalCarnetsFraisResume).toBe(0);
  });

  it('ajoute une ligne CARNET sans envoyer quantite comme montant', () => {
    component.chooseMembre({ id: 101, codeMembre: 'MBR-A', nomComplet: 'Marie A' } as any);
    component.lineForm.patchValue({
      typeLigne: 'CARNET',
      reference: 'CAR-001',
      commentaire: 'carnet terrain'
    });

    component.addLigne();

    const payload = collecteServiceSpy.addLigne.calls.mostRecent().args[1] as any;
    expect(payload.typeLigne).toBe('CARNET');
    expect(payload.montant).toBeUndefined();
    expect(payload.quantite).toBeUndefined();
    expect(payload.montant).not.toBe(1001);
  });

  it('lineTotal n additionne jamais quantite au montant', () => {
    expect(component.lineTotal({ montant: 1000, quantite: 1 })).toBe(1000);
    expect(component.lineTotal({ montant: 1000, quantite: 1 })).not.toBe(1001);
  });
});
