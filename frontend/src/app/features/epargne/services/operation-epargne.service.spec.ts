import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { OperationEpargneService } from './operation-epargne.service';

describe('OperationEpargneService', () => {
  let service: OperationEpargneService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(OperationEpargneService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
