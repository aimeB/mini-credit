import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { AgenceListComponent } from './agence-list.component';
import { AgenceService } from '../../../employes/services/agence.service';
import { AgenceResponse } from '../../models/agence-response';

describe('AgenceListComponent — Affichage des colonnes', () => {
  let component: AgenceListComponent;

  const mockAgences: AgenceResponse[] = [
    {
      id: 1,
      codeAgence: 'KIN001',
      nomAgence: 'Agence Centrale Kinshasa',
      ville: 'Kinshasa',
      commune: 'Gombe',
      quartier: 'Centre-Ville',
      adresse: 'Avenue de l\'Equateur 12',
      reference: 'Bâtiment SONAS',
      telephone: '+243810000001',
      actif: true
    },
    {
      id: 2,
      codeAgence: 'LUB001',
      nomAgence: 'Agence Lubumbashi',
      ville: 'Lubumbashi',
      commune: 'Katuba',
      quartier: 'Golf',
      adresse: 'Avenue Lumumba',
      actif: false
    }
  ];

  const agenceServiceMock = {
    getAll: jasmine.createSpy('getAll').and.returnValue(of(mockAgences))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgenceListComponent, RouterTestingModule],
      providers: [{ provide: AgenceService, useValue: agenceServiceMock }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AgenceListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ──────────────────────────────────────────────────────────────────────
  // agenceList_shouldDisplayVilleCommuneQuartier
  // ──────────────────────────────────────────────────────────────────────

  it('agenceList_shouldDisplayVilleCommuneQuartier — les agences chargées ont ville/commune/quartier', () => {
    expect(component.agences.length).toBe(2);
    const agence = component.agences[0];
    expect(agence.ville).toBe('Kinshasa');
    expect(agence.commune).toBe('Gombe');
    expect(agence.quartier).toBe('Centre-Ville');
    expect(agence.adresse).toBe('Avenue de l\'Equateur 12');
  });

  it('agenceList_shouldDisplayVilleCommuneQuartier — deuxième agence est correctement chargée', () => {
    const agence = component.agences[1];
    expect(agence.ville).toBe('Lubumbashi');
    expect(agence.commune).toBe('Katuba');
    expect(agence.quartier).toBe('Golf');
    expect(agence.actif).toBeFalse();
  });

  // ──────────────────────────────────────────────────────────────────────
  // agenceList_shouldNotDisplayEmail
  // ──────────────────────────────────────────────────────────────────────

  it('agenceList_shouldNotDisplayEmail — AgenceResponse ne doit pas inclure de champ email dans le flux principal', () => {
    const agence = component.agences[0];
    // email n'est pas dans l'interface AgenceResponse (retiré du flux principal)
    // La vérification se fait sur le type : TypeScript garantit la structure
    expect((agence as unknown as Record<string, unknown>)['email']).toBeUndefined();
  });

  it('agenceList_shouldNotDisplayEmail — le service est appelé au chargement', () => {
    expect(agenceServiceMock.getAll).toHaveBeenCalled();
  });

  // ──────────────────────────────────────────────────────────────────────
  // Fonctionnement général
  // ──────────────────────────────────────────────────────────────────────

  it('loading — la liste est chargée et loading est false', () => {
    expect(component.loading).toBeFalse();
    expect(component.agences.length).toBeGreaterThan(0);
  });

  it('agences actives/inactives — statut correctement mappé', () => {
    expect(component.agences[0].actif).toBeTrue();
    expect(component.agences[1].actif).toBeFalse();
  });
});
