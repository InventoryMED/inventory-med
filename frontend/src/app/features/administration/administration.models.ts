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
