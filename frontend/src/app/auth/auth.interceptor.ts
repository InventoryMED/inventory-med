import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const isApiRequest = request.url.startsWith(environment.apiBaseUrl);
  if (!isApiRequest) return next(request);

  return next(
    request.clone({
      withCredentials: true,
    }),
  );
};
