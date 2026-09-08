import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ActivationComponent } from './activation.component';
import { AuthService } from '../../core/services/auth.service';

describe('ActivationComponent', () => {
  let component: ActivationComponent;
  let fixture: ComponentFixture<ActivationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivationComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ code: 'MBR-31747-260615' })
          }
        },
        {
          provide: AuthService,
          useValue: {
            activate: jasmine.createSpy('activate'),
            getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue(null)
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ActivationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should prefill activation code from query param', () => {
    expect(component.activationForm.getRawValue().activationCode).toBe('MBR-31747-260615');
    expect(component.codePreFilled).toBeTrue();
    expect(component.activationForm.get('activationCode')?.disabled).toBeTrue();
  });
});