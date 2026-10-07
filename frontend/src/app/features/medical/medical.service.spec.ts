import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from '../../auth/auth.interceptor';
import { DietPrescriptionDraft, NursingCarePrescriptionDraft } from './medical.models';
import { MedicalService } from './medical.service';

describe('MedicalService', () => {
  let service: MedicalService;
  let http: HttpTestingController;

  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=medical-token; Path=/';
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
    service = TestBed.inject(MedicalService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the diet catalog and requests a validated preview', async () => {
    const catalogPromise = service.dietCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/diets/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      oralConsistencies: ['BRANDA'],
      oralRestrictions: [],
      enteralAccessRoutes: [],
      enteralInfusionRegimens: [],
      enteralFormulaTypes: [],
      parenteralAccessRoutes: [],
      parenteralPreparationTypes: [],
      fastingReasons: [],
    });
    expect((await catalogPromise).oralConsistencies).toEqual(['BRANDA']);

    const draft: DietPrescriptionDraft = {
      type: 'ORAL',
      oral: { consistency: 'BRANDA', restrictions: [] },
      enteral: {
        accessRoute: '',
        infusionRegimen: '',
        formulaType: '',
        rateMlHour: null,
        bolusVolumeMl: null,
        bolusFrequency: '',
        tubeFlushMl: null,
        flushInterval: '',
      },
      parenteral: {
        accessRoute: '',
        preparationType: '',
        totalVolumeMl: null,
        rateMlHour: null,
        totalCaloriesKcalDay: null,
        proteinGoalGramsKgDay: null,
        gastrointestinalFailureJustification: '',
      },
      fasting: { reason: '', reassessment: '' },
    };
    const previewPromise = service.previewDiet(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/diets/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredDiet: {
        type: 'ORAL',
        summaryLine: 'DIETA ORAL BRANDA.',
        prescriptionDetails: '1. DIETA ORAL.',
      },
      billingAudit: { itemsForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredDiet.type).toBe('ORAL');
  });

  it('loads the nursing care catalog and requests a validated preview', async () => {
    const catalogPromise = service.nursingCareCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/nursing-care/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      headPositions: ['ELEVADA A 30° - 45° (PADRÃO / UTI)'],
      repositioningFrequencies: [],
      pressureProtection: [],
      baths: [],
      oralHygiene: [],
      skinCare: [],
      catheterDressings: [],
      acuteWoundCare: [],
      complexWoundCoverages: [],
      dressingChangeFrequencies: [],
      drainCare: [],
      airwaySuction: [],
      deviceCare: [],
      fluidBalance: [],
    });
    expect((await catalogPromise).headPositions).toHaveLength(1);

    const draft: NursingCarePrescriptionDraft = {
      positioning: {
        headPosition: 'ELEVADA A 30° - 45° (PADRÃO / UTI)',
        repositioningFrequency: '',
        pressureProtection: [],
      },
      hygieneSkin: { bath: '', oralHygiene: '', skinCare: [] },
      dressingsDrains: {
        catheterDressing: '',
        acuteWoundCare: '',
        complexWoundCoverage: '',
        dressingChangeFrequency: '',
        drainCare: [],
      },
      procedures: { airwaySuction: '', deviceCare: [], fluidBalance: '' },
    };
    const previewPromise = service.previewNursingCare(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/nursing-care/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredCare: {
        summaryLine: 'CUIDADOS DE ENFERMAGEM: CABECEIRA ELEVADA.',
        prescriptionDetails: 'CUIDADOS DE ENFERMAGEM.',
      },
      billingAudit: { itemsForReview: [], qualitySafetyIndicators: [] },
    });
    expect((await previewPromise).structuredCare.summaryLine).toContain('CUIDADOS');
  });
});
