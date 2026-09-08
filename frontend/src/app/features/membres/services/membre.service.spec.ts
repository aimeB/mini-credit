import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { MembreService } from './membre.service';
import { API_BASE_URL } from '../../../core/services/api.config';

describe('MembreService', () => {
  let service: MembreService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(MembreService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('searchPaginated n envoie pas q ni siteId quand les filtres sont vides', () => {
    service.searchPaginated('   ', undefined, 0, 10).subscribe();

    const request = httpMock.expectOne(`${API_BASE_URL}/membres/search?page=0&size=10`);
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 10 });
  });

  it('searchPaginated envoie uniquement les filtres renseignes', () => {
    service.searchPaginated(' marie ', 12, 1, 20).subscribe();

    const request = httpMock.expectOne(`${API_BASE_URL}/membres/search?q=marie&siteId=12&page=1&size=20`);
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], totalElements: 0, totalPages: 0, currentPage: 1, pageSize: 20 });
  });
});
