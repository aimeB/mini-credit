import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SessionTimelineComponent } from './session-timeline.component';

describe('SessionTimelineComponent', () => {
  let component: SessionTimelineComponent;
  let fixture: ComponentFixture<SessionTimelineComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionTimelineComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SessionTimelineComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render timeline events in readable format', () => {
    component.events = [
      {
        id: 1,
        date: '2026-06-18T10:00:00',
        action: 'OUVERTURE_SESSION',
        actionLabel: 'Session ouverte',
        utilisateur: 'caissier.1',
        role: 'CAISSIER',
        observation: 'Ouverture OK',
        ancienStatut: undefined,
        nouveauStatut: 'OUVERTE',
        success: true
      }
    ];

    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Session ouverte');
    expect(html.textContent).toContain('caissier.1');
    expect(html.textContent).toContain('CAISSIER');
    expect(html.textContent).toContain('OUVERTE');
  });

  it('should display empty state when no events', () => {
    component.events = [];
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Aucune trace d\'audit disponible');
  });
});
