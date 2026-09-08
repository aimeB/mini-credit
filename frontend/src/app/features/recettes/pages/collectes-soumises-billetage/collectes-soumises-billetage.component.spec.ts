import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CollectesSoumisesBilletageComponent } from './collectes-soumises-billetage.component';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';

describe('CollectesSoumisesBilletageComponent', () => {
  let component: CollectesSoumisesBilletageComponent;
  let fixture: ComponentFixture<CollectesSoumisesBilletageComponent>;
  let serviceSpy: jasmine.SpyObj<CollecteTerrainService>;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj<CollecteTerrainService>('CollecteTerrainService', ['list', 'confirmerBilletage']);
    serviceSpy.list.and.returnValue(of({
      content: [{ id: 1, statut: 'SOUMISE', totalGeneralCalcule: 100, especesDeclareesAgent: 90, especesConfirmeesCaissier: null, especesRemises: 90, ecartTresorerie: -10 }],
      totalElements: 1,
      totalPages: 1,
      currentPage: 0,
      pageSize: 50,
      hasNext: false,
      hasPrevious: false,
    } as any));
    serviceSpy.confirmerBilletage.and.returnValue(of({ id: 1, billetageConfirme: true } as any));

    await TestBed.configureTestingModule({
      imports: [CollectesSoumisesBilletageComponent],
      providers: [{ provide: CollecteTerrainService, useValue: serviceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CollectesSoumisesBilletageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('liste les collectes SOUMISES pour billetage', () => {
    expect(serviceSpy.list).toHaveBeenCalledWith({ statut: 'SOUMISE', page: 0, size: 50 });
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('100');
    expect(text).toContain('90');
    expect(text).toContain('Confirmer billetage');
  });

  it('confirme le billetage via service', () => {
    spyOn(window, 'prompt').and.returnValues('95', 'Ecart de caisse');
    component.confirmerBilletage({ id: 1, especesDeclareesAgent: 90 } as any);
    expect(serviceSpy.confirmerBilletage).toHaveBeenCalledWith(1, {
      especesConfirmeesCaissier: 95,
      observationBilletage: 'Ecart de caisse'
    });
  });
});
