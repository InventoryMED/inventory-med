export type MedicalBedStatus = 'AVAILABLE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE' | 'BLOCKED';

export interface MedicalPatient {
  id: string;
  fullName: string;
  birthDate: string | null;
  sex: string;
  weightKg: number | null;
  diagnosis: string | null;
  comorbidities: string | null;
  allergies: string | null;
}

export interface MedicalAdmission {
  id: string;
  admittedAt: string;
  patient: MedicalPatient;
}

export interface MedicalBed {
  id: string;
  code: string;
  status: MedicalBedStatus;
  admission: MedicalAdmission | null;
}

export interface MedicalRoom {
  id: string;
  name: string;
  code: string;
  floorName: string | null;
  beds: MedicalBed[];
}

export interface MedicalCareUnit {
  id: string;
  name: string;
  code: string;
  rooms: MedicalRoom[];
}

export interface MedicalWorkspace {
  hospitalId: string;
  careUnits: MedicalCareUnit[];
}

export type ClinicalFormKind = 'PRESCRIPTION' | 'EVOLUTION' | 'PROCEDURE' | 'AIH';

export interface PrescriptionStarterClinic {
  code: string;
  label: string;
}

export interface PrescriptionStarterTemplate {
  code: string;
  name: string;
  description: string;
  clinicCodes: string[];
}

export interface PrescriptionStarterCatalog {
  clinics: PrescriptionStarterClinic[];
  templates: PrescriptionStarterTemplate[];
}

export type ClinicalFieldType =
  | 'SHORT_TEXT'
  | 'LONG_TEXT'
  | 'INTEGER'
  | 'DECIMAL'
  | 'DATE'
  | 'TIME'
  | 'SINGLE_SELECT'
  | 'MULTI_SELECT'
  | 'BOOLEAN'
  | 'MEDICATION_LINE'
  | 'CLINICAL_TABLE'
  | 'DIET_PLAN'
  | 'NURSING_CARE_PLAN'
  | 'MONITORING_PLAN'
  | 'VENTILATORY_SUPPORT_PLAN'
  | 'REHABILITATION_PLAN'
  | 'ISOLATION_PRECAUTIONS_PLAN'
  | 'THERAPEUTIC_SUPPORT_PLAN'
  | 'CRITICAL_CARE_PLAN'
  | 'MEDICATION_THERAPY_PLAN'
  | 'PROCEDURE_PLAN'
  | 'AIH_PLAN';

export type DietType = 'ORAL' | 'ENTERAL' | 'PARENTERAL' | 'JEJUM';

export interface DietPrescriptionDraft {
  type: DietType | null;
  oral: {
    consistency: string;
    restrictions: string[];
  };
  enteral: {
    accessRoute: string;
    infusionRegimen: string;
    formulaType: string;
    rateMlHour: number | null;
    bolusVolumeMl: number | null;
    bolusFrequency: string;
    tubeFlushMl: number | null;
    flushInterval: string;
  };
  parenteral: {
    accessRoute: string;
    preparationType: string;
    totalVolumeMl: number | null;
    rateMlHour: number | null;
    totalCaloriesKcalDay: number | null;
    proteinGoalGramsKgDay: number | null;
    gastrointestinalFailureJustification: string;
  };
  fasting: {
    reason: string;
    reassessment: string;
  };
}

export interface DietPrescriptionCatalog {
  oralConsistencies: string[];
  oralRestrictions: string[];
  enteralAccessRoutes: string[];
  enteralInfusionRegimens: string[];
  enteralFormulaTypes: string[];
  parenteralAccessRoutes: string[];
  parenteralPreparationTypes: string[];
  fastingReasons: string[];
}

export interface DietPrescriptionResponse {
  structuredDiet: {
    type: DietType;
    summaryLine: string;
    prescriptionDetails: string;
  };
  billingAudit: {
    itemsForReview: string[];
    auditAlerts: string[];
  };
}

export interface DietPrescriptionSelection {
  draft: DietPrescriptionDraft;
  response: DietPrescriptionResponse | null;
}

export interface NursingCarePrescriptionDraft {
  positioning: {
    headPosition: string;
    repositioningFrequency: string;
    pressureProtection: string[];
  };
  hygieneSkin: {
    bath: string;
    oralHygiene: string;
    skinCare: string[];
  };
  dressingsDrains: {
    catheterDressing: string;
    acuteWoundCare: string;
    complexWoundCoverage: string;
    dressingChangeFrequency: string;
    drainCare: string[];
  };
  procedures: {
    airwaySuction: string;
    deviceCare: string[];
    fluidBalance: string;
  };
}

export interface NursingCarePrescriptionCatalog {
  headPositions: string[];
  repositioningFrequencies: string[];
  pressureProtection: string[];
  baths: string[];
  oralHygiene: string[];
  skinCare: string[];
  catheterDressings: string[];
  acuteWoundCare: string[];
  complexWoundCoverages: string[];
  dressingChangeFrequencies: string[];
  drainCare: string[];
  airwaySuction: string[];
  deviceCare: string[];
  fluidBalance: string[];
}

export interface NursingCarePrescriptionResponse {
  structuredCare: {
    summaryLine: string;
    prescriptionDetails: string;
  };
  billingAudit: {
    itemsForReview: string[];
    qualitySafetyIndicators: string[];
  };
}

export interface NursingCarePrescriptionSelection {
  draft: NursingCarePrescriptionDraft;
  response: NursingCarePrescriptionResponse | null;
}

export interface MonitoringPrescriptionDraft {
  vitalSigns: {
    frequency: string;
    painScale: string;
    consciousnessSedationScale: string;
    fallRiskScale: string;
  };
  glucoseMonitoring: {
    frequency: string;
    hypoglycemiaProtocol: boolean;
    slidingScale: boolean;
    insulinType: string;
  };
  fluidBalanceOutputs: {
    fluidBalance: string;
    urineOutput: string;
    drainsTubes: string[];
    otherMeasurements: string[];
  };
  invasiveMonitoring: {
    hemodynamic: string[];
    neurological: string[];
  };
}

export interface MonitoringPrescriptionCatalog {
  vitalSignsFrequencies: string[];
  painScales: string[];
  consciousnessSedationScales: string[];
  fallRiskScales: string[];
  glucoseMonitoringFrequencies: string[];
  insulinTypes: string[];
  fluidBalanceOptions: string[];
  urineOutputOptions: string[];
  drainsTubes: string[];
  otherMeasurements: string[];
  hemodynamicMonitoring: string[];
  neurologicalMonitoring: string[];
}

export interface MonitoringPrescriptionResponse {
  structuredMonitoring: {
    summaryLine: string;
    prescriptionDetails: string;
  };
  billingAudit: {
    itemsForReview: string[];
    auditAlerts: string[];
  };
}

export interface MonitoringPrescriptionSelection {
  draft: MonitoringPrescriptionDraft;
  response: MonitoringPrescriptionResponse | null;
}

export interface VentilatorySupportItemDraft {
  id: number;
  supportType: string;
  frequency: string;
  scheduling: string;
  lowFlow: {
    device: string;
    oxygenFlowLitersMinute: number | null;
    fio2Percent: number | null;
  } | null;
  highFlow: {
    flowLitersMinute: number | null;
    fio2Percent: number | null;
    temperatureCelsius: number | null;
    interfaceSize: string;
  } | null;
  nonInvasive: {
    mode: string;
    ipapCmH2o: number | null;
    epapPeepCmH2o: number | null;
    supportPressureCmH2o: number | null;
    fio2Percent: number | null;
    backupRate: number | null;
    interfaceType: string;
    sessionHours: number | null;
  } | null;
  invasive: {
    airway: string;
    airwayDetail: string;
    mode: string;
    tidalVolumeMl: number | null;
    respiratoryRate: number | null;
    peepCmH2o: number | null;
    fio2Percent: number | null;
    inspiratoryFlowLitersMinute: number | null;
    inspiratoryTimeSeconds: number | null;
    pauseSeconds: number | null;
    inspiratoryPressureCmH2o: number | null;
    supportPressureCmH2o: number | null;
    triggerSensitivity: number | null;
    protectiveGoals: string[];
  } | null;
}

export interface VentilatorySupportDraft {
  selectedTemplate: string;
  items: VentilatorySupportItemDraft[];
}

export interface VentilatorySupportOption {
  code: string;
  label: string;
}

export interface VentilatorySupportCatalog {
  supportTypes: VentilatorySupportOption[];
  lowFlowDevices: VentilatorySupportOption[];
  lowFlowFrequencies: VentilatorySupportOption[];
  highFlowInterfaceSizes: VentilatorySupportOption[];
  nonInvasiveModes: VentilatorySupportOption[];
  nonInvasiveInterfaces: VentilatorySupportOption[];
  nonInvasiveFrequencies: VentilatorySupportOption[];
  invasiveAirways: VentilatorySupportOption[];
  invasiveModes: VentilatorySupportOption[];
  schedulingOptions: VentilatorySupportOption[];
  protectiveGoals: VentilatorySupportOption[];
  templates: Array<{
    code: string;
    label: string;
    item: VentilatorySupportItemDraft;
  }>;
}

export interface VentilatorySupportResponse {
  structuredVentilatorySupport: {
    summaryLine: string;
    prescriptionDetails: string;
    orderRows: Array<{
      description: string;
      interfaceRoute: string;
      frequency: string;
      scheduling: string;
    }>;
  };
  billingAudit: {
    itemsForReview: string[];
    auditAlerts: string[];
  };
}

export interface VentilatorySupportSelection {
  draft: VentilatorySupportDraft;
  response: VentilatorySupportResponse | null;
}

export interface RehabilitationItemDraft {
  id: number;
  specialty: string;
  procedure: string;
  frequency: string;
  scheduling: string;
  clinicalJustification: string;
}

export interface RehabilitationDraft {
  selectedTemplate: string;
  items: RehabilitationItemDraft[];
}

export interface RehabilitationCatalog {
  specialties: VentilatorySupportOption[];
  respiratoryProcedures: VentilatorySupportOption[];
  motorProcedures: VentilatorySupportOption[];
  speechTherapyProcedures: VentilatorySupportOption[];
  occupationalTherapyProcedures: VentilatorySupportOption[];
  respiratoryFrequencies: VentilatorySupportOption[];
  motorFrequencies: VentilatorySupportOption[];
  speechTherapyFrequencies: VentilatorySupportOption[];
  occupationalTherapyFrequencies: VentilatorySupportOption[];
  schedulingOptions: VentilatorySupportOption[];
  templates: Array<{
    code: string;
    label: string;
    item: RehabilitationItemDraft;
  }>;
}

export interface RehabilitationResponse {
  structuredRehabilitation: {
    summaryLine: string;
    prescriptionDetails: string;
    orderRows: Array<{
      description: string;
      specialty: string;
      frequency: string;
      scheduling: string;
    }>;
  };
  billingAudit: {
    itemsForReview: string[];
    auditAlerts: string[];
  };
}

export interface RehabilitationSelection {
  draft: RehabilitationDraft;
  response: RehabilitationResponse | null;
}

export interface IsolationPrecautionItemDraft {
  id: number;
  precautionType: string;
  reasonPathogen: string;
  durationReview: string;
  scheduling: string;
}

export interface IsolationPrecautionDraft {
  selectedTemplate: string;
  items: IsolationPrecautionItemDraft[];
}

export interface IsolationPrecautionCatalog {
  precautionTypes: VentilatorySupportOption[];
  durationOptions: VentilatorySupportOption[];
  schedulingOptions: VentilatorySupportOption[];
  templates: Array<{
    code: string;
    label: string;
    items: IsolationPrecautionItemDraft[];
  }>;
}

export interface IsolationPrecautionResponse {
  structuredIsolation: {
    summaryLine: string;
    prescriptionDetails: string;
    orderRows: Array<{
      description: string;
      durationReview: string;
      scheduling: string;
    }>;
  };
  billingAudit: {
    suppliesEquipmentForReview: string[];
    auditAlerts: string[];
  };
}

export interface IsolationPrecautionSelection {
  draft: IsolationPrecautionDraft;
  response: IsolationPrecautionResponse | null;
}

export interface TherapeuticHydrationDraft {
  id: number;
  baseSolution: string;
  additives: string[];
  route: string;
  frequency: string;
  infusionMode: string;
  rateValue: number | null;
  rateUnit: string;
  scheduling: string;
}

export interface TherapeuticBloodProductDraft {
  id: number;
  product: string;
  modifications: string[];
  quantity: number | null;
  quantityUnit: string;
  route: string;
  infusionMinutes: number | null;
  preMedications: string[];
  scheduling: string;
}

export interface TherapeuticSupportDraft {
  clinicalContext: string;
  hydrationSolutions: TherapeuticHydrationDraft[];
  glucoseControl: {
    frequency: string;
    hypoglycemiaProtocolActive: boolean;
    correctionScaleActive: boolean;
    insulinType: string;
    continuousPump: boolean;
  };
  bloodProducts: TherapeuticBloodProductDraft[];
}

export interface TherapeuticSupportCatalog {
  clinicalContexts: VentilatorySupportOption[];
  baseSolutions: VentilatorySupportOption[];
  electrolyteAdditives: VentilatorySupportOption[];
  hydrationFrequencies: VentilatorySupportOption[];
  infusionModes: VentilatorySupportOption[];
  rateUnits: VentilatorySupportOption[];
  schedulingOptions: VentilatorySupportOption[];
  glucoseFrequencies: VentilatorySupportOption[];
  insulinTypes: VentilatorySupportOption[];
  bloodProducts: Array<VentilatorySupportOption & { category: string }>;
  bloodProductModifications: VentilatorySupportOption[];
  transfusionRoutes: VentilatorySupportOption[];
  quantityUnits: VentilatorySupportOption[];
  preMedications: VentilatorySupportOption[];
}

export interface TherapeuticSupportResponse {
  structuredTherapeuticSupport: {
    summaryLine: string;
    prescriptionDetails: string;
    orderRows: Array<{
      section: string;
      description: string;
      route: string;
      frequency: string;
      scheduling: string;
    }>;
  };
  billingAudit: {
    suppliesEquipmentForReview: string[];
    auditAlerts: string[];
  };
}

export interface TherapeuticSupportSelection {
  draft: TherapeuticSupportDraft;
  response: TherapeuticSupportResponse | null;
}

export interface CriticalCareVasoactiveDraft {
  id: number;
  drug: string;
  dilution: string;
  finalConcentration: string;
  initialRate: number | null;
  rateUnit: string;
  vascularAccess: string;
  bloodPressureMonitoring: string;
  therapeuticGoal: string;
  scheduling: string;
}

export interface CriticalCareSedationDraft {
  id: number;
  drug: string;
  preparation: string;
  administrationMode: string;
  rateDoseValue: number | null;
  rateDoseUnit: string;
  sedationTarget: string;
  ventilatoryStatus: string;
  route: string;
  scheduling: string;
}

export interface CriticalCareEmergencyDraft {
  id: number;
  drug: string;
  doseAdministration: string;
  route: string;
  emergencyIndication: string;
  scheduling: string;
}

export interface CriticalCareDraft {
  clinicalContext: string;
  vasoactiveDrugs: CriticalCareVasoactiveDraft[];
  sedationAnalgesiaBnm: CriticalCareSedationDraft[];
  emergencyMedications: CriticalCareEmergencyDraft[];
}

export interface CriticalCareCatalog {
  clinicalContexts: VentilatorySupportOption[];
  vasoactiveDrugs: Array<
    VentilatorySupportOption & { centralAccessRequired: boolean; specialTubing: string }
  >;
  sedationDrugs: Array<VentilatorySupportOption & { category: string; allowedModes: string[] }>;
  emergencyDrugs: Array<VentilatorySupportOption & { highCost: boolean }>;
  vascularAccesses: VentilatorySupportOption[];
  bloodPressureMonitoring: VentilatorySupportOption[];
  vasoactiveRateUnits: VentilatorySupportOption[];
  administrationModes: VentilatorySupportOption[];
  sedationRateUnits: VentilatorySupportOption[];
  sedationTargets: VentilatorySupportOption[];
  ventilatoryStatuses: VentilatorySupportOption[];
  sedationRoutes: VentilatorySupportOption[];
  emergencyRoutes: VentilatorySupportOption[];
  emergencyScheduling: VentilatorySupportOption[];
}

export interface CriticalCareResponse {
  structuredCriticalCare: {
    summaryLine: string;
    prescriptionDetails: string;
    orderRows: Array<{
      section: string;
      description: string;
      route: string;
      frequency: string;
      scheduling: string;
    }>;
  };
  billingAudit: {
    suppliesEquipmentForReview: string[];
    auditAlerts: string[];
  };
}

export interface CriticalCareSelection {
  draft: CriticalCareDraft;
  response: CriticalCareResponse | null;
}

export interface MedicationTherapyAntimicrobialDraft {
  id: number;
  drug: string;
  customDrug: string;
  dosePreparation: string;
  route: string;
  administrationMode: string;
  diluent: string;
  infusionSet: string;
  frequency: string;
  scheduling: string;
  loadingDose: string;
  conditionalTrigger: string;
  treatmentDay: number | null;
  infectionFocus: string;
  ccihStatus: string;
  ccihOpinion: string;
  renalDoseAssessment: string;
}

export interface MedicationTherapyProphylaxisDraft {
  id: number;
  intervention: string;
  dosePreparation: string;
  route: string;
  frequency: string;
  scheduling: string;
  conditionalTrigger: string;
  suspensionReason: string;
}

export interface MedicationTherapyContinuousDraft {
  id: number;
  medication: string;
  dosePreparation: string;
  route: string;
  frequency: string;
  reconciliationStatus: string;
  scheduling: string;
  conditionalTrigger: string;
  suspensionReason: string;
}

export interface MedicationTherapySymptomaticDraft {
  id: number;
  drug: string;
  customDrug: string;
  dosePreparation: string;
  route: string;
  frequency: string;
  scheduling: string;
  trigger: string;
  minimumInterval: string;
}

export interface MedicationTherapyDraft {
  clinicalContext: string;
  renalFunction: { measure: string; valueMlMin: number | null } | null;
  bleedingRisk: { plateletCount: number | null; activeBleeding: boolean | null } | null;
  antimicrobials: MedicationTherapyAntimicrobialDraft[];
  prophylaxes: MedicationTherapyProphylaxisDraft[];
  continuousMedications: MedicationTherapyContinuousDraft[];
  analgesiaSymptomatics: MedicationTherapySymptomaticDraft[];
}

export interface MedicationTherapyCatalog {
  clinicalContexts: VentilatorySupportOption[];
  antimicrobials: VentilatorySupportOption[];
  antimicrobialRoutes: VentilatorySupportOption[];
  antimicrobialAdministrationModes: VentilatorySupportOption[];
  diluents: VentilatorySupportOption[];
  infusionSets: VentilatorySupportOption[];
  antimicrobialScheduling: VentilatorySupportOption[];
  ccihStatuses: VentilatorySupportOption[];
  renalFunctionMeasures: VentilatorySupportOption[];
  renalDoseAssessments: VentilatorySupportOption[];
  prophylaxisOptions: Array<
    VentilatorySupportOption & {
      category: string;
      anticoagulant: boolean;
      filledSyringe: boolean;
      equipment: boolean;
    }
  >;
  prophylaxisRoutes: VentilatorySupportOption[];
  prophylaxisScheduling: VentilatorySupportOption[];
  reconciliationStatuses: VentilatorySupportOption[];
  continuousMedicationScheduling: VentilatorySupportOption[];
  symptomaticMedications: VentilatorySupportOption[];
  medicationRoutes: VentilatorySupportOption[];
  symptomaticScheduling: VentilatorySupportOption[];
}

export interface MedicationTherapyResponse {
  structuredMedicationTherapy: {
    summaryLine: string;
    prescriptionDetails: string;
    orderRows: Array<{
      section: string;
      description: string;
      route: string;
      frequency: string;
      scheduling: string;
    }>;
  };
  billingAudit: {
    suppliesEquipmentForReview: string[];
    auditAlerts: string[];
  };
}

export interface MedicationTherapySelection {
  draft: MedicationTherapyDraft;
  response: MedicationTherapyResponse | null;
}

export type ProcedureRecordType = 'REQUESTED' | 'PERFORMED';

export interface BedsideProcedureItemDraft {
  id: number;
  recordType: ProcedureRecordType;
  procedureCode: string;
  customProcedure: string;
  clinicalIndication: string;
  cid10Reference: string;
  anatomicalSite: string;
  laterality: string;
  asepsisAntisepsis: string;
  sterileBarrier: string;
  localAnesthesia: string;
  imageGuided: boolean | null;
  imageAttachmentReference: string;
  imageGuidance: string;
  deviceName: string;
  deviceBrand: string;
  deviceCaliber: string;
  deviceLot: string;
  anvisaRegistration: string;
  fixationDressingConnections: string;
  samplesLaboratory: string;
  postProcedureControl: string;
  postProcedureDetails: string;
  monitoringAssistance: string;
  urgency: string;
  techniqueOutcome: string;
  complications: string;
  performedAt: string | null;
}

export interface BedsideProcedureDraft {
  clinicalContext: string;
  selectedTemplate: string;
  items: BedsideProcedureItemDraft[];
}

export interface BedsideProcedureOption {
  code: string;
  label: string;
}

export interface BedsideProcedureDefinition extends BedsideProcedureOption {
  billingReference: string;
  pairedSite: boolean;
  deviceTraceabilityRequired: boolean;
  postProcedureControlRequired: boolean;
  majorInvasiveProcedure: boolean;
  supplyKitItems: string[];
  defaultSite: string;
  defaultAsepsis: string;
  defaultSterileBarrier: string;
  defaultAnesthesia: string;
  defaultImageGuidance: string;
  defaultDevice: string;
  defaultCaliber: string;
  defaultFixation: string;
  defaultSamples: string;
  defaultPostProcedureControl: string;
  defaultMonitoring: string;
}

export interface BedsideProcedureCatalog {
  clinicalContexts: BedsideProcedureOption[];
  recordTypes: BedsideProcedureOption[];
  procedures: BedsideProcedureDefinition[];
  lateralities: BedsideProcedureOption[];
  urgencyOptions: BedsideProcedureOption[];
  postProcedureControls: BedsideProcedureOption[];
  quickKits: Array<{
    code: string;
    label: string;
    procedureCodes: string[];
  }>;
  templates: Array<{
    code: string;
    label: string;
    item: Omit<
      BedsideProcedureItemDraft,
      | 'id'
      | 'recordType'
      | 'customProcedure'
      | 'clinicalIndication'
      | 'cid10Reference'
      | 'imageGuided'
      | 'imageAttachmentReference'
      | 'deviceBrand'
      | 'deviceLot'
      | 'anvisaRegistration'
      | 'postProcedureDetails'
      | 'techniqueOutcome'
      | 'complications'
      | 'performedAt'
    >;
  }>;
}

export interface BedsideProcedureResponse {
  structuredProcedures: {
    summaryLine: string;
    prescriptionDetails: string;
  };
  billingAudit: {
    suppliesEquipmentForReview: string[];
    auditAlerts: string[];
  };
}

export interface AihOption {
  code: string;
  label: string;
}

export interface AihProcedureDefinition extends AihOption {
  tussCode: string | null;
  cbhpmCode: string | null;
  sigtapCode: string | null;
  pairedSite: boolean;
  defaultSite: string;
}

export interface AihCatalog {
  clinicalContexts: AihOption[];
  admissionCharacters: AihOption[];
  lateralities: AihOption[];
  procedures: AihProcedureDefinition[];
  imageGuidance: {
    tussCode: string | null;
    cbhpmCode: string | null;
    sigtapCode: string | null;
  };
}

export interface AihPatientIdentification {
  name: string;
  cns: string;
  motherName: string;
  medicalRecordNumber: string;
  address: string;
  bed: string;
  hospital: string;
  cnes: string;
}

export interface AihRequestedProcedure {
  id: number;
  procedureCode: string;
  anatomicalSite: string;
  laterality: string;
  scheduling: string;
  imageGuided: boolean;
  imageAttachmentReference: string;
  clinicalJustification: string;
}

export interface AihManualData {
  mainSignsSymptoms: string;
  admissionConditions: string;
  examResults: string;
  initialDiagnosis: string;
  primaryCid: string;
  secondaryCids: string;
  requestedProcedureCode: string;
  admissionCharacter: string;
}

export interface AihDraft {
  clinicalContext: string;
  patientId: string;
  patient: AihPatientIdentification;
  requestedProcedures: AihRequestedProcedure[];
  manualData: AihManualData;
}

export interface AihResponse {
  structuredAih: {
    patient: AihPatientIdentification;
    manualData: AihManualData;
    clinicalContext: string;
    admissionCharacter: string;
    requestedProcedures: Array<
      AihRequestedProcedure & {
        name: string;
        tussCode: string | null;
        cbhpmCode: string | null;
        sigtapCode: string | null;
        imageGuidanceTussCode: string | null;
      }
    >;
    reportText: string;
  };
  billingAudit: {
    alerts: string[];
  };
}

export interface ClinicalDocument {
  id: string;
  admissionId: string;
  kind: ClinicalFormKind;
  templateVersionId: string;
  status: string;
  versionNumber: number;
  authorUserId: string;
  values: Record<string, unknown>;
  createdAt: string;
  finalizedAt: string | null;
}

export interface ClinicalFormOption {
  id: string;
  value: string;
  label: string;
  displayOrder: number;
  active: boolean;
}

export interface ClinicalFormField {
  id: string;
  key: string;
  label: string;
  type: ClinicalFieldType;
  required: boolean;
  displayOrder: number;
  active: boolean;
  placeholder: string | null;
  maxLength: number | null;
  options: ClinicalFormOption[];
}

export interface ClinicalFormSection {
  id: string;
  key: string;
  title: string;
  displayOrder: number;
  active: boolean;
  fields: ClinicalFormField[];
}

export interface ClinicalFormTemplate {
  templateId: string;
  name: string;
  kind: ClinicalFormKind;
  active: boolean;
  versionId: string;
  versionNumber: number;
  status: string;
  sections: ClinicalFormSection[];
}
