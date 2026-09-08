import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import {
  RapportCaisseDepenses,
  RapportCaisseEcarts,
  RapportCaisseJournalier,
  RapportCaissePeriode,
  RapportCaisseSession
} from '../models/rapport-caisse.model';

@Injectable({
  providedIn: 'root'
})
export class RapportCaisseService {
  private readonly apiUrl = `${API_BASE_URL}/rapports/caisse`;

  constructor(private http: HttpClient) {}

  getSession(sessionId: number): Observable<RapportCaisseSession> {
    return this.http.get<RapportCaisseSession>(`${this.apiUrl}/session/${sessionId}`);
  }

  exportSessionCsv(sessionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/session/${sessionId}/export`, {
      params: new HttpParams().set('format', 'csv'),
      responseType: 'blob'
    });
  }

  getJournalier(date: string, caisseId?: number, siteId?: number): Observable<RapportCaisseJournalier> {
    let params = new HttpParams().set('date', date);
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get<RapportCaisseJournalier>(`${this.apiUrl}/journalier`, { params });
  }

  exportJournalierCsv(date: string, caisseId?: number, siteId?: number): Observable<Blob> {
    let params = new HttpParams().set('date', date).set('format', 'csv');
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get(`${this.apiUrl}/journalier/export`, { params, responseType: 'blob' });
  }

  getPeriode(dateDebut: string, dateFin: string, caisseId?: number, siteId?: number): Observable<RapportCaissePeriode> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin);
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get<RapportCaissePeriode>(`${this.apiUrl}/periode`, { params });
  }

  exportPeriodeCsv(dateDebut: string, dateFin: string, caisseId?: number, siteId?: number): Observable<Blob> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin).set('format', 'csv');
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get(`${this.apiUrl}/periode/export`, { params, responseType: 'blob' });
  }

  getDepenses(dateDebut: string, dateFin: string, caisseId?: number, siteId?: number): Observable<RapportCaisseDepenses> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin);
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get<RapportCaisseDepenses>(`${this.apiUrl}/depenses`, { params });
  }

  exportDepensesCsv(dateDebut: string, dateFin: string, caisseId?: number, siteId?: number): Observable<Blob> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin).set('format', 'csv');
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get(`${this.apiUrl}/depenses/export`, { params, responseType: 'blob' });
  }

  getEcarts(dateDebut: string, dateFin: string, caisseId?: number, siteId?: number): Observable<RapportCaisseEcarts> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin);
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get<RapportCaisseEcarts>(`${this.apiUrl}/ecarts`, { params });
  }

  exportEcartsCsv(dateDebut: string, dateFin: string, caisseId?: number, siteId?: number): Observable<Blob> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin).set('format', 'csv');
    if (caisseId != null) params = params.set('caisseId', String(caisseId));
    if (siteId != null) params = params.set('siteId', String(siteId));
    return this.http.get(`${this.apiUrl}/ecarts/export`, { params, responseType: 'blob' });
  }
}
