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
  | 'CLINICAL_TABLE';

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
