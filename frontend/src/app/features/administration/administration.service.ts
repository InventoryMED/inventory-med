import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdministrationHospital,
  AdministrationUser,
  HospitalCreatePayload,
  UserCreatePayload,
} from './administration.models';

@Injectable({ providedIn: 'root' })
export class AdministrationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/administration`;

  listHospitals(): Promise<AdministrationHospital[]> {
    return firstValueFrom(this.http.get<AdministrationHospital[]>(`${this.baseUrl}/hospitals`));
  }

  createHospital(payload: HospitalCreatePayload): Promise<AdministrationHospital> {
    return firstValueFrom(
      this.http.post<AdministrationHospital>(`${this.baseUrl}/hospitals`, payload),
    );
  }

  retryHospitalProvisioning(hospitalId: string): Promise<AdministrationHospital> {
    return firstValueFrom(
      this.http.post<AdministrationHospital>(
        `${this.baseUrl}/hospitals/${hospitalId}/provision`,
        null,
      ),
    );
  }

  listUsers(): Promise<AdministrationUser[]> {
    return firstValueFrom(this.http.get<AdministrationUser[]>(`${this.baseUrl}/users`));
  }

  createUser(payload: UserCreatePayload): Promise<AdministrationUser> {
    return firstValueFrom(this.http.post<AdministrationUser>(`${this.baseUrl}/users`, payload));
  }

  setUserActive(userId: string, active: boolean): Promise<AdministrationUser> {
    return firstValueFrom(
      this.http.patch<AdministrationUser>(`${this.baseUrl}/users/${userId}/status`, { active }),
    );
  }

  setHospitalAccessActive(
    userId: string,
    hospitalId: string,
    active: boolean,
  ): Promise<AdministrationUser> {
    return firstValueFrom(
      this.http.patch<AdministrationUser>(
        `${this.baseUrl}/users/${userId}/hospitals/${hospitalId}/status`,
        { active },
      ),
    );
  }
}
