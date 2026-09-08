import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { MemberCredentialsModalComponent } from './member-credentials-modal.component';
import { AuthService } from '../../../../core/services/auth.service';
import { AppConfigService } from '../../../../core/services/app-config.service';

describe('MemberCredentialsModalComponent', () => {
  let component: MemberCredentialsModalComponent;
  let fixture: ComponentFixture<MemberCredentialsModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MemberCredentialsModalComponent],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            message: 'Compte cree avec succes',
            membre: {
              telephonePrincipal: '+243810000000',
              nomComplet: 'Membre Test'
            },
            credentials: {
              username: 'member01'
            },
            activationCode: {
              code: 'MBR-31747-260615',
              activationLink: 'https://app.mini-credit.cd/activate?code=MBR-31747-260615'
            }
          }
        },
        {
          provide: MatDialogRef,
          useValue: {
            close: jasmine.createSpy('close')
          }
        },
        {
          provide: MatDialog,
          useValue: {
            open: jasmine.createSpy('open')
          }
        },
        {
          provide: AuthService,
          useValue: {
            getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ role: 'ADMIN' })
          }
        },
        {
          provide: AppConfigService,
          useValue: {
            getEnvironmentLabel: jasmine.createSpy('getEnvironmentLabel').and.returnValue('PROD'),
            buildActivationLink: jasmine.createSpy('buildActivationLink'),
            getConfiguredAccessUrl: jasmine.createSpy('getConfiguredAccessUrl').and.returnValue('https://app.mini-credit.cd')
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MemberCredentialsModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render manual code fallback message', () => {
    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Si le lien ne s\'ouvre pas, communiquez simplement le code au membre.');
  });

  it('should copy activation link when copy button is clicked', async () => {
    const writeTextSpy = jasmine.createSpy('writeText').and.returnValue(Promise.resolve());
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: writeTextSpy }
    });

    component.copyCode('https://app.mini-credit.cd/activate?code=MBR-31747-260615', 'link');

    expect(writeTextSpy).toHaveBeenCalledWith('https://app.mini-credit.cd/activate?code=MBR-31747-260615');
  });
});