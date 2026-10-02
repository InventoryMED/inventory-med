import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    window.sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    service.logout();
  });

  it('authenticates and scopes the next token to the selected hospital', async () => {
    const loginPromise = service.login('lucas.galante@inventorymed.local', 'secret');
    const loginRequest = http.expectOne('/api/auth/login');
    expect(loginRequest.request.body).toEqual({
      email: 'lucas.galante@inventorymed.local',
      password: 'secret',
    });
    expect(loginRequest.request.headers.has('Authorization')).toBe(false);
    loginRequest.flush({
      accessToken: 'unscoped-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
      user: {
        id: 'user-id',
        name: 'LUCAS GALANTE',
        email: 'lucas.galante@inventorymed.local',
      },
      hospitals: [
        {
          id: 'hospital-id',
          name: 'UPA DE JOÃO PINHEIRO',
          shortName: 'UPA JP',
          city: 'JOÃO PINHEIRO',
          role: 'DOCTOR',
        },
      ],
      requiresHospitalSelection: true,
      selectedHospitalId: null,
    });
    await loginPromise;

    const selectionPromise = service.selectHospital('hospital-id');
    const selectionRequest = http.expectOne('/api/auth/select-hospital');
    expect(selectionRequest.request.headers.get('Authorization')).toBe('Bearer unscoped-token');
    selectionRequest.flush({
      accessToken: 'hospital-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
      hospital: {
        id: 'hospital-id',
        name: 'UPA DE JOÃO PINHEIRO',
        shortName: 'UPA JP',
        city: 'JOÃO PINHEIRO',
        role: 'DOCTOR',
      },
    });
    await selectionPromise;

    expect(service.accessToken()).toBe('hospital-token');
    expect(service.selectedHospitalId()).toBe('hospital-id');
  });
});
