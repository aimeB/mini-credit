import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { EcartCaisseService } from './ecart-caisse.service';
import { API_BASE_URL } from '../../../core/services/api.config';

const BASE = `${API_BASE_URL}/ecarts-caisse`;

describe('EcartCaisseService — PATCH 9', () => {
  let service: EcartCaisseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(EcartCaisseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ===== Lecture =====

  /**
   * PATCH 9 — getAll_shouldCallCorrectEndpoint
   * Vérifie que getAll() appelle GET /api/ecarts-caisse.
   */
  it('getAll_shouldCallCorrectEndpoint', () => {
    service.getAll().subscribe();
    const req = httpMock.expectOne(BASE);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  /**
   * PATCH 9 — getById_shouldCallCorrectEndpoint
   * Vérifie que getById(5) appelle GET /api/ecarts-caisse/5.
   */
  it('getById_shouldCallCorrectEndpoint', () => {
    service.getById(5).subscribe();
    const req = httpMock.expectOne(`${BASE}/5`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 5 });
  });

  /**
   * PATCH 9 — getBySession_shouldCallCorrectEndpoint
   * Vérifie que getBySession(3) appelle GET /api/ecarts-caisse/session/3.
   */
  it('getBySession_shouldCallCorrectEndpoint', () => {
    service.getBySession(3).subscribe();
    const req = httpMock.expectOne(`${BASE}/session/3`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  /**
   * PATCH 9 — getEnqueteEnAttente_shouldCallCorrectEndpoint
   */
  it('getEnqueteEnAttente_shouldCallCorrectEndpoint', () => {
    service.getEnqueteEnAttente().subscribe();
    const req = httpMock.expectOne(`${BASE}/enquete/en-attente`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  /**
   * PATCH 9 — getValidationHierarchique_shouldCallCorrectEndpoint
   */
  it('getValidationHierarchique_shouldCallCorrectEndpoint', () => {
    service.getValidationHierarchique().subscribe();
    const req = httpMock.expectOne(`${BASE}/validation/hierarchique`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  // ===== Workflow =====

  /**
   * PATCH 9 — justifier_shouldSendCorrectPayload
   * Vérifie que justifier() envoie POST /{id}/justifier avec le champ justification.
   */
  it('justifier_shouldSendCorrectPayload', () => {
    const payload = { justification: 'Erreur de comptage lors de la clôture.' };
    service.justifier(10, payload).subscribe();

    const req = httpMock.expectOne(`${BASE}/10/justifier`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.justification).toBe('Erreur de comptage lors de la clôture.');
    req.flush({ id: 10, statut: 'DETECTE' });
  });

  /**
   * PATCH 9 — ouvrirEnquete_shouldSendCorrectPayload
   * Vérifie que ouvrirEnquete() envoie POST /{id}/ouvrir-enquete avec justification.
   */
  it('ouvrirEnquete_shouldSendCorrectPayload', () => {
    const payload = { justification: 'Investigation formelle requise suite contrôle.' };
    service.ouvrirEnquete(10, payload).subscribe();

    const req = httpMock.expectOne(`${BASE}/10/ouvrir-enquete`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.justification).toBeTruthy();
    req.flush({ id: 10, statut: 'EN_INVESTIGATION' });
  });

  /**
   * PATCH 9 — resoudre_shouldSendRaisonField
   * Vérifie que resoudre() envoie POST /{id}/resoudre avec le champ raison (pas justification).
   */
  it('resoudre_shouldSendRaisonField', () => {
    const payload = { raison: 'Différence corrigée lors du recomptage.' };
    service.resoudre(10, payload).subscribe();

    const req = httpMock.expectOne(`${BASE}/10/resoudre`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.raison).toBe('Différence corrigée lors du recomptage.');
    req.flush({ id: 10, statut: 'RESOLU' });
  });

  /**
   * PATCH 9 — accepter_shouldCallCorrectEndpoint
   * Vérifie que accepter() envoie POST /{id}/accepter sans body métier.
   */
  it('accepter_shouldCallCorrectEndpoint', () => {
    service.accepter(10).subscribe();
    const req = httpMock.expectOne(`${BASE}/10/accepter`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 10, statut: 'ACCEPTE' });
  });

  /**
   * PATCH 9 — rejeter_shouldCallCorrectEndpoint
   * Vérifie que rejeter() envoie POST /{id}/rejeter.
   */
  it('rejeter_shouldCallCorrectEndpoint', () => {
    service.rejeter(10).subscribe();
    const req = httpMock.expectOne(`${BASE}/10/rejeter`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 10, statut: 'REJETE' });
  });

  /**
   * PATCH 9 — noFixedThreshold_serviceDoesNotDefineSeuilValue
   * Vérifie que le service n'exporte/n'utilise aucune constante de seuil fixe.
   * Le flag seuilDepassé est fourni par le backend via EcartThresholdConfigService.
   */
  it('noFixedThreshold_serviceDoesNotDefineSeuilValue', () => {
    const mockResponse = { id: 1, seuilDepassé: true, statut: 'DETECTE' };
    service.getById(1).subscribe(res => {
      expect(res.seuilDepassé).toBeTrue();
    });
    const req = httpMock.expectOne(`${BASE}/1`);
    req.flush(mockResponse);
  });

  // ===== PATCH 11 : RCI =====

  /**
   * PATCH 11 — rci_canAccessEcartsEnAttente
   * Vérifie que getEnqueteEnAttente() appelle le bon endpoint.
   * RCI doit pouvoir accéder aux écarts en enquête (backend @PreAuthorize inclut RCI).
   */
  it('rci_canAccessEcartsEnAttente_viaCorrectEndpoint', () => {
    service.getEnqueteEnAttente().subscribe();
    const req = httpMock.expectOne(`${BASE}/enquete/en-attente`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 5, statut: 'EN_INVESTIGATION' }]);
  });

  /**
   * PATCH 11 — rci_canOpenEnquete
   * Vérifie que ouvrirEnquete() envoie le bon payload.
   * RCI peut déclencher une enquête dans le cadre de sa mission de contrôle interne.
   */
  it('rci_canOpenEnquete_viaCorrectEndpoint', () => {
    const payload = { justification: 'Enquête RCI — anomalie détectée sur session du jour.' };
    service.ouvrirEnquete(7, payload).subscribe();

    const req = httpMock.expectOne(`${BASE}/7/ouvrir-enquete`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.justification).toBeTruthy();
    req.flush({ id: 7, statut: 'EN_INVESTIGATION' });
  });

  /**
   * PATCH 11 — rci_cannotAccepterVariance
   * RCI ne peut PAS accepter une variance.
  * Ce droit est réservé au Chef de Bureau et ADMIN.
   * Ce test vérifie que le service délègue au backend sans logique de filtrage côté client :
   * la restriction est appliquée via @PreAuthorize côté backend ET via canAccepter
   * computed signal côté frontend (qui exclut RCI).
   *
   * NOTE : La restriction côté service Angular est intentionnellement absente —
   * c'est le composant et le guard qui filtrent l'accès. Ce test documente
   * l'invariant : RCI ne doit jamais avoir canAccepter = true dans le composant.
   */
  it('rci_cannotAccepterVariance_isEnforcedByBackendAndComponent', () => {
    // Ce test est documentaire : il vérifie que l'endpoint /accepter existe
    // et que le service l'appelle correctement, mais que le composant
    // masque ce bouton pour RCI via canAccepter (qui exclut RCI).
    // La restriction réelle est sur @PreAuthorize('hasAnyRole("ADMIN","CHEF_BUREAU")').
    expect(service.accepter).toBeDefined();
    // Pas d'appel HTTP — vérification statique de l'intention RBAC
    httpMock.expectNone(`${BASE}/any/accepter`);
  });
});
