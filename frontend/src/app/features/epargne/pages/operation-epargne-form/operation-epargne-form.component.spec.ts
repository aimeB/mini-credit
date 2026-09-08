import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { OperationEpargneFormComponent } from './operation-epargne-form.component';
import { AuthService } from '../../../../core/services/auth.service';

describe('OperationEpargneFormComponent', () => {
  let component: OperationEpargneFormComponent;
  let fixture: ComponentFixture<OperationEpargneFormComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  function createComponent(options: { agentTerrain?: boolean } = {}): void {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'AGENT_TERRAIN' && options.agentTerrain === true);

    fixture = TestBed.createComponent(OperationEpargneFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole']);

    await TestBed.configureTestingModule({
      imports: [OperationEpargneFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    createComponent({ agentTerrain: true });
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('limite les types d operations pour AGENT_TERRAIN', () => {
    expect(component.operations).toEqual(['COTISATION', 'EPARGNE']);
    expect(component.operations).not.toContain('RETRAIT' as any);
    expect(component.operations).not.toContain('BLOCAGE_GARANTIE' as any);
    expect(component.operations).not.toContain('DEBLOCAGE_GARANTIE' as any);
    expect(component.operations).not.toContain('AJUSTEMENT' as any);
  });

  it('autorise les operations de garantie hors profil agent terrain', () => {
    createComponent();

    expect(component.operations).toContain('BLOCAGE_GARANTIE' as any);
    expect(component.operations).toContain('DEBLOCAGE_GARANTIE' as any);
  });
});
