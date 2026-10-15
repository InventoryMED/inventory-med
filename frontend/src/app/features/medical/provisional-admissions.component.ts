import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { ProvisionalAdmission } from '../../models';

@Component({
  selector: 'app-provisional-admissions',
  imports: [CommonModule],
  templateUrl: './provisional-admissions.component.html',
  styleUrl: './provisional-admissions.component.scss',
})
export class ProvisionalAdmissionsComponent {
  readonly admissions = input.required<ProvisionalAdmission[]>();
  readonly medicalActionsEnabled = input(true);
  readonly searchTerm = signal('');
  readonly selectedAdmissionId = signal<string | null>(null);
  readonly visibleAdmissions = computed(() => {
    const normalizedSearch = this.searchTerm().trim().toLocaleUpperCase('pt-BR');
    if (!normalizedSearch) return this.admissions();
    return this.admissions().filter((admission) =>
      [admission.patient.name, admission.careUnitName, admission.origin ?? ''].some((value) =>
        value.toLocaleUpperCase('pt-BR').includes(normalizedSearch),
      ),
    );
  });
  readonly admit = output<void>();
  readonly edit = output<ProvisionalAdmission>();
  readonly prescription = output<ProvisionalAdmission>();
  readonly evolution = output<ProvisionalAdmission>();
  readonly procedures = output<ProvisionalAdmission>();
  readonly exams = output<ProvisionalAdmission>();
  readonly aih = output<ProvisionalAdmission>();
  readonly clinicalSummary = output<ProvisionalAdmission>();
  readonly medicalRecipe = output<ProvisionalAdmission>();
  readonly referral = output<ProvisionalAdmission>();
  readonly allocate = output<ProvisionalAdmission>();
  readonly discharge = output<ProvisionalAdmission>();

  toggleAdmission(admissionId: string): void {
    this.selectedAdmissionId.update((current) => (current === admissionId ? null : admissionId));
  }

  patientInitials(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0))
      .join('')
      .toLocaleUpperCase('pt-BR');
  }
}
