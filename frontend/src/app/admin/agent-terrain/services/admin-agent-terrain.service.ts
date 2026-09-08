import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import {
  AgentTerrainResponse,
  CreateAgentTerrainRequest,
  UpdateAgentTerrainRequest,
  UtilisateurSimple,
  SiteSimple,
  PortefeuilleGestionnaireResponse
} from '../models/agent-terrain.model';
import { EmployeResponse } from '../../../features/employes/models/employe-response';
import { UtilisateurResponse } from '../../../features/utilisateurs/models/utilisateur-response';

@Injectable({
  providedIn: 'root'
})
export class AdminAgentTerrainService {
  private baseUrl = `${API_BASE_URL}/agents-terrain`;
  private utilisateurUrl = `${API_BASE_URL}/utilisateurs`;
  private siteUrl = `${API_BASE_URL}/sites`;
  private employeUrl = `${API_BASE_URL}/employes`;
  private gestionnaireUrl = `${API_BASE_URL}/gestionnaires`;

  constructor(private httpClient: HttpClient) {}

  // ===== AGENT TERRAIN CRUD =====
  getAll(): Observable<AgentTerrainResponse[]> {
    return this.httpClient.get<AgentTerrainResponse[]>(this.baseUrl);
  }

  getById(id: number): Observable<AgentTerrainResponse> {
    return this.httpClient.get<AgentTerrainResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateAgentTerrainRequest): Observable<AgentTerrainResponse> {
    return this.httpClient.post<AgentTerrainResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateAgentTerrainRequest): Observable<AgentTerrainResponse> {
    return this.httpClient.put<AgentTerrainResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.baseUrl}/${id}`);
  }

  // ===== LOOKUP DATA =====
  getUtilisateurs(): Observable<UtilisateurSimple[]> {
    return this.httpClient.get<UtilisateurSimple[]>(this.utilisateurUrl);
  }

  /**
    * Utilisateurs disponibles pour créer un Agent Terrain :
    * actifs + rôle AGENT_TERRAIN + employé fonction AGENT_TERRAIN + pas encore agent.
   */
  getUtilisateursDisponibles(): Observable<UtilisateurResponse[]> {
    return this.httpClient.get<UtilisateurResponse[]>(`${this.utilisateurUrl}/disponibles-agent-terrain`);
  }

  getSites(): Observable<SiteSimple[]> {
    return this.httpClient.get<SiteSimple[]>(this.siteUrl);
  }

  /** Charge les employés actifs avec fonction=GESTIONNAIRE pour le dropdown */
  getGestionnaires(): Observable<EmployeResponse[]> {
    return this.httpClient.get<EmployeResponse[]>(`${this.employeUrl}/gestionnaires`);
  }

  // ===== FILTERS & SEARCH =====
  getByUtilisateur(utilisateurId: number): Observable<AgentTerrainResponse[]> {
    return this.httpClient.get<AgentTerrainResponse[]>(`${this.baseUrl}?utilisateurId=${utilisateurId}`);
  }

  getBySite(siteId: number): Observable<AgentTerrainResponse[]> {
    return this.httpClient.get<AgentTerrainResponse[]>(`${this.baseUrl}/by-site/${siteId}`);
  }

  getByGestionnaire(gestionnaireId: number): Observable<AgentTerrainResponse[]> {
    return this.httpClient.get<AgentTerrainResponse[]>(`${this.baseUrl}/by-gestionnaire/${gestionnaireId}`);
  }

  // ===== PORTEFEUILLE GESTIONNAIRE =====
  getPortefeuilleGestionnaire(gestionnaireId: number): Observable<PortefeuilleGestionnaireResponse> {
    return this.httpClient.get<PortefeuilleGestionnaireResponse>(`${this.gestionnaireUrl}/${gestionnaireId}/portefeuille`);
  }
}
