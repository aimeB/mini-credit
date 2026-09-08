import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { OperationCaisseFormComponent } from './operation-caisse-form.component';
import { OperationCaisseService } from '../../services/operation-caisse.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { SessionCaisseResponse } from '../../models/session-caisse-response';

describe('OperationCaisseFormComponent', () => {
  let component: OperationCaisseFormComponent;
  let fixture: ComponentFixture<OperationCaisseFormComponent>;
  let operationServiceSpy: jasmine.SpyObj<OperationCaisseService>;

  const sessionMock: SessionCaisseResponse = {
    id: 1,
    caisseId: 1,
    caisseCode: 'CAI202606250001',
    devise: 'CDF',
    utilisateurId: 5,
    utilisateurNom: 'Caissier caisse',
    dateComptable: new Date().toISOString().slice(0, 10),
    dateOuverture: new Date().toISOString(),
    soldeOuverture: 500000,
    totalEntrees: 0,
    totalSorties: 0,
    soldeTheorique: 500000,
    statut: 'OUVERTE',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(async () => {
    const operationSpy = jasmine.createSpyObj('OperationCaisseService', ['enregistrer']);
    const sessionSpy = jasmine.createSpyObj('SessionCaisseService', ['getById']);

    sessionSpy.getById.and.returnValue(of(sessionMock));
    operationSpy.enregistrer.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [OperationCaisseFormComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['sessionId', '1']]) } } },
        { provide: OperationCaisseService, useValue: operationSpy },
        { provide: SessionCaisseService, useValue: sessionSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OperationCaisseFormComponent);
    component = fixture.componentInstance;
    operationServiceSpy = TestBed.inject(OperationCaisseService) as jasmine.SpyObj<OperationCaisseService>;
    fixture.detectChanges();
  });

  it('should display Montant (CDF) in label', () => {
    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('Montant (CDF)');
  });

  it('should compute increased balance for ENTREE', () => {
    component.form.patchValue({ typeOperation: 'ENTREE', montant: 100000, motif: 'Approvisionnement' });
    expect(component.soldeApresOperation).toBe(600000);
  });

  it('should compute decreased balance for SORTIE', () => {
    component.form.patchValue({ typeOperation: 'SORTIE', montant: 50000, motif: 'Dépense caisse' });
    expect(component.soldeApresOperation).toBe(450000);
  });

  it('should block submit and show insufficient balance when sortie exceeds balance', () => {
    component.form.patchValue({ typeOperation: 'SORTIE', montant: 600000, motif: 'Décaissement' });
    expect(component.hasSoldeInsuffisant).toBeTrue();
    expect(component.canSubmit).toBeFalse();
    expect(component.disabledSubmitReason).toContain('Solde insuffisant');
  });

  it('should disable submit when amount is empty and expose clear helper', () => {
    component.form.patchValue({ montant: null, motif: '' });
    expect(component.canSubmit).toBeFalse();
    expect(component.disabledSubmitReason).toContain('Renseignez le montant et le motif');
  });

  it('should submit valid payload with required fields and manual source', () => {
    component.form.patchValue({
      typeOperation: 'ENTREE',
      categorieOperation: 'APPROVISIONNEMENT',
      natureFinancement: 'APPORT_PROPRIETAIRE',
      montant: 100000,
      modePaiement: 'ESPECES',
      motif: 'Approvisionnement',
      observation: 'RAS',
    });

    component.submit();

    expect(operationServiceSpy.enregistrer).toHaveBeenCalled();
    const payload = operationServiceSpy.enregistrer.calls.mostRecent().args[0];
    expect(payload.sessionCaisseId).toBe(1);
    expect(payload.typeOperation).toBe('ENTREE');
    expect(payload.montant).toBe(100000);
    expect(payload.categorieOperation).toBe('APPROVISIONNEMENT');
    expect(payload.natureFinancement).toBe('APPORT_PROPRIETAIRE');
    expect(payload.modePaiement).toBe('ESPECES');
    expect(payload.description).toBe('Approvisionnement');
    expect(payload.source).toBe('APPROVISIONNEMENT');
    expect(payload.devise).toBeUndefined();
  });
});
