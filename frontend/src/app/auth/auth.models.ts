export interface AuthenticatedUser {
  id: string;
  name: string;
  email: string;
}

export interface HospitalAccess {
  id: string;
  name: string;
  shortName: string;
  city: string;
  role: 'ADMIN' | 'DOCTOR' | 'NURSE' | string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: AuthenticatedUser;
  hospitals: HospitalAccess[];
  requiresHospitalSelection: boolean;
  selectedHospitalId: string | null;
}

export interface HospitalSelectionResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  hospital: HospitalAccess;
}

export interface MeResponse {
  user: AuthenticatedUser;
  hospitals: HospitalAccess[];
  selectedHospitalId: string | null;
}
