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

export type ClinicalFormKind = 'PRESCRIPTION' | 'EVOLUTION';
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
  | 'MONITORING_PLAN';

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
