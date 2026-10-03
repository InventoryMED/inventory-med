import { computed, Injectable, signal } from '@angular/core';
import {
  AuthenticatedUser,
  HospitalAccess,
  HospitalSelectionResponse,
  LoginResponse,
  MeResponse,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthSessionStore {
  private readonly userState = signal<AuthenticatedUser | null>(null);
  private readonly hospitalsState = signal<HospitalAccess[]>([]);
  private readonly selectedHospitalIdState = signal<string | null>(null);

  readonly user = this.userState.asReadonly();
  readonly hospitals = this.hospitalsState.asReadonly();
  readonly selectedHospitalId = this.selectedHospitalIdState.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.userState()));
  readonly isSystemAdministrator = computed(
    () => this.userState()?.systemRoles?.includes('ADMIN_SISTEMA') ?? false,
  );
  readonly selectedHospital = computed(() =>
    this.hospitalsState().find((hospital) => hospital.id === this.selectedHospitalIdState()),
  );

  applyLogin(response: LoginResponse): void {
    this.userState.set(response.user);
    this.hospitalsState.set(response.hospitals);
    this.selectedHospitalIdState.set(response.selectedHospitalId);
  }

  applyHospitalSelection(response: HospitalSelectionResponse): void {
    this.selectedHospitalIdState.set(response.hospital.id);
    this.hospitalsState.update((hospitals) =>
      hospitals.map((hospital) =>
        hospital.id === response.hospital.id ? response.hospital : hospital,
      ),
    );
  }

  applyProfile(response: MeResponse): void {
    this.userState.set(response.user);
    this.hospitalsState.set(response.hospitals);
    this.selectedHospitalIdState.set(response.selectedHospitalId);
  }

  applyUser(user: AuthenticatedUser): void {
    this.userState.set(user);
  }

  clear(): void {
    this.userState.set(null);
    this.hospitalsState.set([]);
    this.selectedHospitalIdState.set(null);
  }
}
