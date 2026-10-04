import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ClinicalFormKind,
  ClinicalFormTemplate,
  MedicalAdmission,
  MedicalWorkspace,
} from './medical.models';

@Injectable({ providedIn: 'root' })
export class MedicalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/clinical`;

  workspace(): Promise<MedicalWorkspace> {
    return firstValueFrom(this.http.get<MedicalWorkspace>(`${this.baseUrl}/workspace`));
  }

  admit(payload: Record<string, unknown>): Promise<MedicalAdmission> {
    return firstValueFrom(this.http.post<MedicalAdmission>(`${this.baseUrl}/admissions`, payload));
  }

  transfer(admissionId: string, targetBedId: string): Promise<MedicalAdmission> {
    return firstValueFrom(
      this.http.post<MedicalAdmission>(`${this.baseUrl}/admissions/${admissionId}/transfer`, {
        targetBedId,
      }),
    );
  }

  updatePatient(admissionId: string, payload: Record<string, unknown>): Promise<MedicalAdmission> {
    return firstValueFrom(
      this.http.put<MedicalAdmission>(`${this.baseUrl}/admissions/${admissionId}/patient`, payload),
    );
  }

  discharge(admissionId: string, reason: string): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(`${this.baseUrl}/admissions/${admissionId}/discharge`, { reason }),
    );
  }

  templates(kind: ClinicalFormKind): Promise<ClinicalFormTemplate[]> {
    return firstValueFrom(
      this.http.get<ClinicalFormTemplate[]>(`${this.baseUrl}/form-templates`, {
        params: { kind },
      }),
    );
  }

  createDocument(
    admissionId: string,
    template: ClinicalFormTemplate,
    values: Record<string, unknown>,
    finalizeDocument: boolean,
  ): Promise<unknown> {
    return firstValueFrom(
      this.http.post(`${this.baseUrl}/admissions/${admissionId}/documents`, {
        templateVersionId: template.versionId,
        kind: template.kind,
        values,
        finalizeDocument,
      }),
    );
  }
}
