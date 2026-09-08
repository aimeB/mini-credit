import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { EmployeResponse } from '../models/employe-response';
import { CreateEmployeRequest } from '../models/employe-create-request';
import { UpdateEmployeRequest } from '../models/employe-update-request';

@Injectable({
  providedIn: 'root'
})
export class EmployeService {
  private readonly apiUrl = `${API_BASE_URL}/employes`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<EmployeResponse[]> {
    return this.http.get<EmployeResponse[]>(this.apiUrl);
  }

  getAllActive(): Observable<EmployeResponse[]> {
    return this.http.get<EmployeResponse[]>(`${this.apiUrl}/actifs`);
  }

  /**
   * Employés actifs sans compte utilisateur lié.
   * Utilisé par le formulaire "Créer Utilisateur" pour le dropdown.
   */
  getDisponibles(): Observable<EmployeResponse[]> {
    return this.http.get<EmployeResponse[]>(`${this.apiUrl}/disponibles`);
  }

  getById(id: number): Observable<EmployeResponse> {
    return this.http.get<EmployeResponse>(`${this.apiUrl}/${id}`);
  }

  getByMatricule(matricule: string): Observable<EmployeResponse> {
    return this.http.get<EmployeResponse>(`${this.apiUrl}/matricule/${matricule}`);
  }

  create(request: CreateEmployeRequest): Observable<EmployeResponse> {
    return this.http.post<EmployeResponse>(this.apiUrl, request);
  }

  update(id: number, request: UpdateEmployeRequest): Observable<EmployeResponse> {
    return this.http.put<EmployeResponse>(`${this.apiUrl}/${id}`, request);
  }

  uploadPhoto(id: number, file: File): Observable<EmployeResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EmployeResponse>(`${this.apiUrl}/${id}/photo`, formData);
  }

  deletePhoto(id: number): Observable<EmployeResponse> {
    return this.http.delete<EmployeResponse>(`${this.apiUrl}/${id}/photo`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Employés actifs rattachés à une agence donnée.
   * Utilisé par le détail Agence pour afficher le personnel.
   */
  getByAgence(agenceId: number): Observable<EmployeResponse[]> {
    return this.http.get<EmployeResponse[]>(`${this.apiUrl}/by-agence/${agenceId}`);
  }

  /**
   * Transfère un employé vers une nouvelle agence.
   * Le site doit appartenir à l'agence cible.
   */
  changerAgence(id: number, agenceId: number, siteId: number): Observable<EmployeResponse> {
    return this.http.patch<EmployeResponse>(`${this.apiUrl}/${id}/agence`, { agenceId, siteId });
  }

  /**
   * Employés actifs n'appartenant PAS à l'agence donnée.
   * Utilisé par la modal "Affecter un employé existant".
   */
  getEmployesAAffecterAgence(agenceId: number): Observable<EmployeResponse[]> {
    return this.http.get<EmployeResponse[]>(`${this.apiUrl}/a-affecter-agence/${agenceId}`);
  }
}
