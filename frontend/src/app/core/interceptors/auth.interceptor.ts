import { Injectable, inject } from '@angular/core';
import { HttpContextToken, HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { WorkflowMessageService } from '../../shared/services/workflow-message.service';

export const SKIP_FORBIDDEN_REDIRECT = new HttpContextToken<boolean>(() => false);

function handleForbidden(
  error: HttpErrorResponse,
  req: HttpRequest<any>,
  authService: AuthService,
  router: Router,
  workflowMessageService: WorkflowMessageService
): Observable<never> {
  if (error.status !== 403) {
    return throwError(() => error);
  }

  if (req.context.get(SKIP_FORBIDDEN_REDIRECT)) {
    return throwError(() => error);
  }

  const role = authService.getCurrentUser()?.role;
  const backendMessage = typeof error.error?.message === 'string' ? error.error.message : '';
  const message = workflowMessageService.buildForbiddenMessage({
    requestUrl: req.url,
    currentRole: role,
    metadata: {
      backendMessage
    }
  });

  router.navigate(['/access-denied'], {
    queryParams: {
      title: message.title,
      detail: message.detail,
      from: router.url,
      requestUrl: req.url,
    },
  });

  return throwError(() => error);
}

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);
  private router = inject(Router);
  private workflowMessageService = inject(WorkflowMessageService);

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => handleForbidden(error, request, this.authService, this.router, this.workflowMessageService))
    );
  }
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const workflowMessageService = inject(WorkflowMessageService);
  const token = authService.getToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => handleForbidden(error, req, authService, router, workflowMessageService))
  );
};