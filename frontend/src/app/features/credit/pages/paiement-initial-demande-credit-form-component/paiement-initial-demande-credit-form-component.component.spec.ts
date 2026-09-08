import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PaiementInitialDemandeCreditFormComponent } from './paiement-initial-demande-credit-form-component.component';
import { PaiementInitialDemandeCreditService } from '../../services/paiement-initial-demande-credit.service';
import { DemandeCreditService } from '../../services/demande-credit.service';
import { AuthService } from '../../../../core/services/auth.service';
import { SessionCaisseService } from '../../../caisse/services/session-caisse.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('PaiementInitialDemandeCreditFormComponent', () => {
  let fixture: ComponentFixture<PaiementInitialDemandeCreditFormComponent>;
  let component: PaiementInitialDemandeCreditFormComponent;

  const paiementServiceMock = {
    enregistrerPaiementInitial: jasmine.createSpy('enregistrerPaiementInitial').and.returnValue(of({}))
  };

  const demandeServiceMock = {
    getById: jasmine.createSpy('getById').and.returnValue(of({
      id: 1,
      numeroDemande: 'DEMANDE-DE0901C4',
      membreNomComplet: 'Kingani jean',
      montantDemande: 50000,
      fraisDemande: 5000,
      fraisDemandePayes: 1500,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 0,
      statut: 'SOUMISE',
      devise: 'CDF'
    }))
  };

  const authServiceMock = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ role: 'CAISSIER', nomComplet: 'Caissier Test', permissions: [] })
  };

  const sessionCaisseServiceMock = {
    getSessionActive: jasmine.createSpy('getSessionActive').and.returnValue(of({
      id: 7,
      caisseId: 3,
      caisseCode: 'CAISSE-01',
      utilisateurId: 99,
      utilisateurNom: 'Caissier Test',
      antenneNom: 'Antenne Centre',
      dateOuverture: '2026-07-01T08:00:00',
      soldeOuverture: 0,
      totalEntrees: 0,
      totalSorties: 0,
      soldeTheorique: 0,
      statut: 'OUVERTE',
      createdAt: '2026-07-01T08:00:00',
      updatedAt: '2026-07-01T08:00:00'
    }))
  };

  const workflowMessageServiceMock = {
    getGuidance: jasmine.createSpy('getGuidance').and.returnValue(null)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaiementInitialDemandeCreditFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: '1' }),
              queryParamMap: convertToParamMap({ returnUrl: '/credits/frais-a-encaisser' })
            }
          }
        },
        { provide: PaiementInitialDemandeCreditService, useValue: paiementServiceMock },
        { provide: DemandeCreditService, useValue: demandeServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: SessionCaisseService, useValue: sessionCaisseServiceMock },
        { provide: WorkflowMessageService, useValue: workflowMessageServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaiementInitialDemandeCreditFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('prefill le reste frais à payer pour une demande SOUMISE', () => {
    expect(component.errorMessage).toBe('');
    expect(component.form.enabled).toBeTrue();
    expect(component.fraisDemandeRestant).toBe(3500);
    expect(component.form.controls.fraisPayes.value).toBe(3500);
  });

  it('envoie uniquement le paiement des frais sans ids techniques ni depot garantie', () => {
    component.submit();

    expect(paiementServiceMock.enregistrerPaiementInitial).toHaveBeenCalledWith(1, {
      modePaiement: 'ESPECES',
      fraisPayes: 3500,
      observation: undefined
    });
    const request = paiementServiceMock.enregistrerPaiementInitial.calls.mostRecent().args[1] as Record<string, unknown>;
    expect(request['sessionCaisseId']).toBeUndefined();
    expect(request['caisseId']).toBeUndefined();
    expect(request['agentId']).toBeUndefined();
    expect(request['utilisateurId']).toBeUndefined();
    expect(request['depotGarantiePaye']).toBeUndefined();
  });

  it('affiche la session caisse en lecture seule sans champs techniques', () => {
    const text = fixture.nativeElement.textContent || '';
    const inputs: HTMLInputElement[] = Array.from(fixture.nativeElement.querySelectorAll('input'));
    const inputNames = inputs.map((input) => input.getAttribute('formControlName') || input.getAttribute('name') || '');

    expect(text).toContain('Caissier connecté');
    expect(text).toContain('Caisse active');
    expect(text).toContain('Session caisse active');
    expect(inputNames).not.toContain('sessionCaisseId');
    expect(inputNames).not.toContain('caisseId');
    expect(inputNames).not.toContain('agentId');
    expect(inputNames).not.toContain('utilisateurId');
    expect(inputNames).not.toContain('depotGarantiePaye');
  });

  it('desactive l encaissement si aucune session caisse n est ouverte', () => {
    component.sessionActive = null;
    fixture.detectChanges();

    const submitButton: HTMLButtonElement | null = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(component.canSubmit).toBeFalse();
    expect(submitButton?.disabled).toBeTrue();
  });
});
