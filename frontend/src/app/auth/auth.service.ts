import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { HospitalSelectionResponse, LoginResponse, MeResponse } from './auth.models';
import { AuthSessionStore } from './auth-session.store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(AuthSessionStore);

  readonly accessToken = this.session.accessToken;
  readonly user = this.session.user;
  readonly hospitals = this.session.hospitals;
  readonly selectedHospitalId = this.session.selectedHospitalId;
  readonly selectedHospital = this.session.selectedHospital;
  readonly isAuthenticated = this.session.isAuthenticated;

  async login(email: string, password: string): Promise<LoginResponse> {
    this.session.clear();
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, {
        email: email.trim(),
        password,
      }),
    );
    this.session.applyLogin(response);
    return response;
  }

  async selectHospital(hospitalId: string): Promise<HospitalSelectionResponse> {
    const response = await firstValueFrom(
      this.http.post<HospitalSelectionResponse>(`${environment.apiBaseUrl}/auth/select-hospital`, {
        hospitalId,
      }),
    );
    this.session.applyHospitalSelection(response);
    return response;
  }

  async validateSession(): Promise<boolean> {
    if (!this.session.isAuthenticated()) return false;

    try {
      const response = await firstValueFrom(
        this.http.get<MeResponse>(`${environment.apiBaseUrl}/auth/me`),
      );
      this.session.applyProfile(response);
      return true;
    } catch {
      this.session.clear();
      return false;
    }
  }

  logout(): void {
    this.session.clear();
  }
}
