import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ClinicalFormKind,
  ClinicalFormTemplate,
  DietPrescriptionCatalog,
  DietPrescriptionDraft,
  DietPrescriptionResponse,
  MedicalAdmission,
  MedicalWorkspace,
  MonitoringPrescriptionCatalog,
  MonitoringPrescriptionDraft,
  MonitoringPrescriptionResponse,
  NursingCarePrescriptionCatalog,
  NursingCarePrescriptionDraft,
  NursingCarePrescriptionResponse,
  VentilatorySupportCatalog,
  VentilatorySupportDraft,
  VentilatorySupportResponse,
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

  dietCatalog(): Promise<DietPrescriptionCatalog> {
    return firstValueFrom(this.http.get<DietPrescriptionCatalog>(`${this.baseUrl}/diets/catalog`));
  }

  previewDiet(draft: DietPrescriptionDraft): Promise<DietPrescriptionResponse> {
    return firstValueFrom(
      this.http.post<DietPrescriptionResponse>(`${this.baseUrl}/diets/preview`, draft),
    );
  }

  nursingCareCatalog(): Promise<NursingCarePrescriptionCatalog> {
    return firstValueFrom(
      this.http.get<NursingCarePrescriptionCatalog>(`${this.baseUrl}/nursing-care/catalog`),
    );
  }

  previewNursingCare(
    draft: NursingCarePrescriptionDraft,
  ): Promise<NursingCarePrescriptionResponse> {
    return firstValueFrom(
      this.http.post<NursingCarePrescriptionResponse>(
        `${this.baseUrl}/nursing-care/preview`,
        draft,
      ),
    );
  }

  monitoringCatalog(): Promise<MonitoringPrescriptionCatalog> {
    return firstValueFrom(
      this.http.get<MonitoringPrescriptionCatalog>(`${this.baseUrl}/monitoring/catalog`),
    );
  }

  previewMonitoring(draft: MonitoringPrescriptionDraft): Promise<MonitoringPrescriptionResponse> {
    return firstValueFrom(
      this.http.post<MonitoringPrescriptionResponse>(`${this.baseUrl}/monitoring/preview`, draft),
    );
  }

  ventilatorySupportCatalog(): Promise<VentilatorySupportCatalog> {
    return firstValueFrom(
      this.http.get<VentilatorySupportCatalog>(`${this.baseUrl}/ventilatory-support/catalog`),
    );
  }

  previewVentilatorySupport(draft: VentilatorySupportDraft): Promise<VentilatorySupportResponse> {
    return firstValueFrom(
      this.http.post<VentilatorySupportResponse>(
        `${this.baseUrl}/ventilatory-support/preview`,
        draft,
      ),
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
