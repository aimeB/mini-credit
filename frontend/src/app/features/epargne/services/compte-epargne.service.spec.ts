import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { CompteEpargneService } from './compte-epargne.service';

describe('CompteEpargneService', () => {
  let service: CompteEpargneService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(CompteEpargneService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
