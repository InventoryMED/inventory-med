import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from '../../auth/auth.interceptor';
import {
  DietPrescriptionDraft,
  MonitoringPrescriptionDraft,
  NursingCarePrescriptionDraft,
  RehabilitationDraft,
  VentilatorySupportDraft,
} from './medical.models';
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

  it('loads the monitoring catalog and requests a backend-validated preview', async () => {
    const catalogPromise = service.monitoringCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/monitoring/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      vitalSignsFrequencies: ['6/6H'],
      painScales: ['EVA'],
      consciousnessSedationScales: [],
      fallRiskScales: [],
      glucoseMonitoringFrequencies: ['6/6H'],
      insulinTypes: ['REGULAR'],
      fluidBalanceOptions: [],
      urineOutputOptions: [],
      drainsTubes: [],
      otherMeasurements: [],
      hemodynamicMonitoring: [],
      neurologicalMonitoring: [],
    });
    expect((await catalogPromise).vitalSignsFrequencies).toEqual(['6/6H']);

    const draft: MonitoringPrescriptionDraft = {
      vitalSigns: {
        frequency: '6/6H',
        painScale: 'EVA',
        consciousnessSedationScale: '',
        fallRiskScale: '',
      },
      glucoseMonitoring: {
        frequency: '6/6H',
        hypoglycemiaProtocol: true,
        slidingScale: true,
        insulinType: 'REGULAR',
      },
      fluidBalanceOutputs: {
        fluidBalance: '',
        urineOutput: '',
        drainsTubes: [],
        otherMeasurements: [],
      },
      invasiveMonitoring: { hemodynamic: [], neurological: [] },
    };
    const previewPromise = service.previewMonitoring(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/monitoring/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredMonitoring: {
        summaryLine: 'MONITORIZAÇÃO E CONTROLES GLOBAIS: SSVV 6/6H; DXT 6/6H.',
        prescriptionDetails: 'MONITORIZAÇÃO E CONTROLES GLOBAIS.',
      },
      billingAudit: { itemsForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredMonitoring.summaryLine).toContain('SSVV 6/6H');
  });

  it('loads the ventilatory support catalog and validates the structured plan in the backend', async () => {
    const catalogPromise = service.ventilatorySupportCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/ventilatory-support/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      supportTypes: [{ code: 'LOW_FLOW', label: 'OXIGENOTERAPIA DE BAIXO FLUXO' }],
      lowFlowDevices: [{ code: 'NASAL_CANNULA', label: 'CATETER NASAL DE O₂' }],
      lowFlowFrequencies: [{ code: 'PRN_SPO2_92', label: 'SN SE SPO₂ < 92%' }],
      highFlowInterfaceSizes: [],
      nonInvasiveModes: [],
      nonInvasiveInterfaces: [],
      nonInvasiveFrequencies: [],
      invasiveAirways: [],
      invasiveModes: [],
      schedulingOptions: [{ code: 'PRN', label: 'SN' }],
      protectiveGoals: [],
      templates: [],
    });
    expect((await catalogPromise).supportTypes[0].code).toBe('LOW_FLOW');

    const draft: VentilatorySupportDraft = {
      selectedTemplate: '',
      items: [
        {
          id: 1,
          supportType: 'LOW_FLOW',
          frequency: 'PRN_SPO2_92',
          scheduling: 'PRN',
          lowFlow: {
            device: 'NASAL_CANNULA',
            oxygenFlowLitersMinute: 2,
            fio2Percent: null,
          },
          highFlow: null,
          nonInvasive: null,
          invasive: null,
        },
      ],
    };
    const previewPromise = service.previewVentilatorySupport(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/ventilatory-support/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredVentilatorySupport: {
        summaryLine: 'CATETER NASAL DE O₂ 2 L/MIN, SN SE SPO₂ < 92%.',
        prescriptionDetails: 'CATETER NASAL DE O₂ 2 L/MIN.',
        orderRows: [
          {
            description: 'CATETER NASAL DE O₂ 2 L/MIN',
            interfaceRoute: 'CATETER NASAL',
            frequency: 'SN SE SPO₂ < 92%',
            scheduling: 'SN',
          },
        ],
      },
      billingAudit: { itemsForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredVentilatorySupport.orderRows).toHaveLength(1);
  });

  it('loads the rehabilitation catalog and validates the multidisciplinary plan in the backend', async () => {
    const catalogPromise = service.rehabilitationCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/rehabilitation/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      specialties: [{ code: 'MOTOR_PHYSIOTHERAPY', label: 'FISIOTERAPIA MOTORA' }],
      respiratoryProcedures: [],
      motorProcedures: [{ code: 'PASSIVE_MOBILIZATION', label: 'MOBILIZAÇÃO PASSIVA' }],
      speechTherapyProcedures: [],
      occupationalTherapyProcedures: [],
      respiratoryFrequencies: [],
      motorFrequencies: [{ code: 'DAILY', label: '1X AO DIA' }],
      speechTherapyFrequencies: [],
      occupationalTherapyFrequencies: [],
      schedulingOptions: [{ code: 'FIXED', label: 'FIXO' }],
      templates: [],
    });
    expect((await catalogPromise).motorProcedures[0].code).toBe('PASSIVE_MOBILIZATION');

    const draft: RehabilitationDraft = {
      selectedTemplate: 'MOTOR_PASSIVE_DAILY',
      items: [
        {
          id: 1,
          specialty: 'MOTOR_PHYSIOTHERAPY',
          procedure: 'PASSIVE_MOBILIZATION',
          frequency: 'DAILY',
          scheduling: 'FIXED',
          clinicalJustification: '',
        },
      ],
    };
    const previewPromise = service.previewRehabilitation(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/rehabilitation/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredRehabilitation: {
        summaryLine: 'FISIOTERAPIA MOTORA: MOBILIZAÇÃO PASSIVA — 1X AO DIA.',
        prescriptionDetails: 'REABILITAÇÃO MULTIDISCIPLINAR.',
        orderRows: [
          {
            description: 'MOBILIZAÇÃO PASSIVA',
            specialty: 'FISIOTERAPIA MOTORA',
            frequency: '1X AO DIA',
            scheduling: 'FIXO',
          },
        ],
      },
      billingAudit: { itemsForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredRehabilitation.orderRows).toHaveLength(1);
  });
});
