import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

import { OperationCaisseListComponent } from './operation-caisse-list.component';

describe('OperationCaisseListComponent', () => {
  let component: OperationCaisseListComponent;
  let fixture: ComponentFixture<OperationCaisseListComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    authServiceSpy.hasAnyRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [OperationCaisseListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } }, params: of({}) } },
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OperationCaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('restreint la création d\'opération aux rôles autorisés', () => {
    component.session = { statut: 'OUVERTE' } as any;

    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CAISSIER'));
    expect(component.canCreateOperation).toBeTrue();

    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));
    expect(component.canCreateOperation).toBeFalse();
  });
});
