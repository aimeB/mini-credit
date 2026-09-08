import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { GarantieCreditService } from './garantie-credit.service';

describe('GarantieCreditService', () => {
  let service: GarantieCreditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(GarantieCreditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('appelle GET garantie', () => {
    service.getGarantie(12).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/12/garantie'));
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('appelle POST verifier', () => {
    service.verifierGarantie(12, { commentaire: 'check' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/12/garantie/verifier'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.commentaire).toBe('check');
    req.flush({});
  });

  it('appelle POST bloquer-epargne', () => {
    service.bloquerEpargne(12, { commentaire: 'blocage' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/12/garantie/bloquer-epargne'));
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('appelle POST materielle', () => {
    service.ajouterGarantieMaterielle(12, {
      typeBien: 'TELEVISION',
      description: 'TV 4K',
      valeurEstimee: 2000000,
      devise: 'CDF'
    }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/12/garantie/materielle'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeBien).toBe('TELEVISION');
    req.flush({});
  });

  it('appelle GET materielles', () => {
    service.getGarantiesMaterielles(12).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/demandes-credit/12/garantie/materielles'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
