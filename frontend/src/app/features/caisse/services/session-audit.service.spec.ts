import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { SessionAuditService } from './session-audit.service';

describe('SessionAuditService', () => {
  let service: SessionAuditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(SessionAuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getSessionTimeline() should call entity endpoint and map paged timeline events', () => {
    let result: any;

    service.getSessionTimeline(12, 0, 50).subscribe((data) => {
      result = data;
    });

    const req = httpMock.expectOne((r) =>
      r.method === 'GET' && r.urlWithParams.includes('/api/audit/logs/entity/SessionCaisse/12')
    );

    expect(req.request.urlWithParams).toContain('page=0');
    expect(req.request.urlWithParams).toContain('size=50');

    req.flush({
      content: [
        {
          id: 99,
          action: 'PRE_CLOTURE',
          entityType: 'SessionCaisse',
          entityId: 12,
          username: 'cashier.user',
          roleCode: 'CAISSIER',
          success: true,
          createdDate: '2026-06-18T16:45:00',
          oldValuesJson: JSON.stringify({ statut: 'OUVERTE' }),
          newValuesJson: JSON.stringify({ statut: 'PRE_CLOTUREE', observation: 'Solde vérifié' })
        }
      ],
      totalElements: 1,
      totalPages: 1,
      currentPage: 0,
      pageSize: 50,
      hasNext: false,
      hasPrevious: false
    });

    expect(result.events.length).toBe(1);
    expect(result.events[0].actionLabel).toBe('Pré-clôture effectuée');
    expect(result.events[0].ancienStatut).toBe('OUVERTE');
    expect(result.events[0].nouveauStatut).toBe('PRE_CLOTUREE');
    expect(result.events[0].observation).toBe('Solde vérifié');
    expect(result.events[0].utilisateur).toBe('cashier.user');
    expect(result.page).toBe(0);
    expect(result.hasNext).toBeFalse();
  });

  it('getSessionTimeline() should return an empty event list when timeline is empty', () => {
    let result: any;

    service.getSessionTimeline(12, 0, 50).subscribe((data) => {
      result = data;
    });

    const req = httpMock.expectOne((r) =>
      r.method === 'GET' && r.urlWithParams.includes('/api/audit/logs/entity/SessionCaisse/12')
    );

    req.flush({
      content: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      pageSize: 50,
      hasNext: false,
      hasPrevious: false
    });

    expect(result.events).toEqual([]);
    expect(result.totalElements).toBe(0);
    expect(result.hasNext).toBeFalse();
  });
});
