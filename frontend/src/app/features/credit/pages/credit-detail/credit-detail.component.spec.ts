import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { CreditService } from '../../services/credit.service';
import { CreditDetailComponent } from './credit-detail.component';
import { CreditDetailResponse } from '../../models/credit-detail-response';

describe('CreditDetailComponent', () => {
  let component: CreditDetailComponent;
  let fixture: ComponentFixture<CreditDetailComponent>;

  const dossier: CreditDetailResponse = {
    resume: {
      id: 1,
      numeroCredit: 'CR-2026-1',
      statut: 'DECAISSE',
      montantAccorde: 750000,
      montantDecaisse: 750000,
      devise: 'CDF'
    },
    responsables: {
      gestionnaireNomComplet: 'Gestionnaire Test'
    },
    garantie: {
      montantGarantieRequis: 150000,
      montantGarantieBloque: 150000
    },
    echeancier: [],
    suiviFinancier: {
      capitalInitial: 750000,
      capitalRembourse: 0,
      capitalRestant: 750000,
      interetsAttendus: 0,
      interetsPayes: 0,
      interetsRestants: 0,
      penalitesDues: 0,
      penalitesPayees: 0,
      totalPaye: 0,
      totalRestant: 750000
    },
    remboursements: [],
    historique: []
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreditDetailComponent, HttpClientTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => '1'
              }
            }
          }
        },
        {
          provide: CreditService,
          useValue: {
            getDetail: jasmine.createSpy('getDetail').and.returnValue(of(dossier))
          }
        },
        {
          provide: AuthService,
          useValue: {
            hasAnyRole: jasmine.createSpy('hasAnyRole').and.returnValue(true)
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreditDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load credit file detail', () => {
    expect(component.dossier?.resume.numeroCredit).toBe('CR-2026-1');
  });

  it('should display credit title and gestionnaire fallback name', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('h1')?.textContent).toContain('Dossier crédit CR-2026-1');
    expect(component.getGestionnaireNom(dossier)).toBe('Gestionnaire Test');
  });
});
