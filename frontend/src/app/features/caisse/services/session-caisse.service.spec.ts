import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { SessionCaisseService } from './session-caisse.service';

describe('SessionCaisseService', () => {
  let service: SessionCaisseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(SessionCaisseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('cloturer() appelle l\'alias de pré-clôture', () => {
    service.cloturer(123, { soldePhysique: 1000, observation: 'ok' } as any).subscribe();

    const req = httpMock.expectOne((r) =>
      r.method === 'POST' && r.url.endsWith('/api/caisses/sessions/123/pre-cloturer'));
    expect(req.request.body.soldePhysique).toBe(1000);
    req.flush({});
  });

  it('preCloturer() appelle le bon endpoint', () => {
    service.preCloturer(7, { soldePhysique: 500 } as any).subscribe();

    const req = httpMock.expectOne((r) =>
      r.method === 'POST' && r.url.endsWith('/api/caisses/sessions/7/pre-cloturer'));
    expect(req.request.url).toContain('/api/caisses/sessions/7/pre-cloturer');
    req.flush({});
  });

  it('cloturerFinale() appelle le bon endpoint final', () => {
    service.cloturerFinale(9, { observation: 'final' }).subscribe();

    const req = httpMock.expectOne((r) =>
      r.method === 'POST' && r.url.endsWith('/api/caisses/sessions/9/cloturer-finale'));
    expect(req.request.url).toContain('/api/caisses/sessions/9/cloturer-finale');
    req.flush({});
  });
});
