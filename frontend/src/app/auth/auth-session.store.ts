import { computed, Injectable, signal } from '@angular/core';
import {
  AuthenticatedUser,
  HospitalAccess,
  HospitalSelectionResponse,
  LoginResponse,
  MeResponse,
} from './auth.models';

const STORAGE_KEY = 'inventory-med-auth-v1';

interface StoredAuthSession {
  accessToken: string;
  user: AuthenticatedUser;
  hospitals: HospitalAccess[];
  selectedHospitalId: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionStore {
  private readonly accessTokenState = signal<string | null>(null);
  private readonly userState = signal<AuthenticatedUser | null>(null);
  private readonly hospitalsState = signal<HospitalAccess[]>([]);
  private readonly selectedHospitalIdState = signal<string | null>(null);

  readonly accessToken = this.accessTokenState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly hospitals = this.hospitalsState.asReadonly();
  readonly selectedHospitalId = this.selectedHospitalIdState.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.accessTokenState()));
  readonly selectedHospital = computed(() =>
    this.hospitalsState().find((hospital) => hospital.id === this.selectedHospitalIdState()),
  );

  constructor() {
    this.restore();
  }

  applyLogin(response: LoginResponse): void {
    this.accessTokenState.set(response.accessToken);
    this.userState.set(response.user);
    this.hospitalsState.set(response.hospitals);
    this.selectedHospitalIdState.set(response.selectedHospitalId);
    this.persist();
  }

  applyHospitalSelection(response: HospitalSelectionResponse): void {
    this.accessTokenState.set(response.accessToken);
    this.selectedHospitalIdState.set(response.hospital.id);
    this.hospitalsState.update((hospitals) =>
      hospitals.map((hospital) =>
        hospital.id === response.hospital.id ? response.hospital : hospital,
      ),
    );
    this.persist();
  }

  applyProfile(response: MeResponse): void {
    this.userState.set(response.user);
    this.hospitalsState.set(response.hospitals);
    this.selectedHospitalIdState.set(response.selectedHospitalId);
    this.persist();
  }

  clear(): void {
    this.accessTokenState.set(null);
    this.userState.set(null);
    this.hospitalsState.set([]);
    this.selectedHospitalIdState.set(null);
    this.storage()?.removeItem(STORAGE_KEY);
  }

  private restore(): void {
    const raw = this.storage()?.getItem(STORAGE_KEY);
    if (!raw) return;

    try {
      const session = JSON.parse(raw) as StoredAuthSession;
      if (!session.accessToken || !session.user || !Array.isArray(session.hospitals)) {
        this.clear();
        return;
      }
      this.accessTokenState.set(session.accessToken);
      this.userState.set(session.user);
      this.hospitalsState.set(session.hospitals);
      this.selectedHospitalIdState.set(session.selectedHospitalId ?? null);
    } catch {
      this.clear();
    }
  }

  private persist(): void {
    const accessToken = this.accessTokenState();
    const user = this.userState();
    if (!accessToken || !user) return;

    const session: StoredAuthSession = {
      accessToken,
      user,
      hospitals: this.hospitalsState(),
      selectedHospitalId: this.selectedHospitalIdState(),
    };
    this.storage()?.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  private storage(): Storage | null {
    return typeof window === 'undefined' ? null : window.sessionStorage;
  }
}
