import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import {
  DashboardControleInterneFilters,
  DashboardControleInterneResponse
} from '../models/dashboard-controle-interne.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardControleInterneService {
  private readonly apiUrl = `${API_BASE_URL}/dashboard/controle-interne`;

  constructor(private http: HttpClient) {}

  getDashboard(filters: DashboardControleInterneFilters): Observable<DashboardControleInterneResponse> {
    let params = new HttpParams();

    Object.entries(filters || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, `${value}`);
      }
    });

    return this.http.get<DashboardControleInterneResponse>(this.apiUrl, { params });
  }
}
