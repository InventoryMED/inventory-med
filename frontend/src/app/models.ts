export type AppScreen = 'login' | 'hospital-select' | 'rooms' | 'prescription';

export type BedStatus = 'AVAILABLE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE';
export type PrescriptionScheduling = 'ACM' | 'SN' | 'FIXO';
export type MedicationSectionId =
  | 'ANALGESIA'
  | 'SYMPTOMATICS'
  | 'PROPHYLAXIS'
  | 'ANTIBIOTICS'
  | 'CONTINUOUS_USE'
  | 'OTHER_MEDICATIONS';

export interface PrescriptionItem {
  id: string;
  medication: string;
  dose: string;
  route: string;
  frequency: string;
  scheduling: PrescriptionScheduling;
}

export interface VitalSignOrder {
  id: string;
  description: string;
  frequency: string;
  guidance?: string;
}

export interface PrescriptionMedicationSection {
  id: MedicationSectionId;
  items: PrescriptionItem[];
}

export interface Prescription {
  id: string;
  createdAt: string;
  diet: string;
  notes: string;
  observations?: string[];
  abnormalities?: string[];
  items: PrescriptionItem[];
  vitalSigns?: VitalSignOrder[];
  hydrationItems?: PrescriptionItem[];
  analgesiaItems?: PrescriptionItem[];
  medicationSections?: PrescriptionMedicationSection[];
}

export interface Patient {
  id: string;
  name: string;
  birthDate?: string;
  sex?: string;
  weightKg?: number;
  diagnosis?: string;
  comorbidities?: string;
  allergies?: string;
  admissionAt: string;
  prescriptions: Prescription[];
}

export interface PrescriptionDraftRow {
  description: string;
  route: string;
  frequency: string;
  scheduling: PrescriptionScheduling;
}

export interface VitalSignDraftRow {
  description: string;
  frequency: string;
  guidance?: string;
}

export interface MedicationSectionDraft {
  id: MedicationSectionId;
  items: PrescriptionDraftRow[];
}

export interface Bed {
  id: string;
  code: string;
  status: BedStatus;
  patient?: Patient;
}

export interface Room {
  id: string;
  name: string;
  floor: string;
  unit: string;
  beds: Bed[];
}

export interface Hospital {
  id: string;
  name: string;
  shortName: string;
  city: string;
  rooms: Room[];
}
