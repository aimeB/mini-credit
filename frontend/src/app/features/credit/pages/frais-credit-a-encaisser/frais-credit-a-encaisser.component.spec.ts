import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { FraisCreditAEncaisserComponent } from './frais-credit-a-encaisser.component';
import { DemandeCreditService } from '../../services/demande-credit.service';

describe('FraisCreditAEncaisserComponent', () => {
  let fixture: ComponentFixture<FraisCreditAEncaisserComponent>;
  let component: FraisCreditAEncaisserComponent;

  const demandeCreditServiceStub = {
    getFraisCreditAEncaisser: jasmine.createSpy('getFraisCreditAEncaisser').and.returnValue(of([
      {
        demandeCreditId: 42,
        numeroDemande: 'DCR-2026-42',
        membreNomComplet: 'Membre Test',
        siteNom: 'Site A',
        antenneNom: 'Antenne 1',
        dateDemande: '2026-07-01',
        montantDemande: 100000,
        fraisDemande: 5000,
        fraisDemandePayes: 1500,
        resteFraisAPayer: 3500,
        statutDemande: 'SOUMISE',
        sessionCaisseRequise: true
      }
    ]))
  };

  beforeEach(async () => {
    demandeCreditServiceStub.getFraisCreditAEncaisser.calls.reset();

    await TestBed.configureTestingModule({
      imports: [FraisCreditAEncaisserComponent],
      providers: [
        provideRouter([]),
        { provide: DemandeCreditService, useValue: demandeCreditServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FraisCreditAEncaisserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge les demandes avec frais credit incomplets', () => {
    expect(demandeCreditServiceStub.getFraisCreditAEncaisser).toHaveBeenCalled();
    expect(component.demandes.length).toBe(1);

    const text = fixture.nativeElement.textContent || '';
    expect(text).toContain('Frais crédit à encaisser');
    expect(text).toContain('DCR-2026-42');
    expect(text).toContain('Membre Test');
    expect(text).toContain('Reste à payer');
  });

  it('oriente uniquement vers l encaissement des frais avec retour liste', () => {
    const link: HTMLAnchorElement | null = fixture.nativeElement.querySelector('a[href*="paiement-initial"]');

    expect(link).toBeTruthy();
    expect(link?.getAttribute('href')).toContain('/credits/demandes/42/paiement-initial');
    expect(link?.getAttribute('href')).toContain('returnUrl=%2Fcredits%2Ffrais-a-encaisser');
    expect(fixture.nativeElement.textContent).toContain('Encaisser frais');
  });
});