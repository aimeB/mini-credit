import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CollectesAControlerComponent } from './collectes-a-controler.component';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';

describe('CollectesAControlerComponent', () => {
  let component: CollectesAControlerComponent;
  let fixture: ComponentFixture<CollectesAControlerComponent>;
  let serviceSpy: jasmine.SpyObj<CollecteTerrainService>;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj<CollecteTerrainService>('CollecteTerrainService', ['list', 'valider', 'rejeter']);
    serviceSpy.list.and.returnValue(of({
      content: [{ id: 1, statut: 'SOUMISE', billetageConfirme: true, generationSummary: 'En attente validation' }],
      totalElements: 1,
      totalPages: 1,
      currentPage: 0,
      pageSize: 50,
      hasNext: false,
      hasPrevious: false,
    } as any));
    serviceSpy.valider.and.returnValue(of({ id: 1, generationSummary: 'Opérations générées: 3' } as any));
    serviceSpy.rejeter.and.returnValue(of({ id: 1, statut: 'REJETEE' } as any));

    await TestBed.configureTestingModule({
      imports: [CollectesAControlerComponent],
      providers: [{ provide: CollecteTerrainService, useValue: serviceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CollectesAControlerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche les collectes a controler et bouton Valider', () => {
    expect(serviceSpy.list).toHaveBeenCalled();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Valider');
  });

  it('rejeter exige un motif', () => {
    spyOn(window, 'prompt').and.returnValue('');
    component.rejeter({ id: 1, statut: 'SOUMISE', billetageConfirme: true } as any);
    expect(serviceSpy.rejeter).not.toHaveBeenCalled();

    (window.prompt as jasmine.Spy).and.returnValue('Erreur de billetage');
    component.rejeter({ id: 1, statut: 'SOUMISE', billetageConfirme: true } as any);
    expect(serviceSpy.rejeter).toHaveBeenCalledWith(1, { decision: 'REJETEE', motifRejet: 'Erreur de billetage' });
  });

  it('valider appelle service', () => {
    spyOn(component, 'load').and.callThrough();

    component.valider({ id: 1, statut: 'SOUMISE', billetageConfirme: true } as any);

    expect(serviceSpy.valider).toHaveBeenCalledWith(1, { decision: 'VALIDEE' });
    expect(component.lastGenerationSummary).toBe('Opérations générées: 3');
    expect(component.successMessage).toBe('Collecte validée avec succès.');
    expect(component.errorMessage).toBe('');
    expect(component.load).toHaveBeenCalled();
  });

  it('affiche une erreur si la validation echoue', () => {
    serviceSpy.valider.and.returnValue(throwError(() => ({ error: { message: 'Validation impossible: billetage non confirmé par le caissier' } })));
    spyOn(console, 'error');

    component.valider({ id: 1, statut: 'SOUMISE', billetageConfirme: true } as any);

    expect(component.errorMessage).toContain('Validation impossible');
    expect(component.successMessage).toBe('');
    expect(console.error).toHaveBeenCalled();
  });

  it('valider bloque si billetage non confirme', () => {
    component.valider({ id: 1, statut: 'SOUMISE', billetageConfirme: false } as any);
    expect(serviceSpy.valider).not.toHaveBeenCalled();
    expect(component.errorMessage).toContain('billetage non confirmé');
  });

  it('affiche attente billetage sans boutons controleur', () => {
    serviceSpy.list.and.returnValue(of({
      content: [{ id: 2, statut: 'SOUMISE', billetageConfirme: false }],
      totalElements: 1,
      totalPages: 1,
      currentPage: 0,
      pageSize: 50,
      hasNext: false,
      hasPrevious: false,
    } as any));

    component.load();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('En attente de billetage caissier');
    expect(text).not.toContain('Valider');
    expect(text).not.toContain('Rejeter');
  });
});
