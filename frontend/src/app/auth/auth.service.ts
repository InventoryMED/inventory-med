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

  readonly user = this.session.user;
  readonly hospitals = this.session.hospitals;
  readonly selectedHospitalId = this.session.selectedHospitalId;
  readonly selectedHospital = this.session.selectedHospital;
  readonly isAuthenticated = this.session.isAuthenticated;

  async login(email: string, password: string): Promise<LoginResponse> {
    this.session.clear();
    await this.ensureCsrfCookie();
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
    await this.ensureCsrfCookie();
    const response = await firstValueFrom(
      this.http.post<HospitalSelectionResponse>(`${environment.apiBaseUrl}/auth/select-hospital`, {
        hospitalId,
      }),
    );
    this.session.applyHospitalSelection(response);
    return response;
  }

  async validateSession(): Promise<boolean> {
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

  async logout(): Promise<void> {
    if (!environment.useRealApi) {
      this.session.clear();
      return;
    }

    try {
      await this.ensureCsrfCookie();
      await firstValueFrom(this.http.post<void>(`${environment.apiBaseUrl}/auth/logout`, null));
    } catch {
      // O estado local deve ser encerrado mesmo se a sessão já tiver expirado.
    } finally {
      this.session.clear();
    }
  }

  private async ensureCsrfCookie(): Promise<void> {
    await firstValueFrom(this.http.get(`${environment.apiBaseUrl}/auth/csrf`));
  }
}
