import { computed, Injectable, signal } from '@angular/core';
import { INITIAL_HOSPITALS } from './mock-data';
import {
  Bed,
  DischargeReason,
  Hospital,
  MedicationSectionDraft,
  Prescription,
  ProvisionalAdmission,
  PrescriptionDraftRow,
  Room,
  VitalSignDraftRow,
} from './models';

interface PatientFormData {
  name: string;
  birthDate?: string;
  cns?: string;
  motherName?: string;
  sex?: string;
  weightKg?: number;
  diagnosis?: string;
  comorbidities?: string;
  allergies?: string;
}

@Injectable({ providedIn: 'root' })
export class DemoStore {
  private readonly hospitalsState = signal<Hospital[]>(this.loadHospitals());
  readonly hospitals = this.hospitalsState.asReadonly();
  readonly activeHospitalId = signal<string | null>(null);
  readonly activeHospital = computed(() =>
    this.hospitalsState().find((hospital) => hospital.id === this.activeHospitalId()),
  );

  selectHospital(hospitalId: string): void {
    if (this.hospitalsState().some((hospital) => hospital.id === hospitalId)) {
      this.activeHospitalId.set(hospitalId);
    }
  }

  replaceHospital(hospital: Hospital): void {
    this.hospitalsState.update((hospitals) => {
      const remaining = hospitals.filter((item) => item.id !== hospital.id);
      return [...remaining, hospital];
    });
    this.activeHospitalId.set(hospital.id);
  }

  findRoom(roomId: string): Room | undefined {
    return this.activeHospital()?.rooms.find((room) => room.id === roomId);
  }

  findBed(bedId: string): Bed | undefined {
    return this.activeHospital()
      ?.rooms.flatMap((room) => room.beds)
      .find((bed) => bed.id === bedId);
  }

  admitPatient(bedId: string, patient: PatientFormData): void {
    this.updateBed(bedId, (bed) => {
      if (bed.status !== 'AVAILABLE') return bed;
      return {
        ...bed,
        status: 'OCCUPIED',
        lastDischarge: undefined,
        patient: {
          ...patient,
          name: patient.name.toLocaleUpperCase('pt-BR'),
          diagnosis: patient.diagnosis?.toLocaleUpperCase('pt-BR'),
          comorbidities: patient.comorbidities?.toLocaleUpperCase('pt-BR'),
          allergies: patient.allergies?.toLocaleUpperCase('pt-BR'),
          id: crypto.randomUUID(),
          admissionAt: new Date().toISOString(),
          prescriptions: [],
        },
      };
    });
  }

  admitProvisionalPatient(
    careUnitId: string,
    careUnitName: string,
    origin: string | undefined,
    patient: PatientFormData,
  ): ProvisionalAdmission | null {
    const hospitalId = this.activeHospitalId();
    if (!hospitalId) return null;

    const admissionId = crypto.randomUUID();
    const admission: ProvisionalAdmission = {
      id: admissionId,
      admittedAt: new Date().toISOString(),
      careUnitId,
      careUnitName,
      origin,
      patient: {
        ...patient,
        id: crypto.randomUUID(),
        admissionId,
        name: patient.name.toLocaleUpperCase('pt-BR'),
        motherName: patient.motherName?.toLocaleUpperCase('pt-BR'),
        diagnosis: patient.diagnosis?.toLocaleUpperCase('pt-BR'),
        comorbidities: patient.comorbidities?.toLocaleUpperCase('pt-BR'),
        allergies: patient.allergies?.toLocaleUpperCase('pt-BR'),
        admissionAt: new Date().toISOString(),
        prescriptions: [],
      },
    };
    admission.patient.admissionAt = admission.admittedAt;
    this.hospitalsState.update((hospitals) =>
      hospitals.map((hospital) =>
        hospital.id === hospitalId
          ? {
              ...hospital,
              provisionalAdmissions: [...(hospital.provisionalAdmissions ?? []), admission],
            }
          : hospital,
      ),
    );
    return admission;
  }

  findProvisionalAdmission(admissionId: string): ProvisionalAdmission | undefined {
    return this.activeHospital()?.provisionalAdmissions?.find(
      (admission) => admission.id === admissionId,
    );
  }

  updateProvisionalPatient(admissionId: string, patient: PatientFormData): void {
    this.updateProvisionalAdmission(admissionId, (admission) => ({
      ...admission,
      patient: {
        ...admission.patient,
        ...patient,
        name: patient.name.toLocaleUpperCase('pt-BR'),
        motherName: patient.motherName?.toLocaleUpperCase('pt-BR'),
        diagnosis: patient.diagnosis?.toLocaleUpperCase('pt-BR'),
        comorbidities: patient.comorbidities?.toLocaleUpperCase('pt-BR'),
        allergies: patient.allergies?.toLocaleUpperCase('pt-BR'),
      },
    }));
  }

  allocateProvisionalPatient(admissionId: string, targetBedId: string): boolean {
    const hospitalId = this.activeHospitalId();
    if (!hospitalId) return false;
    let allocated = false;
    this.hospitalsState.update((hospitals) =>
      hospitals.map((hospital) => {
        if (hospital.id !== hospitalId) return hospital;
        const admission = hospital.provisionalAdmissions?.find((item) => item.id === admissionId);
        const targetBed = hospital.rooms
          .flatMap((room) => room.beds)
          .find((bed) => bed.id === targetBedId);
        if (!admission || targetBed?.status !== 'AVAILABLE') return hospital;
        allocated = true;
        return {
          ...hospital,
          provisionalAdmissions: (hospital.provisionalAdmissions ?? []).filter(
            (item) => item.id !== admissionId,
          ),
          rooms: hospital.rooms.map((room) => ({
            ...room,
            beds: room.beds.map((bed) =>
              bed.id === targetBedId
                ? {
                    ...bed,
                    status: 'OCCUPIED' as const,
                    patient: admission.patient,
                    lastDischarge: undefined,
                  }
                : bed,
            ),
          })),
        };
      }),
    );
    return allocated;
  }

  dischargeProvisionalPatient(admissionId: string): string | null {
    const hospitalId = this.activeHospitalId();
    if (!hospitalId) return null;
    let patientName: string | null = null;
    this.hospitalsState.update((hospitals) =>
      hospitals.map((hospital) => {
        if (hospital.id !== hospitalId) return hospital;
        const admission = hospital.provisionalAdmissions?.find((item) => item.id === admissionId);
        if (!admission) return hospital;
        patientName = admission.patient.name;
        return {
          ...hospital,
          provisionalAdmissions: (hospital.provisionalAdmissions ?? []).filter(
            (item) => item.id !== admissionId,
          ),
        };
      }),
    );
    return patientName;
  }

  dischargePatient(bedId: string, reason: DischargeReason): string | null {
    let dischargedPatientName: string | null = null;
    this.updateBed(bedId, (bed) => {
      if (bed.status !== 'OCCUPIED' || !bed.patient) return bed;
      dischargedPatientName = bed.patient.name;
      return {
        ...bed,
        status: 'AVAILABLE',
        patient: undefined,
        lastDischarge: {
          patientName: bed.patient.name,
          reason,
          dischargedAt: new Date().toISOString(),
        },
      };
    });
    return dischargedPatientName;
  }

  transferPatient(sourceBedId: string, targetBedId: string): boolean {
    const activeHospitalId = this.activeHospitalId();
    if (!activeHospitalId || sourceBedId === targetBedId) return false;

    let transferred = false;
    this.hospitalsState.update((hospitals) => {
      const activeHospital = hospitals.find((hospital) => hospital.id === activeHospitalId);
      const beds = activeHospital?.rooms.flatMap((room) => room.beds) ?? [];
      const sourceBed = beds.find((bed) => bed.id === sourceBedId);
      const targetBed = beds.find((bed) => bed.id === targetBedId);
      if (
        !sourceBed?.patient ||
        sourceBed.status !== 'OCCUPIED' ||
        targetBed?.status !== 'AVAILABLE'
      ) {
        return hospitals;
      }

      const patient = sourceBed.patient;
      const next = hospitals.map((hospital) =>
        hospital.id !== activeHospitalId
          ? hospital
          : {
              ...hospital,
              rooms: hospital.rooms.map((room) => ({
                ...room,
                beds: room.beds.map((bed) => {
                  if (bed.id === sourceBedId) {
                    return { ...bed, status: 'AVAILABLE' as const, patient: undefined };
                  }
                  if (bed.id === targetBedId) {
                    return {
                      ...bed,
                      status: 'OCCUPIED' as const,
                      patient,
                      lastDischarge: undefined,
                    };
                  }
                  return bed;
                }),
              })),
            },
      );
      transferred = true;
      return next;
    });
    return transferred;
  }

  updatePatient(bedId: string, patient: PatientFormData): void {
    this.updateBed(bedId, (bed) => {
      if (!bed.patient) return bed;
      return {
        ...bed,
        patient: {
          ...bed.patient,
          ...patient,
          name: patient.name.toLocaleUpperCase('pt-BR'),
          diagnosis: patient.diagnosis?.toLocaleUpperCase('pt-BR'),
          comorbidities: patient.comorbidities?.toLocaleUpperCase('pt-BR'),
          allergies: patient.allergies?.toLocaleUpperCase('pt-BR'),
        },
      };
    });
  }

  addPrescription(
    bedId: string,
    diet: string,
    observations: string[],
    abnormalities: string[],
    vitalSigns: VitalSignDraftRow[],
    hydrationRows: PrescriptionDraftRow[],
    medicationSections: MedicationSectionDraft[],
  ): void {
    const prescription = this.createPrescription(
      diet,
      observations,
      abnormalities,
      vitalSigns,
      hydrationRows,
      medicationSections,
    );
    this.updateBed(bedId, (bed) => {
      if (!bed.patient) return bed;
      return {
        ...bed,
        patient: {
          ...bed.patient,
          prescriptions: [prescription, ...bed.patient.prescriptions],
        },
      };
    });
  }

  addProvisionalPrescription(
    admissionId: string,
    diet: string,
    observations: string[],
    abnormalities: string[],
    vitalSigns: VitalSignDraftRow[],
    hydrationRows: PrescriptionDraftRow[],
    medicationSections: MedicationSectionDraft[],
  ): void {
    const prescription = this.createPrescription(
      diet,
      observations,
      abnormalities,
      vitalSigns,
      hydrationRows,
      medicationSections,
    );
    this.updateProvisionalAdmission(admissionId, (admission) => ({
      ...admission,
      patient: {
        ...admission.patient,
        prescriptions: [prescription, ...admission.patient.prescriptions],
      },
    }));
  }

  resetDemo(): void {
    const hospitals = structuredClone(INITIAL_HOSPITALS);
    this.hospitalsState.set(hospitals);
  }

  private updateBed(bedId: string, updater: (bed: Bed) => Bed): void {
    const activeHospitalId = this.activeHospitalId();
    if (!activeHospitalId) return;

    this.hospitalsState.update((hospitals) => {
      const next = hospitals.map((hospital) =>
        hospital.id !== activeHospitalId
          ? hospital
          : {
              ...hospital,
              rooms: hospital.rooms.map((room) => ({
                ...room,
                beds: room.beds.map((bed) => (bed.id === bedId ? updater(bed) : bed)),
              })),
            },
      );
      return next;
    });
  }

  private updateProvisionalAdmission(
    admissionId: string,
    updater: (admission: ProvisionalAdmission) => ProvisionalAdmission,
  ): void {
    const hospitalId = this.activeHospitalId();
    if (!hospitalId) return;
    this.hospitalsState.update((hospitals) =>
      hospitals.map((hospital) =>
        hospital.id !== hospitalId
          ? hospital
          : {
              ...hospital,
              provisionalAdmissions: (hospital.provisionalAdmissions ?? []).map((admission) =>
                admission.id === admissionId ? updater(admission) : admission,
              ),
            },
      ),
    );
  }

  private createPrescription(
    diet: string,
    observations: string[],
    abnormalities: string[],
    vitalSigns: VitalSignDraftRow[],
    hydrationRows: PrescriptionDraftRow[],
    medicationSections: MedicationSectionDraft[],
  ): Prescription {
    return {
      id: crypto.randomUUID(),
      createdAt: new Date().toISOString(),
      diet,
      notes: observations.join('\n'),
      observations,
      abnormalities,
      vitalSigns: vitalSigns.map((row) => ({
        id: crypto.randomUUID(),
        description: row.description,
        frequency: row.frequency,
        guidance: row.guidance,
      })),
      hydrationItems: hydrationRows.map((row) => ({
        id: crypto.randomUUID(),
        medication: row.description,
        dose: '',
        route: row.route,
        frequency: row.frequency,
        scheduling: row.scheduling,
      })),
      medicationSections: medicationSections.map((section) => ({
        id: section.id,
        items: section.items.map((row) => ({
          id: crypto.randomUUID(),
          medication: row.description,
          dose: '',
          route: row.route,
          frequency: row.frequency,
          scheduling: row.scheduling,
        })),
      })),
      items: [],
    };
  }

  private loadHospitals(): Hospital[] {
    return structuredClone(INITIAL_HOSPITALS);
  }
}
