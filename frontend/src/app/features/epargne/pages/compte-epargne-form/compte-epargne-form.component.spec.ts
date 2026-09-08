import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { CompteEpargneFormComponent } from './compte-epargne-form.component';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('CompteEpargneFormComponent', () => {
  let component: CompteEpargneFormComponent;
  let fixture: ComponentFixture<CompteEpargneFormComponent>;
  let compteServiceSpy: jasmine.SpyObj<CompteEpargneService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    compteServiceSpy = jasmine.createSpyObj<CompteEpargneService>('CompteEpargneService', ['create']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole']);
    compteServiceSpy.create.and.returnValue(of({ id: 1 } as any));
    authServiceSpy.hasRole.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [CompteEpargneFormComponent],
      providers: [
        provideRouter([]),
        { provide: CompteEpargneService, useValue: compteServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompteEpargneFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('masque le formulaire de creation pour AGENT_TERRAIN', () => {
    authServiceSpy.hasRole.and.returnValue(false);
    fixture.detectChanges();

    expect(component.canCreateMissingAccount).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('réservée à l\'administrateur');
  });
});
