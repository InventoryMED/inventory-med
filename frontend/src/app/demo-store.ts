import { computed, Injectable, signal } from '@angular/core';
import { INITIAL_HOSPITALS } from './mock-data';
import { Bed, Hospital, PrescriptionDraftRow, Room } from './models';

const STORAGE_KEY = 'inventory-med-demo-v3';

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

  findRoom(roomId: string): Room | undefined {
    return this.activeHospital()?.rooms.find((room) => room.id === roomId);
  }

  findBed(bedId: string): Bed | undefined {
    return this.activeHospital()
      ?.rooms.flatMap((room) => room.beds)
      .find((bed) => bed.id === bedId);
  }

  admitPatient(
    bedId: string,
    patient: { name: string; birthDate: string; document: string; sex?: string; weightKg: number },
  ): void {
    this.updateBed(bedId, (bed) => {
      if (bed.status !== 'AVAILABLE') return bed;
      return {
        ...bed,
        status: 'OCCUPIED',
        patient: {
          ...patient,
          id: crypto.randomUUID(),
          admissionAt: new Date().toISOString(),
          prescriptions: [],
        },
      };
    });
  }

  addPrescription(bedId: string, rows: PrescriptionDraftRow[], notes: string): void {
    this.updateBed(bedId, (bed) => {
      if (!bed.patient) return bed;
      return {
        ...bed,
        patient: {
          ...bed.patient,
          prescriptions: [
            {
              id: crypto.randomUUID(),
              createdAt: new Date().toISOString(),
              notes,
              items: rows.map((row) => ({
                id: crypto.randomUUID(),
                medication: row.description,
                dose: '',
                route: row.route,
                frequency: row.hours.length
                  ? `${row.frequency} • ${row.hours.map((hour) => `${String(hour).padStart(2, '0')}h`).join(', ')}`
                  : row.frequency,
              })),
            },
            ...bed.patient.prescriptions,
          ],
        },
      };
    });
  }

  resetDemo(): void {
    const hospitals = structuredClone(INITIAL_HOSPITALS);
    this.hospitalsState.set(hospitals);
    this.persist(hospitals);
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
      this.persist(next);
      return next;
    });
  }

  private loadHospitals(): Hospital[] {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? (JSON.parse(saved) as Hospital[]) : structuredClone(INITIAL_HOSPITALS);
    } catch {
      return structuredClone(INITIAL_HOSPITALS);
    }
  }

  private persist(hospitals: Hospital[]): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(hospitals));
  }
}
