import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdministrationHospital,
  AdministrationUser,
  HospitalCreatePayload,
  UserCreatePayload,
  AdministrationStructure,
  AdministrationCareUnit,
  AdministrationRoom,
  AdministrationBed,
} from './administration.models';
import { ClinicalFormKind, ClinicalFormTemplate } from '../medical/medical.models';

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

  structure(hospitalId: string): Promise<AdministrationStructure> {
    return firstValueFrom(
      this.http.get<AdministrationStructure>(`${this.baseUrl}/hospitals/${hospitalId}/structure`),
    );
  }

  createCareUnit(hospitalId: string, payload: object): Promise<AdministrationCareUnit> {
    return firstValueFrom(
      this.http.post<AdministrationCareUnit>(
        `${this.baseUrl}/hospitals/${hospitalId}/structure/care-units`,
        payload,
      ),
    );
  }

  updateCareUnit(
    hospitalId: string,
    careUnitId: string,
    payload: object,
  ): Promise<AdministrationCareUnit> {
    return firstValueFrom(
      this.http.put<AdministrationCareUnit>(
        `${this.baseUrl}/hospitals/${hospitalId}/structure/care-units/${careUnitId}`,
        payload,
      ),
    );
  }

  createRoom(hospitalId: string, payload: object): Promise<AdministrationRoom> {
    return firstValueFrom(
      this.http.post<AdministrationRoom>(
        `${this.baseUrl}/hospitals/${hospitalId}/structure/rooms`,
        payload,
      ),
    );
  }

  updateRoom(hospitalId: string, roomId: string, payload: object): Promise<AdministrationRoom> {
    return firstValueFrom(
      this.http.put<AdministrationRoom>(
        `${this.baseUrl}/hospitals/${hospitalId}/structure/rooms/${roomId}`,
        payload,
      ),
    );
  }

  createBed(hospitalId: string, payload: object): Promise<AdministrationBed> {
    return firstValueFrom(
      this.http.post<AdministrationBed>(
        `${this.baseUrl}/hospitals/${hospitalId}/structure/beds`,
        payload,
      ),
    );
  }

  updateBed(hospitalId: string, bedId: string, payload: object): Promise<AdministrationBed> {
    return firstValueFrom(
      this.http.put<AdministrationBed>(
        `${this.baseUrl}/hospitals/${hospitalId}/structure/beds/${bedId}`,
        payload,
      ),
    );
  }

  formTemplates(hospitalId: string): Promise<ClinicalFormTemplate[]> {
    return firstValueFrom(
      this.http.get<ClinicalFormTemplate[]>(
        `${this.baseUrl}/hospitals/${hospitalId}/form-templates`,
      ),
    );
  }

  createFormTemplate(
    hospitalId: string,
    payload: { name: string; kind: ClinicalFormKind },
  ): Promise<ClinicalFormTemplate> {
    return firstValueFrom(
      this.http.post<ClinicalFormTemplate>(
        `${this.baseUrl}/hospitals/${hospitalId}/form-templates`,
        payload,
      ),
    );
  }

  saveFormTemplateDraft(
    hospitalId: string,
    templateId: string,
    payload: object,
  ): Promise<ClinicalFormTemplate> {
    return firstValueFrom(
      this.http.put<ClinicalFormTemplate>(
        `${this.baseUrl}/hospitals/${hospitalId}/form-templates/${templateId}/draft`,
        payload,
      ),
    );
  }

  publishFormTemplate(hospitalId: string, templateId: string): Promise<ClinicalFormTemplate> {
    return firstValueFrom(
      this.http.post<ClinicalFormTemplate>(
        `${this.baseUrl}/hospitals/${hospitalId}/form-templates/${templateId}/publish`,
        null,
      ),
    );
  }

  createFormTemplateVersion(hospitalId: string, templateId: string): Promise<ClinicalFormTemplate> {
    return firstValueFrom(
      this.http.post<ClinicalFormTemplate>(
        `${this.baseUrl}/hospitals/${hospitalId}/form-templates/${templateId}/versions`,
        null,
      ),
    );
  }

  setFormTemplateActive(
    hospitalId: string,
    templateId: string,
    active: boolean,
  ): Promise<ClinicalFormTemplate> {
    return firstValueFrom(
      this.http.patch<ClinicalFormTemplate>(
        `${this.baseUrl}/hospitals/${hospitalId}/form-templates/${templateId}/status`,
        { active },
      ),
    );
  }
}
