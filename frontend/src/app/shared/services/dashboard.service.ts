import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/services/api.config';
import {
  PortefeuilleDashboardResponse,
  RetardDashboardResponse,
  CaisseDashboardResponse,
  DashboardGlobalResponse
} from '../models/dashboard-response.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/dashboard`;

  /**
   * Get portfolio dashboard analytics
   */
  getPortefeuilleDashboard(): Observable<PortefeuilleDashboardResponse> {
    return this.http.get<PortefeuilleDashboardResponse>(`${this.baseUrl}/portefeuille`);
  }

  /**
   * Get overdue/retard dashboard analytics
   * @param dateReference Reference date for calculating overdue (YYYY-MM-DD format)
   */
  getRetardDashboard(dateReference: string): Observable<RetardDashboardResponse> {
    const params = new HttpParams().set('dateReference', dateReference);
    return this.http.get<RetardDashboardResponse>(`${this.baseUrl}/retard`, { params });
  }

  /**
   * Get caisse session dashboard analytics
   * @param sessionCaisseId The caisse session ID
   */
  getCaisseDashboard(sessionCaisseId: number): Observable<CaisseDashboardResponse> {
    return this.http.get<CaisseDashboardResponse>(`${this.baseUrl}/caisse/${sessionCaisseId}`);
  }

  /**
   * Get global dashboard analytics combining all metrics
   * @param sessionCaisseId The caisse session ID
   * @param dateReference Reference date (YYYY-MM-DD format)
   */
  getDashboardGlobal(sessionCaisseId: number, dateReference: string): Observable<DashboardGlobalResponse> {
    const params = new HttpParams()
      .set('sessionCaisseId', sessionCaisseId.toString())
      .set('dateReference', dateReference);
    return this.http.get<DashboardGlobalResponse>(`${this.baseUrl}/global`, { params });
  }
}
