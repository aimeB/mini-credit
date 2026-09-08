import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { EcartCaisseResponse } from '../models/ecart-caisse-response';
import { EcartCaisseJustifierRequest } from '../models/ecart-caisse-justifier-request';
import { EcartCaisseResoudreRequest } from '../models/ecart-caisse-resoudre-request';

/**
 * PATCH 9 — Service Angular pour la gestion des écarts de caisse.
 *
 * RBAC appliqué côté backend (@PreAuthorize). Ce service ne filtre pas les actions
 * selon le rôle — c'est la responsabilité des composants et du guard de route.
 *
 * Mapping rôles 3N → métier :
 *   Chef de Bureau  = supervision d'antenne
 *   Contrôleur      = contrôle caisse
 *   Caissier        = opérations journée caisse
 *
 * Seuils : aucun seuil fixe côté frontend.
 * Le flag seuilDepassé est fourni par le backend via EcartThresholdConfigService.
 */
@Injectable({
  providedIn: 'root'
})
export class EcartCaisseService {
  private readonly apiUrl = `${API_BASE_URL}/ecarts-caisse`;

  constructor(private http: HttpClient) {}

  // ===== LECTURE =====

  /** ADMIN, CONTROLEUR, CHEF_BUREAU — liste tous les écarts. */
  getAll(): Observable<EcartCaisseResponse[]> {
    return this.http.get<EcartCaisseResponse[]>(this.apiUrl);
  }

  /** ADMIN, CONTROLEUR, CHEF_BUREAU, CAISSIER — détail d'un écart. */
  getById(id: number): Observable<EcartCaisseResponse> {
    return this.http.get<EcartCaisseResponse>(`${this.apiUrl}/${id}`);
  }

  /** ADMIN, CONTROLEUR, CHEF_BUREAU, CAISSIER — écarts d'une session. */
  getBySession(sessionId: number): Observable<EcartCaisseResponse[]> {
    return this.http.get<EcartCaisseResponse[]>(`${this.apiUrl}/session/${sessionId}`);
  }

  /** ADMIN, CONTROLEUR, CHEF_BUREAU — écarts par date (format ISO 8601 : yyyy-MM-dd). */
  getByJour(dateJour: string): Observable<EcartCaisseResponse[]> {
    return this.http.get<EcartCaisseResponse[]>(`${this.apiUrl}/jour/${dateJour}`);
  }

  /** ADMIN, CONTROLEUR — écarts DETECTE ou EN_INVESTIGATION. */
  getEnqueteEnAttente(): Observable<EcartCaisseResponse[]> {
    return this.http.get<EcartCaisseResponse[]>(`${this.apiUrl}/enquete/en-attente`);
  }

  /** ADMIN, CHEF_BUREAU — écarts nécessitant validation hiérarchique (seuilDepassé=true). */
  getValidationHierarchique(): Observable<EcartCaisseResponse[]> {
    return this.http.get<EcartCaisseResponse[]>(`${this.apiUrl}/validation/hierarchique`);
  }

  /** ADMIN, CONTROLEUR, CHEF_BUREAU — total non résolu pour un jour (yyyy-MM-dd). */
  getTotalNonResoluByJour(dateJour: string): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/total-non-resolu/jour/${dateJour}`);
  }

  // ===== WORKFLOW =====

  /**
   * ADMIN, CONTROLEUR, CAISSIER — justification initiale (n'change pas le statut).
   *
   * NOTE TECHNIQUE : La validation 10 caractères minimum est provisoire.
   * La règle métier 3N impose uniquement qu'une justification soit documentée.
   */
  justifier(id: number, request: EcartCaisseJustifierRequest): Observable<EcartCaisseResponse> {
    return this.http.post<EcartCaisseResponse>(`${this.apiUrl}/${id}/justifier`, request);
  }

  /**
   * ADMIN, CONTROLEUR — ouvre une enquête formelle (statut → EN_INVESTIGATION).
   * Utilise le même payload que justifier (champ justification).
   */
  ouvrirEnquete(id: number, request: EcartCaisseJustifierRequest): Observable<EcartCaisseResponse> {
    return this.http.post<EcartCaisseResponse>(`${this.apiUrl}/${id}/ouvrir-enquete`, request);
  }

  /** ADMIN, CONTROLEUR — résolution avec raison documentée (statut → RESOLU). */
  resoudre(id: number, request: EcartCaisseResoudreRequest): Observable<EcartCaisseResponse> {
    return this.http.post<EcartCaisseResponse>(`${this.apiUrl}/${id}/resoudre`, request);
  }

  /** ADMIN, CHEF_BUREAU — accepte la variance (statut → ACCEPTE). */
  accepter(id: number): Observable<EcartCaisseResponse> {
    return this.http.post<EcartCaisseResponse>(`${this.apiUrl}/${id}/accepter`, {});
  }

  /** ADMIN uniquement — rejette l'écart (erreur système présumée, statut → REJETE). */
  rejeter(id: number): Observable<EcartCaisseResponse> {
    return this.http.post<EcartCaisseResponse>(`${this.apiUrl}/${id}/rejeter`, {});
  }
}
