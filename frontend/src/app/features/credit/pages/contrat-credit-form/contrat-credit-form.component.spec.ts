import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { ContratCreditFormComponent } from './contrat-credit-form.component';

describe('ContratCreditFormComponent', () => {
  let fixture: ComponentFixture<ContratCreditFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContratCreditFormComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    fixture = TestBed.createComponent(ContratCreditFormComponent);
    fixture.detectChanges();
  });

  it('affiche la guidance globale du workflow crédit', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Suivi du dossier crédit');
    expect(text).toContain('cycle crédit');
  });
});
