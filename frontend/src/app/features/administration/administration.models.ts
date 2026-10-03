export type HospitalProvisioningStatus =
  'PROVISIONING' | 'ACTIVE' | 'SUSPENDED' | 'PROVISIONING_FAILED';

export interface AdministrationHospital {
  id: string;
  name: string;
  shortName: string;
  city: string;
  status: HospitalProvisioningStatus;
  provisionedAt: string | null;
}

export interface AdministrationUserHospital {
  hospitalId: string;
  hospitalName: string;
  role: string;
  active: boolean;
}

export interface AdministrationUser {
  id: string;
  name: string;
  email: string;
  active: boolean;
  mustChangePassword: boolean;
  systemRoles: string[];
  hospitals: AdministrationUserHospital[];
}

export interface HospitalCreatePayload {
  name: string;
  shortName: string;
  city: string;
}

export interface UserCreatePayload {
  fullName: string;
  email: string;
  initialPassword: string;
  systemRoles: string[];
  hospitalAssignments: Array<{
    hospitalId: string;
    role: string;
  }>;
}

export type AdministrationBedStatus =
  'AVAILABLE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE' | 'BLOCKED';

export interface AdministrationBed {
  id: string;
  roomId: string;
  code: string;
  status: AdministrationBedStatus;
  displayOrder: number;
  active: boolean;
}

export interface AdministrationRoom {
  id: string;
  careUnitId: string;
  name: string;
  code: string;
  floorName: string | null;
  displayOrder: number;
  active: boolean;
  beds: AdministrationBed[];
}

export interface AdministrationCareUnit {
  id: string;
  name: string;
  code: string;
  displayOrder: number;
  active: boolean;
  rooms: AdministrationRoom[];
}

export interface AdministrationStructure {
  careUnits: AdministrationCareUnit[];
}
