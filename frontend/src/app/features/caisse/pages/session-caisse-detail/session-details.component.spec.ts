import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { SessionCaisseDetailComponent } from './session-caisse-detail.component';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { SessionAuditService } from '../../services/session-audit.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('SessionDetailsComponent (SessionCaisseDetailComponent)', () => {
  let fixture: ComponentFixture<SessionCaisseDetailComponent>;
  let component: SessionCaisseDetailComponent;

  const sessionCaisseServiceMock = {
    getById: jasmine.createSpy('getById'),
    validerControle: jasmine.createSpy('validerControle').and.returnValue(of({})),
    getAnomaliesBySession: jasmine.createSpy('getAnomaliesBySession').and.returnValue(of([]))
  };

  const sessionAuditServiceMock = {
    getSessionTimeline: jasmine.createSpy('getSessionTimeline')
  };

  const authServiceMock = {
    hasAnyRole: jasmine.createSpy('hasAnyRole').and.returnValue(true),
    hasPermission: jasmine.createSpy('hasPermission').and.returnValue(true),
    hasAnyPermission: jasmine.createSpy('hasAnyPermission').and.returnValue(false),
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ role: 'CONTROLEUR', permissions: [] })
  };

  beforeEach(async () => {
    sessionCaisseServiceMock.getById.and.returnValue(of(defaultSession()));
    sessionCaisseServiceMock.getAnomaliesBySession.and.returnValue(of([]));
    sessionAuditServiceMock.getSessionTimeline.and.returnValue(of(defaultTimelinePage()));
    authServiceMock.hasAnyRole.and.returnValue(true);
    authServiceMock.hasPermission.and.returnValue(true);
    authServiceMock.hasAnyPermission.and.returnValue(false);
    authServiceMock.getCurrentUser.and.returnValue({ role: 'CONTROLEUR', permissions: [] });

    await TestBed.configureTestingModule({
      imports: [SessionCaisseDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: '12' })
            }
          }
        },
        { provide: SessionCaisseService, useValue: sessionCaisseServiceMock },
        { provide: SessionAuditService, useValue: sessionAuditServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SessionCaisseDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load session and timeline on init', () => {
    expect(component).toBeTruthy();
    expect(sessionCaisseServiceMock.getById).toHaveBeenCalledWith(12);
    expect(sessionAuditServiceMock.getSessionTimeline).toHaveBeenCalledWith(12, 0, 50);
  });

  it('should render timeline section', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Timeline de session');
    expect(html.textContent).toContain('Session ouverte');
  });

  it('should render a clear message when timeline is empty', () => {
    sessionAuditServiceMock.getSessionTimeline.and.returnValue(
      of({
        events: [],
        page: 0,
        size: 50,
        hasNext: false,
        totalElements: 0
      })
    );

    component.loadTimeline(12);
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Aucune trace d\'audit disponible pour cette session.');
  });

  function defaultSession() {
    return {
      id: 12,
      caisseId: 4,
      caisseCode: 'C-04',
      utilisateurId: 2,
      utilisateurNom: 'caissier.test',
      dateOuverture: '2026-06-18T08:00:00',
      soldeOuverture: 1000,
      totalEntrees: 250,
      totalSorties: 80,
      soldeTheorique: 1170,
      statut: 'PRE_CLOTUREE',
      createdAt: '2026-06-18T08:00:00',
      updatedAt: '2026-06-18T17:00:00'
    };
  }

  function defaultTimelinePage() {
    return {
      events: [
        {
          id: 1,
          date: '2026-06-18T10:00:00',
          action: 'OUVERTURE_SESSION',
          actionLabel: 'Session ouverte',
          utilisateur: 'caissier.test',
          role: 'CAISSIER',
          success: true
        }
      ],
      page: 0,
      size: 50,
      hasNext: false,
      totalElements: 1
    };
  }
});
