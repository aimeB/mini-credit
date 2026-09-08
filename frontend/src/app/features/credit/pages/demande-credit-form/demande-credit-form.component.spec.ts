import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DemandeCreditFormComponent } from './demande-credit-form.component';
import { DemandeCreditService } from '../../services/demande-credit.service';

describe('DemandeCreditFormComponent', () => {
  let fixture: ComponentFixture<DemandeCreditFormComponent>;

  const demandeCreditServiceMock = {
    create: jasmine.createSpy('create').and.returnValue(of({}))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandeCreditFormComponent],
      providers: [
        provideRouter([]),
        { provide: DemandeCreditService, useValue: demandeCreditServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DemandeCreditFormComponent);
    fixture.detectChanges();
  });

  it('affiche la guidance globale du workflow crédit', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Suivi du dossier crédit');
    expect(text).toContain('Aucun décaissement n’est autorisé');
  });
});
