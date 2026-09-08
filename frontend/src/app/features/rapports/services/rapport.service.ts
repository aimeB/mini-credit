import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { RapportFinancier, KPIDashboard, RapportRisque, RapportCollecte } from '../models/rapport.model';

@Injectable({
  providedIn: 'root'
})
export class RapportService {
  private readonly apiUrl = `${API_BASE_URL}/rapports`;

  constructor(private http: HttpClient) {}

  // Daily Reports
  getBilanJournalier(date: string): Observable<any> {
    const params = new HttpParams().set('date', date);
    return this.http.get<any>(`${this.apiUrl}/bilan-journalier`, { params });
  }

  // Weekly Reports
  getBilanHebdomadaire(dateDebut: string): Observable<any> {
    const params = new HttpParams().set('dateDebut', dateDebut);
    return this.http.get<any>(`${this.apiUrl}/bilan-hebdomadaire`, { params });
  }

  // Monthly Reports
  getBilanMensuel(mois: string): Observable<any> {
    const params = new HttpParams().set('mois', mois);
    return this.http.get<any>(`${this.apiUrl}/bilan-mensuel`, { params });
  }

  // KPIs Dashboard
  getKPIDashboard(): Observable<KPIDashboard> {
    return this.http.get<KPIDashboard>(`${this.apiUrl}/kpi`);
  }

  // Rapport Financier
  getRapportFinancier(dateDebut: string, dateFin: string): Observable<RapportFinancier> {
    let params = new HttpParams()
      .set('dateDebut', dateDebut)
      .set('dateFin', dateFin);
    return this.http.get<RapportFinancier>(`${this.apiUrl}/financier`, { params });
  }

  // Rapport Risque
  getRapportRisque(): Observable<RapportRisque> {
    return this.http.get<RapportRisque>(`${this.apiUrl}/risque`);
  }

  // Rapport Collecte
  getRapportCollecte(dateDebut: string, dateFin: string): Observable<RapportCollecte> {
    let params = new HttpParams()
      .set('dateDebut', dateDebut)
      .set('dateFin', dateFin);
    return this.http.get<RapportCollecte>(`${this.apiUrl}/collecte`, { params });
  }

  // Export
  exportRapportPDF(type: string, dateDebut: string, dateFin: string): Observable<Blob> {
    let params = new HttpParams()
      .set('type', type)
      .set('dateDebut', dateDebut)
      .set('dateFin', dateFin);
    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  exportRapportExcel(type: string, dateDebut: string, dateFin: string): Observable<Blob> {
    let params = new HttpParams()
      .set('type', type)
      .set('dateDebut', dateDebut)
      .set('dateFin', dateFin);
    return this.http.get(`${this.apiUrl}/export/excel`, { params, responseType: 'blob' });
  }
}
