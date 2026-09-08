import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { API_BASE_URL } from '../../core/services/api.config';
import {
  WorkflowTaskCount,
  WorkflowTaskDashboard,
  WorkflowTaskItem,
  WorkflowTaskStatus
} from '../models/workflow-task.model';

@Injectable({
  providedIn: 'root'
})
export class WorkflowTaskService {
  private readonly http = inject(HttpClient);
  private readonly refreshCountSubject = new Subject<void>();

  readonly refreshCount$ = this.refreshCountSubject.asObservable();

  getMyActions(statut?: WorkflowTaskStatus | ''): Observable<WorkflowTaskItem[]> {
    let params = new HttpParams();
    if (statut) {
      params = params.set('statut', statut);
    }

    return this.http.get<WorkflowTaskItem[]>(`${API_BASE_URL}/me/actions`, { params });
  }

  getSupervisionActions(statut?: WorkflowTaskStatus | ''): Observable<WorkflowTaskItem[]> {
    let params = new HttpParams();
    if (statut) {
      params = params.set('statut', statut);
    }

    return this.http.get<WorkflowTaskItem[]>(`${API_BASE_URL}/actions/supervision`, { params });
  }

  getMyActionCount(): Observable<WorkflowTaskCount> {
    return this.http.get<WorkflowTaskCount>(`${API_BASE_URL}/me/actions/count`);
  }

  getDashboard(): Observable<WorkflowTaskDashboard> {
    return this.http.get<WorkflowTaskDashboard>(`${API_BASE_URL}/actions/dashboard`);
  }

  markAsViewed(id: number): Observable<WorkflowTaskItem> {
    return this.http.post<WorkflowTaskItem>(`${API_BASE_URL}/actions/${id}/marquer-vue`, {}).pipe(
      tap(() => this.refreshCount())
    );
  }

  complete(id: number, commentaireCompletion?: string): Observable<WorkflowTaskItem> {
    return this.http.post<WorkflowTaskItem>(`${API_BASE_URL}/actions/${id}/terminer`, {
      commentaireCompletion: commentaireCompletion ?? null
    }).pipe(
      tap(() => this.refreshCount())
    );
  }

  refreshCount(): void {
    this.refreshCountSubject.next();
  }
}
