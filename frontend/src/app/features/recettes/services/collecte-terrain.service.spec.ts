import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CollecteTerrainService } from './collecte-terrain.service';

describe('CollecteTerrainService', () => {
  let service: CollecteTerrainService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CollecteTerrainService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('construit la requete de listing avec statut', () => {
    service.list({ statut: 'SOUMISE', page: 0, size: 50 }).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/collectes-terrain') && r.urlWithParams.includes('statut=SOUMISE'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 50, hasNext: false, hasPrevious: false });
  });

  it('appelle valider avec payload', () => {
    service.valider(10, { decision: 'VALIDEE' }).subscribe();
    const req = httpMock.expectOne((r) => r.url.endsWith('/collectes-terrain/10/valider'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.decision).toBe('VALIDEE');
    req.flush({});
  });

  it('charge une collecte par id', () => {
    service.getById(15).subscribe();
    const req = httpMock.expectOne((r) => r.url.endsWith('/collectes-terrain/15'));
    expect(req.request.method).toBe('GET');
    req.flush({ id: 15 });
  });

  it('appelle confirmer billetage avec payload', () => {
    service.confirmerBilletage(10, { especesConfirmeesCaissier: 9000, observationBilletage: 'RAS' }).subscribe();
    const req = httpMock.expectOne((r) => r.url.endsWith('/collectes-terrain/10/billetage/confirmer'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.especesConfirmeesCaissier).toBe(9000);
    req.flush({});
  });
});
