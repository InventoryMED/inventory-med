import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from '../../auth/auth.interceptor';
import { AdministrationService } from './administration.service';

describe('AdministrationService', () => {
  let service: AdministrationService;
  let http: HttpTestingController;

  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=administration-token; Path=/';
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
    service = TestBed.inject(AdministrationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('creates a hospital and can retry a failed database provisioning', async () => {
    const createPromise = service.createHospital({
      name: 'Hospital de João Pinheiro',
      shortName: 'HJP',
      city: 'João Pinheiro, MG',
    });
    const createRequest = http.expectOne('/api/v1/administration/hospitals');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.headers.get('X-XSRF-TOKEN')).toBe('administration-token');
    createRequest.flush({
      id: 'hospital-id',
      name: 'HOSPITAL DE JOÃO PINHEIRO',
      shortName: 'HJP',
      city: 'JOÃO PINHEIRO, MG',
      status: 'ACTIVE',
      provisionedAt: '2026-10-03T19:00:00Z',
    });
    expect((await createPromise).status).toBe('ACTIVE');

    const retryPromise = service.retryHospitalProvisioning('hospital-id');
    const retryRequest = http.expectOne('/api/v1/administration/hospitals/hospital-id/provision');
    expect(retryRequest.request.method).toBe('POST');
    retryRequest.flush({
      id: 'hospital-id',
      name: 'HOSPITAL DE JOÃO PINHEIRO',
      shortName: 'HJP',
      city: 'JOÃO PINHEIRO, MG',
      status: 'ACTIVE',
      provisionedAt: '2026-10-03T19:00:00Z',
    });
    expect((await retryPromise).status).toBe('ACTIVE');
  });
});
