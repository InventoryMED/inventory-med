import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withXsrfConfiguration({
            cookieName: 'XSRF-TOKEN',
            headerName: 'X-XSRF-TOKEN',
          }),
          withInterceptors([authInterceptor]),
        ),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('authenticates with CSRF and scopes the server session to a hospital', async () => {
    const loginPromise = service.login('lucas.galante@inventorymed.local', 'secret');
    const loginCsrfRequest = http.expectOne('/api/v1/auth/csrf');
    expect(loginCsrfRequest.request.method).toBe('GET');
    expect(loginCsrfRequest.request.withCredentials).toBe(true);
    loginCsrfRequest.flush({
      headerName: 'X-XSRF-TOKEN',
    });
    document.cookie = 'XSRF-TOKEN=login-csrf-token; Path=/';
    await Promise.resolve();
    await Promise.resolve();

    const loginRequest = http.expectOne('/api/v1/auth/login');
    expect(loginRequest.request.body).toEqual({
      email: 'lucas.galante@inventorymed.local',
      password: 'secret',
    });
    expect(loginRequest.request.headers.get('X-XSRF-TOKEN')).toBe('login-csrf-token');
    expect(loginRequest.request.withCredentials).toBe(true);
    loginRequest.flush({
      user: {
        id: 'user-id',
        name: 'LUCAS GALANTE',
        email: 'lucas.galante@inventorymed.local',
        systemRoles: [],
        mustChangePassword: false,
      },
      hospitals: [
        {
          id: 'hospital-id',
          name: 'UPA DE JOÃO PINHEIRO',
          shortName: 'UPA JP',
          city: 'JOÃO PINHEIRO',
          role: 'MEDICO',
        },
      ],
      requiresHospitalSelection: true,
      selectedHospitalId: null,
      selectedHospitalRole: null,
    });
    await loginPromise;

    const selectionPromise = service.selectHospital('hospital-id');
    const selectionCsrfRequest = http.expectOne('/api/v1/auth/csrf');
    selectionCsrfRequest.flush({
      headerName: 'X-XSRF-TOKEN',
    });
    document.cookie = 'XSRF-TOKEN=selection-csrf-token; Path=/';
    await Promise.resolve();
    await Promise.resolve();

    const selectionRequest = http.expectOne('/api/v1/auth/select-hospital');
    expect(selectionRequest.request.headers.get('X-XSRF-TOKEN')).toBe('selection-csrf-token');
    selectionRequest.flush({
      hospital: {
        id: 'hospital-id',
        name: 'UPA DE JOÃO PINHEIRO',
        shortName: 'UPA JP',
        city: 'JOÃO PINHEIRO',
        role: 'MEDICO',
      },
    });
    await selectionPromise;

    expect(service.selectedHospitalId()).toBe('hospital-id');
    expect(window.sessionStorage.length).toBe(0);
  });

  it('restores the visible state from the HttpOnly server session', async () => {
    const validationPromise = service.validateSession();
    const profileRequest = http.expectOne('/api/v1/auth/me');
    expect(profileRequest.request.withCredentials).toBe(true);
    profileRequest.flush({
      user: {
        id: 'user-id',
        name: 'LUCAS GALANTE',
        email: 'lucas.galante@inventorymed.local',
        systemRoles: [],
        mustChangePassword: false,
      },
      hospitals: [],
      selectedHospitalId: null,
      selectedHospitalRole: null,
    });

    expect(await validationPromise).toBe(true);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('treats a session created by the previous API version as non-administrative', async () => {
    const validationPromise = service.validateSession();
    const profileRequest = http.expectOne('/api/v1/auth/me');
    profileRequest.flush({
      user: {
        id: 'legacy-user-id',
        name: 'USUÁRIO EXISTENTE',
        email: 'usuario@inventorymed.local',
        mustChangePassword: false,
      },
      hospitals: [],
      selectedHospitalId: null,
      selectedHospitalRole: null,
    });

    expect(await validationPromise).toBe(true);
    expect(service.isSystemAdministrator()).toBe(false);
  });

  it('changes the initial password through the protected session', async () => {
    const changePromise = service.changePassword('Initial@Password1', 'Personal@Password2');
    const csrfRequest = http.expectOne('/api/v1/auth/csrf');
    csrfRequest.flush({ headerName: 'X-XSRF-TOKEN' });
    document.cookie = 'XSRF-TOKEN=password-csrf-token; Path=/';
    await Promise.resolve();
    await Promise.resolve();

    const request = http.expectOne('/api/v1/auth/change-password');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      currentPassword: 'Initial@Password1',
      newPassword: 'Personal@Password2',
    });
    request.flush({
      id: 'user-id',
      name: 'ADMINISTRADOR GERAL',
      email: 'admin@inventorymed.local',
      systemRoles: ['ADMIN_SISTEMA'],
      mustChangePassword: false,
    });

    await changePromise;
    expect(service.user()?.mustChangePassword).toBe(false);
  });
});
