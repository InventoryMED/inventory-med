import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from '../../auth/auth.interceptor';
import {
  BedsideProcedureDraft,
  CriticalCareDraft,
  MedicationTherapyDraft,
  DietPrescriptionDraft,
  IsolationPrecautionDraft,
  MonitoringPrescriptionDraft,
  NursingCarePrescriptionDraft,
  RehabilitationDraft,
  TherapeuticSupportDraft,
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

  it('loads prescription clinics and compatible starter models', async () => {
    const catalogPromise = service.prescriptionStartOptions();
    const request = http.expectOne('/api/v1/clinical/prescriptions/start-options');
    expect(request.request.method).toBe('GET');
    request.flush({
      clinics: [{ code: 'CLINICA_MEDICA_UPA', label: 'CLÍNICA MÉDICA / UPA' }],
      templates: [
        {
          code: 'ADMISSION',
          name: 'ADMISSÃO',
          description: 'MODELO INICIAL PARA ADMISSÃO HOSPITALAR',
          clinicCodes: ['CLINICA_MEDICA_UPA'],
        },
      ],
    });

    const catalog = await catalogPromise;
    expect(catalog.clinics[0].label).toBe('CLÍNICA MÉDICA / UPA');
    expect(catalog.templates[0].clinicCodes).toEqual(['CLINICA_MEDICA_UPA']);
  });

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

  it('loads the isolation catalog and validates precautions in the backend', async () => {
    const catalogPromise = service.isolationPrecautionCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/isolation-precautions/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      precautionTypes: [{ code: 'CONTACT', label: 'PRECAUÇÃO DE CONTATO' }],
      durationOptions: [{ code: 'ENTIRE_HOSPITALIZATION', label: 'DURANTE TODA A INTERNAÇÃO' }],
      schedulingOptions: [{ code: 'CONTINUOUS', label: 'CONTÍNUO' }],
      templates: [],
    });
    expect((await catalogPromise).precautionTypes[0].code).toBe('CONTACT');

    const draft: IsolationPrecautionDraft = {
      selectedTemplate: 'CONTACT_KPC_MDR',
      items: [
        {
          id: 1,
          precautionType: 'CONTACT',
          reasonPathogen: 'COLONIZAÇÃO POR KPC',
          durationReview: 'ENTIRE_HOSPITALIZATION',
          scheduling: 'CONTINUOUS',
        },
      ],
    };
    const previewPromise = service.previewIsolationPrecautions(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/isolation-precautions/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredIsolation: {
        summaryLine: 'PRECAUÇÃO DE CONTATO — COLONIZAÇÃO POR KPC.',
        prescriptionDetails: '1. PRECAUÇÕES E ISOLAMENTO.',
        orderRows: [
          {
            description: 'PRECAUÇÃO DE CONTATO — COLONIZAÇÃO POR KPC',
            durationReview: 'DURANTE TODA A INTERNAÇÃO',
            scheduling: 'CONTÍNUO',
          },
        ],
      },
      billingAudit: { suppliesEquipmentForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredIsolation.orderRows).toHaveLength(1);
  });

  it('loads and validates hydration, glucose control and blood products in the backend', async () => {
    const catalogPromise = service.therapeuticSupportCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/therapeutic-support/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      clinicalContexts: [{ code: 'ICU', label: 'UTI' }],
      baseSolutions: [{ code: 'SF09_500', label: 'SORO FISIOLÓGICO 0,9% 500 ML' }],
      electrolyteAdditives: [],
      hydrationFrequencies: [],
      infusionModes: [],
      rateUnits: [],
      schedulingOptions: [],
      glucoseFrequencies: [],
      insulinTypes: [],
      bloodProducts: [],
      bloodProductModifications: [],
      transfusionRoutes: [],
      quantityUnits: [],
      preMedications: [],
    });
    expect((await catalogPromise).clinicalContexts[0].code).toBe('ICU');

    const draft: TherapeuticSupportDraft = {
      clinicalContext: 'ICU',
      hydrationSolutions: [],
      glucoseControl: {
        frequency: 'EVERY_4_HOURS',
        hypoglycemiaProtocolActive: true,
        correctionScaleActive: true,
        insulinType: 'REGULAR',
        continuousPump: false,
      },
      bloodProducts: [],
    };
    const previewPromise = service.previewTherapeuticSupport(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/therapeutic-support/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredTherapeuticSupport: {
        summaryLine: 'DXT 4/4H COM PROTOCOLO E ESCALA SC.',
        prescriptionDetails: '8. CONTROLE GLICÊMICO E INSULINOTERAPIA.',
        orderRows: [],
      },
      billingAudit: { suppliesEquipmentForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredTherapeuticSupport.summaryLine).toContain('DXT');
  });

  it('loads and validates critical care orders in the backend', async () => {
    const catalogPromise = service.criticalCareCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/critical-care/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      clinicalContexts: [{ code: 'ICU', label: 'UTI' }],
      vasoactiveDrugs: [],
      sedationDrugs: [],
      emergencyDrugs: [],
      vascularAccesses: [],
      bloodPressureMonitoring: [],
      vasoactiveRateUnits: [],
      administrationModes: [],
      sedationRateUnits: [],
      sedationTargets: [],
      ventilatoryStatuses: [],
      sedationRoutes: [],
      emergencyRoutes: [],
      emergencyScheduling: [],
    });
    expect((await catalogPromise).clinicalContexts[0].code).toBe('ICU');

    const draft: CriticalCareDraft = {
      clinicalContext: 'ADULT_EMERGENCY',
      vasoactiveDrugs: [],
      sedationAnalgesiaBnm: [],
      emergencyMedications: [
        {
          id: 1,
          drug: 'EPINEPHRINE_BOLUS',
          doseAdministration: '1 MG EM BOLUS',
          route: 'IO',
          emergencyIndication: 'PCR',
          scheduling: 'EMERGENCY',
        },
      ],
    };
    const previewPromise = service.previewCriticalCare(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/critical-care/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredCriticalCare: {
        summaryLine: 'ADRENALINA — EMERGÊNCIA.',
        prescriptionDetails: '16. ANTÍDOTOS, REVERSORES E EMERGÊNCIA (PCR):',
        orderRows: [],
      },
      billingAudit: { suppliesEquipmentForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredCriticalCare.prescriptionDetails).toContain('16.');
  });

  it('loads and validates structured medication therapy in the backend', async () => {
    const catalogPromise = service.medicationTherapyCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/medication-therapy/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      clinicalContexts: [{ code: 'WARD_HOSPITAL', label: 'CLÍNICA MÉDICA / HOSPITAL' }],
      antimicrobials: [],
      antimicrobialRoutes: [],
      antimicrobialAdministrationModes: [],
      diluents: [],
      infusionSets: [],
      antimicrobialScheduling: [],
      ccihStatuses: [],
      renalFunctionMeasures: [],
      renalDoseAssessments: [],
      prophylaxisOptions: [],
      prophylaxisRoutes: [],
      prophylaxisScheduling: [],
      reconciliationStatuses: [],
      continuousMedicationScheduling: [],
      symptomaticMedications: [],
      medicationRoutes: [],
      symptomaticScheduling: [],
    });
    expect((await catalogPromise).clinicalContexts[0].code).toBe('WARD_HOSPITAL');

    const draft: MedicationTherapyDraft = {
      clinicalContext: 'WARD_HOSPITAL',
      renalFunction: null,
      bleedingRisk: null,
      antimicrobials: [],
      prophylaxes: [],
      continuousMedications: [
        {
          id: 1,
          medication: 'LEVOTIROXINA',
          dosePreparation: '50 MCG',
          route: 'VO',
          frequency: '1X/DIA EM JEJUM',
          reconciliationStatus: 'MAINTAINED_HOME',
          scheduling: 'FIXED',
          conditionalTrigger: '',
          suspensionReason: '',
        },
      ],
      analgesiaSymptomatics: [],
    };
    const previewPromise = service.previewMedicationTherapy(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/medication-therapy/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredMedicationTherapy: {
        summaryLine: 'LEVOTIROXINA.',
        prescriptionDetails: '13. MEDICAMENTOS DE USO CONTÍNUO E ROTINA:',
        orderRows: [],
      },
      billingAudit: { suppliesEquipmentForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredMedicationTherapy.prescriptionDetails).toContain('13.');
  });

  it('loads the procedure catalog, validates a preview and lists admission documents', async () => {
    const catalogPromise = service.bedsideProcedureCatalog();
    const catalogRequest = http.expectOne('/api/v1/clinical/procedures/catalog');
    expect(catalogRequest.request.method).toBe('GET');
    catalogRequest.flush({
      clinicalContexts: [],
      recordTypes: [{ code: 'REQUESTED', label: 'SOLICITADO / PLANEJADO' }],
      procedures: [],
      lateralities: [],
      urgencyOptions: [],
      postProcedureControls: [],
      quickKits: [],
      templates: [],
    });
    expect((await catalogPromise).recordTypes[0].code).toBe('REQUESTED');

    const draft: BedsideProcedureDraft = {
      clinicalContext: 'ADULT_ICU',
      selectedTemplate: 'CVC',
      items: [],
    };
    const previewPromise = service.previewBedsideProcedure(draft);
    const previewRequest = http.expectOne('/api/v1/clinical/procedures/preview');
    expect(previewRequest.request.method).toBe('POST');
    expect(previewRequest.request.headers.get('X-XSRF-TOKEN')).toBe('medical-token');
    expect(previewRequest.request.body).toEqual(draft);
    previewRequest.flush({
      structuredProcedures: {
        summaryLine: 'CVC — SOLICITADO.',
        prescriptionDetails: '11. PROCEDIMENTOS E INTERVENÇÕES BEIRA-LEITO:',
      },
      billingAudit: { suppliesEquipmentForReview: [], auditAlerts: [] },
    });
    expect((await previewPromise).structuredProcedures.prescriptionDetails).toContain('11.');

    const documentsPromise = service.documents('admission-1');
    const documentsRequest = http.expectOne('/api/v1/clinical/admissions/admission-1/documents');
    expect(documentsRequest.request.method).toBe('GET');
    documentsRequest.flush([]);
    expect(await documentsPromise).toEqual([]);
  });
});
