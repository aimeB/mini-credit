import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { PaiementInitialDemandeCreditService } from './paiement-initial-demande-credit.service';

describe('PaiementInitialDemandeCreditService', () => {
  let service: PaiementInitialDemandeCreditService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(PaiementInitialDemandeCreditService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
