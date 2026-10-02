import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { AuthSessionStore } from './auth-session.store';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(AuthSessionStore).accessToken();
  const isApiRequest = request.url.startsWith(environment.apiBaseUrl);
  const isLoginRequest = request.url.endsWith('/auth/login');

  if (!token || !isApiRequest || isLoginRequest) return next(request);

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
