import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { DemandeCreditService } from './demande-credit.service';

describe('DemandeCreditService', () => {
  let service: DemandeCreditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(DemandeCreditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('appelle pre-analyse', () => {
    service.preAnalyser(7, 'ok').subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/7/pre-analyse'));
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('commentaire')).toBe('ok');
    req.flush({ id: 7 });
  });

  it('appelle validation analyse-risque via controlerRisque (compat)', () => {
    service.controlerRisque(7).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/7/analyse-risque/valider'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 7 });
  });

  it('appelle observation risque', () => {
    service.enregistrerObservationRisque(7, 'obs').subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/7/analyse-risque/observation'));
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('commentaire')).toBe('obs');
    req.flush({ id: 7 });
  });

  it('appelle valider analyse-risque', () => {
    service.validerAnalyseRisque(7, 'ok').subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/7/analyse-risque/valider'));
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('commentaire')).toBe('ok');
    req.flush({ id: 7 });
  });

  it('appelle controle-garantie', () => {
    service.controlerGarantie(7, 'garantie ok').subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/7/controle-garantie'));
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('commentaire')).toBe('garantie ok');
    req.flush({ id: 7 });
  });

  it('appelle rejet avec commentaire obligatoire', () => {
    service.rejeter(7, 'motif').subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/7/rejeter'));
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('commentaire')).toBe('motif');
    req.flush({ id: 7 });
  });
});
