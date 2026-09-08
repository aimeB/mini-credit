import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { MembreListComponent } from './membre-list.component';
import { MembreService } from '../../services/membre.service';
import { Page } from '../../../../shared/models/page.model';
import { AuthService } from '../../../../core/services/auth.service';

describe('MembreListComponent', () => {
  let component: MembreListComponent;
  let fixture: ComponentFixture<MembreListComponent>;
  let membreServiceSpy: jasmine.SpyObj<MembreService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const membresPage: Page<any> = {
    content: [
      { id: 101, nom: 'Marie', prenom: 'A', statut: 'ACTIF', siteId: 10, agentId: 5 },
      { id: 102, nom: 'Jean', prenom: 'B', statut: 'EN_ATTENTE', siteId: 10, agentId: 6 },
    ],
    totalElements: 2,
    totalPages: 1,
    currentPage: 0,
    pageSize: 10,
    hasNext: false,
    hasPrevious: false,
  };

  beforeEach(async () => {
    membreServiceSpy = jasmine.createSpyObj<MembreService>('MembreService', ['searchPaginated', 'delete']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole']);
    membreServiceSpy.searchPaginated.and.returnValue(of(membresPage));
    membreServiceSpy.delete.and.returnValue(of(void 0));
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'AGENT_TERRAIN');

    await TestBed.configureTestingModule({
      imports: [MembreListComponent],
      providers: [
        provideRouter([]),
        { provide: MembreService, useValue: membreServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MembreListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loadMembres lit response.content (Page) et met a jour totalElements/totalPages', () => {
    component.loadMembres();

    expect(membreServiceSpy.searchPaginated).toHaveBeenCalledWith('', undefined, 0, 10);
    expect(component.membres.length).toBe(2);
    expect(component.totalElements).toBe(2);
    expect(component.totalPages).toBe(1);
  });

  it('charge la liste sans erreur pour GESTIONNAIRE avec filtres vides', () => {
    authServiceSpy.hasRole.and.returnValue(false);
    component.searchTerm = '';
    component.siteFilter = '';
    component.agentFilter = '';
    component.statutFilter = '';

    component.loadMembres();

    expect(membreServiceSpy.searchPaginated).toHaveBeenCalledWith('', undefined, 0, 10);
    expect(component.error).toBe('');
    expect(component.membres.length).toBe(2);
    expect(component.canEditMembers).toBeTrue();
  });

  it('resetFilters reinitialise les filtres et recharge la liste', () => {
    component.searchTerm = 'marie';
    component.statutFilter = 'ACTIF';
    component.siteFilter = '10';
    component.agentFilter = '6';

    component.resetFilters();

    expect(component.searchTerm).toBe('');
    expect(component.statutFilter).toBe('');
    expect(component.siteFilter).toBe('');
    expect(component.agentFilter).toBe('');
    expect(membreServiceSpy.searchPaginated).toHaveBeenCalled();
  });

  it('applique le filtre statut sur le contenu de la page', () => {
    component.statutFilter = 'ACTIF';

    component.loadMembres();

    expect(component.membres.length).toBe(1);
    expect(component.membres[0].id).toBe(101);
  });

  it('masque le bouton Modifier pour AGENT_TERRAIN', () => {
    fixture.detectChanges();

    expect(component.canEditMembers).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('Modifier');
    expect(fixture.nativeElement.textContent).toContain('Ajouter à ma collecte');
  });
});
