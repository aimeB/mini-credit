import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SuiviCollectesTerrainComponent } from './suivi-collectes-terrain.component';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';

describe('SuiviCollectesTerrainComponent', () => {
  let component: SuiviCollectesTerrainComponent;
  let fixture: ComponentFixture<SuiviCollectesTerrainComponent>;
  let serviceSpy: jasmine.SpyObj<CollecteTerrainService>;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj<CollecteTerrainService>('CollecteTerrainService', ['list']);
    serviceSpy.list.and.returnValue(of({
      content: [{ id: 1, statut: 'VALIDEE', operationsGeneratedCount: 4 }],
      totalElements: 1,
      totalPages: 1,
      currentPage: 0,
      pageSize: 100,
      hasNext: false,
      hasPrevious: false,
    } as any));

    await TestBed.configureTestingModule({
      imports: [SuiviCollectesTerrainComponent],
      providers: [{ provide: CollecteTerrainService, useValue: serviceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(SuiviCollectesTerrainComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche suivi en lecture seule sans bouton validation', () => {
    expect(serviceSpy.list).toHaveBeenCalledWith({ page: 0, size: 100 });
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('VALIDEE');
    expect(text).not.toContain('Valider');
    expect(text).not.toContain('Rejeter');
  });
});
