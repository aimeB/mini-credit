import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { OperationCaisseService } from './operation-caisse.service';
import { OperationCaisseRequest } from '../models/operation-caisse-request';
import { SourceOperationCaisse } from '../models/source-operation-caisse';
import { API_BASE_URL } from '../../../core/services/api.config';

const BASE = `${API_BASE_URL}/operations-caisse`;

/** Payload de base valide pour les tests */
function buildRequest(source: SourceOperationCaisse, extra: Partial<OperationCaisseRequest> = {}): OperationCaisseRequest {
  return {
    sessionCaisseId: 1,
    caisseId: 1,
    dateOperation: new Date().toISOString(),
    typeOperation: 'ENTREE',
    categorieOperation: 'ENTREE_DIVERSE',
    montant: 1000,
    source,
    ...extra,
  };
}

describe('OperationCaisseService', () => {
  let service: OperationCaisseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(OperationCaisseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ===== PATCH 7 : Tests source =====

  /**
   * PATCH 7 — operationManuelle_shouldSendSourceMANUEL
   * La saisie manuelle caisse doit envoyer source=MANUEL.
   */
  it('operationManuelle_shouldSendSourceMANUEL', () => {
    const payload = buildRequest(SourceOperationCaisse.MANUEL, { description: 'Frais divers' });
    service.enregistrer(payload).subscribe();

    const req = httpMock.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.source).toBe('MANUEL');
    req.flush({ id: 99 });
  });

  /**
   * PATCH 7 — approvisionnement_shouldSendSourceAPPROVISIONNEMENT
   * Alimentation caisse doit envoyer source=APPROVISIONNEMENT.
   */
  it('approvisionnement_shouldSendSourceAPPROVISIONNEMENT', () => {
    const payload = buildRequest(SourceOperationCaisse.APPROVISIONNEMENT, {
      referenceExterne: 'BON-2026-001',
      typeOperation: 'ENTREE',
    });
    service.enregistrer(payload).subscribe();

    const req = httpMock.expectOne(BASE);
    expect(req.request.body.source).toBe('APPROVISIONNEMENT');
    expect(req.request.body.referenceExterne).toBe('BON-2026-001');
    req.flush({ id: 100 });
  });

  /**
   * PATCH 7 — ajustement_shouldSendSourceAJUSTEMENT
   * Contrepassation doit envoyer source=AJUSTEMENT.
   */
  it('ajustement_shouldSendSourceAJUSTEMENT', () => {
    const payload = buildRequest(SourceOperationCaisse.AJUSTEMENT, {
      referenceExterne: 'OP-2026-042',
      typeOperation: 'SORTIE',
    });
    service.enregistrer(payload).subscribe();

    const req = httpMock.expectOne(BASE);
    expect(req.request.body.source).toBe('AJUSTEMENT');
    expect(req.request.body.referenceExterne).toBe('OP-2026-042');
    req.flush({ id: 101 });
  });

  /**
   * PATCH 7 — newOperation_shouldNeverSendLEGACY
   * Le frontend ne doit jamais envoyer LEGACY dans un payload.
   * LEGACY n'existe pas dans l'enum SourceOperationCaisse frontend,
   * ce test vérifie que la valeur envoyée n'est pas 'LEGACY'.
   */
  it('newOperation_shouldNeverSendLEGACY', () => {
    const payload = buildRequest(SourceOperationCaisse.MANUEL);
    service.enregistrer(payload).subscribe();

    const req = httpMock.expectOne(BASE);
    expect(req.request.body.source).not.toBe('LEGACY');
    req.flush({ id: 102 });
  });
});

