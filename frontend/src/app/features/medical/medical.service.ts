import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BedsideProcedureCatalog,
  BedsideProcedureDraft,
  BedsideProcedureResponse,
  ClinicalDocument,
  ClinicalFormKind,
  ClinicalFormTemplate,
  CriticalCareCatalog,
  CriticalCareDraft,
  CriticalCareResponse,
  DietPrescriptionCatalog,
  DietPrescriptionDraft,
  DietPrescriptionResponse,
  IsolationPrecautionCatalog,
  IsolationPrecautionDraft,
  IsolationPrecautionResponse,
  MedicalAdmission,
  MedicalWorkspace,
  MedicationTherapyCatalog,
  MedicationTherapyDraft,
  MedicationTherapyResponse,
  MonitoringPrescriptionCatalog,
  MonitoringPrescriptionDraft,
  MonitoringPrescriptionResponse,
  NursingCarePrescriptionCatalog,
  NursingCarePrescriptionDraft,
  NursingCarePrescriptionResponse,
  PrescriptionStarterCatalog,
  RehabilitationCatalog,
  RehabilitationDraft,
  RehabilitationResponse,
  TherapeuticSupportCatalog,
  TherapeuticSupportDraft,
  TherapeuticSupportResponse,
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

  prescriptionStartOptions(): Promise<PrescriptionStarterCatalog> {
    return firstValueFrom(
      this.http.get<PrescriptionStarterCatalog>(`${this.baseUrl}/prescriptions/start-options`),
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

  rehabilitationCatalog(): Promise<RehabilitationCatalog> {
    return firstValueFrom(
      this.http.get<RehabilitationCatalog>(`${this.baseUrl}/rehabilitation/catalog`),
    );
  }

  previewRehabilitation(draft: RehabilitationDraft): Promise<RehabilitationResponse> {
    return firstValueFrom(
      this.http.post<RehabilitationResponse>(`${this.baseUrl}/rehabilitation/preview`, draft),
    );
  }

  isolationPrecautionCatalog(): Promise<IsolationPrecautionCatalog> {
    return firstValueFrom(
      this.http.get<IsolationPrecautionCatalog>(`${this.baseUrl}/isolation-precautions/catalog`),
    );
  }

  previewIsolationPrecautions(
    draft: IsolationPrecautionDraft,
  ): Promise<IsolationPrecautionResponse> {
    return firstValueFrom(
      this.http.post<IsolationPrecautionResponse>(
        `${this.baseUrl}/isolation-precautions/preview`,
        draft,
      ),
    );
  }

  therapeuticSupportCatalog(): Promise<TherapeuticSupportCatalog> {
    return firstValueFrom(
      this.http.get<TherapeuticSupportCatalog>(`${this.baseUrl}/therapeutic-support/catalog`),
    );
  }

  previewTherapeuticSupport(draft: TherapeuticSupportDraft): Promise<TherapeuticSupportResponse> {
    return firstValueFrom(
      this.http.post<TherapeuticSupportResponse>(
        `${this.baseUrl}/therapeutic-support/preview`,
        draft,
      ),
    );
  }

  criticalCareCatalog(): Promise<CriticalCareCatalog> {
    return firstValueFrom(
      this.http.get<CriticalCareCatalog>(`${this.baseUrl}/critical-care/catalog`),
    );
  }

  previewCriticalCare(draft: CriticalCareDraft): Promise<CriticalCareResponse> {
    return firstValueFrom(
      this.http.post<CriticalCareResponse>(`${this.baseUrl}/critical-care/preview`, draft),
    );
  }

  medicationTherapyCatalog(): Promise<MedicationTherapyCatalog> {
    return firstValueFrom(
      this.http.get<MedicationTherapyCatalog>(`${this.baseUrl}/medication-therapy/catalog`),
    );
  }

  previewMedicationTherapy(draft: MedicationTherapyDraft): Promise<MedicationTherapyResponse> {
    return firstValueFrom(
      this.http.post<MedicationTherapyResponse>(
        `${this.baseUrl}/medication-therapy/preview`,
        draft,
      ),
    );
  }

  bedsideProcedureCatalog(): Promise<BedsideProcedureCatalog> {
    return firstValueFrom(
      this.http.get<BedsideProcedureCatalog>(`${this.baseUrl}/procedures/catalog`),
    );
  }

  previewBedsideProcedure(draft: BedsideProcedureDraft): Promise<BedsideProcedureResponse> {
    return firstValueFrom(
      this.http.post<BedsideProcedureResponse>(`${this.baseUrl}/procedures/preview`, draft),
    );
  }

  documents(admissionId: string): Promise<ClinicalDocument[]> {
    return firstValueFrom(
      this.http.get<ClinicalDocument[]>(`${this.baseUrl}/admissions/${admissionId}/documents`),
    );
  }

  createDocument(
    admissionId: string,
    template: ClinicalFormTemplate,
    values: Record<string, unknown>,
    finalizeDocument: boolean,
  ): Promise<ClinicalDocument> {
    return firstValueFrom(
      this.http.post<ClinicalDocument>(`${this.baseUrl}/admissions/${admissionId}/documents`, {
        templateVersionId: template.versionId,
        kind: template.kind,
        values,
        finalizeDocument,
      }),
    );
  }
}
