import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';

import { MembreDetailComponent } from './membre-detail.component';
import { MembreService } from '../../services/membre.service';
import { CompteEpargneService } from '../../../epargne/services/compte-epargne.service';
import { CreditService } from '../../../credit/services/credit.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('MembreDetailComponent', () => {
  let component: MembreDetailComponent;
  let fixture: ComponentFixture<MembreDetailComponent>;
  let membreServiceSpy: jasmine.SpyObj<MembreService>;
  let compteServiceSpy: jasmine.SpyObj<CompteEpargneService>;
  let creditServiceSpy: jasmine.SpyObj<CreditService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const membre = {
    id: 101,
    nom: 'Doe',
    prenom: 'Jane',
    statut: 'ACTIF',
    dateAdhesion: '2026-06-15',
    siteId: 10,
    siteNom: 'Site 1',
    createdAt: '2026-06-15T00:00:00',
    updatedAt: '2026-06-15T00:00:00',
  } as any;

  const compte = {
    id: 201,
    membreId: 101,
    membreNomComplet: 'Jane Doe',
    numeroCompte: 'CEP001',
    typeCompte: 'EPARGNE_VOLONTAIRE',
    soldeDisponible: 2000,
    soldeBloque: 500,
    statut: 'ACTIF',
    dateOuverture: '2026-06-15',
    createdAt: '2026-06-15T00:00:00',
    updatedAt: '2026-06-15T00:00:00',
  } as any;

  const credit = {
    id: 1,
    numeroCredit: 'CR-2026-1',
    membreId: 101,
    membreNomComplet: 'Jane Doe',
    montantOctroye: 750000,
    devise: 'CDF',
    statut: 'DECAISSE'
  } as any;

  beforeEach(async () => {
    membreServiceSpy = jasmine.createSpyObj<MembreService>('MembreService', ['getById', 'delete']);
    compteServiceSpy = jasmine.createSpyObj<CompteEpargneService>('CompteEpargneService', ['getActiveByMembre', 'repairMissingForMember']);
    creditServiceSpy = jasmine.createSpyObj<CreditService>('CreditService', ['getByMembre']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole']);

    membreServiceSpy.getById.and.returnValue(of(membre));
    compteServiceSpy.getActiveByMembre.and.returnValue(of(compte));
    compteServiceSpy.repairMissingForMember.and.returnValue(of(compte));
    creditServiceSpy.getByMembre.and.returnValue(of([credit]));
    authServiceSpy.hasRole.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [MembreDetailComponent],
      providers: [
        provideRouter([]),
        { provide: MembreService, useValue: membreServiceSpy },
        { provide: CompteEpargneService, useValue: compteServiceSpy },
        { provide: CreditService, useValue: creditServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => '101'
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MembreDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche la section compte epargne avec boutons quand compte actif existe', () => {
    expect(component.compteEpargne?.id).toBe(201);
    expect(fixture.nativeElement.textContent).toContain('Compte epargne');
    expect(fixture.nativeElement.textContent).toContain('Voir compte epargne');
    expect(fixture.nativeElement.textContent).toContain('Historique epargne');
  });

  it('charge et affiche les crédits réels du membre', () => {
    expect(creditServiceSpy.getByMembre).toHaveBeenCalledWith(101);
    expect(component.credits.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Crédits du membre');
    expect(fixture.nativeElement.textContent).toContain('CR-2026-1');
    expect(fixture.nativeElement.textContent).toContain('DECAISSE');
  });

  it('affiche un message clair si les crédits du membre échouent', () => {
    creditServiceSpy.getByMembre.and.returnValue(throwError(() => ({ status: 403 })));

    component.loadCredits(101);
    fixture.detectChanges();

    expect(component.loadingCredits).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Impossible de charger les crédits du membre.');
  });

  it('affiche alerte compte absent et bouton reparation pour ADMIN', () => {
    compteServiceSpy.getActiveByMembre.and.returnValue(throwError(() => ({ status: 404 })));
    authServiceSpy.hasRole.and.returnValue(true);

    component.loadCompteEpargne(101);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Compte epargne non trouve');
    expect(fixture.nativeElement.textContent).toContain('Reparer / creer compte manquant');
  });

  it('masque le bouton reparation si utilisateur non ADMIN', () => {
    compteServiceSpy.getActiveByMembre.and.returnValue(throwError(() => ({ status: 404 })));
    authServiceSpy.hasRole.and.returnValue(false);

    component.loadCompteEpargne(101);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Compte epargne non trouve');
    expect(fixture.nativeElement.textContent).not.toContain('Reparer / creer compte manquant');
  });

  it('affiche la photo du membre quand photoUrl est renseigne', () => {
    component.membre = { ...membre, nomComplet: 'Jane Doe', photoUrl: '/uploads/membres/101.jpg' };
    fixture.detectChanges();

    const image = fixture.nativeElement.querySelector('img[alt="Photo de Jane Doe"]') as HTMLImageElement;
    expect(image).not.toBeNull();
    expect(image.src).toContain('/uploads/membres/101.jpg');
    expect(fixture.nativeElement.querySelector('[aria-label="Avatar par défaut"]')).toBeNull();
  });

  it('affiche un avatar par défaut sans photo et après erreur de chargement', () => {
    component.membre = { ...membre, nomComplet: 'Jane Doe', photoUrl: undefined };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[aria-label="Avatar par défaut"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('input[type="file"]')).toBeNull();

    component.membre = { ...membre, nomComplet: 'Jane Doe', photoUrl: '/photo-invalide.jpg' };
    fixture.detectChanges();
    const image = fixture.nativeElement.querySelector('img[alt="Photo de Jane Doe"]') as HTMLImageElement;
    image.dispatchEvent(new Event('error'));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[aria-label="Avatar par défaut"]')).not.toBeNull();
  });
});
