import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      // 401 from the gateway = token missing/expired/invalid; a failed login is handled by the login page
      if (err.status === 401 && !req.url.startsWith('/auth/')) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};
