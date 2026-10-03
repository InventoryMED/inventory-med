export interface AuthenticatedUser {
  id: string;
  name: string;
  email: string;
  systemRoles: string[];
  mustChangePassword: boolean;
}

export interface HospitalAccess {
  id: string;
  name: string;
  shortName: string;
  city: string;
  role: 'ADMIN_HOSPITAL' | 'RESPONSAVEL_CLINICO' | 'MEDICO' | 'ENFERMAGEM' | 'RECEPCAO' | string;
}

export interface LoginResponse {
  user: AuthenticatedUser;
  hospitals: HospitalAccess[];
  requiresHospitalSelection: boolean;
  selectedHospitalId: string | null;
  selectedHospitalRole: string | null;
}

export interface HospitalSelectionResponse {
  hospital: HospitalAccess;
}

export interface MeResponse {
  user: AuthenticatedUser;
  hospitals: HospitalAccess[];
  selectedHospitalId: string | null;
  selectedHospitalRole: string | null;
}
